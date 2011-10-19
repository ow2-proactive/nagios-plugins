package qosprober.main;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;



import org.apache.log4j.Logger;
import org.objectweb.proactive.core.config.ProActiveConfiguration;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.extensions.annotation.ActiveObject;

import qosprober.misc.Misc;



public class Server {
    private final static Logger logger = ProActiveLogger.getLogger(Loggers.EXAMPLES);
    protected String messageOfTheDay;
    protected java.util.ArrayList<Client> clients;
    private final static String SERVER_NAME = "server";
    private final static String PREFIX_URL = "pamr://";

	/** 
	 * Create a java.policy file to grant permissions, and load it for the current JVM. */
	

    public Server() {
    }

    public Server(String messageOfTheDay) {
        this.clients = new java.util.ArrayList<Client>();
        this.messageOfTheDay = messageOfTheDay;
    }

    public String getMessageOfTheDay() {
        return messageOfTheDay;
    }

    public void setMessageOfTheDay(String messageOfTheDay) {
        logger.info("Server: new message: " + messageOfTheDay);
        this.messageOfTheDay = messageOfTheDay;
        this.notifyClients();
    }

    public void register(Client c) {
        this.clients.add(c);
    }

    public void unregister(Client c) {
        this.clients.remove(c);
    }

    protected void notifyClients() {
        java.util.Iterator<Client> it = this.clients.iterator();
        Client currentClient;

        while (it.hasNext()) {
            currentClient = it.next();
            try {
                currentClient.messageChanged(this.messageOfTheDay);
            } catch (Exception t) {
                it.remove();
            }
        }
    }

    
    public static  String getResourceNumberFromURL(String url) throws Exception{
    	// pamr://9607/Node1807777269
    	
    	if (!url.startsWith(PREFIX_URL)){
    		throw new Exception("Expected '" + PREFIX_URL + "' at the beginning but found '" + url + "'.");
    	}
    	String rem = url.substring(PREFIX_URL.length());
    	rem = rem.substring(0,rem.indexOf('/'));
    	return rem;
    }
    
    public static void main(String[] args) throws Exception {
    	System.setProperty("proactive.configuration", Misc.getProActiveConfigurationFile());
    	Misc.createPolicyAndLoadIt();
    	String serverurl = null;	
        try {
            // Creates an active object for the server
            Server server = org.objectweb.proactive.api.PAActiveObject.newActive(Server.class,
                    new Object[] { "This is the first message" });

            ProActiveConfiguration.load();
            //Server theServer = (Server) org.objectweb.proactive.ProActive.newActive(Server.class.getName(), null, null);
            // Binds the server to a specific URL
            org.objectweb.proactive.api.PAActiveObject.registerByName(server, SERVER_NAME);
            String url = org.objectweb.proactive.api.PAActiveObject.getActiveObjectNodeUrl(server);
            String url2 = org.objectweb.proactive.api.PAActiveObject.getUrl(server);
            logger.info("Server is ready.");
            logger.info("Returned URL for the ActiveObjectNode: '" + url + "'.");
            logger.info("Returned URL for the server: '" + url2 + "'.");
            logger.info("Server resource name: '" + SERVER_NAME + "', resource number: '" + getResourceNumberFromURL(url) + "'.");
            serverurl = PREFIX_URL + getResourceNumberFromURL(url) + "/" + SERVER_NAME;
            logger.info("Server standard URL: "+ serverurl);
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        
        
        
    
        String clientName;
        
        
        clientName = "client";
        
    

        try {
            // Creates an active object for the client
            Client theClient = org.objectweb.proactive.api.PAActiveObject.newActive(Client.class,
                    new Object[] {clientName, serverurl});
            ProActiveConfiguration.load();
            if (theClient.init()) {
                Thread t = new Thread(new RunClient(theClient));
                t.start();
                t.join();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
        }
        System.exit(0);
    }
     
}