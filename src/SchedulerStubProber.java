import java.io.FileNotFoundException;
import java.io.Serializable;
import java.net.URI;

import javax.security.auth.login.LoginException;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
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
import java.io.IOException;
import java.net.URI;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import java.security.KeyException;
import java.util.ArrayList;

import org.ow2.proactive.authentication.crypto.Credentials;

import com.google.gson.Gson;
import com.sun.xml.fastinfoset.Encoder;


public class SchedulerStubProber implements SchedulerEventListener, Serializable{
	
	private static final long serialVersionUID = 1L;
	private Scheduler schedulerStub;
	private ProActiveProxyProtocol protocol = ProActiveProxyProtocol.UNKNOWN;
	private String sessionId = null;
	private URI uri;
	
	public SchedulerStubProber(){}
	
	public void init(String protocolStr, String url, String user, String pass) throws IllegalArgumentException, LoginException, SchedulerException, KeyException, ActiveObjectCreationException, NodeException, InvalidProtocolException, HttpException, IOException{
		protocol = ProActiveProxyProtocol.parseProtocol(protocolStr);
		if (protocol == ProActiveProxyProtocol.JAVAPA){ 
	        SchedulerAuthenticationInterface auth = SchedulerConnection.waitAndJoin(url);
	        Credentials cred = Credentials.createCredentials(new CredData(user, pass), auth.getPublicKey());
	        schedulerStub = auth.login(cred);
	        schedulerStub.addEventListener((SchedulerStubProber) PAActiveObject.getStubOnThis(), true);
		}else if (protocol == ProActiveProxyProtocol.REST){
		    uri = URI.create(url);
		    long result;
		    
		    // login

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
	
	public void disconnect() throws NotConnectedException, PermissionException, InvalidProtocolException, HttpException, IOException{	
		System.out.println("Disconnecting...");
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
	
	/************************************/
	/* Interface SchedulerEventListener */
	/************************************/

	@Override
	public void jobStateUpdatedEvent(NotificationData<JobInfo> info) {
		//System.out.println(">>Event " + info.getData() +  " " + info.getEventType().toString());
		if (info.getEventType().equals(SchedulerEvent.JOB_RUNNING_TO_FINISHED)){
			JobId jobId = info.getData().getJobId();
			//System.out.println("Gettign job's result: " + jobId);
			JobResult jr = null;
			try {			
				try {
					jr = this.getJobResult(jobId);
				} catch (HttpException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
			//System.out.println("Job " + jobId + " Result: \n" + jr.toString());
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
	 * @throws InvalidProtocolException 
	 * @throws IOException 
	 * @throws HttpException 
	 * @throws ElementNotFoundException **********************************/
	
	public static void main(String[] args) throws LoginException, SchedulerException, InterruptedException, ProActiveTimeoutException, IllegalArgumentException, KeyException, ActiveObjectCreationException, NodeException, InvalidProtocolException, HttpException, IOException, ElementNotFoundException{
		/* All this information should come from a configuration file. */
		
		
		
		String conffile = Misc.readAllFile("conf.conf");
		ArrayList<String> confparts = Misc.filterEmptyLines(Misc.getLines(conffile));
		
		
		boolean debug = Boolean.parseBoolean(Misc.getValueUsingKey("debug", confparts));
		
		Misc.redirectStdOut(debug);
		
		String url = Misc.getValueUsingKey("url", confparts);
		String user = Misc.getValueUsingKey("user", confparts);
		String pass = Misc.getValueUsingKey("pass", confparts);
		String[] jobdescpaths = Misc.getValueUsingKey("jobdescpaths", confparts).split(";");
		String protocol = Misc.getValueUsingKey("protocol", confparts);
		int timeoutsec = Integer.parseInt(Misc.getValueUsingKey("timeoutsec", confparts));
		
		System.setProperty("java.security.policy","java.policy");
		
		SchedulerStubProber schedulerstub = PAActiveObject.newActive(SchedulerStubProber.class, new Object[]{});
		
		System.out.println("Connecting...");
		schedulerstub.init(protocol, url, user, pass);
		
		
		
		for(String jobpath: jobdescpaths){
			//System.out.println("Submitting job...");
			JobId jobId = schedulerstub.submitJob(jobpath);
		
			//System.out.println("Waiting for job " + jobId + " a time of " + timeoutsec + " seconds...");
			try{
				Thread.sleep(1000 * timeoutsec);
			}catch(InterruptedException e){e.printStackTrace();}
		
			JobResult jr = schedulerstub.getJobResult(jobId);
			//System.out.println("Finished period for job  " + jobId + ". Result: " + jr.toString());
		}
	
		
		schedulerstub.disconnect();
		
		Misc.log("Done1.");
		Misc.print("Done.");
		
		System.exit(0);
	}
	
}
