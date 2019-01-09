package com.jaspersoft.jasperserver.jaxrs.client.actual;

import static org.junit.Assert.*;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import com.jaspersoft.jasperserver.jaxrs.client.dto.reports.ExportExecutionDescriptor;
import com.jaspersoft.jasperserver.jaxrs.client.dto.reports.ExportExecutionOptions;
import com.jaspersoft.jasperserver.jaxrs.client.dto.reports.ReportExecutionDescriptor;
import com.jaspersoft.jasperserver.jaxrs.client.dto.reports.ReportExecutionRequest;
import com.jaspersoft.jasperserver.jaxrs.client.dto.reports.ReportExecutionStatusEntity;

import org.junit.*;

/**
 * Unit test for Jaspersoft REST API client.
 * 
 * jasperreports.io has the same REST API as JasperReports Server for reports.
 * By default, jr.io does not authenticate.
 * 
 */
public class JRIORestApiTest {

	// a report in the jasperreports.io repository. Part of the samples
	
	public static String TEST_REPORT_URI = "/samples/reports/FirstJasper";

	Properties connection;
	JasperserverRestClient client;
	Session session;
	String reportUri;

   public JRIORestApiTest() {}

    /**
     * Connect and do several operations.
     * Want to make sure that all cookies are tracked, so that cookies from proxies and load balancers
     * are preserved.
     */
    @Before
    public void setup() {
        
        connection = new Properties();
        try (final InputStream stream =
                this.getClass().getResourceAsStream("/jrio-connection.properties")) {
        	connection.load(stream);
        } catch (IOException e) {
			fail("connection properties load fail: " + e.getMessage());
		}
        
    	RestClientConfiguration config = RestClientConfiguration.loadConfiguration(connection);
    	
        assertNotNull( config.getJasperReportsServerUrl() );
        
        client = new JasperserverRestClient(config);
        
        // no authentication in jr.io
        
        session = client.getUnauthenicatedSession();
        
        reportUri =  connection.get("reportUri") != null ? (String) connection.get("reportUri") : TEST_REPORT_URI;
    }
    
