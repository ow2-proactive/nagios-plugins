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

package qosprober.main;

import java.security.KeyException;
import java.util.concurrent.*;

import javax.security.auth.login.LoginException;
import org.apache.log4j.Logger;
import org.objectweb.proactive.core.util.wrapper.BooleanWrapper;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.resourcemanager.authentication.RMAuthentication;
import org.ow2.proactive.resourcemanager.common.RMState;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.resourcemanager.frontend.RMConnection;
import org.ow2.proactive.resourcemanager.frontend.ResourceManager;

/**
 * Class that represents locally the remote Resource Manager. */
public class RMStateGetter {

	public static Logger logger = Logger.getLogger(RMStateGetter.class.getName()); 	// Logger.
	private ResourceManager rmStub; 												// ResourceManager locally.
	private Future<RMState> future;
	
	public RMStateGetter(String url, String user, String pass){
		logger.info("Executing RMState check...");
		if (url.startsWith("pamr:")){
			url = "pamr://0";	
		}
		final String url1 = url;
		final String user1 = user;
		final String pass1 = pass;
		Callable<RMState> task = new Callable<RMState>(){
			public RMState call() throws Exception{
				init(url1, user1, pass1);
				RMState ret = getRMState();
				disconnect();
				return ret;
			}
		};
		ExecutorService executor = Executors.newFixedThreadPool(1);
		future = executor.submit(task);
	}
	
	/**
	 * Initialize the connection with the remote Resource Manager.
	 * Uses the url of the RM, and the user/pass to login to it. */
	private void init(String url, String user, String pass) throws RMException, KeyException, LoginException{
    	logger.info("Joining the Resource Manager...");
        RMAuthentication auth = RMConnection.join(url); 	// Join the RM.
        logger.info("Done.");
        logger.info("Creating credentials...");
        Credentials cred = Credentials.createCredentials(new CredData(user, pass), auth.getPublicKey());
        logger.info("Done.");
        logger.info("Logging in...");
        rmStub = auth.login(cred);								// Login against the RM.
        logger.info("Done.");
	}
	
	/**
	 * Release the given set of nodes.
	 * @return state of the RM. */
	private RMState getRMState(){
    	logger.info("Getting state...");
		RMState ret = rmStub.getState();
    	logger.info("Done.");
		return ret;
	}

	/**
	 * Disconnect from the Resource Manager. */
	private void disconnect(){
    	logger.info("Disconnecting...");					// Disconnecting from RM.
		BooleanWrapper ret = rmStub.disconnect();
    	logger.info("Done (returned '" + ret + "'.");	
	}
	
	public RMState getQueryResult(){
		if (future == null){
			throw new RuntimeException("Cannot get any result, before you have to performQuery(...)...");
		}
		
		RMState ret = null;
		try {
			if (future.isDone() == true){
		    	logger.info("RMState was ready... Getting it...");
				ret = future.get();
		    	logger.info("RMState obtained OK.");
			}else{
		    	logger.info("RMState was not ready... Avoiding...");
			}
		} catch (Exception e) {
	    	logger.info("Problem getting RMState...", e);
		}
		return ret;
	}
	
}