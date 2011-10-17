package qosprober.main;

import java.security.KeyException;

import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.resourcemanager.authentication.RMAuthentication;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.resourcemanager.frontend.RMConnection;
import org.ow2.proactive.resourcemanager.frontend.ResourceManager;
import org.ow2.proactive.utils.NodeSet;

/**
 * Class that represents locally the remote Resource Manager. 
 */
public class RMStubProber {

	public static Logger logger = Logger.getLogger(RMProber.class.getName()); // Logger.
	private ResourceManager rmStub; // ResourceManager locally.
	
	/**
	 * Initialize the connection with the remote Resource Manager.
	 * Uses the url of the RM, and the user/pass to login to it. 
	 */
	public void init(String url, String user, String pass) throws RMException, KeyException, LoginException{
    	logger.info("\tJoining the Resource Manager...");
        RMAuthentication auth = RMConnection.join(url); 	// Join the RM.
        logger.info("\tDone.");
        logger.info("\tCreating credentials...");
        Credentials cred = Credentials.createCredentials(new CredData(user, pass), auth.getPublicKey());
        logger.info("\tDone.");
        logger.info("\tLogging in...");
        rmStub = auth.login(cred);								// Login against the RM.
        logger.info("\tDone.");
        
        //RMEventsListener aa = PAActiveObject.newActive(RMEventsListener.class, new Object[]{}); 
        //rmStub.addEventListener((RMEventsListener) aa, true);
	}
	
	/**
	 * Get the given amount of nodes (or as many as possible) from the Resource Manager.
	 * @return the set with the nodes obtained.
	 */
	public NodeSet getNodes(int amountOfNodesRequired){
		NodeSet ns = rmStub.getAtMostNodes(amountOfNodesRequired, null);
		return ns;
	}
	
	/**
	 * Release the given set of nodes.
	 */
	public void releaseNodes(NodeSet setOfNodesToRelease){
		rmStub.releaseNodes(setOfNodesToRelease);
	}

	/**
	 * Disconnect from the Resource Manager.
	 */
	public void disconnect(){
		rmStub.disconnect();
	}
}