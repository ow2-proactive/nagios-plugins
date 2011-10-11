package qosprober.main;

import javax.security.auth.login.LoginException;
import org.apache.commons.httpclient.HttpException;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.core.node.NodeException;
import org.ow2.proactive.scheduler.common.exception.JobCreationException;
import org.ow2.proactive.scheduler.common.exception.NotConnectedException;
import org.ow2.proactive.scheduler.common.exception.PermissionException;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import org.ow2.proactive.scheduler.common.exception.SubmissionClosedException;
import org.ow2.proactive.scheduler.common.exception.UnknownJobException;
import org.ow2.proactive.scheduler.common.job.*;
import java.io.IOException;
import java.security.KeyException;
import java.util.Vector;

/** 
 * Class that connects the test with the real scheduler, works as a stub. 
 * This is our interface to the remote Scheduler.
 * The interaction with the Scheduler is done using the specified protocol, either JAVAPA (Java ProActive) or REST. */
public interface SchedulerStubProber{
	

	
	/** 
	 * Initialize the connection/session with the scheduler.
	 * @param protocolStr, protocol to be used to get connected to the scheduler. 
	 * @param url, url of the scheduler. 
	 * @param user, username to access the scheduler.
	 * @param pass, password to access the scheduler.
	 * */
	public void init(String protocolStr, String url, String user, String pass) throws IllegalArgumentException, LoginException, SchedulerException, KeyException, ActiveObjectCreationException, NodeException, HttpException, IOException;
		
	/** 
	 * Submit a job to the scheduler. 
	 * @param jobpath, path of the job descriptor file (xml). 
	 * @return and ID of the submitted job in case of success. */
	public String submitJob(String jobpath) throws IOException, NotConnectedException, PermissionException, SubmissionClosedException, JobCreationException;
		
	/**
	 * Get the result of the job. 
	 * @param jobId, the ID of the job. 
	 * @return The raw output of the job. */
	public String getJobResult(String jobId) throws NotConnectedException, PermissionException, UnknownJobException, HttpException, IOException;
		
	
	/** 
	 * Wait for a job to finish. 
	 * @param jobId, the ID of the job to wait. 
	 * @param timeoutms, the maximum time in milliseconds to wait for this job. */
	public void waitUntilJobFinishes(String jobId, int timeoutms) throws NotConnectedException, PermissionException, UnknownJobException, HttpException, IOException;
	
	/** 
	 * Wait for a job to be cleaned (removed or finished). 
	 * @param jobId, the ID of the job to wait for. 
	 * @param timeoutms, the maximum time in milliseconds to wait for this job removal. */
	public void waitUntilJobIsCleaned(String jobId, int timeoutms) throws NotConnectedException, PermissionException, UnknownJobException, HttpException, IOException;

	/** 
	 * Return the status of the job (running, finished, etc.). 
	 * @param jobId, the ID of the job. 
	 * @return the status of the job. */
	public JobStatus getJobStatus(String jobId) throws NotConnectedException, PermissionException, UnknownJobException, HttpException, IOException;

	/** 
	 * Remove the job from the Scheduler. No leftovers of the job in the server.
	 * This is specially useful to delete the probe job, so we do not contaminate what the administrator sees.
	 * @param jobId, the ID of the job. */
	public void removeJob(String jobId) throws Exception, NotConnectedException, UnknownJobException, PermissionException;
	
	/** 
	 * Disconnect from the Scheduler. */
	public void disconnect() throws NotConnectedException, PermissionException, HttpException, IOException;	

	/**
	 * Get a list of all jobs with the given name that are in pending, running and finished queues 
	 * of the scheduler (my jobs only). */
	public Vector<String> getAllCurrentJobsList(String jobname) throws NotConnectedException, PermissionException;
}
