package qosprober.main;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;

import qosprober.misc.Misc;

/** 
 * Client class.
 * It receives the url of the server, later it contacts the server, and sends to it a message that goes
 * go through the PAMR router (so we check its service is up). If the server receives correctly this message, 
 * then we can say that the PAMR router service is up. */
public class Client {
	
    public static Logger logger = Logger.getLogger(Client.class.getName()); // Logger.
    private String serverURL; 												// URL of the server, we want to contact it to send a message.
    private Server server;													// Stub of the server. 
    
    private static final String MY_MESSAGE = 
    		Misc.generateFibString(PAMRProber.MESSAGE_LENGTH);				// Message to send to the server.
    
    public Client() {}														// Empty constructor. ProActive needs it.

    /**
     * Constructor of the client. It requires the URL of the server to contact. */
    public Client(String serverURL) throws Exception {
        this.serverURL = serverURL;
    }

    /** 
     * Initialize the client. Captures also the registered server to contact it. */
    public void init() throws ActiveObjectCreationException {
    	String urlAsString = serverURL;
        logger.info("Client is looking up server at " + urlAsString);
        try {
            server = org.objectweb.proactive.api.PAActiveObject.lookupActive(Server.class,
                    urlAsString);											// Connect with the server.
            logger.info("Client successfully found the server.");
        } catch (IOException e) {
            logger.error("Server not found at " + urlAsString);
        }
    }
    
    /** 
     * Send a message to the server, and ask it to check the message. */
    public boolean sendMessageToServer(){
    	boolean ret = false;
    	logger.info("Putting message in server...");
    	ret = server.putMessageAndCheckIt(MY_MESSAGE); 						// Send a message to the server (call one of its methods).
    	logger.info("Done.");
        return ret;
    }
    
    /**
     * Start the client. */
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
        boolean bb = client.sendMessageToServer();
        logger.info("Done.");
        
        System.exit(0);
    }
}