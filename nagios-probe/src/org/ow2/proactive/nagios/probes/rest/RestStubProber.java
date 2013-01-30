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
import java.security.InvalidParameterException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import org.apache.log4j.Logger;
import java.io.IOException;
import java.io.InputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.HttpParams;
import org.apache.commons.codec.binary.StringUtils;
import org.apache.commons.io.IOUtils;

/** 
 * Class that connects the test with the real scheduler, works as a stub. 
 * This is our interface to the remote Scheduler.
 * The interaction with the Scheduler is done using the specified protocol, either JAVAPA (Java ProActive) or REST. 
 * This class is specific for REST protocol. */
public class RestStubProber{
	private static final int OK = 200;
	private static final int OKNONOTIF = 204;
	private static Logger logger = Logger.getLogger(RestStubProber.class.getName()); 	// Logger.
	
	/** REST attributes. */
	private String sessionId = null; 					// For the REST protocol, it defines the ID of the session.
	private URI uri; 									// It defines the URI used as a suffix to get the final URL for the REST server.
	private boolean skipauthentication;					// If true, no https authentication is checked.
	
	/**
	 * Constructor method. */
	public RestStubProber(boolean skipauthentication){
		this.skipauthentication = skipauthentication;
	}
	
	/** 
	 * Initialize the connection/session with the scheduler.
	 * @param url url of the scheduler for REST API. 
	 * @throws IOException 
	 * @throws HttpException */
	public void connect(String url) throws IOException {
		if (url.endsWith("/")){
			url = url.substring(0, url.length()-1);
		}
	    uri = URI.create(url);
	    logger.info("Connecting at '" + uri + "'...");
	    logger.info("Done.");
	}
	
	/** 
	 * Initialize the connection/session with the scheduler.
	 * @param user username to access the scheduler.
	 * @param pass password to access the scheduler. 
	 * @throws IllegalStateException 
	 * @throws Exception 
	 * @throws IOException 
	 * @throws HttpException */
    public void login(String user, String pass) throws Exception {
    	String loginurl = uri.toString() + "/login";
	    logger.info("Logging to '" + loginurl + "' with user '" + user + "'...");
        HttpPost request = new HttpPost(loginurl);
        StringEntity entity = new StringEntity("username=" + user + "&password=" + pass, ContentType.APPLICATION_FORM_URLENCODED);
        request.setEntity(entity);
        HttpResponse response = execute(request);
        chechResponseIsOK(response, "Bad response for method POST on '" + request.getURI().toString() + "'.");
        sessionId = getStringFromResponse(response);
	    logger.info("Done.");
    }

	/**
	 * Get a boolean telling if the prober is still connected to the scheduler through REST API or not. 
	 * @return a boolean telling if we are connected to the scheduler. 
	 * @throws Exception */
	public Boolean isConnected() throws Exception{
	    logger.info("Checking if connected...");
        HttpGet request = new HttpGet(uri.toString() + "/isconnected");
        HttpResponse response = execute(request);
        String responsestr = getStringFromResponse(response);
        chechResponseIsOK(response, "Bad response for method GET on '" + request.getURI().toString() + "'.");
	    logger.info("IsConnected result: " + responsestr);
	    logger.info("Done.");
		return Boolean.parseBoolean(responsestr);
	}
	
	/**
	 * Get the version of the REST API. 
	 * @return the version. 
	 * @throws Exception */
	public String getVersion() throws Exception{
	    logger.info("Getting version...");
        HttpGet request = new HttpGet(uri.toString() + "/version");
        HttpResponse response = execute(request);
        chechResponseIsOK(response, "Bad response for method GET on '" + request.getURI().toString() + "'.");
        String responsestr = getStringFromResponse(response);
	    logger.info("Version result: " + responsestr);
	    logger.info("Done.");
		return responsestr;
	}

	/**
	 * Perform a standard GET. 
	 * @return the value. 
	 * @throws Exception */
	public String get(String resource) throws Exception{
	    logger.info("Asking for " + uri.toString() + resource);
        HttpGet request = new HttpGet(uri.toString() + resource);
        HttpResponse response = execute(request);
        chechResponseIsOK(response, "Bad response for method GET on '" + request.getURI().toString() + "'.");
        String responsestr = getStringFromResponse(response);
	    logger.info("Result: " + responsestr);
	    logger.info("Done.");
		return responsestr;
	}

	/** 
	 * Disconnect from the Scheduler. 
	 * @throws IOException 
	 * @throws HttpException */
	public void disconnect() throws Exception{	
	    logger.info("Disconnecting...");
        HttpPut request = new HttpPut(uri.toString() + "/disconnect");
        HttpResponse response = execute(request);
        chechResponseIsOK(response, "Bad response for method PUT on '" + request.getURI().toString() + "'.");
	    logger.info("Done.");
	}

	private static class RelaxedTrustStrategy implements TrustStrategy {
        @Override
        public boolean isTrusted(X509Certificate[] arg0, String arg1)
                throws CertificateException {
            return true;
        }
    }
	
   protected static HttpClient threadSafeClient() {
        DefaultHttpClient client = new DefaultHttpClient();
        ClientConnectionManager mgr = client.getConnectionManager();
        HttpParams params = client.getParams();
        client = new DefaultHttpClient(new PoolingClientConnectionManager(
                mgr.getSchemeRegistry()), params);
        return client;
    }
 
   public static void setInsecureAccess(HttpClient client)
            throws KeyManagementException, UnrecoverableKeyException,
            NoSuchAlgorithmException, KeyStoreException {

        SSLSocketFactory socketFactory = new SSLSocketFactory(
                new RelaxedTrustStrategy(),
                SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        Scheme https = new Scheme("https", 443, socketFactory);
        client.getConnectionManager().getSchemeRegistry().register(https);
    }
    
    private HttpResponse execute(HttpUriRequest request) throws Exception {
        if (sessionId != null) {
            request.setHeader("sessionId", sessionId);
        }
        HttpClient client = threadSafeClient();
        try {
            if (skipauthentication == true) {
                setInsecureAccess(client);
            }
            HttpResponse response = client.execute(request);
            return response;
        } catch (Exception e) {
            throw e;
        } finally {
            ((HttpRequestBase) request).releaseConnection();
        }
    }	
	
	private String getStringFromResponse(HttpResponse response) throws IllegalStateException, IOException{
        InputStream inputStream = response.getEntity().getContent();
        byte[] buffer = IOUtils.toByteArray(inputStream);
        String responsestr = StringUtils.newStringUtf8(buffer);
        return responsestr;
	}
	
	private void chechResponseIsOK(HttpResponse response, String errormsg) throws HttpResponseException{
		if (response != null){
			int returnedcode = response.getStatusLine().getStatusCode() ;
			if (returnedcode != OK && returnedcode != OKNONOTIF){
				throw new HttpResponseException(returnedcode, 
						errormsg + " Reason: '" + response.getStatusLine().getReasonPhrase() + "' ("+returnedcode+").");
			}
		}else{
			throw new InvalidParameterException("The response parameter cannot be null.");
		}
	}
}
