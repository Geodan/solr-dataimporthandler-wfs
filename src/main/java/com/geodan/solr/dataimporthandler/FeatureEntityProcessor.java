package com.geodan.solr.dataimporthandler;

import java.util.Iterator;
import java.util.Map;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.DataImportHandlerException;
import org.apache.solr.handler.dataimport.DataSource;
import org.apache.solr.handler.dataimport.EntityProcessorBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FeatureEntityProcessor extends EntityProcessorBase {

	private static final Logger LOGGER = LoggerFactory
			.getLogger(FeatureEntityProcessor.class);

	private static final String TYPE_NAME = "typeName";

	protected DataSource<Iterator<Map<String, Object>>> dataSource;

	@SuppressWarnings("unchecked")
	@Override
	public void init(Context context) {
		super.init(context);

		dataSource = context.getDataSource();
	}

	@Override
	public Map<String, Object> nextRow() {
		if (rowIterator == null) {
			String typeName = getTypeName();
			initWFSDataSource(context.replaceTokens(typeName));
		}
		return getNext();
	}

	public String getTypeName() {
		String queryString = context.getEntityAttribute(TYPE_NAME);

		LOGGER.info("Query WFS for typeName '{}'.", queryString);
		return queryString;
	}

	protected void initWFSDataSource(String typeName) {
		try {
			rowIterator = dataSource.getData(typeName);
			this.query = typeName;
		} catch (DataImportHandlerException e) {
			throw e;
		} catch (Exception e) {
			LOGGER.error(
					"The WFS request for typeName '{}' failed with the following Exception: {}.",
					typeName, e);
			throw new DataImportHandlerException(
					DataImportHandlerException.SEVERE, e);
		}
	}

}
