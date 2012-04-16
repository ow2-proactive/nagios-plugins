/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */

package org.ow2.proactive.nagios.probes.rest;

import java.net.URI;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethodBase;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.PutMethod;
import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;

/** 
 * Class that connects the test with the real scheduler, works as a stub. 
 * This is our interface to the remote Scheduler.
 * The interaction with the Scheduler is done using the specified protocol, either JAVAPA (Java ProActive) or REST. 
 * This class is specific for REST protocol. */
public class RestStubProber{
	
	private static Logger logger = Logger.getLogger(RestStubProber.class.getName()); 	// Logger.
	
	/** REST attributes. */
	private String sessionId = null; 					// For the REST protocol, it defines the ID of the session.
	private URI uri; 									// It defines the URI used as a suffix to get the final URL for the REST server.
	
	/**
	 * Constructor method. */
	public RestStubProber(){}
	
	/** 
	 * Initialize the connection/session with the scheduler.
	 * @param url url of the scheduler for REST API. 
	 * @throws IOException 
	 * @throws HttpException */
	public void connect(String url) throws HttpException, IOException {
		if (url.endsWith("/")){
			url = url.substring(0, url.length()-1);
		}
	    logger.info("Connecting at '" + url + "'...");
	    uri = URI.create(url);
	    logger.info("Done.");
	}
	
	/** 
	 * Initialize the connection/session with the scheduler.
	 * @param user username to access the scheduler.
	 * @param pass password to access the scheduler. 
	 * @throws IOException 
	 * @throws HttpException */
	public void login(String user, String pass) throws HttpException, IOException {
	    logger.info("Login...");
	    PostMethod methodLogin = new PostMethod(uri.toString() + "/login");
	    methodLogin.addParameter("username", user);
	    methodLogin.addParameter("password", pass);
	    HttpClient client = new HttpClient();
	    client.executeMethod(methodLogin);
	    sessionId = getResponseBodyAsString(methodLogin, 1024);
	    logger.info("Logged in with sessionId: " + sessionId);
	    logger.info("Done.");
	}
	
	/**
	 * Get a boolean telling if the prober is still connected to the scheduler through REST API or not. 
	 * @return a boolean telling if we are connected to the scheduler. 
	 * @throws IOException 
	 * @throws HttpException */
	public Boolean isConnected() throws HttpException, IOException{
	    logger.info("Asking if connected...");
	    GetMethod method = new GetMethod(uri.toString()+  "/isconnected");
	    method.addRequestHeader("sessionid", sessionId);
	    HttpClient client = new HttpClient();
	    client.executeMethod(method);
	    String response = getResponseBodyAsString(method, 1024);
	    logger.info("IsConnected result: " + response);
	    logger.info("Done.");
		return Boolean.parseBoolean(response);
	}
	
	/**
	 * Get the version of the REST API. 
	 * @return the version. 
	 * @throws IOException 
	 * @throws HttpException */
	public String getVersion() throws HttpException, IOException{
	    logger.info("Asking version...");
	    GetMethod method = new GetMethod(uri.toString()+  "/version");
	    if (sessionId != null){
		    method.addRequestHeader("sessionid", sessionId);
	    }
	    HttpClient client = new HttpClient();
	    client.executeMethod(method);
	    String response = getResponseBodyAsString(method, 1024);
	    logger.info("Version result: " + response);
	    logger.info("Done.");
		return response;
	}

	/** 
	 * Disconnect from the Scheduler. 
	 * @throws IOException 
	 * @throws HttpException */
	public void disconnect() throws HttpException, IOException{	
	    logger.info("Disconnecting...");
	    PutMethod method = new PutMethod(uri.toString() + "/disconnect");
	    if (sessionId != null){
		    method.addRequestHeader("sessionid", sessionId);
	    }
	    HttpClient client = new HttpClient();
	    client.executeMethod(method);
	    logger.info("Done.");
	}

	/** 
	 * Get the Response Body of the method, as a String.
	 * @param method method from where to extract the ResponseBody. 
	 * @param maximumlength maximum number of characters to accept retrieve. 
	 * @return the Response Body as a String. 
	 * @throws IOException 
	 * @throws HttpException */
	private String getResponseBodyAsString(HttpMethodBase method, int maximumlength) throws IOException{
		InputStream response = method.getResponseBodyAsStream();
	    byte[] buffer = new byte[maximumlength];
	    int total = response.read(buffer);
	    if (total > 0){
	    	return new String(buffer,0,total);
	    }else{
	    	return "<Nothing returned>";
	    }
	}
}
