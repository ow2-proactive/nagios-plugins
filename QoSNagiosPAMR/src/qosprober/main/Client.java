package qosprober.main;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.core.config.ProActiveConfiguration;
import org.objectweb.proactive.core.node.NodeException;

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
    	
    	logger.info("Started Client...");
    	
    	TimeTick timing = new TimeTick();
    	
    	System.setProperty("proactive.configuration", args[0]);
    	Misc.createPolicyAndLoadIt();
    	String serverurl = args[1];
    
        Client client = org.objectweb.proactive.api.PAActiveObject.newActive(Client.class, new Object[] {serverurl});
        //ProActiveConfiguration.load();
            
        client.init();
        
        
        logger.info("Sending message to '" + serverurl + "'...");
        timing.tickSec();
        boolean bb = client.sendMessageToServerAndCheckIt();
        System.out.println(" TOOK " + timing.tickSec() + " sec ");
        System.exit(0);
    }


    
}