    @Test
    public void testSynchronousReport() {
        
        // Synchronous report call
        OperationResult<InputStream> result = session
                .reportingService()
                .report(reportUri)
                .prepareForRun(ReportOutputFormat.HTML, 1)
                .run();
        InputStream report = result.getEntity();
        
        assertTrue(result.getResponseStatus() == 200);
        
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
        }
       
    }

    @Test
    public void testAsynchronousReportPDF() {
    	
        String uniqueCookieValue = null;
        
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
	        
	        if (connection.get("expectedCookie") != null) {
	        	Cookie expectedCookie = session.getStorage().getCookies().get(connection.get("expectedCookie"));
	        	assertNotNull(expectedCookie);
	            if (connection.get("uniqueCookieValue") != null) {
	            	assertFalse(uniqueCookieValue.equals(expectedCookie.getValue()));
	            	uniqueCookieValue = expectedCookie.getValue();
	            }
	        }
	
	        statusEntity = operationResultRESE.getEntity();
	        //System.out.println(statusEntity.getValue());
        } while (statusEntity.getValue().equalsIgnoreCase("queued")  ||
        		statusEntity.getValue().equalsIgnoreCase("execution"));
    
        assertTrue(statusEntity.getValue().equalsIgnoreCase("ready"));
        
        // export request
        
        ExportExecutionOptions exportExecutionOptions = new ExportExecutionOptions()
                .setOutputFormat(ReportOutputFormat.PDF);

        OperationResult<ExportExecutionDescriptor> operationResultEED =
                session
                        .reportingService()
                        .reportExecutionRequest(reportExecutionDescriptor.getRequestId())
                        .runExport(exportExecutionOptions);

        assertTrue(operationResultEED.getResponseStatus() == 200);

        ExportExecutionDescriptor statusEntityEED = operationResultEED.getEntity();
        System.out.println(statusEntityEED);
        
        String exportStatus = statusEntityEED.getStatus();
        String exportId = statusEntityEED.getId();
        System.out.println("initial export status: " + exportStatus + ", " + exportId);
    	
        // check export status
        while (exportStatus.equalsIgnoreCase("queued")  ||
        		exportStatus.equalsIgnoreCase("execution")) {
        	if (exportStatus != null) {
        		try {
					Thread.sleep(100);
				} catch (InterruptedException e) { }
        	}
        	OperationResult<ReportExecutionStatusEntity> operationResultRESE =
        	        session
        	                .reportingService()
        	                .reportExecutionRequest(reportExecutionDescriptor.getRequestId())
        	                .export(exportId)
        	                .status();

        	assertTrue(operationResultRESE.getResponseStatus() == 200);

        	ReportExecutionStatusEntity statusEntityRESE = operationResultRESE.getEntity();	
	
	        //System.out.println(statusEntityRESE);
	        exportStatus = statusEntityRESE.getValue();
        };

        assertTrue(exportStatus.equalsIgnoreCase("ready"));      
 
        // get export output
        
    	OperationResult<InputStream> operationResultIS =
    	        session
    	                .reportingService()
    	                .reportExecutionRequest(reportExecutionDescriptor.getRequestId())
    	                .export(exportId)
    	                .outputResource();

        assertTrue(operationResultIS.getResponseStatus() == 200);
        
        checkInputStream(operationResultIS.getEntity(), "output.pdf");

    }

    @Test
    public void testAsynchronousReportHTML() {
    	
        String uniqueCookieValue = null;
        
        // Asynch report
        ReportExecutionRequest request = new ReportExecutionRequest();
        request.setReportUnitUri(reportUri);
        request
                .setAsync(true)                         	// run on server asynchronously
                .setPages("1") 								// page 1 only
                .setAttachmentsPrefix("/my/reportExecutions/{reportExecutionId}/exports/{exportExecutionId}/attachments/")
                .setOutputFormat(ReportOutputFormat.HTML);   //report can be requested in different formats e.g. html, pdf, etc.

        // Asynch submission
        OperationResult<ReportExecutionDescriptor> operationResult =
                session
                        .reportingService()
                        .newReportExecutionRequest(request);
        
        assertTrue(operationResult.getResponseStatus() == 200);
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
	        
	        if (connection.get("expectedCookie") != null) {
	        	Cookie expectedCookie = session.getStorage().getCookies().get(connection.get("expectedCookie"));
	        	assertNotNull(expectedCookie);
	            if (connection.get("uniqueCookieValue") != null) {
	            	assertFalse(uniqueCookieValue.equals(expectedCookie.getValue()));
	            	uniqueCookieValue = expectedCookie.getValue();
	            }
	        }
	
	        statusEntity = operationResultRESE.getEntity();
	        //System.out.println(statusEntity.getValue());
        } while (statusEntity.getValue().equalsIgnoreCase("queued")  ||
        		statusEntity.getValue().equalsIgnoreCase("execution"));
    
        assertTrue(statusEntity.getValue().equalsIgnoreCase("ready"));
        
        // export request
        
        ExportExecutionOptions exportExecutionOptions = new ExportExecutionOptions()
                .setOutputFormat(ReportOutputFormat.HTML)
                .setAttachmentsPrefix("/my/rest_v2/reportExecutions/{reportExecutionId}/exports/{exportExecutionId}/attachments/")
                .setPages("1");

        OperationResult<ExportExecutionDescriptor> operationResultEED =
                session
                        .reportingService()
                        .reportExecutionRequest(reportExecutionDescriptor.getRequestId())
                        .runExport(exportExecutionOptions);

        assertTrue(operationResultEED.getResponseStatus() == 200);

        ExportExecutionDescriptor statusEntityEED = operationResultEED.getEntity();
        System.out.println(statusEntityEED);
        
        String exportStatus = statusEntityEED.getStatus();
        String exportId = statusEntityEED.getId();
    	
        // check export status
        while (exportStatus.equalsIgnoreCase("queued")  ||
        		exportStatus.equalsIgnoreCase("execution")) {
        	if (exportStatus != null) {
        		try {
					Thread.sleep(100);
				} catch (InterruptedException e) { }
        	}
        	OperationResult<ReportExecutionStatusEntity> operationResultRESE =
        	        session
        	                .reportingService()
        	                .reportExecutionRequest(reportExecutionDescriptor.getRequestId())
        	                .export(exportId)
        	                .status();

        	assertTrue(operationResultRESE.getResponseStatus() == 200);

        	ReportExecutionStatusEntity statusEntityRESE = operationResultRESE.getEntity();	
	
	        //System.out.println(statusEntityRESE);
	        exportStatus = statusEntityRESE.getValue();
        };

        assertTrue(exportStatus.equalsIgnoreCase("ready"));      
        
        // get execution details
        OperationResult<ReportExecutionDescriptor> operationResultRED =
                session
                        .reportingService()
                        .reportExecutionRequest(reportExecutionDescriptor.getRequestId())
                        .executionDetails();
        
        assertTrue(operationResultRED.getResponseStatus() == 200);
        
        ReportExecutionDescriptor descriptor = operationResultRED.getEntity();
    	System.out.println(descriptor);
 
        // get export output
        
    	OperationResult<InputStream> operationResultIS =
    	        session
    	                .reportingService()
    	                .reportExecutionRequest(reportExecutionDescriptor.getRequestId())
    	                .export(exportId)
    	                .outputResource();

        assertTrue(operationResultIS.getResponseStatus() == 200);
        
        checkInputStream(operationResultIS.getEntity(), "output.html");
        
        for (ExportDescriptor ed : descriptor.getExports()) {
        	System.out.println(ed);
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
	                    checkInputStream(operationResultAttach.getEntity(), attDescriptor.getFileName());
	                }
        	}
        }

    }
    
    private void checkInputStream(InputStream is, String name) {
        Long size = 0L;
        
        try {
			FileOutputStream fos = new FileOutputStream("target/surefire/" + name, false);
	        int nRead;
	        byte[] data = new byte[1024];       
	        try {
				while ((nRead = is.read(data, 0, data.length)) != -1) {
				    size += nRead;
				    fos.write(data, 0, nRead);
				}
			} catch (IOException e) {
				fail("reading input stream - " + name + ": " + e.getMessage());
				fos.close();
			}
	        fos.close();
		} catch (FileNotFoundException e1) {
			fail("FileNotFoundException " + name + ": " + e1.getMessage());
		} catch (IOException e) {
			fail("IOException " + name + ": " + e.getMessage());
		}
        System.out.println(name + " - read bytes: " + size);
        assertTrue(size > 0L);
    }
}
