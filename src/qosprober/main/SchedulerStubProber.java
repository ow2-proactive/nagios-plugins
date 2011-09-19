package qosprober.main;

import java.net.URI;

import javax.security.auth.login.LoginException;

import qosprober.misc.Misc;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.api.PAActiveObject;
import org.objectweb.proactive.core.node.NodeException;
import org.ow2.proactive.scheduler.common.SchedulerConnection;
import org.ow2.proactive.scheduler.common.exception.JobCreationException;
import org.ow2.proactive.scheduler.common.exception.NotConnectedException;
import org.ow2.proactive.scheduler.common.exception.PermissionException;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import org.ow2.proactive.scheduler.common.exception.SubmissionClosedException;
import org.ow2.proactive.scheduler.common.exception.UnknownJobException;
import org.ow2.proactive.scheduler.common.job.*;
import org.ow2.proactive.scheduler.common.job.factories.JobFactory;
import org.ow2.proactive.scheduler.common.Scheduler;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.scheduler.common.SchedulerAuthenticationInterface;
import java.io.IOException;

import java.security.KeyException;
import java.util.Date;

import org.ow2.proactive.authentication.crypto.Credentials;

import qosprober.exceptions.InvalidProtocolException;


public class SchedulerStubProber{
	
	private static Logger logger = Logger.getLogger(SchedulerStubProber.class.getName());	
	private Scheduler schedulerStub;
	private ProActiveProxyProtocol protocol = ProActiveProxyProtocol.UNKNOWN;
	private String sessionId = null;
	private URI uri;
	
	public SchedulerStubProber(){}
	
	public void init(String protocolStr, String url, String user, String pass) throws IllegalArgumentException, LoginException, SchedulerException, KeyException, ActiveObjectCreationException, NodeException, InvalidProtocolException, HttpException, IOException{
		protocol = ProActiveProxyProtocol.parseProtocol(protocolStr);
		if (protocol == ProActiveProxyProtocol.JAVAPA){ 
			
			logger .info("Joining to the scheduler...");
	        SchedulerAuthenticationInterface auth = SchedulerConnection.waitAndJoin(url);
	        logger .info("Creating credentials...");
	        Credentials cred = Credentials.createCredentials(new CredData(user, pass), auth.getPublicKey());
	        logger .info("Logging in...");
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
		    logger.info("Logged in with sessionId " + sessionId);

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
			
			stt = new String(stt.getBytes(), "UTF8");
			method.setRequestEntity(new StringRequestEntity(stt, "HTTP_CONTENT_TYPE", "UTF8"));
		    HttpClient client = new HttpClient();
		   
		    client.executeMethod(method);
		    String response = method.getResponseBodyAsString();
		    logger.info("Submitting job response: " + response);

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
			return null;
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		}
	}
	
	public void waitUntilJobFinishes(JobId jobId, int timeoutms) throws NotConnectedException, PermissionException, UnknownJobException, InvalidProtocolException, HttpException, IOException{

		if (protocol == ProActiveProxyProtocol.JAVAPA){
			long start = (new Date()).getTime();
			long end;
			boolean timeout;
			do{
				synchronized(SchedulerStubProber.class){
					
					try {
						SchedulerStubProber.class.wait(timeoutms);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					end = (new Date()).getTime();
					timeout = (end-start>=timeoutms);
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
			return null;
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		}
	}

	
	public void removeJob(JobId jobId) throws NotConnectedException, UnknownJobException, PermissionException, InvalidProtocolException{
		if (protocol == ProActiveProxyProtocol.JAVAPA){	
			schedulerStub.removeJob(jobId);
		}else if (protocol == ProActiveProxyProtocol.REST){
			logger.error("Not implemented");
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
		}else{
			throw new InvalidProtocolException("Invalid protocol selected.");
		}
	}
		
}