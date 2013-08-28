/**
 * Solr DataImportHandler for WFS services
 * 
 * Copyright 2013 Jan Boonen (jan.boonen@geodan.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */
package com.geodan.solr.dataimporthandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataSource;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.opengis.feature.GeometryAttribute;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.ReferenceIdentifier;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.simplify.DouglasPeuckerSimplifier;

public class WFSDataSource extends DataSource<Iterator<Map<String, Object>>> {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(WFSDataSource.class);

	private static final String CAPABILITIES_URL = "capabilitiesUrl";

	private static final String EPSG = "epsg";

	private static final String SIMPLIFY_GEOMETRY = "simplifyGeometry";

	private static final String FID_KEY = "fid";

	private static final String SRS_NAMESPACE = "EPSG:";

	private DataStore dataStore;

	private int targetEpsg = 4326;

	private int simplifyGeometry = 0;

	@Override
	public void init(Context context, Properties initProps) {
		// Make these parameters dynamic
		String getCapabilitiesUrl = initProps.getProperty(CAPABILITIES_URL);
		String epsgCode = initProps.getProperty(EPSG);
		String simplifyGeometry = initProps.getProperty(SIMPLIFY_GEOMETRY);
		try {
			this.targetEpsg = Integer.valueOf(epsgCode);
		} catch (RuntimeException e) {
			LOGGER.info(
					"Could not read EPSG code form configuration, using default EPSG ({}).",
					targetEpsg);
		}
		try {
			this.simplifyGeometry = Integer.valueOf(simplifyGeometry);
		} catch (RuntimeException e) {
			LOGGER.info("Could not read simplification value from configuration, no simplification will be applied.");
		}

		// Step 1: define connection parameters
		Map<String, Object> connectionParameters = new HashMap<String, Object>();
		connectionParameters.put("WFSDataStoreFactory:GET_CAPABILITIES_URL",
				getCapabilitiesUrl);

		try {
			// Step 2: establish connection
			dataStore = DataStoreFinder.getDataStore(connectionParameters);
			LOGGER.info("Connection established with URL '{}'.");
		} catch (IOException e1) {
			LOGGER.warn("Could not establish connection to URL '{}'.",
					getCapabilitiesUrl);
		}
	}

	@Override
	public Iterator<Map<String, Object>> getData(String typeName) {
		try {
			// Step 3: discover schema
			SimpleFeatureType schema = dataStore.getSchema(typeName);
			if (schema == null) {
				LOGGER.warn("TypeName '{}' is not available on endpoint.",
						typeName);
			}

			// Step 4: load target
			FeatureSource<SimpleFeatureType, SimpleFeature> source;
			source = dataStore.getFeatureSource(typeName);
			LOGGER.info("Metadata Bounds of type '{}': {}", typeName,
					source.getBounds());
			int sourceEpsg = determineSourceEpsg(source);

			// Step 5: load features
			FeatureCollection<SimpleFeatureType, SimpleFeature> features = source
					.getFeatures();
			LOGGER.info("Return {} features for typeName '{}'.",
					features.size(), typeName);
			return new FeatureCollectionIterator(features, sourceEpsg);
		} catch (IOException e) {
			LOGGER.warn(
					"An exception occured while executing query '{}', cause: {}.",
					typeName, e.getMessage());
			return new FeatureCollectionIterator(
					new DefaultFeatureCollection(), targetEpsg);
		}
	}

	private int determineSourceEpsg(
			FeatureSource<SimpleFeatureType, SimpleFeature> source) {
		if (source != null) {
			try {
				Set<ReferenceIdentifier> identifiers = source.getSchema()
						.getCoordinateReferenceSystem().getIdentifiers();
				if (identifiers.size() > 0) {
					String code = identifiers.iterator().next().getCode();
					return Integer.valueOf(code);
				}

			} catch (RuntimeException e) {
				LOGGER.warn("Could not determine EPSG code from WFS DataSource.");
			}
		}
		return targetEpsg;
	}

	@Override
	public void close() {
		// No action required
	}

	private class FeatureCollectionIterator implements
			Iterator<Map<String, Object>> {

		private MathTransform transform = null;

		private FeatureIterator<SimpleFeature> iter;

		public FeatureCollectionIterator(
				FeatureCollection<SimpleFeatureType, SimpleFeature> features,
				int sourceEpsg) {
			if (features != null) {
				iter = features.features();
				if (sourceEpsg != targetEpsg) {
					try {
						CoordinateReferenceSystem targetCRS = CRS.decode(
								SRS_NAMESPACE + targetEpsg, true);
						CoordinateReferenceSystem sourceCRS = CRS.decode(
								SRS_NAMESPACE + sourceEpsg, true);
						transform = CRS.findMathTransform(sourceCRS, targetCRS,
								true);
					} catch (Exception e) {
						LOGGER.warn("Could not detemine source and target CRS definitions. Geometry will not be indexed!");
					}
				}
			}
		}

		public boolean hasNext() {
			if (iter != null) {
				return iter.hasNext();
			}
			return false;
		}

		public Map<String, Object> next() {
			if (iter != null) {
				return transformFeature(iter.next());
			}
			return null;
		}

		public void remove() {
			throw new IllegalArgumentException("Iterator is readonly!");
		}

		private Map<String, Object> transformFeature(SimpleFeature feature) {
			Map<String, Object> attributes = new HashMap<String, Object>();
			for (Property prop : feature.getProperties()) {
				attributes.put(prop.getName().getLocalPart(), prop.getValue());
			}

			GeometryAttribute geometryProperty = feature
					.getDefaultGeometryProperty();
			if (geometryProperty != null) {
				String wktGeometry = "";
				try {
					if (feature.getDefaultGeometry() != null) {
						Geometry geometry = (Geometry) feature
								.getDefaultGeometry();
						if (simplifyGeometry > 0) {
							DouglasPeuckerSimplifier simplifier = new DouglasPeuckerSimplifier(
									geometry);
							simplifier.setDistanceTolerance(simplifyGeometry);
							geometry = simplifier.getResultGeometry();
						}
						if (transform != null) {
							geometry = JTS.transform(geometry, transform);
						}
						wktGeometry = geometry.toString();
					}
				} catch (Exception e) {
					LOGGER.warn("Could not convert Geometry to WKT String. Geometry will not be indexed!");
				}
				attributes.put(geometryProperty.getName().getLocalPart(),
						wktGeometry);
				attributes.put(FID_KEY, feature.getID());
			}

			return attributes;
		}

	}

}
