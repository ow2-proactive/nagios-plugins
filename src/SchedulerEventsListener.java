import java.io.Serializable;
import java.util.ArrayList;

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
	public static ArrayList<JobId> lastFinishedJobs = new ArrayList<JobId>(100);
	public static int currentCounter = 0;
	
	/************************************/
	/* Interface SchedulerEventListener */
	/************************************/

	@Override
	public void jobStateUpdatedEvent(NotificationData<JobInfo> info) {
		System.out.println(">>Event " + info.getData() +  " " + info.getEventType().toString());
		if (info.getEventType().equals(SchedulerEvent.JOB_RUNNING_TO_FINISHED)){
			addFinishedJobId(info.getData().getJobId());
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
		for (JobId j:lastFinishedJobs){
			System.out.println("\t" + j + "==" + jobId);
			if (j.equals(jobId)){
				System.out.println("\t" + j + "==" + jobId + " YESS");
				return true;
			}
		}
		return false;
	}
	
	public static synchronized void addFinishedJobId(JobId jobId){
		lastFinishedJobs.add(currentCounter, jobId);
		currentCounter = (currentCounter + 1) % lastFinishedJobs.size(); 
	}
	
}
