package com.jaspersoft.jasperserver.jaxrs.client.actual;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.ws.rs.core.Cookie;

import com.jaspersoft.jasperserver.jaxrs.client.apiadapters.reporting.ReportOutputFormat;
import com.jaspersoft.jasperserver.jaxrs.client.core.JasperserverRestClient;
import com.jaspersoft.jasperserver.jaxrs.client.core.RestClientConfiguration;
import com.jaspersoft.jasperserver.jaxrs.client.core.Session;
import com.jaspersoft.jasperserver.jaxrs.client.core.operationresult.OperationResult;

import org.junit.*;

/**
 * Unit test for Jaspersoft REST API client.
 */
public class RestApiTest {
	
   public RestApiTest() {}

    /**
     * Login and do several operations.
     * Want to make sure that all cookies are tracked, so that cookies from proxies and load balancers
     * are preserved.
     */
    @Test
    public void testSessionAndCookies() {
        
        Properties connection = new Properties();
        try (final InputStream stream =
                this.getClass().getResourceAsStream("/connection.properties")) {
        	connection.load(stream);
        } catch (IOException e) {
			fail("connection properties load fail: " + e.getMessage());
		}
        
    	RestClientConfiguration config = RestClientConfiguration.loadConfiguration(connection);
    	
        assertNotNull( config.getJasperReportsServerUrl() );
        
        JasperserverRestClient client = new JasperserverRestClient(config);
        
        Properties credentials = new Properties();
        try (final InputStream stream =
                this.getClass().getResourceAsStream("/credentials.properties")) {
        	credentials.load(stream);
        } catch (IOException e) {
			fail("credentials properties load fail: " + e.getMessage());
		}
        
        Session session = client.authenticate(credentials.getProperty("username"), credentials.getProperty("password"));
        
        String uniqueCookieValue = null;
        assertNotNull(session.getStorage().getSessionId());
        
        /*
         * If a proxy/load balancer injects additional cookies into responses,
         * like the AWS Application Load Balancer adding an "AWSALB" cookie for sticky sessions,
         * you can test for it by pointing the connection URL at the proxy and
         * adding the following into the connection.properties:
         * 
         * expectedCookie=<injected cookie name>
         * 
         * Sometimes these injected cookies are different for every request, so you
         * want to test that they are changing. To do so, add:
         * 
         * uniqueCookieValue=true
         */
        if (connection.get("expectedCookie") != null) {
        	Cookie expectedCookie = session.getStorage().getCookies().get(connection.get("expectedCookie"));
        	assertNotNull(expectedCookie);
            if (connection.get("uniqueCookieValue") != null) {
            	uniqueCookieValue = expectedCookie.getValue();
            }
        }
        
        OperationResult<InputStream> result = session
                .reportingService()
                .report("/public/Samples/Reports/9.CustomerDetailReport")
                .prepareForRun(ReportOutputFormat.HTML, 1)
                .run();
        InputStream report = result.getEntity();
        
        if (connection.get("expectedCookie") != null) {
        	Cookie expectedCookie = session.getStorage().getCookies().get(connection.get("expectedCookie"));
        	assertNotNull(expectedCookie);
            if (connection.get("uniqueCookieValue") != null) {
            	assertFalse(uniqueCookieValue.equals(expectedCookie.getValue()));
            }
        }
        
        session.logout();
       
    }
}
