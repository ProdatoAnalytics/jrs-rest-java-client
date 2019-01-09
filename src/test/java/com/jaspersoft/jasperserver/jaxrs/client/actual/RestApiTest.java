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
import com.jaspersoft.jasperserver.jaxrs.client.dto.reports.AttachmentDescriptor;
import com.jaspersoft.jasperserver.jaxrs.client.dto.reports.ExportDescriptor;
import com.jaspersoft.jasperserver.jaxrs.client.dto.reports.ReportExecutionDescriptor;
import com.jaspersoft.jasperserver.jaxrs.client.dto.reports.ReportExecutionRequest;
import com.jaspersoft.jasperserver.jaxrs.client.dto.reports.ReportExecutionStatusEntity;

import org.junit.*;

/**
 * Unit test for JasperReports Server REST API client.
 */
public class RestApiTest {
	
	// a report in JasperReports Server. Part of the samples
	
	public static String TEST_REPORT_URI = "/public/Samples/Reports/9.CustomerDetailReport";
	
	
	Properties connection, credentials;
	JasperserverRestClient client;
	Session session;
	String reportUri;
	
	
   public RestApiTest() {}
   
   @Before
   public void setup() {
       //System.out.println("setup");
       connection = new Properties();
       try (final InputStream stream =
               this.getClass().getResourceAsStream("/connection.properties")) {
       	connection.load(stream);
       } catch (IOException e) {
			fail("connection properties load fail: " + e.getMessage());
		}
       
   		RestClientConfiguration config = RestClientConfiguration.loadConfiguration(connection);
   	
       assertNotNull( config.getJasperReportsServerUrl() );
       
       client = new JasperserverRestClient(config);
       
       credentials = new Properties();
       try (final InputStream stream =
               this.getClass().getResourceAsStream("/credentials.properties")) {
       	credentials.load(stream);
       } catch (IOException e) {
			fail("credentials properties load fail: " + e.getMessage());
		}
       
       //System.out.println("session");
       session = client.authenticate(credentials.getProperty("username"), credentials.getProperty("password"));
       
       reportUri =  connection.get("reportUri") != null ? (String) connection.get("reportUri") : TEST_REPORT_URI;	   
       //System.out.println("setup done");
   }
   
   @After
   public void tearDown() {
       
	   if (session != null) {
		   session.logout();
	   }
	   
   }

    /**
     * Login and do several operations.
     * Want to make sure that all cookies are tracked, so that cookies from proxies and load balancers
     * are preserved.
     */
    @Test
    public void testSynchronousReport() {
        
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
        
        // Synch report
        OperationResult<InputStream> result = session
                .reportingService()
                .report(reportUri)
                .prepareForRun(ReportOutputFormat.HTML, 1)
                .run();
        
        assertTrue(result.getResponseStatus() == 200);
        
        if (connection.get("expectedCookie") != null) {
        	Cookie expectedCookie = session.getStorage().getCookies().get(connection.get("expectedCookie"));
        	assertNotNull(expectedCookie);
            if (connection.get("uniqueCookieValue") != null) {
            	assertFalse(uniqueCookieValue.equals(expectedCookie.getValue()));
            }
        }
    }

    @Test
    public void testAsynchronousReportPDF() {
        
        // Asynch report
        ReportExecutionRequest request = new ReportExecutionRequest();
        request.setReportUnitUri(reportUri);
        request
                .setAsync(true)                         	// run on server asynchronously
                .setPages("1") 								// page 1 only
                .setOutputFormat(ReportOutputFormat.PDF);   //report can be requested in different formats e.g. html, pdf, etc.

        // Asynch submission
        OperationResult<ReportExecutionDescriptor> operationResult =
                session
                        .reportingService()
                        .newReportExecutionRequest(request);
        
        assertTrue(operationResult.getResponseStatus() == 200);

        ReportExecutionDescriptor reportExecutionDescriptor = operationResult.getEntity();
        
        ReportExecutionStatusEntity statusEntity = null;

        // check execution status
        do {
        	if (statusEntity != null) {
        		try {
					Thread.sleep(100);
				} catch (InterruptedException e) { }
        	}
	        OperationResult<ReportExecutionStatusEntity> operationResultRESE =
	                session
	                        .reportingService()
	                        .reportExecutionRequest(reportExecutionDescriptor.getRequestId())
	                        .status();
	
	        assertTrue(operationResultRESE.getResponseStatus() == 200);
	
	        statusEntity = operationResultRESE.getEntity();
	        //System.out.println(statusEntity.getValue());
        } while (statusEntity.getValue().equalsIgnoreCase("queued")  ||
        		statusEntity.getValue().equalsIgnoreCase("execution"));
    
        assertTrue(statusEntity.getValue().equalsIgnoreCase("ready"));
        
        String exportStatus = null;
        String exportId = null;
        do {
        	if (exportStatus != null) {
        		try {
					Thread.sleep(100);
				} catch (InterruptedException e) { }
        	}
	        // get execution details
	        OperationResult<ReportExecutionDescriptor> operationResultRED =
	                session
	                        .reportingService()
	                        .reportExecutionRequest(reportExecutionDescriptor.getRequestId())
	                        .executionDetails();
	
	        assertTrue(operationResultRED.getResponseStatus() == 200);
	
	        ReportExecutionDescriptor descriptor = operationResultRED.getEntity();
	        //System.out.println(descriptor);
	        
	        for (ExportDescriptor ed : descriptor.getExports()) {
	        	//System.out.println(ed);
	        	// find PDF descriptor
	        	if (ed.getOutputResource() != null && ed.getOutputResource().getContentType().contains("pdf")) {
	        			exportStatus = ed.getStatus();
	        			exportId = ed.getId();
	        	}
	        }
	        //System.out.println("export status: " + exportStatus + ", " + exportId);
        } while (exportStatus == null || exportStatus.equalsIgnoreCase("queued")  ||
        		exportStatus.equalsIgnoreCase("execution"));

        assertTrue(exportStatus.equalsIgnoreCase("ready"));

        OperationResult<InputStream> operationResultIS =
                session
                        .reportingService()
                        .reportExecutionRequest(reportExecutionDescriptor.getRequestId())
                        .export(exportId)
                        .outputResource();

        assertTrue(operationResultIS.getResponseStatus() == 200);
        
        //InputStream file = operationResultIS.getEntity();
       
    }

