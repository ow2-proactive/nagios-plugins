package qosprober.main;

//import static junit.framework.Assert.assertTrue;
//import junit.framework.Assert;

import org.apache.log4j.Logger;
import org.objectweb.proactive.core.node.Node;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.resourcemanager.authentication.RMAuthentication;
import org.ow2.proactive.resourcemanager.frontend.RMConnection;
import org.ow2.proactive.resourcemanager.frontend.ResourceManager;
import org.ow2.proactive.utils.NodeSet;


public class NodeProber {

	public static Logger logger = Logger.getLogger(JobProber.class.getName()); // Logger.
	
	private static ResourceManager rmStub;
    public static void main(String[] args) throws Exception {

    	JobProber.createPolicyAndLoadIt();
    	
    	
    	
    	logger.info("Joining the Resource Manager...");
        RMAuthentication auth = RMConnection.waitAndJoin("rmi://localhost:1099/");
        logger.info("Creating credentials...");
        Credentials cred = Credentials.createCredentials(new CredData("demo", "demo"), auth.getPublicKey());
        logger.info("Logging in...");
        rmStub = auth.login(cred);
        
        //RMEventsListener aa = PAActiveObject.newActive(SchedulerEventsListener.class, new Object[]{}); 
        //rmStub.addEventListener((SchedulerEventsListener) aa, true);
    	
    	logger.info("Getting nodes...");
		NodeSet ns = rmStub.getAtMostNodes(4, null);
		
    	logger.info("Listing nodes...");
    	for(Node n:ns){
    		logger.info(" - " + n.getNodeInformation().getName());
    	}
		

    	logger.info("Releasing nodes...");
    	rmStub.releaseNodes(ns);
    	
    	logger.info("Disconnecting...");
    	rmStub.disconnect();
    	
    	System.exit(0);
    	
        
    }

}