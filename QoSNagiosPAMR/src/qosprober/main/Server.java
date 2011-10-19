package qosprober.main;

import org.apache.log4j.Logger;

public class Server {
    public static Logger logger = Logger.getLogger(Server.class.getName()); // Logger.
    
    
    public Server() {}

    public boolean putMessageAndCheckIt(String message){
    	boolean ret = false;
    	
    	//logger.info("New message put: '" + message + "'.");
    	//logger.info("Checking message obtained '" + message + "' against '" + PAMRProber.MESSAGE + "'....");
    	if (!PAMRProber.MESSAGE.equals(message)){
    		ret = false;
    	}else{
    		ret = true;
    	}
    	return ret;
    }
    
    
    
    
}