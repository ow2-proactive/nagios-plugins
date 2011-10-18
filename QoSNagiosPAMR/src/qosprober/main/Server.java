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

    public static void main(String[] args) throws Exception {
    	System.setProperty("proactive.configuration", Misc.getProActiveConfigurationFile());
    	Misc.createPolicyAndLoadIt();
    	
    	
        //ProActiveConfiguration.load();
        try {
            // Creates an active object for the server
            Server theServer = org.objectweb.proactive.api.PAActiveObject.newActive(Server.class,
                    new Object[] { "This is the first message" });

            ProActiveConfiguration.load();
            //Server theServer = (Server) org.objectweb.proactive.ProActive.newActive(Server.class.getName(), null, null);
            // Binds the server to a specific URL
            org.objectweb.proactive.api.PAActiveObject.registerByName(theServer, "server");
            String url = org.objectweb.proactive.api.PAActiveObject.getActiveObjectNodeUrl(theServer);
            logger.info("Server is ready: '" + url + "'.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}