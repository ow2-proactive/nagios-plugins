package qosprober.main;

import java.lang.reflect.Type;
import java.net.URI;

import javax.security.auth.login.LoginException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.DeleteMethod;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.node.NodeException;
import org.ow2.proactive.scheduler.common.SchedulerConnection;
import org.ow2.proactive.scheduler.common.exception.JobCreationException;
import org.ow2.proactive.scheduler.common.exception.NotConnectedException;
import org.ow2.proactive.scheduler.common.exception.PermissionException;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import org.ow2.proactive.scheduler.common.exception.SubmissionClosedException;
import org.ow2.proactive.scheduler.common.exception.UnknownJobException;
import org.ow2.proactive.scheduler.common.job.*;
import org.ow2.proactive.scheduler.common.job.factories.JobFactory;
import org.ow2.proactive.scheduler.common.Scheduler;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.scheduler.common.SchedulerAuthenticationInterface;
import org.ow2.proactive.scheduler.job.InternalTaskFlowJob;
import org.ow2.proactive.scheduler.job.JobIdImpl;

import java.io.File;
import java.io.IOException;

import java.security.KeyException;
import java.util.Date;

import org.ow2.proactive.authentication.crypto.Credentials;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;

import qosprober.exceptions.InvalidProtocolException;

/** Class to work as a Stub or point of access to the Scheduler. 
 * This is our interface to the remote Scheduler.
 * The interaction with the Scheduler is done using the specified protocol, either JAVAPA (Java ProActive) or REST.  
 * */
public class SchedulerStubProber{
	
	private static Logger logger = Logger.getLogger(SchedulerStubProber.class.getName()); // Logger.
	private Scheduler schedulerStub; 											// Stub to the scheduler.
	private ProActiveProxyProtocol protocol = ProActiveProxyProtocol.UNKNOWN; 	// Protocol to get connected with the Scheduler.
	
	/* REST attributes. */
	private String sessionId = null; 						// For the REST protocol, it defines the ID of the session.
	private URI uri; 										// It defines the URI used as a suffix to get the final URL for the REST server.
	private static final int RETRY_SEC_PERIOD_REST = 10; 	// Wait for X seconds to poll again and get a job status (only REST protocol mode).
	
	public SchedulerStubProber(){}
	
	/* Initialize the connection/session. */
	public void init(String protocolStr, String url, String user, String pass) throws IllegalArgumentException, LoginException, SchedulerException, KeyException, ActiveObjectCreationException, NodeException, InvalidProtocolException, HttpException, IOException{
		protocol = ProActiveProxyProtocol.parseProtocol(protocolStr);
		if (protocol == ProActiveProxyProtocol.JAVAPA){ // Java ProActive protocol. 
			logger .info("Joining to the scheduler...");
	        SchedulerAuthenticationInterface auth = SchedulerConnection.waitAndJoin(url);
	        logger .info("Creating credentials...");
	        Credentials cred = Credentials.createCredentials(new CredData(user, pass), auth.getPublicKey());
	        logger .info("Logging in...");
	        schedulerStub = auth.login(cred);
	        SchedulerEventsListener aa = PAActiveObject.newActive(SchedulerEventsListener.class, new Object[]{}); 
	        schedulerStub.addEventListener((SchedulerEventsListener) aa, true);
		}else if (protocol == ProActiveProxyProtocol.REST){ // REST protocol.
		    uri = URI.create(url);
		    PostMethod methodLogin = new PostMethod(uri.toString() + "/login");
		    methodLogin.addParameter("username", user);
		    methodLogin.addParameter("password", pass);
		    HttpClient client = new HttpClient();
		    client.executeMethod(methodLogin);
		    sessionId = methodLogin.getResponseBodyAsString();
		    logger.info("Logged in with sessionId " + sessionId);
		}else{
			throw new InvalidProtocolException("Protocol " + protocolStr + " not supported.");
		}
	}
	
	/* Submit a job. */
	public String submitJob(String jobpath) throws IOException, NotConnectedException, PermissionException, SubmissionClosedException, JobCreationException, InvalidProtocolException{
		Job job = JobFactory.getFactory().createJob(jobpath);
		if (protocol == ProActiveProxyProtocol.JAVAPA){ // Java ProActive protocol.
			JobId ret = schedulerStub.submit(job);
			if (ret!=null){
				return ret.value();
			}else{
				return null;
			}
		}else if (protocol == ProActiveProxyProtocol.REST){ // REST protocol.
			PostMethod method = new PostMethod(uri.toString() + "/submit");
			method.setRequestHeader("sessionid", sessionId);
			File f = new File(jobpath);
			Part[] parts = {new FilePart(f.getName(),f, "application/xml", "UTF-8")};
			
			HttpClient client = new HttpClient(); 
			method.setRequestEntity(new MultipartRequestEntity(parts,method.getParams())); // The jobdescriptor goes embedded in the request.		   
		    client.executeMethod(method);
		    String response = method.getResponseBodyAsString();
		    
		    logger.info("Job submission response: " + response);
		    
		    /* GSon requires a default creator for JobIdImpl. Since it does not have, with use this special mechanism. */
		    Gson gson = new GsonBuilder().registerTypeAdapter(JobIdImpl.class, new JobIdImplInstanceCreator()).create();
		    
		    JobIdImpl jobidimpl = gson.fromJson(response, JobIdImpl.class);
		    
			return jobidimpl.value();
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		} 
	}
	
