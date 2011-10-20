package qosprober.main;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;

import qosprober.misc.Misc;

public class Client {
	
    public static Logger logger = Logger.getLogger(Client.class.getName()); // Logger.
    private String serverHostName;
    private Server server;
    
    private static final String MY_MESSAGE = Misc.generateFibString(PAMRProber.MESSAGE_LENGTH);
    
    public Client() {}

    public Client(String serverHostName) throws Exception {
        this.serverHostName = serverHostName;
    }

    public void init() throws ActiveObjectCreationException {
    	String urlAsString = serverHostName;
        logger.info("Client is looking up server at " + urlAsString);
        try {
            server = org.objectweb.proactive.api.PAActiveObject.lookupActive(Server.class,
                    urlAsString);
            logger.info("Client successfully found the server");
            // Registers myself with the server
        } catch (IOException e) {
            logger.error("Server not found at " + urlAsString);
            
        }
    }
    
    public boolean sendMessageToServerAndCheckIt(){
    	boolean ret = false;
    	logger.info("Putting message in server...");
    	ret = server.putMessageAndCheckIt(MY_MESSAGE);
    	logger.info("Done.");
        return ret;
    }
    
    public static void main(String args[]) throws Exception{
    	String paconffile = args[0];
    	String serverurl = args[1];
    	
    	//Misc.log4jConfiguration2();
    	
    	logger.info("Started PAMR client-side probe...");
    	
    	logger.info("Loading ProActive configuration file...");
    	System.setProperty("proactive.configuration", paconffile);
    	logger.info("Done.");
    	
    	logger.info("Setting up security policy...");
    	Misc.createPolicyAndLoadIt();
    	logger.info("Done.");
    	
        logger.info("Creating Client Active object...");
    	Client client = org.objectweb.proactive.api.PAActiveObject.newActive(Client.class, new Object[] {serverurl});
        logger.info("Done.");
    	
        logger.info("Initializing Client Active object...");
        client.init();
        logger.info("Done.");
        
        
        logger.info("Sending message to '" + serverurl + "'...");
        boolean bb = client.sendMessageToServerAndCheckIt();
        logger.info("Done.");
        
        System.exit(0);
    }


    
}