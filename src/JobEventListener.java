import java.io.Serializable;

import org.ow2.proactive.scheduler.common.NotificationData;
import org.ow2.proactive.scheduler.common.SchedulerEvent;
import org.ow2.proactive.scheduler.common.SchedulerEventListener;
import org.ow2.proactive.scheduler.common.job.JobInfo;
import org.ow2.proactive.scheduler.common.job.JobState;
import org.ow2.proactive.scheduler.common.job.UserIdentification;
import org.ow2.proactive.scheduler.common.task.TaskInfo;


public class JobEventListener implements SchedulerEventListener, Serializable{
	private SchedulerProxyProber spp;
	
	public JobEventListener(){
		
	}
	public void setSchedulerProxyServer(SchedulerProxyProber spp){
		this.spp = spp;
	}
	@Override
	public void jobStateUpdatedEvent(NotificationData<JobInfo> arg0) {
		if (arg0.getEventType().equals(SchedulerEvent.JOB_RUNNING_TO_FINISHED)){
			spp.notifyFinishedJob(arg0.getData().getJobId());
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
