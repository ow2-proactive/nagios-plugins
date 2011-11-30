package qosprober.main;

import java.net.URI;
import javax.security.auth.login.LoginException;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.core.node.NodeException;
import org.ow2.proactive.scheduler.common.exception.NotConnectedException;
import org.ow2.proactive.scheduler.common.exception.PermissionException;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import org.ow2.proactive.scheduler.common.exception.UnknownJobException;
import java.io.IOException;
import java.security.KeyException;

//import com.google.gson.ExclusionStrategy;
//import com.google.gson.FieldAttributes;
//import com.google.gson.Gson;
//import com.google.gson.GsonBuilder;
//import com.google.gson.InstanceCreator;

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
	public RestStubProber() throws Exception{
	}
	
	/** 
	 * Initialize the connection/session with the scheduler.
	 * @param protocolStr, protocol to be used to get connected to the scheduler. 
	 * @param url, url of the scheduler. 
	 * @param user, username to access the scheduler.
	 * @param pass, password to access the scheduler. */
	public void init(String url, String user, String pass) throws IllegalArgumentException, LoginException, SchedulerException, KeyException, ActiveObjectCreationException, NodeException, HttpException, IOException{
	    uri = URI.create(url);
	    PostMethod methodLogin = new PostMethod(uri.toString() + "/login");
	    methodLogin.addParameter("username", user);
	    methodLogin.addParameter("password", pass);
	    HttpClient client = new HttpClient();
	    client.executeMethod(methodLogin);
	    sessionId = methodLogin.getResponseBodyAsString();
	    logger.info("Logged in with sessionId: " + sessionId);
	}
	
	/**
	 * Get the result of the job. 
	 * @param jobId, the ID of the job. 
	 * @return The raw output of the job. */
	public String getJobResult(String jobId) throws NotConnectedException, PermissionException, UnknownJobException, HttpException, IOException{
 
	    GetMethod method = new GetMethod(uri.toString()+ "/jobs/" + jobId + "/result");
	    method.addRequestHeader("sessionid", sessionId);
	    HttpClient client = new HttpClient();
	    client.executeMethod(method);
	    String response = method.getResponseBodyAsString();
	    logger.info("Job result: " + response);
	    //Gson gsonn = new GsonBuilder().setExclusionStrategies(new MyExclusionStrategy()).serializeNulls().create();
	    //JobResultImpl ret = gsonn.fromJson(response, JobResultImpl.class);
	    System.out.println("RESULTS NOT PARSED");
	    logger.warn("RESULTS NOT PARSED");
		return "RESULTS NOT PARSED";
	}
	
	/** 
	 * Remove the job from the Scheduler. No leftovers of the job in the server.
	 * This is specially useful to delete the probe job, so we do not contaminate what the administrator sees.
	 * @param jobId, the ID of the job. */
	public void removeJob(String jobId) throws Exception, NotConnectedException, UnknownJobException, PermissionException{
		DeleteMethod method = new DeleteMethod(uri.toString()+ "/jobs/" + jobId);
	    method.addRequestHeader("sessionid", sessionId);
	    HttpClient client = new HttpClient();
		client.executeMethod(method);
		String response = method.getResponseBodyAsString();
	    logger.info("Delete job response: " + response);
	}
	
	/** 
	 * Disconnect from the Scheduler. */
	public void disconnect() throws NotConnectedException, PermissionException, HttpException, IOException{	
	    PutMethod method = new PutMethod(uri.toString() + "/disconnect");
	    method.addRequestHeader("sessionid", sessionId);
	    HttpClient client = new HttpClient();
	    client.executeMethod(method);
	}

}
