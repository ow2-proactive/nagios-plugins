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
import javax.security.auth.login.LoginException;
import org.apache.log4j.Logger;
import org.objectweb.proactive.core.util.wrapper.BooleanWrapper;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.resourcemanager.authentication.RMAuthentication;
import org.ow2.proactive.resourcemanager.common.RMState;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.resourcemanager.frontend.RMConnection;
//import org.objectweb.proactive.core.node.Node;
import org.ow2.proactive.resourcemanager.frontend.ResourceManager;
import org.ow2.proactive.utils.NodeSet;

/**
 * Class that represents locally the remote Resource Manager. */
public class RMStubProber {

	public static Logger logger = Logger.getLogger(RMProber.class.getName()); 	// Logger.
	private ResourceManager rmStub; 											// ResourceManager locally.
	
	/**
	 * Initialize the connection with the remote Resource Manager.
	 * Uses the url of the RM, and the user/pass to login to it. */
	public void init(String url, String user, String pass) throws RMException, KeyException, LoginException{
    	logger.info("Joining the Resource Manager at '" + url + "'...");
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
	 * Get the given amount of nodes (or as many as possible) from the Resource Manager.
	 * @param amountOfNodesRequired nodes to be requested to the RM.
	 * @return the set with the nodes obtained. */
	public NodeSet getNodes(int amountOfNodesRequired){
    	logger.info("Getting nodes...");					
		NodeSet ns = rmStub.getAtMostNodes(amountOfNodesRequired, null);
		logger.info("\tNodes obtained: " + ns.size());
//		logger.info("\tListing nodes...");					// List the nodes obtained.
//    	for(Node n:ns){
//    		logger.info("\t\t - " + n.getNodeInformation().getName());
//    	}
//    	logger.info("\tDone.");
    	logger.info("Done.");
		return ns;
	}
	
	/**
	 * Release the given set of nodes. */
	public void releaseNodes(NodeSet setOfNodesToRelease){
    	logger.info("Releasing nodes...");					
		rmStub.releaseNodes(setOfNodesToRelease);
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
		BooleanWrapper ret = rmStub.disconnect();
    	logger.info("Done (returned '" + ret + "'.");	
	}
}