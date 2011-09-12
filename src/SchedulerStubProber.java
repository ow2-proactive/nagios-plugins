import java.io.FileWriter;
import java.io.Serializable;
import java.util.ArrayList;

import javax.security.auth.login.LoginException;


import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.ProActiveTimeoutException;
import org.objectweb.proactive.core.node.NodeException;
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
import org.ow2.proactive.scheduler.common.job.*;
import org.ow2.proactive.scheduler.common.job.factories.JobFactory;
import org.springframework.util.Assert;
import org.ow2.proactive.scheduler.common.Scheduler;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.scheduler.common.SchedulerAuthenticationInterface;

import java.security.KeyException;
import org.ow2.proactive.authentication.crypto.Credentials;


public class SchedulerStubProber implements SchedulerEventListener, Serializable{

	private Scheduler scheduler;
	
	private ProActiveProxyProtocol protocol = ProActiveProxyProtocol.UNKNOWN;
	private ArrayList<JobWaiter> pendingJobWaiters;
	
	
	public SchedulerStubProber(){
		
	}
	
	public void init(String protocolStr, String url, String user, String pass) throws IllegalArgumentException, LoginException, SchedulerException, KeyException, ActiveObjectCreationException, NodeException{
		protocol = ProActiveProxyProtocol.parseProtocol(protocolStr);
		System.out.println("Initializing...");
		
        SchedulerAuthenticationInterface auth = SchedulerConnection.waitAndJoin(url);
        //2. get the user interface using the retrieved SchedulerAuthenticationInterface
        Credentials cred = Credentials.createCredentials(new CredData(user, pass), auth.getPublicKey());
        scheduler = auth.login(cred);

        //let the client be notified of its own 'job termination' -> job running to finished event
        scheduler.addEventListener((SchedulerStubProber) PAActiveObject.getStubOnThis(), true);
        //scheduler.addEventListener(this, true, SchedulerEvent.JOB_RUNNING_TO_FINISHED);
        System.out.println("Done initializing...");
		
        pendingJobWaiters = new ArrayList<JobWaiter>();
        		
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
		
		//System.out.println("\tStarted the waiter. Waiting for the waiter...");
		try {
			fjw.join();
		} catch (InterruptedException e) {
			/* Job finished before deadline. */
			/* This notification comes from jobStateUpdatedEvent */
			//System.out.println("\t Interrupted the Waiter. We assume job finished correctly.");
			beforedeadline = true;
		}
		
		if (beforedeadline == true){
			//System.out.println("[DD]Job " + jobId + " finished OK!!!!!!!");
			return;
		}else{	
			//System.out.println("[DD]Job " + jobId + " deadlined.");
			this.notifyTimedoutJob(jobId);
			throw new ProActiveTimeoutException("Timeout for job: " + jobId);
		}
		
	}
	
	public synchronized void notifyStartedJob(JobWaiter jobWaiter){
		//System.out.println(pendingJobWaiters.hashCode() + "   [DD] STARTa Job waiters size: " + pendingJobWaiters.size());
		pendingJobWaiters.add(jobWaiter);
		 
	}
	
	public synchronized void notifyFinishedJob(JobId jobId){
		/* Interrupt all the threads with the given jobId. */
		
		System.out.println("   [DD]Job finished: " + jobId);
		//System.out.println(pendingJobWaiters.hashCode() + "   [DD] FINISHEDa Job waiters size: " + pendingJobWaiters.size());
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
		pendingJobWaiters.removeAll(toRemove);
		
	}
	
	public synchronized void notifyTimedoutJob(JobId jobId){
		/* Interrupt all the threads with the given jobId. */
		System.out.println("   [DD]Job deadlined: " + jobId);
		//System.out.println(pendingJobWaiters.hashCode() + "   [DD] TIMEDOUTa Job waiters size: " + pendingJobWaiters.size());
		ArrayList<JobWaiter> toRemove = new ArrayList<JobWaiter>();
		for (JobWaiter jw: pendingJobWaiters){ 
			if (jw.getJobId().equals(jobId)){
				//jw.interrupt(); They finished.
				Assert.isTrue(!jw.isAlive());
				toRemove.add(jw);
			}
		}
		pendingJobWaiters.removeAll(toRemove);
		
	}
	
	/************************************/
	/* Interface SchedulerEventListener */
	/************************************/


	@Override
	public void jobStateUpdatedEvent(NotificationData<JobInfo> info) {
		System.out.println(">>Event " + info.getData() +  " " + info.getEventType().toString());
		if (info.getEventType().equals(SchedulerEvent.JOB_RUNNING_TO_FINISHED)){
			notifyFinishedJob(info.getData().getJobId());
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
	
	
	
	
	
	

	public static void main(String[] args) throws LoginException, SchedulerException, InterruptedException, ProActiveTimeoutException, IllegalArgumentException, KeyException, ActiveObjectCreationException, NodeException{
		
		System.setProperty("java.security.policy","java.policy");
		
		/* All this information should come from a configuration file. */
		String url = "rmi://shainese.inria.fr:1099/";
		String user = "demo";
		String pass = "demo";
		String jobDescPath = "/user/mjost/home/Download/jobs/Job_2_tasks.xml";
		String protocol = "JAVAPA";
		
		SchedulerStubProber schedulerstub = PAActiveObject.newActive(SchedulerStubProber.class, new Object[]{});
		
		System.out.println("Connecting...");
		schedulerstub.init(protocol, url, user, pass);
		//schedulerproxy = PAActiveObject.turnActive(schedulerproxy);
		
		System.out.println("Creating job...");
		Job job = null;
		try {
			job = JobFactory.getFactory().createJob(jobDescPath);
		} catch (JobCreationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		System.out.println("Submitting job...");
		JobId jobId = schedulerstub.submitJob(job);
		

		int waiting = 15;
		System.out.println("Waiting for job " + jobId + " a time of " + waiting + " seconds");

		try{
			schedulerstub.waitForEventJobFinished(jobId, waiting * 1000);
		}catch(ProActiveTimeoutException e){
			System.out.println("Timedout job " + jobId);
			e.printStackTrace();
		}
		
		//y despues ver por que no importa el timeout que pongas siempre despues del timeout vienen los eventos de job finalizado
		//proba tocar periodicamente el scheduler para ver si ahi te responde
		
		//System.out.println("Gettign job's result: " + jobId);
		//JobResult jr = schedulerproxy.getJobResult(jobId);
		
		//System.out.println("Job Result: \n" + jr.toString());
		
		schedulerstub.disconnect();
		
		System.out.println("Done.");
		
		System.exit(0);
	}
	
}
