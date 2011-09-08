import javax.security.auth.login.LoginException;

import org.ow2.proactive.scheduler.common.exception.JobCreationException;
import org.ow2.proactive.scheduler.common.exception.NotConnectedException;
import org.ow2.proactive.scheduler.common.exception.PermissionException;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import org.ow2.proactive.scheduler.common.exception.SubmissionClosedException;
import org.ow2.proactive.scheduler.common.exception.UnknownJobException;
import org.ow2.proactive.scheduler.common.util.SchedulerProxyUserInterface;
import org.ow2.proactive.scheduler.common.job.Job;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobResult;

public class SchedulerProxyProber {
	
	private static SchedulerMonitorsHandler monitorsHandler;  
	private SchedulerProxyUserInterface schedulerproxy;
	
	public SchedulerProxyUserInterface getSchedulerproxy() {
		return schedulerproxy;
	}
	
	public void init(String url, String user, String pass) throws LoginException, SchedulerException{
		schedulerproxy = new SchedulerProxyUserInterface();
		System.out.println("Initializing...");
		schedulerproxy.init(url, user, pass);
	}
	
	public JobId submitJob(Job job) throws NotConnectedException, PermissionException, SubmissionClosedException, JobCreationException{
		return schedulerproxy.submit(job);
	}
	
	public JobResult getJobResult(JobId jobId) throws NotConnectedException, PermissionException, UnknownJobException{
		return schedulerproxy.getJobResult(jobId);

	}
	
	public void disconnect() throws NotConnectedException, PermissionException{	
		System.out.println("Disconnecting...");
		schedulerproxy.disconnect();
		// PAActiveObject.terminateActiveObject(true); Given by a technician
	}
	
	 private static SchedulerMonitorsHandler getMonitorsHandler() {
        if (monitorsHandler == null) {
            monitorsHandler = new SchedulerMonitorsHandler();
        }
        return monitorsHandler;
    }
	
}
