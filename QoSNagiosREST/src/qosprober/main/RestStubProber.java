package qosprober.main;

import java.net.URI;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import java.io.IOException;

/** 
 * Class that connects the test with the real scheduler, works as a stub. 
 * This is our interface to the remote Scheduler.
 * The interaction with the Scheduler is done using the specified protocol, either JAVAPA (Java ProActive) or REST. 
 * This class is specific for REST protocol. */
public class RestStubProber{
	
	private static Logger logger = Logger.getLogger(RestStubProber.class.getName()); 	// Logger.
	
	/** REST attributes. */
	private String sessionId = null; 					// For the REST protocol, it defines the ID of the session.
	private URI uri; 									// It defines the URI used as a suffix to get the final URL for the REST server.
	
	/**
	 * Constructor method. */
	public RestStubProber(){}
	
	/** 
	 * Initialize the connection/session with the scheduler.
	 * @param url, url of the scheduler for REST API. 
	 * @param user, username to access the scheduler.
	 * @param pass, password to access the scheduler. 
	 * @throws IOException 
	 * @throws HttpException */
	public void init(String url, String user, String pass) throws HttpException, IOException {
		if (url.endsWith("/")){
			url = url.substring(0, url.length()-1);
		}
	    logger.info("Connecting at '" + url + "'...");
	    uri = URI.create(url);
	    PostMethod methodLogin = new PostMethod(uri.toString() + "/login");
	    methodLogin.addParameter("username", user);
	    methodLogin.addParameter("password", pass);
	    HttpClient client = new HttpClient();
	    client.executeMethod(methodLogin);
	    sessionId = methodLogin.getResponseBodyAsString();
	    logger.info("Logged in with sessionId: " + sessionId);
	    logger.info("Done.");
	}
	
	/**
	 * Get a boolean telling if the prober is still connected to the scheduler through REST API or not. 
	 * @return a boolean telling if we are connected to the scheduler. 
	 * @throws IOException 
	 * @throws HttpException */
	public Boolean isConnected() throws HttpException, IOException{
	    logger.info("Asking if connected...");
	    GetMethod method = new GetMethod(uri.toString()+  "/isconnected");
	    method.addRequestHeader("sessionid", sessionId);
	    HttpClient client = new HttpClient();
	    client.executeMethod(method);
	    String response = method.getResponseBodyAsString();
	    logger.info("IsConnected result: " + response);
	    logger.info("Done.");
		return Boolean.parseBoolean(response);
	}
	
	/**
	 * Get the version of the REST API. 
	 * @return the version. 
	 * @throws IOException 
	 * @throws HttpException */
	public String getVersion() throws HttpException, IOException{
	    logger.info("Asking version...");
	    GetMethod method = new GetMethod(uri.toString()+  "/version");
	    method.addRequestHeader("sessionid", sessionId);
	    HttpClient client = new HttpClient();
	    client.executeMethod(method);
	    String response = method.getResponseBodyAsString();
	    logger.info("Version result: " + response);
	    logger.info("Done.");
		return response;
	}

	/** 
	 * Disconnect from the Scheduler. 
	 * @throws IOException 
	 * @throws HttpException */
	public void disconnect() throws HttpException, IOException{	
	    logger.info("Disconnecting...");
	    PutMethod method = new PutMethod(uri.toString() + "/disconnect");
	    method.addRequestHeader("sessionid", sessionId);
	    HttpClient client = new HttpClient();
	    client.executeMethod(method);
	    logger.info("Done.");
	}

}
