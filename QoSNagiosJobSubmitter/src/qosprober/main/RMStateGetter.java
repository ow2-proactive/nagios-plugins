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
	
	/**
	 * Initialize the connection with the remote Resource Manager.
	 * Uses the url of the RM, and the user/pass to login to it. */
	public void init(String url, String user, String pass) throws RMException, KeyException, LoginException{
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
	public RMState getRMState(){
		return rmStub.getState();
	}

	/**
	 * Disconnect from the Resource Manager. */
	public void disconnect(){
    	logger.info("Disconnecting...");					// Disconnecting from RM.
		rmStub.disconnect();
    	logger.info("Done.");	
	}
	
	public void performQuery(final String url, final String user, final String pass) throws Exception{
		Callable<RMState> task = new Callable<RMState>(){
			public RMState call() throws Exception{
				init(url, user, pass);
				RMState ret = getRMState();
				disconnect();
				return ret;
			}
		};
		ExecutorService executor = Executors.newFixedThreadPool(1);
		future = executor.submit(task);
	}
	
	public RMState getQueryResult(){
		if (future == null){
			throw new RuntimeException("Cannot get any result, before you have to performQuery(...)...");
		}
		
		RMState ret = null;
		try {
			ret = future.get();
	    	logger.info("RMState obtained OK.");
		} catch (InterruptedException e) {
	    	logger.info("Problem getting RMState...", e);
		} catch (ExecutionException e) {
	    	logger.info("Problem getting RMState...", e);
		}
		return ret;
	}
	
}