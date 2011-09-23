package qosprober.main;

import javax.security.auth.login.LoginException;
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
import java.io.IOException;
import java.security.KeyException;
import java.util.Date;
import org.ow2.proactive.authentication.crypto.Credentials;
import qosprober.exceptions.InvalidProtocolException;

/** 
 * Class that connects the test with the real scheduler, works as a stub. 
 * This is our interface to the remote Scheduler.
 * The interaction with the Scheduler is done using the specified protocol, either JAVAPA (Java ProActive) or REST. */
public class SchedulerStubProberJava implements SchedulerStubProber{
	private static Logger logger = Logger.getLogger(SchedulerStubProberJava.class.getName()); 	// Logger.
	private Scheduler schedulerStub; 														// Stub to the scheduler.
	
	/**
	 * Constructor method. */
	public SchedulerStubProberJava(){}
	
	/** 
	 * Initialize the connection/session with the scheduler.
	 * @param protocolStr, protocol to be used to get connected to the scheduler. 
	 * @param url, url of the scheduler. 
	 * @param user, username to access the scheduler.
	 * @param pass, password to access the scheduler.
	 * */
	public void init(String protocolStr, String url, String user, String pass) throws IllegalArgumentException, LoginException, SchedulerException, KeyException, ActiveObjectCreationException, NodeException, HttpException, IOException{
		logger .info("Joining the scheduler...");
        SchedulerAuthenticationInterface auth = SchedulerConnection.waitAndJoin(url);
        logger .info("Creating credentials...");
        Credentials cred = Credentials.createCredentials(new CredData(user, pass), auth.getPublicKey());
        logger .info("Logging in...");
        schedulerStub = auth.login(cred);
        SchedulerEventsListener aa = PAActiveObject.newActive(SchedulerEventsListener.class, new Object[]{}); 
        schedulerStub.addEventListener((SchedulerEventsListener) aa, true);
	}
	
	/** 
	 * Submit a job to the scheduler. 
	 * @param jobpath, path of the job descriptor file (xml). 
	 * @return and ID of the submitted job in case of success. */
	public String submitJob(String jobpath) throws IOException, NotConnectedException, PermissionException, SubmissionClosedException, JobCreationException{
		Job job = JobFactory.getFactory().createJob(jobpath);
	
		JobId ret = schedulerStub.submit(job);
		if (ret!=null){
			return ret.value();
		}else{
			return null;
		}
	}
	
	/**
	 * Get the result of the job. 
	 * @param jobId, the ID of the job. 
	 * @return The raw output of the job. */
	public String getJobResult(String jobId) throws NotConnectedException, PermissionException, UnknownJobException, HttpException, IOException{
	
		JobResult jr = schedulerStub.getJobResult(jobId);
		if (jr != null){
			return jr.toString();
		}else{
			return null;
		}
	}
	
	/** 
	 * Wait for a job to finish. 
	 * @param jobId, the ID of the job to wait. 
	 * @param timeoutms, the maximum time in milliseconds to wait for this job. */
	public void waitUntilJobFinishes(String jobId, int timeoutms) throws NotConnectedException, PermissionException, UnknownJobException, HttpException, IOException{

		long start = (new Date()).getTime();
		long end;
		boolean timeout;

		
		do{
			synchronized(SchedulerStubProberJava.class){
				
				try {
					SchedulerStubProberJava.class.wait(timeoutms); // This thread is blocked until the SchedulerEventsListener notifies of a new finished job.
				} catch (InterruptedException e) {
					logger.warn("Not supposed to happen...", e);
				}
				end = (new Date()).getTime(); 
				timeout = (end-start>=timeoutms);
			}
			
		}while(SchedulerEventsListener.checkIfJobIdHasJustFinished(jobId)==false && timeout==false);
	}

	/** 
	 * Return the status of the job (running, finished, etc.). 
	 * @param jobId, the ID of the job. 
	 * @return the status of the job. */
	public JobStatus getJobStatus(String jobId) throws NotConnectedException, PermissionException, UnknownJobException, HttpException, IOException{
		return schedulerStub.getJobState(jobId).getStatus();
	}

	/** 
	 * Remove the job from the Scheduler. No leftovers of the job in the server.
	 * This is specially useful to delete the probe job, so we do not contaminate what the administrator sees.
	 * @param jobId, the ID of the job. */
	public void removeJob(String jobId) throws Exception, NotConnectedException, UnknownJobException, PermissionException, InvalidProtocolException{
		schedulerStub.removeJob(jobId);
	}
	
	/** 
	 * Disconnect from the Scheduler. */
	public void disconnect() throws NotConnectedException, PermissionException, HttpException, IOException{		
		schedulerStub.disconnect();
	}
}
