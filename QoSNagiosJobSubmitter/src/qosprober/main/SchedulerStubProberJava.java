/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */

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
import qosprobercore.exceptions.InvalidProtocolException;

/** 
 * Class that connects the test with the real scheduler, works as a stub. 
 * This is our interface to the remote Scheduler.
 * This class is specific for JAVAPA protocol. */
public class SchedulerStubProberJava {
	private boolean usePolling = false;									// Waiting through polling mechanism? (otherwise event based).
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
	public void init(String url, String user, String pass, boolean polling) throws IllegalArgumentException, LoginException, SchedulerException, KeyException, ActiveObjectCreationException, NodeException, HttpException, IOException{
		logger.info("Joining the scheduler at '" + url + "'...");
        SchedulerAuthenticationInterface auth = SchedulerConnection.join(url);
        logger.info("Done.");
        logger.info("Logging in...");
        Credentials cred = Credentials.createCredentials(new CredData(user, pass), auth.getPublicKey());
        schedulerStub = auth.login(cred);
        logger.info("Done.");
        logger.info("Completing connection...");
        SchedulerEventsListener aa = PAActiveObject.newActive(SchedulerEventsListener.class, new Object[]{}); 
        usePolling = polling;
        if (usePolling == false){
	        schedulerStub.addEventListener((SchedulerEventsListener) aa, true);
        }
        logger.info("Done.");
	}
	
	
	/** 
	 * Submit a job to the scheduler. 
	 * @param name, name which will be seen by the administrator regarding this job.
	 * @param taskname, name of the class to be instantiated and executed as the task for this job. 
	 * @return and ID of the submitted job in case of success. 
	 */
	public String submitJob(String name, String taskname, Boolean highpriority) throws NotConnectedException, PermissionException, SubmissionClosedException, JobCreationException, UserException{
		// Configuration of the job.
		logger.info("Submitting '" + name + "' job...");
		TaskFlowJob job = new TaskFlowJob();
        job.setName(name);
        job.setPriority(highpriority?JobPriority.HIGH:JobPriority.NORMAL);
        job.setCancelJobOnError(true);
        job.setDescription("Nagios plugin probe job.");
        JavaTask task = new JavaTask(); 		// Create the java task.
        task.setName("task"); 					// Add the task to the job.
        task.setExecutableClassName(taskname);	// Scpecify which class will be instantiated and executed as a task for this job.
        task.setPreciousResult(true);			
        job.addTask(task); 						// Add the task to the current job.
        
        // Submission of the job.
		JobId ret = schedulerStub.submit(job);	// Submit the job to the scheduler.
		
		logger.info("Done.");
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
		logger.info("Waiting for " + jobId + " job...");
		if (usePolling == false){
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
		logger.info("Done.");
	}


	/** 
	 * Wait for a job to be cleaned (removed or finished). 
	 * @param jobId, the ID of the job to wait for. 
	 * @throws InterruptedException */
	public void waitUntilJobIsCleaned(String jobId) throws NotConnectedException, PermissionException, UnknownJobException, HttpException, IOException, InterruptedException{
		if (usePolling == false){
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
		logger.info("Removing job "+ jobId + "...");
		schedulerStub.removeJob(jobId);
		logger.info("Done.");
	}
	
	
	/** 
	 * Disconnect from the Scheduler. */
	public void disconnect() throws NotConnectedException, PermissionException, HttpException, IOException{		
		logger.info("Disconnecting...");
		schedulerStub.disconnect();
		logger.info("Done.");
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

	// Removal of old probe jobs. 
	public void removeOldProbeJobs(String jobname, boolean deleteallold) throws UnknownJobException, InvalidProtocolException, Exception{
		Vector<String> schedulerjobs;
		if (deleteallold==true){
			logger.info("Removing ALL old jobs (that belong to this user)...");
			schedulerjobs = getAllCurrentJobsList("*");		// Get ALL jobs (no matter their name).
		}else{
			logger.info("Removing same-name old jobs...");
			schedulerjobs = getAllCurrentJobsList(jobname);	// Get all jobs with the same name as this probe job.
		}
		
		if (schedulerjobs.size()>0){
			logger.info("\tThere are old jobs...");
			for(String jobb:schedulerjobs){
				logger.info("\tRemoving old job with JobId " + jobb + "...");
				forceJobKillingAndRemoval(jobb);
				logger.info("\tWaiting until cleaned...");
				waitUntilJobIsCleaned(jobb); // Wait until either job's end or removal.
				logger.info("\tDone.");
			}
		}else{
			logger.info("\tThere are no old jobs...");
		}
		schedulerjobs = getAllCurrentJobsList(jobname);
		if (schedulerjobs.size()!=0){
			throw new Exception("ERROR (not possible to remove all previous '"+jobname+"' probe jobs in the scheduler)");
		}
		logger.info("Done.");
	}	
}
