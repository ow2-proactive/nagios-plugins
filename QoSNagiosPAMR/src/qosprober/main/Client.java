package qosprober.main;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.core.config.ProActiveConfiguration;

import qosprober.misc.Misc;

/** 
 * Client class.
 * It is executed by the server, which gives to this client all what it needs to become an Active object connected to the 
 * same PAMR router as the server, and contact the server afterwards. 
 * This client sends to the server a message that goes through the PAMR router (so we can check if its service is up). 
 * If the server receives correctly this message, then we can say that the PAMR router service for incoming connections is up. */
public class Client {
	
	public static final String COMMUNICATION_PROTOCOL =
			"pamr";															// Default protocol to be used to get connected to the RM.
	public static boolean booleanvalue;										// Value returned by the client. We get it to make sure no optimizations will be done during the communication.
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
    	ret = server.putMessageAndCheckIt(MY_MESSAGE); 			// Send a message to the server (call one of its methods).
    	logger.info("Done.");
        return ret;
    }
    
    /**
     * Start the client. */
    public static void main(String args[]) throws Exception{
    	
    	//Misc.log4jConfiguration2();							// Use in case you want to see what happened with the client (dumps in file 'output'). 
    	
    	logger.info("Started PAMR client-side probe...");
    	
    	// Two possible argument-format for this module:
    	// SERVERURL PACONFIGURATIONFILE				(2 arguments, assume PA conf. file is given).
    	// SERVERURL PAMRADDRESS PAMRPORT				(3 arguments, assume server address and port are given).
    	
		String serverurl = args[0];
    	if (args.length == 2){									// Specific way to communicate either ProActive conf. file or host&port.
	    	logger.info("Loading ProActive configuration file...");
	    	String paconffile = args[1];
	    	System.setProperty("proactive.configuration", paconffile);
    	}else{
    		logger.info("Avoiding ProActive configuration file...");
    		String pamrhost = args[1];
    		String pamrport = args[2];	
			ProActiveConfiguration pac = ProActiveConfiguration.getInstance();	
			pac.setProperty("proactive.communication.protocol", COMMUNICATION_PROTOCOL, false);
			pac.setProperty("proactive.net.router.address", pamrhost, false);
			pac.setProperty("proactive.net.router.port", pamrport, false);
    	}
    	
    	logger.info("Done.");
    	
    	logger.info("Setting up security policy...");
    	Misc.createPolicyAndLoadIt();
    	logger.info("Done.");
    	
        logger.info("Creating Client Active object..."); 		// We create the client telling about the server url.
    	Client client = org.objectweb.proactive.api.PAActiveObject.newActive(Client.class, new Object[] {serverurl});
        logger.info("Done.");
    	
        logger.info("Initializing Client Active object...");
        client.init();
        logger.info("Done.");
        
        logger.info("Sending message to '" + serverurl + "'...");
        booleanvalue = client.sendMessageToServer();
        logger.info("Done.");
        
        System.exit(0);
    }
}
