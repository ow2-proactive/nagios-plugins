package qosprober.main;

import org.apache.log4j.Logger;

import qosprober.misc.Misc;

public class Server {
    public static Logger logger = Logger.getLogger(Server.class.getName()); // Logger.
    private boolean done = false;
    
    private static final String MY_MESSAGE = Misc.generateFibString(PAMRProber.MESSAGE_LENGTH);
    
    public Server() {}

    public synchronized boolean putMessageAndCheckIt(String message){
    	boolean ret = false;
    	
    	logger.info("Putting message in Server. Checking '" + message.length() + "' against '" + MY_MESSAGE.length() + "'....");
    	if (!MY_MESSAGE.equals(message)){
    		ret = false;
    		logger.info("Checking: failed...");
    	}else{
    		ret = true;
    		logger.info("Checking: OK");
    		System.exit(0);
    		
    	}
    	
    	done = true;
    	return ret;
    }
    
    public synchronized boolean didAll(){
    	return done;
    }
    
    public static void main(String args[]) throws Exception{
    	System.setProperty("proactive.configuration", args[0]);
    	Misc.createPolicyAndLoadIt();
    	String serverurl = null;
    	    	
    	Server server = null;
    
        // Creates an active object for the server
        server = org.objectweb.proactive.api.PAActiveObject.newActive(Server.class, null);

        //ProActiveConfiguration.load();
        
        org.objectweb.proactive.api.PAActiveObject.registerByName(server, PAMRProber.SERVER_NAME);
        String url = org.objectweb.proactive.api.PAActiveObject.getActiveObjectNodeUrl(server);
        String url2 = org.objectweb.proactive.api.PAActiveObject.getUrl(server);
        logger.info("Server is ready.");
        logger.info("Returned URL for the ActiveObjectNode: '" + url + "'.");
        logger.info("Returned URL for the server: '" + url2 + "'.");
        logger.info("Server resource name: '" + PAMRProber.SERVER_NAME + "', resource number: '" + Misc.getResourceNumberFromURL(url) + "'.");
        serverurl = PAMRProber.PREFIX_URL + Misc.getResourceNumberFromURL(url) + "/" + PAMRProber.SERVER_NAME;
        logger.info("Server standard URL:\t\t" + serverurl);
            

        do{
        	logger.info("Waiting...");
        	Thread.sleep(3000);
        }while(!server.didAll());
        
        logger.info("Done all.");
        
        System.exit(0);
    }
    
    
}