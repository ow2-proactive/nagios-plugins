package qosprober.main;
import java.io.Serializable;

import qosprober.misc.Misc;

import org.apache.log4j.Logger;
import org.ow2.proactive.scheduler.common.NotificationData;
import org.ow2.proactive.scheduler.common.SchedulerEvent;
import org.ow2.proactive.scheduler.common.SchedulerEventListener;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobInfo;
import org.ow2.proactive.scheduler.common.job.JobState;
import org.ow2.proactive.scheduler.common.job.UserIdentification;
import org.ow2.proactive.scheduler.common.task.TaskInfo;


/** 
 * Class that listens to events that happen in the remote Scheduler. */ 
public class SchedulerEventsListener implements SchedulerEventListener, Serializable{
	private static Logger logger = Logger.getLogger(SchedulerEventsListener.class.getName()); 	// Logger.	
	private static final int lastFinishedJobsBufferSize = 100; 									// Maximum amount of elements in the lastFinishedJobs array.
	public static String[] lastFinishedJobs = new String[lastFinishedJobsBufferSize]; 			// List of last finished jobs. 
	public static int currentCounter = 0; 														// Circular index. 
	
	/** 
	 * Check if the given job is in the list of last finished jobs. */
	public static synchronized boolean checkIfJobIdHasJustFinished(String jobId){
		logger.info("Checking if " + jobId + " has already finished...");
		printList();
		for (String j:lastFinishedJobs){
			if (j!=null && j.equals(jobId)){
				logger.info("\t" + "yes");
				return true;
			}
		}
		logger.info("\t" + "no");
		return false;
	}
	
	/**
	 * Add a job to the list of last finished jobs. */
	public static synchronized void addFinishedJobId(String jobId){
		lastFinishedJobs[currentCounter] = jobId;
		currentCounter = (currentCounter + 1) % lastFinishedJobsBufferSize;
		printList();
	}
	
	/**
	 * Print the list of last finished jobs. */
	private static synchronized void printList(){
		logger.info("Last finished jobs: ");
		logger.info(Misc.getDescriptiveString((Object)lastFinishedJobs));
	}

	
	/************************************/
	/* Interface SchedulerEventListener */
	/************************************/

	/* Notification of events that happen in the Scheduler. */
	
	@Override
	public void jobStateUpdatedEvent(NotificationData<JobInfo> info) {
		logger.info(">>Event " + info.getData() +  " " + info.getEventType().toString());
		
		if (info.getEventType().equals(SchedulerEvent.JOB_RUNNING_TO_FINISHED)){
			/* If we receive a running-to-finished event for a job, we add this job to the 
			 * list of last finished jobs. */ 
			addFinishedJobId(info.getData().getJobId().value());
			synchronized(SchedulerStubProber.class){
				SchedulerStubProber.class.notifyAll();
			}
		} 
	}

	@Override
	public void jobSubmittedEvent(JobState arg0) {}
	@Override
	public void schedulerStateUpdatedEvent(SchedulerEvent arg0) {}
	@Override
	public void taskStateUpdatedEvent(NotificationData<TaskInfo> arg0) {}
	@Override
	public void usersUpdatedEvent(NotificationData<UserIdentification> arg0) {}
	
}
