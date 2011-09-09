import java.io.Serializable;
import java.util.ArrayList;

import javax.security.auth.login.LoginException;


import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.api.PARemoteObject;
import org.objectweb.proactive.core.ProActiveException;
import org.objectweb.proactive.core.ProActiveTimeoutException;
import org.ow2.proactive.scheduler.common.NotificationData;
import org.ow2.proactive.scheduler.common.SchedulerConnection;
import org.ow2.proactive.scheduler.common.SchedulerEvent;
import org.ow2.proactive.scheduler.common.SchedulerEventListener;
import org.ow2.proactive.scheduler.common.exception.JobCreationException;
import org.ow2.proactive.scheduler.common.exception.NotConnectedException;
import org.ow2.proactive.scheduler.common.exception.PermissionException;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import org.ow2.proactive.scheduler.common.exception.SubmissionClosedException;
import org.ow2.proactive.scheduler.common.exception.UnknownJobException;
import org.ow2.proactive.scheduler.common.task.TaskInfo;
import org.ow2.proactive.scheduler.common.util.SchedulerProxyUserInterface;
import org.ow2.proactive.scheduler.common.job.*;
import org.springframework.util.Assert;
import org.ow2.proactive.scheduler.common.Scheduler;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.scheduler.common.SchedulerAuthenticationInterface;

import java.security.KeyException;
import java.security.PublicKey;
import org.ow2.proactive.authentication.crypto.Credentials;


public class SchedulerProxyProber implements Serializable{

	private Scheduler scheduler;
	
	private ProActiveProxyProtocol protocol = ProActiveProxyProtocol.UNKNOWN;
	private ArrayList<JobWaiter> pendingJobWaiters;
	
	
	public SchedulerProxyProber(String protocolStr, String url, String user, String pass) throws IllegalArgumentException, LoginException, SchedulerException, KeyException{
		protocol = ProActiveProxyProtocol.parseProtocol(protocolStr);
		
		System.out.println("Initializing...");
		
		CredData cred = new CredData(CredData.parseLogin(user), CredData.parseDomain(user), pass);
        SchedulerAuthenticationInterface auth = SchedulerConnection.join(url);
        PublicKey pubKey = auth.getPublicKey();
        Credentials crede = Credentials.createCredentials(cred, pubKey);
        scheduler = auth.login(crede);
		
		pendingJobWaiters = new ArrayList<JobWaiter>();
		
		JobEventListener gw = null;
		try {
			gw = PAActiveObject.newActive(JobEventListener.class, new Object[] {});
		} catch (ProActiveException e) {
			e.printStackTrace();
		}
		gw.setSchedulerProxyServer(this);
		
		scheduler.addEventListener(gw, true, SchedulerEvent.JOB_RUNNING_TO_FINISHED);
	}
	
	public Scheduler getScheduler(){
		return scheduler;
	}
	
	public JobId submitJob(Job job) throws NotConnectedException, PermissionException, SubmissionClosedException, JobCreationException{
		return scheduler.submit(job);
	}
	
	public JobResult getJobResult(JobId jobId) throws NotConnectedException, PermissionException, UnknownJobException{
		return scheduler.getJobResult(jobId);
	}
	
	public void disconnect() throws NotConnectedException, PermissionException{	
		System.out.println("Disconnecting...");
		scheduler.disconnect();
		// PAActiveObject.terminateActiveObject(true); Given by a technician
	}
		
	public void waitForEventJobFinished(JobId jobId, long timeoutms) throws ProActiveTimeoutException{
		
		JobWaiter fjw = new JobWaiter(jobId, timeoutms);
		
		this.notifyStartedJob(fjw);
		
		fjw.start();
		
		boolean beforedeadline = false ;
		
		System.out.println("[DD]Started the waiter. Waiting for the waiter...");
		try {
			fjw.join();
		} catch (InterruptedException e) {
			/* Job finished before deadline. */
			/* This notification comes from jobStateUpdatedEvent */
			System.out.println("[DD]Interrupted the Waiter. We assume job finished correctly.");
			beforedeadline = true;
			
		}
		
		if (beforedeadline == true){
			System.out.println("[DD]Job correct.");
			return;
		}else{	
			System.out.println("[DD]Job deadlined.");
			this.notifyTimedoutJob(jobId);
			throw new ProActiveTimeoutException("Timeout for job: " + jobId);
		}
		
		/* Here we put a thread to sleep during the timeoutms.
		 * We add this threads to a list of threads that are waiting for a particular job. 
		 * The thread knows about the jobId of the job that is being waited for itself.
		 * 		If that jobId is notified to be done, then the particular thread is picked up from the set and interrupted. 
		 * 		Otherwise the thread finishes its sleeping period, removes itself from the set, and throws an exception. 
		 * 
		 * 
		 * NOTE: we execute the thread and we do join immediately after.
		 * */
		
		
	}
	
	public synchronized void notifyStartedJob(JobWaiter jobWaiter){
		System.out.println(pendingJobWaiters.hashCode() + "   [DD] STARTa Job waiters size: " + pendingJobWaiters.size());
		pendingJobWaiters.add(jobWaiter);
		System.out.println(pendingJobWaiters.hashCode() + "   [DD] STARTb Job waiters size: " + pendingJobWaiters.size()); 
	}
	
	public synchronized void notifyFinishedJob(JobId jobId){
		/* Interrupt all the threads with the given jobId. */
		
		System.out.println("   [DD]Job finished: " + jobId);
		System.out.println(pendingJobWaiters.hashCode() + "   [DD] FINISHEDa Job waiters size: " + pendingJobWaiters.size());
		ArrayList<JobWaiter> toRemove = new ArrayList<JobWaiter>();
		for (JobWaiter jw: pendingJobWaiters){
			System.out.println("     Comparing " + jw.getJobId() + " " + jobId);
			if (jw.getJobId().equals(jobId)){
				System.out.println("Waiter " + jw.getJobId() + " interrupted.");
				jw.interrupt();
				toRemove.add(jw);
			}else{
				System.out.println("     FALSE Comparing " + jw.getJobId() + " " + jobId);
			}
		}
		//pendingJobWaiters.removeAll(toRemove);
		System.out.println(pendingJobWaiters.hashCode() + "   [DD] FINISHEDb Job waiters size: " + pendingJobWaiters.size());
	}
	
	public synchronized void notifyTimedoutJob(JobId jobId){
		/* Interrupt all the threads with the given jobId. */
		System.out.println("   [DD]Job deadlined: " + jobId);
		System.out.println(pendingJobWaiters.hashCode() + "   [DD] TIMEDOUTa Job waiters size: " + pendingJobWaiters.size());
		ArrayList<JobWaiter> toRemove = new ArrayList<JobWaiter>();
		for (JobWaiter jw: pendingJobWaiters){ 
			if (jw.getJobId().equals(jobId)){
				//jw.interrupt(); They finished.
				Assert.isTrue(!jw.isAlive());
				toRemove.add(jw);
			}
		}
		//pendingJobWaiters.removeAll(toRemove);
		System.out.println(pendingJobWaiters.hashCode() + "   [DD] TIMEDOUTb Job waiters size: " + pendingJobWaiters.size());
	}
	
	/************************************/
	/* Interface SchedulerEventListener */
	/************************************/

}
