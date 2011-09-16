import java.io.File;
import java.io.Serializable;
import java.net.URI;

import javax.security.auth.login.LoginException;

import misc.Misc;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.ProActiveTimeoutException;
import org.objectweb.proactive.core.config.ProActiveConfiguration;
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
import org.ow2.proactive.scheduler.common.Scheduler;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.scheduler.common.SchedulerAuthenticationInterface;
import java.io.IOException;
import org.apache.commons.httpclient.HttpException;
import java.security.KeyException;
import java.util.ArrayList;
import java.util.Date;

import org.ow2.proactive.authentication.crypto.Credentials;

import exceptions.InvalidProtocolException;


public class SchedulerStubProber{
	
	private static final long serialVersionUID = 1L;
	private Scheduler schedulerStub;
	private ProActiveProxyProtocol protocol = ProActiveProxyProtocol.UNKNOWN;
	private String sessionId = null;
	private URI uri;
	
	public SchedulerStubProber(){}
	
	public void init(String protocolStr, String url, String user, String pass) throws IllegalArgumentException, LoginException, SchedulerException, KeyException, ActiveObjectCreationException, NodeException, InvalidProtocolException, HttpException, IOException{
		protocol = ProActiveProxyProtocol.parseProtocol(protocolStr);
		if (protocol == ProActiveProxyProtocol.JAVAPA){ 
			
			System.out.println("Joining to the scheduler...");
	        SchedulerAuthenticationInterface auth = SchedulerConnection.waitAndJoin(url);
	        //SchedulerAuthenticationInterface auth = SchedulerConnection.join(url);
	        System.out.println("Creating credentials...");
	        Credentials cred = Credentials.createCredentials(new CredData(user, pass), auth.getPublicKey());
	        System.out.println("Logging in...");
	        schedulerStub = auth.login(cred);
	        SchedulerEventsListener aa = PAActiveObject.newActive(SchedulerEventsListener.class, new Object[]{}); 
	        schedulerStub.addEventListener((SchedulerEventsListener) aa, true);
		}else if (protocol == ProActiveProxyProtocol.REST){
		    uri = URI.create(url);

		    PostMethod methodLogin = new PostMethod(uri.toString() + "/login");
		    methodLogin.addParameter("username", user);
		    methodLogin.addParameter("password", pass);
		    HttpClient client = new HttpClient();
		    client.executeMethod(methodLogin);
		    
		    sessionId = methodLogin.getResponseBodyAsString();
		    System.out.println("Logged in with sessionId " + sessionId);

		}else{
			throw new InvalidProtocolException("Protocol " + protocolStr + " not supported.");
		}
	}
	
	public JobId submitJob(String jobpath) throws IOException, NotConnectedException, PermissionException, SubmissionClosedException, JobCreationException, InvalidProtocolException{
		Job job = JobFactory.getFactory().createJob(jobpath);
		if (protocol == ProActiveProxyProtocol.JAVAPA){
			return schedulerStub.submit(job);
		}else if (protocol == ProActiveProxyProtocol.REST){
			PostMethod method = new PostMethod(uri.toString() + "/submit");
			method.addRequestHeader("sessionid", sessionId);
			String stt = Misc.readAllFile(jobpath);
			System.out.println(stt);
			//method.setQueryString(stt);
			
			stt = new String(stt.getBytes(), "UTF8");
			method.setRequestEntity(new StringRequestEntity(stt, "HTTP_CONTENT_TYPE", "UTF8"));
		    HttpClient client = new HttpClient();
		   
		    client.executeMethod(method);
		    String response = method.getResponseBodyAsString();
		    System.out.println("Submitting job response: " + response);

			return null;
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		} 
	}
	
	public JobResult getJobResult(JobId jobId) throws NotConnectedException, PermissionException, UnknownJobException, InvalidProtocolException, HttpException, IOException{
		if (protocol == ProActiveProxyProtocol.JAVAPA){
			return schedulerStub.getJobResult(jobId);
		}else if (protocol == ProActiveProxyProtocol.REST){
		    GetMethod method = new GetMethod(uri.toString()+ "/jobs/" + jobId + "/result");
		    method.addRequestHeader("sessionid", sessionId);
		    HttpClient client = new HttpClient();
		    client.executeMethod(method);
		    //result = method.getResponseBodyAsString().length();
		    System.out.println(method.getResponseBodyAsString());
			return null;
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		}
	}
	
	public void waitUntilJobFinishes(JobId jobId, int timeoutms) throws NotConnectedException, PermissionException, UnknownJobException, InvalidProtocolException, HttpException, IOException{
		//jobWaited = jobId;
		//System.out.println ("We will wait for job: " + jobWaited); 
		if (protocol == ProActiveProxyProtocol.JAVAPA){
			long start = (new Date()).getTime();
			long end;
			boolean timeout;
			do{
				synchronized(SchedulerStubProber.class){
					//System.out.println("WAIT");
					try {
						SchedulerStubProber.class.wait(timeoutms);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					end = (new Date()).getTime();
					timeout = (end-start>=timeoutms);
					//if (timeout){
						//System.out.println("TIMEOUT");
					//}else{
						//System.out.println("NOTIFIED");
					//}
					
				}
				
			}while(SchedulerEventsListener.checkIfJobIdHasJustFinished(jobId)==false && timeout==false);

		}else if (protocol == ProActiveProxyProtocol.REST){
			// polling mechanism
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		}
	}

	
	public JobState getJobState(JobId jobId) throws NotConnectedException, PermissionException, UnknownJobException, InvalidProtocolException, HttpException, IOException{
		if (protocol == ProActiveProxyProtocol.JAVAPA){
			return schedulerStub.getJobState(jobId);
		}else if (protocol == ProActiveProxyProtocol.REST){
			
		    GetMethod method = new GetMethod(uri.toString()+ "/jobs/" + jobId);
		    method.addRequestHeader("sessionid", sessionId);
		    HttpClient client = new HttpClient();
		    client.executeMethod(method);
		    System.out.println(method.getResponseBodyAsString());
			
			return null;
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		}
	}

	
	public void removeJob(JobId jobId) throws NotConnectedException, UnknownJobException, PermissionException, InvalidProtocolException{
		if (protocol == ProActiveProxyProtocol.JAVAPA){	
			schedulerStub.removeJob(jobId);
		}else if (protocol == ProActiveProxyProtocol.REST){
			System.out.println("Not implemented");
			//throw new Exception("Not implemented.");
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		}
		
	}
	
	public void disconnect() throws NotConnectedException, PermissionException, InvalidProtocolException, HttpException, IOException{	
		if (protocol == ProActiveProxyProtocol.JAVAPA){	
			schedulerStub.disconnect();
			
		}else if (protocol == ProActiveProxyProtocol.REST){
			
		    PutMethod dismethod = new PutMethod(uri.toString() + "/disconnect");
		    dismethod.addRequestHeader("sessionid", sessionId);
		    HttpClient client = new HttpClient();
		    client.executeMethod(dismethod);
		    System.out.println(dismethod.getResponseBodyAsString());
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		}
	}
		
}
