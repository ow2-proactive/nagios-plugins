import java.io.Serializable;
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

import com.google.gson.Gson;


public class SchedulerStubProber implements SchedulerEventListener, Serializable{
	
	private static final long serialVersionUID = 1L;
	private Scheduler schedulerStub;
	private ProActiveProxyProtocol protocol = ProActiveProxyProtocol.UNKNOWN;
	public SchedulerStubProber(){}
	
	public void init(String protocolStr, String url, String user, String pass) throws IllegalArgumentException, LoginException, SchedulerException, KeyException, ActiveObjectCreationException, NodeException, InvalidProtocolException{
		protocol = ProActiveProxyProtocol.parseProtocol(protocolStr);
		if (protocol == ProActiveProxyProtocol.JAVAPA){ 
	        SchedulerAuthenticationInterface auth = SchedulerConnection.waitAndJoin(url);
	        Credentials cred = Credentials.createCredentials(new CredData(user, pass), auth.getPublicKey());
	        schedulerStub = auth.login(cred);
	        schedulerStub.addEventListener((SchedulerStubProber) PAActiveObject.getStubOnThis(), true);
		}else if (protocol == ProActiveProxyProtocol.REST){
			
		}else{
			throw new InvalidProtocolException("Protocol " + protocolStr + " not supported.");
		}
	}
	
	public JobId submitJob(Job job) throws NotConnectedException, PermissionException, SubmissionClosedException, JobCreationException, InvalidProtocolException{
		if (protocol == ProActiveProxyProtocol.JAVAPA){
			return schedulerStub.submit(job);
		}else if (protocol == ProActiveProxyProtocol.REST){
			return null;
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		}
	}
	
	public JobResult getJobResult(JobId jobId) throws NotConnectedException, PermissionException, UnknownJobException, InvalidProtocolException{
		if (protocol == ProActiveProxyProtocol.JAVAPA){
			return schedulerStub.getJobResult(jobId);
		}else if (protocol == ProActiveProxyProtocol.REST){
			return null;
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		}
	}
	
	public void tellTimeout(){
		System.out.println("Timeout reached...");	
	}
	
	public void disconnect() throws NotConnectedException, PermissionException, InvalidProtocolException{	
		System.out.println("Disconnecting...");
		if (protocol == ProActiveProxyProtocol.JAVAPA){	
			schedulerStub.disconnect();
		}else if (protocol == ProActiveProxyProtocol.REST){
			
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		}
	}
	
	/************************************/
	/* Interface SchedulerEventListener */
	/************************************/

	@Override
	public void jobStateUpdatedEvent(NotificationData<JobInfo> info) {
		System.out.println(">>Event " + info.getData() +  " " + info.getEventType().toString());
		if (info.getEventType().equals(SchedulerEvent.JOB_RUNNING_TO_FINISHED)){
			JobId jobId = info.getData().getJobId();
			System.out.println("Gettign job's result: " + jobId);
			JobResult jr = null;
			try {			
				jr = this.getJobResult(jobId);
			} catch (InvalidProtocolException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (NotConnectedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (PermissionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnknownJobException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("Job " + jobId + " Result: \n" + jr.toString());
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
	
	
	
	/************************************/
	/*           Main method            */
	/**
	 * @throws InvalidProtocolException **********************************/
	
	public static void main(String[] args) throws LoginException, SchedulerException, InterruptedException, ProActiveTimeoutException, IllegalArgumentException, KeyException, ActiveObjectCreationException, NodeException, InvalidProtocolException{
		/* All this information should come from a configuration file. */
		String url = "rmi://shainese.inria.fr:1099/";
		String user = "demo";
		String pass = "demo";
		String jobDescPath = "/user/mjost/home/Download/jobs/Job_2_tasks.xml";
		String protocol = "JAVAPA";
		int timeoutsec = 10;
		

		SchedulerStubProber obj = new SchedulerStubProber();
		Gson gson = new Gson();
		String json = gson.toJson(obj); 
		
		System.setProperty("java.security.policy","java.policy");
		
		SchedulerStubProber schedulerstub = PAActiveObject.newActive(SchedulerStubProber.class, new Object[]{});
		
		System.out.println("Connecting...");
		schedulerstub.init(protocol, url, user, pass);
		
		System.out.println("Creating jobs...");
		Job job = JobFactory.getFactory().createJob(jobDescPath);
		
		
		System.out.println("Submitting jobs...");
		JobId jobId = schedulerstub.submitJob(job);
		
		System.out.println("Waiting for jobs a time of " + timeoutsec + " seconds...");
		try{
			Thread.sleep(1000 * timeoutsec);
		}catch(InterruptedException e){e.printStackTrace();}
		
		schedulerstub.tellTimeout();
		
		schedulerstub.disconnect();
		
		System.out.println("Done.");
		
		System.exit(0);
	}
	
}
