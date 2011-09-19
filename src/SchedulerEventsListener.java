import java.io.Serializable;
import java.util.ArrayList;

import misc.Misc;

import org.apache.log4j.Logger;
import org.ow2.proactive.scheduler.common.NotificationData;
import org.ow2.proactive.scheduler.common.SchedulerEvent;
import org.ow2.proactive.scheduler.common.SchedulerEventListener;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobInfo;
import org.ow2.proactive.scheduler.common.job.JobState;
import org.ow2.proactive.scheduler.common.job.UserIdentification;
import org.ow2.proactive.scheduler.common.task.TaskInfo;


public class SchedulerEventsListener implements SchedulerEventListener, Serializable{

	private static final long serialVersionUID = 1L;
	
	private static final int lastFinishedJobsBufferSize = 100;
	public static String[] lastFinishedJobs = new String[lastFinishedJobsBufferSize];
	public static int currentCounter = 0;
	
	/************************************/
	/* Interface SchedulerEventListener */
	/************************************/

	@Override
	public void jobStateUpdatedEvent(NotificationData<JobInfo> info) {
		Logger.getRootLogger().info(">>Event " + info.getData() +  " " + info.getEventType().toString());
		if (info.getEventType().equals(SchedulerEvent.JOB_RUNNING_TO_FINISHED)){
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
	
	public static synchronized boolean checkIfJobIdHasJustFinished(JobId jobId){
		Logger.getRootLogger().info("Checking if " + jobId.value() + " has already finished...");
		printList();
		for (String j:lastFinishedJobs){
			if (j!=null && j.equals(jobId.value())){
				Logger.getRootLogger().info("\t" + "yes");
				return true;
			}
		}
		Logger.getRootLogger().info("\t" + "no");
		return false;
	}
	
	public static synchronized void addFinishedJobId(String jobId){
		lastFinishedJobs[currentCounter] = jobId;
		currentCounter = (currentCounter + 1) % lastFinishedJobsBufferSize;
		printList();
	}
	private static synchronized void printList(){
		Logger.getRootLogger().info("Last finished jobs: ");
		Logger.getRootLogger().info(Misc.getDescriptiveString((Object)lastFinishedJobs));
	}
	
}