	/* Get the result of the job. */
	public String getJobResult(String jobId) throws NotConnectedException, PermissionException, UnknownJobException, InvalidProtocolException, HttpException, IOException{
		if (protocol == ProActiveProxyProtocol.JAVAPA){ // Java ProActive protocol.
			JobResult jr = schedulerStub.getJobResult(jobId);
			if (jr != null){
				return jr.toString();
			}else{
				return null;
			}
		}else if (protocol == ProActiveProxyProtocol.REST){
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
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		}
	}
	
	/* Wait for a job to finish. */
	public void waitUntilJobFinishes(String jobId, int timeoutms) throws NotConnectedException, PermissionException, UnknownJobException, InvalidProtocolException, HttpException, IOException{

		long start = (new Date()).getTime();
		long end;
		boolean timeout;
		if (protocol == ProActiveProxyProtocol.JAVAPA){ // Java ProActive protocol.
			
			do{
				synchronized(SchedulerStubProber.class){
					
					try {
						SchedulerStubProber.class.wait(timeoutms); // This thread is blocked until the SchedulerEventsListener notifies of a new finished job.
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					end = (new Date()).getTime(); 
					timeout = (end-start>=timeoutms);
				}
				
			}while(SchedulerEventsListener.checkIfJobIdHasJustFinished(jobId)==false && timeout==false);

		}else if (protocol == ProActiveProxyProtocol.REST){
			
			Boolean finished = false;
			do{
				try {
					Thread.sleep(RETRY_SEC_PERIOD_REST * 1000);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				JobStatus status = this.getJobStatus(jobId);
				logger.info("Current status: " +  status);
				finished = status.equals(JobStatus.FINISHED);
				end = (new Date()).getTime(); 
				timeout = (end-start>=timeoutms);
			}while(!finished && timeout==false);
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		}
	}

	/* Return the state of the job in the current Scheduler */
	public JobStatus getJobStatus(String jobId) throws NotConnectedException, PermissionException, UnknownJobException, InvalidProtocolException, HttpException, IOException{
		if (protocol == ProActiveProxyProtocol.JAVAPA){ // Java ProActive protocol.
			return schedulerStub.getJobState(jobId).getStatus();
		}else if (protocol == ProActiveProxyProtocol.REST){
			GetMethod method = new GetMethod(uri.toString()+ "/jobs/" + jobId);
			method.addRequestHeader("sessionid", sessionId);
			HttpClient client = new HttpClient();
			client.executeMethod(method);
			String response = method.getResponseBodyAsString();
			//logger.info("Job submission response: " + response);
			Gson gsonn = new GsonBuilder().setExclusionStrategies(new MyExclusionStrategy()).serializeNulls().create();
			InternalTaskFlowJob ret = gsonn.fromJson(response, InternalTaskFlowJob.class);
			System.out.println (ret.getStatus());
		    return ret.getStatus(); 
			
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		}
	}

	/* Remove the job from the Scheduler. */
	public void removeJob(String jobId) throws Exception, NotConnectedException, UnknownJobException, PermissionException, InvalidProtocolException{
		if (protocol == ProActiveProxyProtocol.JAVAPA){ // Java ProActive protocol.	
			schedulerStub.removeJob(jobId);
		}else if (protocol == ProActiveProxyProtocol.REST){

			DeleteMethod method = new DeleteMethod(uri.toString()+ "/jobs/" + jobId);
		    method.addRequestHeader("sessionid", sessionId);
		    HttpClient client = new HttpClient();
			client.executeMethod(method);
			String response = method.getResponseBodyAsString();
		    logger.info("Delete job response: " + response);
		    
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		}
	}
	
	/* Disconnect from the Scheduler. */
	public void disconnect() throws NotConnectedException, PermissionException, InvalidProtocolException, HttpException, IOException{	
		if (protocol == ProActiveProxyProtocol.JAVAPA){ // Java ProActive protocol.	
			schedulerStub.disconnect();
			
		}else if (protocol == ProActiveProxyProtocol.REST){
		    PutMethod method = new PutMethod(uri.toString() + "/disconnect");
		    method.addRequestHeader("sessionid", sessionId);
		    HttpClient client = new HttpClient();
		    client.executeMethod(method);
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		}
	}
}

/* This class helps performing the deserealization of JobId objects. */
class JobIdImplInstanceCreator implements InstanceCreator<JobIdImpl> {
	public JobIdImpl createInstance(Type type) {
		return (JobIdImpl)JobIdImpl.makeJobId("0");
	}
}

/* This class helps avoiding the parsing of a few heavy attributes 
 * while doing deserialization of JobStatus objects. */
class MyExclusionStrategy implements ExclusionStrategy {
    public MyExclusionStrategy() {}

    public boolean shouldSkipClass(Class<?> clazz) {
    	String name = clazz.getName();
    	Boolean skip = name.contains("JobId");
    	return skip;
    }

    public boolean shouldSkipField(FieldAttributes f) {
    	String name = f.getName();
    	Boolean skipit = name.equals("tasks");
    	return skipit;
    }
  }