    @Test
    public void testAsynchronousReportHTML() {
        
        // Asynch report
        ReportExecutionRequest request = new ReportExecutionRequest();
        request.setReportUnitUri(reportUri);
        request
                .setAsync(true)                         	// run on server asynchronously
                .setPages("1") 								// page 1 only
                .setOutputFormat(ReportOutputFormat.HTML);   //report can be requested in different formats e.g. html, pdf, etc.

        // Asynch submission
        OperationResult<ReportExecutionDescriptor> operationResult =
                session
                        .reportingService()
                        .newReportExecutionRequest(request);
        
        assertTrue(operationResult.getResponseStatus() == 200);

        ReportExecutionDescriptor reportExecutionDescriptor = operationResult.getEntity();
        
        ReportExecutionStatusEntity statusEntity = null;

        // check execution status
        do {
        	if (statusEntity != null) {
        		try {
					Thread.sleep(100);
				} catch (InterruptedException e) { }
        	}
	        OperationResult<ReportExecutionStatusEntity> operationResultRESE =
	                session
	                        .reportingService()
	                        .reportExecutionRequest(reportExecutionDescriptor.getRequestId())
	                        .status();
	
	        assertTrue(operationResultRESE.getResponseStatus() == 200);
	
	        statusEntity = operationResultRESE.getEntity();
	        //System.out.println(statusEntity.getValue());
        } while (statusEntity.getValue().equalsIgnoreCase("queued")  ||
        		statusEntity.getValue().equalsIgnoreCase("execution"));
    
        assertTrue(statusEntity.getValue().equalsIgnoreCase("ready"));
        
        String exportStatus = null;
        String exportId = null;
        ReportExecutionDescriptor descriptor = null;
        do {
        	if (exportStatus != null) {
        		try {
					Thread.sleep(100);
				} catch (InterruptedException e) { }
        	}
	        // get execution details
	        OperationResult<ReportExecutionDescriptor> operationResultRED =
	                session
	                        .reportingService()
	                        .reportExecutionRequest(reportExecutionDescriptor.getRequestId())
	                        .executionDetails();
	
	        assertTrue(operationResultRED.getResponseStatus() == 200);
	
	        descriptor = operationResultRED.getEntity();
	        //System.out.println(descriptor);
	        
	        for (ExportDescriptor ed : descriptor.getExports()) {
	        	//System.out.println(ed);
	        	// find HTML descriptor
	        	if (ed.getOutputResource() != null && ed.getOutputResource().getContentType().contains("html")) {
	        			exportStatus = ed.getStatus();
	        			exportId = ed.getId();
	        	}
	        }
	        System.out.println("export status: " + exportStatus + ", " + exportId);
        } while (exportStatus == null || exportStatus.equalsIgnoreCase("queued")  ||
        		exportStatus.equalsIgnoreCase("execution"));

        assertTrue(exportStatus.equalsIgnoreCase("ready"));

        OperationResult<InputStream> operationResultIS =
                session
                        .reportingService()
                        .reportExecutionRequest(reportExecutionDescriptor.getRequestId())
                        .export(exportId)
                        .outputResource();

        assertTrue(operationResultIS.getResponseStatus() == 200);
 
        for (ExportDescriptor ed : descriptor.getExports()) {
        	//System.out.println(ed);
        	// find HTML descriptor
        	if (ed.getOutputResource() != null &&
        			ed.getOutputResource().getContentType().contains("html") &&
        			ed.getAttachments() != null) {
                for (AttachmentDescriptor attDescriptor : ed.getAttachments()) {
                	System.out.println(attDescriptor);
                    OperationResult<InputStream> operationResultAttach =
                            session
                                    .reportingService()
                                    .reportExecutionRequest(reportExecutionDescriptor.getRequestId())
                                    .export(ed.getId())
                                    .attachment(attDescriptor.getFileName());
                    
                    assertTrue(operationResultAttach.getResponseStatus() == 200);
                    
                    InputStream file = operationResultAttach.getEntity();
                }
        	}
        }


       
    }
}
