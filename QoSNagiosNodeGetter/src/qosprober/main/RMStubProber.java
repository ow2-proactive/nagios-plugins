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



public class RMStubProber {

	public static Logger logger = Logger.getLogger(RMProber.class.getName()); // Logger.
	
	private ResourceManager rmStub;
	
	public void init(String url, String user, String pass) throws RMException, KeyException, LoginException{
    	logger.info("Joining the Resource Manager...");
        RMAuthentication auth = RMConnection.waitAndJoin(url);
        logger.info("Creating credentials...");
        Credentials cred = Credentials.createCredentials(new CredData(user, pass), auth.getPublicKey());
        logger.info("Logging in...");
        rmStub = auth.login(cred);
        
        //RMEventsListener aa = PAActiveObject.newActive(RMEventsListener.class, new Object[]{}); 
        //rmStub.addEventListener((RMEventsListener) aa, true);
	}
	
	/**
	 * */
	public NodeSet getNodes(int amountOfNodesRequired){
		NodeSet ns = rmStub.getAtMostNodes(amountOfNodesRequired, null);
		return ns;
	}
	
	public void releaseNodes(NodeSet setOfNodesToRelease){
		rmStub.releaseNodes(setOfNodesToRelease);
	}
	
	public void disconnect(){
		rmStub.disconnect();
	}
}