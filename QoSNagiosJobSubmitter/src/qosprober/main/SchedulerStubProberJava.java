package qosprober.main;

import javax.security.auth.login.LoginException;
import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.node.NodeException;
import org.ow2.proactive.scheduler.common.SchedulerConnection;
import org.ow2.proactive.scheduler.common.SchedulerState;
import org.ow2.proactive.scheduler.common.exception.JobCreationException;
import org.ow2.proactive.scheduler.common.exception.NotConnectedException;
import org.ow2.proactive.scheduler.common.exception.PermissionException;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import org.ow2.proactive.scheduler.common.exception.SubmissionClosedException;
import org.ow2.proactive.scheduler.common.exception.UnknownJobException;
import org.ow2.proactive.scheduler.common.exception.UserException;
import org.ow2.proactive.scheduler.common.job.*;
import org.ow2.proactive.scheduler.common.task.JavaTask;
import org.ow2.proactive.scheduler.common.Scheduler;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.scheduler.common.SchedulerAuthenticationInterface;
import java.io.IOException;
import java.security.KeyException;
import java.util.Vector;
import org.ow2.proactive.authentication.crypto.Credentials;
import qosprober.exceptions.InvalidProtocolException;

/** 
 * Class that connects the test with the real scheduler, works as a stub. 
 * This is our interface to the remote Scheduler.
 * This class is specific for JAVAPA protocol. */
