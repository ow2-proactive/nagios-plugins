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
    	
    	logger.info("Client is putting a message in Server...");
    	logger.info("Checking '" + message.length() + "' against '" + MY_MESSAGE.length() + "'....");
    	if (!MY_MESSAGE.equals(message)){
    		ret = false;
    		logger.info("Checking: failed...");
    	}else{
    		ret = true;
    		logger.info("Checking: OK");
    	}
    	
    	done = true;
    	return ret;
    }
    
    public synchronized boolean isDone(){
    	return done;
    }
    
    
    
}