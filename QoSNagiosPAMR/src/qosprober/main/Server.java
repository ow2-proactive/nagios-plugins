package qosprober.main;

import org.apache.log4j.Logger;

import qosprober.misc.Misc;

/**
 * Server class. It is instantiated as an Active object within the plugin process. Then a secondary 
 * process having a client is launched, and it will contact the server sending a predefined message.
 * If that message is okay, then the server (and the plugin) tells that the PAMR router behaved
 * correctly during this process. */
public class Server {
    
	public static Logger logger = Logger.getLogger(Server.class.getName()); // Logger.
    private boolean done = false;											// This flag tells whether the call from the client was already done or not.
    private static final String MY_MESSAGE =
    		Misc.generateFibString(PAMRProber.MESSAGE_LENGTH);				// This message should be received from the client. 
    																		// If not, there was a problem.
    
    public Server() {}														// Needed by ProActive to initialize the Active object.

    /**
     * This method is supposed to be called by the client (through PAMR router).
     * If the argument passed equals MY_MESSAGE, then everything went okay. */
    public synchronized boolean putMessageAndCheckIt(String message){
    	boolean bothequal = false;
    	
    	logger.info("Client is putting a message in Server...");
    	logger.info("Checking '" + message.length() + "' against '" + MY_MESSAGE.length() + "'....");
    	if (MY_MESSAGE.equals(message)){
	    	bothequal = true;
    		logger.info("Checking: OK");
    	}else{
			bothequal = false;
    		logger.info("Checking: failed...");
    	}
    	done = true;
    	return bothequal;
    }
    
    /**
     * Tell whether the call from the client was already done or not. */
    public synchronized boolean isDone(){
    	return done;
    }
    
}