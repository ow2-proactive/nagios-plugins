package qosprober.main;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;

public class Client {
	
    public static Logger logger = Logger.getLogger(Client.class.getName()); // Logger.
    private String serverHostName;
    private Server server;
    
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
    	ret = server.putMessageAndCheckIt(PAMRProber.MESSAGE);
        return ret;
    }


    
}