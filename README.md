solr-dataimporthandler-wfs
==========================

A Solr plugin for the DataImportHandler (DIH) that allows indexing from WFS services. 
This Solr provides a new DataSource (of type com.geodan.solr.dataimporthandler.WFSDataSource) to 
Solr's DataImportHandler. The DataSource makes it possible to retrieve entities from a WFS service.
Geometries are retrieved in the WKT format and the requested SRS (default is EPSG:4326).

Configuration example
---------------------
It is assumed that you have a working DIH configuration in your Solr instance and a spatial field in your schema (using [JTS](http://wiki.apache.org/solr/SolrAdaptersForLuceneSpatial4)).
In the file data-config.xml add a DataSource (i.e. a connection to a WFS service using its Capabilities URL):

```xml
	<dataSource name="bestuurlijkegrenzen" type="com.geodan.solr.dataimporthandler.WFSDataSource" encoding="UTF-8" 
		capabilitiesUrl="http://geodata.nationaalgeoregister.nl/bestuurlijkegrenzen/wfs?request=GetCapabilities&amp;version=1.0.0&amp;srsName=EPSG:4326" 
		simplifyGeometry="1000" />
```

* name can be any text (it is advised not to use spaces) and encoding can be any type (see the [DIH wiki](http://wiki.apache.org/solr/DataImportHandler) for details)
* the type should be as specified
* the capabilitiesUrl should point to a valid Capabilities document of a WFS 1.0.0 or 1.1.0 service (WFS 2.0 is NOT supported yet)
* simplifyGeometry should be used to achieve good performance (it lowers the resolution of the geometry to the specified number of meters)

In the mapping you can following the same notation as for JDBC DataSources:

```xml
		<entity name="provincies" pk="id" dataSource="bestuurlijkegrenzen" query="bestuurlijkegrenzen:provincies_2012" transformer="TemplateTransformer">
			<field name="id" column="fid"/>
			<field name="name" column="provincienaam" />
			<field name="geom" column="geom" />
			<field name="cat" column="cat" template="province" />
			<field name="wfsurl" column="wfsurl" template="http://geodata.nationaalgeoregister.nl/bestuurlijkegrenzen/wfs?request=GetFeature&amp;service=WFS&amp;version=1.0.0&amp;typeName=bestuurlijkegrenzen:provincies_2012&amp;featureID=${provincies.fid}" />
		</entity>
```

* point to the right DataSource with the attribute *dataSource*
* use the *query* attribute to specify the typeName of the WFS service
* use a *transformer* to specify static values (such as the URL of the original feature, see the example above)
* two column from the WFSDataSource have a special meaning (and fixed key name):
  * *fid*: the featureId
  * *geom*: the default geometry

Deployment
----------
Use Maven3 to build the JAR file and its dependencies:
```
mvn package -Dmaven.test.skip=true
```
Copy the artifacts from the target directory and the target/dependency-jars directory to the lib directory of Solr. 
Restart Solr and the WFS plugin for DIH should be available.
