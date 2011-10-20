package qosprober.main;
import java.io.Serializable;

import qosprober.misc.Misc;

import org.apache.log4j.Logger;
import org.ow2.proactive.scheduler.common.NotificationData;
import org.ow2.proactive.scheduler.common.SchedulerEvent;
import org.ow2.proactive.scheduler.common.SchedulerEventListener;
import org.ow2.proactive.scheduler.common.job.JobInfo;
import org.ow2.proactive.scheduler.common.job.JobState;
import org.ow2.proactive.scheduler.common.job.UserIdentification;
import org.ow2.proactive.scheduler.common.task.TaskInfo;


/** 
 * Class that listens to events that happen in the remote Scheduler. */ 
public class SchedulerEventsListener implements SchedulerEventListener, Serializable{
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(SchedulerEventsListener.class.getName());// Logger.	
	private static final int lastSomethingJobsBufferSize = 200; 							 // Maximum amount of elements in the lastFinished/RemovedJobs array.
	public static String[] lastFinishedJobs = new String[lastSomethingJobsBufferSize]; 		 // List of last finished jobs.
	public static String[] lastRemovedJobs  = new String[lastSomethingJobsBufferSize]; 		 // List of last finished jobs.
	public static int currentCounterFinished = 0; 											 // Circular index.
	public static int currentCounterRemoved  = 0;
	
	/** 
	 * Check if the given job is in the list of last finished jobs. */
	public static synchronized boolean checkIfJobIdHasJustFinished(String jobId){
		logger.debug("\tChecking if " + jobId + " has already finished...");
		printListFinished();
		for (String j:lastFinishedJobs){
			if (j!=null && j.equals(jobId)){
				logger.debug("\t\tjobId " + jobId + " finished cleanly.");
				return true;
			}
		}
		logger.debug("\t\tjobId " + jobId + " not finished cleanly...");
		return false;
	}

	/** 
	 * Check if the given job is in the list of last removed jobs. */
	public static synchronized boolean checkIfJobIdHasJustBeenRemoved(String jobId){
		logger.debug("\tChecking if " + jobId + " has been already removed...");
		printListRemoved();
		for (String j:lastRemovedJobs){
			if (j!=null && j.equals(jobId)){
				logger.debug("\t\tjobId " + jobId + " removed.");
				return true;
			}
		}
		logger.debug("\t\tjobId " + jobId + " not removed...");
		return false;
	}

	
	/**
	 * Add a job to the list of last removed jobs. */
	public static synchronized void addRemovedJobId(String jobId){
		lastRemovedJobs[currentCounterRemoved] = jobId;
		currentCounterRemoved = (currentCounterRemoved + 1) % lastSomethingJobsBufferSize;
		printListRemoved();
	}

	
	/**
	 * Add a job to the list of last finished jobs. */
	public static synchronized void addFinishedJobId(String jobId){
		lastFinishedJobs[currentCounterFinished] = jobId;
		currentCounterFinished = (currentCounterFinished + 1) % lastSomethingJobsBufferSize ;
		printListFinished();
	}
	
	/**
	 * Print the list of last finished jobs. */
	private static synchronized void printListFinished(){
		logger.debug("\tLast finished jobs: " + Misc.getDescriptiveString((Object)lastFinishedJobs));
	}

	
	/**
	 * Print the list of last removed jobs. */
	private static synchronized void printListRemoved(){
		logger.debug("\tLast removed jobs: " + Misc.getDescriptiveString((Object)lastRemovedJobs));
	}

	
	/** 
	 * Interface SchedulerEventListener
	 * Notification of events that happen in the Scheduler.  
	 */

	
	public void jobStateUpdatedEvent(NotificationData<JobInfo> info) {
		String name = info.getData().getJobId().getReadableName();
		logger.info(">> Event " + name + ":" +  info.getData().getJobId().value() + " -> " + info.getEventType().toString());
		
		if (info.getEventType().equals(SchedulerEvent.JOB_RUNNING_TO_FINISHED)){
			/* If we receive a running-to-finished event for a job, we add this job to the 
			 * list of last finished jobs. */ 
			addFinishedJobId(info.getData().getJobId().value());
			synchronized(SchedulerStubProberJava.class){
				SchedulerStubProberJava.class.notifyAll();
			}
		} 
		
		if (info.getEventType().equals(SchedulerEvent.JOB_REMOVE_FINISHED)){
			/* If we receive a remove-finished event for a job, we add this job to the 
			 * list of last removed jobs. */ 
			addRemovedJobId(info.getData().getJobId().value());
			synchronized(SchedulerStubProberJava.class){
				SchedulerStubProberJava.class.notifyAll();
			}
		}
	}

	
	public void jobSubmittedEvent(JobState arg0) {}
	
	public void schedulerStateUpdatedEvent(SchedulerEvent arg0) {}
	
	public void taskStateUpdatedEvent(NotificationData<TaskInfo> arg0) {}
	
	public void usersUpdatedEvent(NotificationData<UserIdentification> arg0) {}
	
}