public class SchedulerStubProberJava {
	private static boolean POLLING = false;								// Waiting through polling mechanism? (otherwise event based).
	private static int POLLING_PERIOD_MS = 200;							// Polling period time (if polling is activated).
	private static Logger logger =
			Logger.getLogger(SchedulerStubProberJava.class.getName()); 	// Logger.
	private Scheduler schedulerStub; 									// Stub to the scheduler.
	
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
		logger.info("Joining the scheduler...");
        SchedulerAuthenticationInterface auth = SchedulerConnection.join(url);
        logger.info("Creating credentials...");
        Credentials cred = Credentials.createCredentials(new CredData(user, pass), auth.getPublicKey());
        logger.info("Logging in...");
        schedulerStub = auth.login(cred);
        SchedulerEventsListener aa = PAActiveObject.newActive(SchedulerEventsListener.class, new Object[]{}); 
        if (SchedulerStubProberJava.POLLING == false){
	        schedulerStub.addEventListener((SchedulerEventsListener) aa, true);
        }
	}
	
	
	/** 
	 * Submit a job to the scheduler. 
	 * @param name, name which will be seen by the administrator regarding this job.
	 * @param taskname, name of the class to be instantiated and executed as the task for this job. 
	 * @return and ID of the submitted job in case of success. 
	 */
	public String submitJob(String name, String taskname) throws NotConnectedException, PermissionException, SubmissionClosedException, JobCreationException, UserException{
		// Configuration of the job.
		TaskFlowJob job = new TaskFlowJob();
        job.setName(name);
        job.setPriority(
        		JobProber.DEFAULT_JOB_PRIORITY);// Configure the priority of the probe job.
        job.setCancelJobOnError(true);
        job.setDescription("Nagios plugin probe job.");
        JavaTask task = new JavaTask(); 		// Create the java task.
        task.setName("task"); 					// Add the task to the job.
        task.setExecutableClassName(taskname);	// Scpecify which class will be instantiated and executed as a task for this job.
        task.setPreciousResult(true);			
        job.addTask(task); 						// Add the task to the current job.
        
        // Submission of the job.
		JobId ret = schedulerStub.submit(job);	// Submit the job to the scheduler.
		
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
	 * @throws InterruptedException */
	public void waitUntilJobFinishes(String jobId) throws NotConnectedException, PermissionException, UnknownJobException, HttpException, IOException, InterruptedException{
		if (POLLING == false){
			do{
				synchronized(SchedulerStubProberJava.class){
				
					try {
						SchedulerStubProberJava.class.wait(); // This thread is blocked until the SchedulerEventsListener notifies of a new finished job.
					} catch (InterruptedException e) {
						logger.warn("Not supposed to happen...", e);
					}
				}
			
			}while(SchedulerEventsListener.checkIfJobIdHasJustFinished(jobId)==false);
		}else{
			boolean finished = false;
			do{
				Thread.sleep(POLLING_PERIOD_MS);
				JobStatus status = schedulerStub.getJobState(jobId).getStatus();
				logger.info("Waiting 'Finished' status for '" + status + "'.");
				finished = (status.equals(JobStatus.FINISHED));
			}while(finished == false);	
		}
	}


	/** 
	 * Wait for a job to be cleaned (removed or finished). 
	 * @param jobId, the ID of the job to wait for. 
	 * @throws InterruptedException */
	public void waitUntilJobIsCleaned(String jobId) throws NotConnectedException, PermissionException, UnknownJobException, HttpException, IOException, InterruptedException{
		if (POLLING == false){
			do{
				synchronized(SchedulerStubProberJava.class){
					
					try {
						SchedulerStubProberJava.class.wait(); // This thread is blocked until the SchedulerEventsListener notifies of a new removed job.
					} catch (InterruptedException e) {
						logger.warn("Not supposed to happen...", e);
					}
				}
				
			}while(
					SchedulerEventsListener.checkIfJobIdHasJustFinished(jobId) == false    && 
					SchedulerEventsListener.checkIfJobIdHasJustBeenRemoved(jobId) == false
					);
		}else{
			boolean cleaned = false;
			do{
				Thread.sleep(POLLING_PERIOD_MS);
				JobStatus status;
				try{
					status = schedulerStub.getJobState(jobId).getStatus();
					logger.info("Waiting 'Cleaned' status for '" + status + "'.");
				}catch(UnknownJobException e){
					cleaned = true;
				}
			}while(cleaned == false);
		}
	}

	
	/** 
	 * Return the status of the job (running, finished, etc.). 
	 * @param jobId, the ID of the job. 
	 * @return the status of the job. */
	public JobStatus getJobStatus(String jobId) throws NotConnectedException, PermissionException, UnknownJobException, HttpException, IOException{
		return schedulerStub.getJobState(jobId).getStatus();
	} /** 
	 * Kill and remove the job from the Scheduler. No leftovers of the job in the server.
	 * This is specially useful to delete the probe job, so we do not contaminate what the administrator sees.
	 * @param jobId, the ID of the job. */
	public void forceJobKillingAndRemoval(String jobId) throws Exception, NotConnectedException, UnknownJobException, PermissionException, InvalidProtocolException{
		schedulerStub.killJob(jobId);
		schedulerStub.removeJob(jobId);
	}
	
	
	/** 
	 * Remove the job from the Scheduler (only in case it is finished).
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
	
	/**
	 * Get a list of all jobs with the given name that are in pending, running and finished queues 
	 * of the scheduler (my jobs only). */
	public Vector<String> getAllCurrentJobsList(String jobname) throws NotConnectedException, PermissionException{
		
		logger.debug("\tGetting list of jobs...");
		
		if (jobname == null){
			throw new IllegalArgumentException("'name' argument cannot be null");
		}
		
		SchedulerState st = schedulerStub.getState(true);
		
		Vector<JobState> vector = new Vector<JobState>();
		
		vector.addAll(st.getPendingJobs());
		vector.addAll(st.getRunningJobs());
		vector.addAll(st.getFinishedJobs());
		
		Vector<String> jobs = new Vector<String>(); 
		
		for(JobState j: vector){
			logger.debug("\tcomparing " + jobname + " with " + j.getName() + "...");
			
			if (j.getName().equals(jobname) || jobname.equals("*")){
				jobs.add(j.getId().value());
				logger.debug("\t\tyes!");
			}else{
				logger.debug("\t\tno...");
			}
		}
		
		return jobs;
		
	}
}
