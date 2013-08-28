package com.geodan.solr.dataimporthandler;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.solr.handler.dataimport.Context;
import org.apache.solr.handler.dataimport.ContextImpl;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.geodan.util.WFSUtils;
import com.google.mockwebserver.MockResponse;
import com.google.mockwebserver.MockWebServer;

public class WFSDataSourceTest extends WFSDataSource {

	private static MockWebServer mockServer;

	private WFSDataSource classUnderTest = new WFSDataSource();

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		mockServer = new MockWebServer();
		mockServer.play();
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		mockServer.shutdown();
	}

	@Test
	public void testGetDataString() throws InterruptedException {
		// Define WFS mock responses
		mockServer.enqueue(new MockResponse().clearHeaders().setBody(
				createStubGetCapabilitiesResponse()));
		mockServer.enqueue(new MockResponse().clearHeaders().setBody(
				createStubGetFeatureResponse()));

		classUnderTest.init(createStubContext(), createStubProperties());
		Iterator<Map<String, Object>> result = classUnderTest
				.getData("bestuurlijkegrenzen:provincies_2012");

		assertThat("Result should not be NULL.", result, notNullValue());

		Map<String, Object> transformedFeature = result.next();
		assertThat("Result should contain at least 1 feature.",
				transformedFeature, notNullValue());
		assertThat("Feature should contain 3 properties", transformedFeature
				.entrySet().size(), is(3));
	}

	@Ignore
	public void testClose() {
		fail("Not yet implemented: no need to test.");
	}

	private Context createStubContext() {
		ContextImpl context = new ContextImpl(null, null, classUnderTest, null,
				null, null, null);

		return context;
	}

	private Properties createStubProperties() {
		Properties properties = new Properties();
		// properties.put("capabilitiesUrl",
		// " http://localhost:" + mockServer.getPort()
		// + "/geoserver/wfs?request=GetCapabilities");
		properties
				.put("capabilitiesUrl",
						"http://geodata.nationaalgeoregister.nl/bestuurlijkegrenzen/wfs?request=GetCapabilities&version=1.0.0");

		return properties;
	}

	/**
	 * <p>
	 * Returns a valid WFS response (from a GeoServer 1.1.1 WFS)
	 * 
	 * @see {@code http://geodata.nationaalgeoregister.nl/bestuurlijkegrenzen/wfs?request=GetCapabilities}
	 * @return
	 */
	private String createStubGetCapabilitiesResponse() {
		return WFSUtils
				.loadResponseFromClasspath("/wfs_response_capabilities.xml");
	}

	/**
	 * <p>
	 * Returns a valid WFS response (from a GeoServer 1.1.0 WFS)
	 * 
	 * @see {@code http://geodata.nationaalgeoregister.nl/bestuurlijkegrenzen/wfs?request=GetFeatures&typeName=provincies_2012}
	 * @return
	 */
	private String createStubGetFeatureResponse() {
		return WFSUtils.loadResponseFromClasspath("/wfs_response_features.xml");
	}

}
