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
import org.objectweb.proactive.core.node.NodeException;
import org.ow2.proactive.scheduler.common.exception.JobCreationException;
import org.ow2.proactive.scheduler.common.exception.NotConnectedException;
import org.ow2.proactive.scheduler.common.exception.PermissionException;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import org.ow2.proactive.scheduler.common.exception.SubmissionClosedException;
import org.ow2.proactive.scheduler.common.exception.UnknownJobException;
import org.ow2.proactive.scheduler.common.job.*;
import org.ow2.proactive.scheduler.common.job.factories.JobFactory;
import org.ow2.proactive.scheduler.job.InternalTaskFlowJob;
import org.ow2.proactive.scheduler.job.JobIdImpl;
import java.io.File;
import java.io.IOException;
import java.security.KeyException;
import java.util.Date;
import java.util.Vector;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;

/** 
 * Class that connects the test with the real scheduler, works as a stub. 
 * This is our interface to the remote Scheduler.
 * The interaction with the Scheduler is done using the specified protocol, either JAVAPA (Java ProActive) or REST. */
public class SchedulerStubProberRest implements SchedulerStubProber{
	
	private static Logger logger = Logger.getLogger(SchedulerStubProberRest.class.getName()); 	// Logger.
	
	/** REST attributes. */
	private String sessionId = null; 					// For the REST protocol, it defines the ID of the session.
	private URI uri; 									// It defines the URI used as a suffix to get the final URL for the REST server.
	private static final int RETRY_SEC_PERIOD_REST = 10;// Wait for X seconds to poll again and get a job status (only REST protocol mode).
	
	/**
	 * Constructor method. */
	public SchedulerStubProberRest(){}
	
	/** 
	 * Initialize the connection/session with the scheduler.
	 * @param protocolStr, protocol to be used to get connected to the scheduler. 
	 * @param url, url of the scheduler. 
	 * @param user, username to access the scheduler.
	 * @param pass, password to access the scheduler.
	 * */
	public void init(String protocolStr, String url, String user, String pass) throws IllegalArgumentException, LoginException, SchedulerException, KeyException, ActiveObjectCreationException, NodeException, HttpException, IOException{
	    uri = URI.create(url);
	    PostMethod methodLogin = new PostMethod(uri.toString() + "/login");
	    methodLogin.addParameter("username", user);
	    methodLogin.addParameter("password", pass);
	    HttpClient client = new HttpClient();
	    client.executeMethod(methodLogin);
	    sessionId = methodLogin.getResponseBodyAsString();
	    logger.info("Logged in with sessionId " + sessionId);
	
	}
	
	/** 
	 * Submit a job to the scheduler. 
	 * @param jobpath, path of the job descriptor file (xml). 
	 * @return and ID of the submitted job in case of success. */
	public String submitJob(String jobpath) throws IOException, NotConnectedException, PermissionException, SubmissionClosedException, JobCreationException{
		Job job = JobFactory.getFactory().createJob(jobpath);
	
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
	 * Wait for a job to finish. 
	 * @param jobId, the ID of the job to wait. 
	 * @param timeoutms, the maximum time in milliseconds to wait for this job. */
	public void waitUntilJobFinishes(String jobId, int timeoutms) throws NotConnectedException, PermissionException, UnknownJobException, HttpException, IOException{

		long start = (new Date()).getTime();
		long end;
		boolean timeout;
		Boolean finished = false;
		do{
			try {
				Thread.sleep(RETRY_SEC_PERIOD_REST * 1000);
			} catch (Exception e) {
				logger.warn("Not supposed to happen...", e);
			}
			JobStatus status = this.getJobStatus(jobId);
			logger.info("Current status: " +  status);
			finished = status.equals(JobStatus.FINISHED);
			end = (new Date()).getTime(); 
			timeout = (end-start>=timeoutms);
		}while(!finished && timeout==false);	
	}

	/** 
	 * Return the status of the job (running, finished, etc.). 
	 * @param jobId, the ID of the job. 
	 * @return the status of the job. */
	public JobStatus getJobStatus(String jobId) throws NotConnectedException, PermissionException, UnknownJobException, HttpException, IOException{
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

	@Override
	public Vector<String> getAllCurrentJobsList(String jobname)
			throws NotConnectedException, PermissionException {
		throw new RuntimeException("Not implemented yet");
	}

	@Override
	public void waitUntilJobIsCleaned(String jobId, int timeoutms)
			throws NotConnectedException, PermissionException,
			UnknownJobException, HttpException, IOException {
		throw new RuntimeException("Not implemented yet");
		
	}
}

/** 
 * This class helps performing the deserialization of JobId objects. */
class JobIdImplInstanceCreator implements InstanceCreator<JobIdImpl> {
	public JobIdImpl createInstance(Type type) {
		return (JobIdImpl)JobIdImpl.makeJobId("0");
	}
}

/** 
 * This class helps avoiding the parsing of a few heavy attributes 
 * while doing deserialization of JobStatus objects. */
class MyExclusionStrategy implements ExclusionStrategy {
    public MyExclusionStrategy() {}

    /** This method helps to identify which attributes should be avoided when deserializing. */
    public boolean shouldSkipClass(Class<?> clazz) {
    	String name = clazz.getName();
    	Boolean skip = name.contains("JobId");
    	return skip;
    }

    /** This method helps to identify which attributes should be avoided when deserializing. */
    public boolean shouldSkipField(FieldAttributes f) {
    	String name = f.getName();
    	Boolean skipit = name.equals("tasks");
    	return skipit;
    }
  }
