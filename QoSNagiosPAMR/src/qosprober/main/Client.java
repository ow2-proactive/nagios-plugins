package qosprober.main;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.core.config.ProActiveConfiguration;
import org.objectweb.proactive.core.util.log.Loggers;
import org.objectweb.proactive.core.util.log.ProActiveLogger;
import org.objectweb.proactive.extensions.annotation.ActiveObject;

import qosprober.misc.Misc;

@ActiveObject
public class Client {
    private final static Logger logger = ProActiveLogger.getLogger(Loggers.EXAMPLES);
    protected String myName;
    protected String serverHostName;
    protected Server theServer;
    protected boolean connected = false;
    private java.text.DateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

    public Client() {
    }

    public Client(String clientName, String serverHostName) throws Exception {
        this.myName = clientName;
        this.serverHostName = serverHostName;
    }

    public boolean init() throws ActiveObjectCreationException {
        // Looks up for the server
        //String urlAsString = "//" + serverHostName + "/server";
    	String urlAsString = serverHostName;
        logger.info("Client " + myName + " is looking up server at " + urlAsString);
        try {
            this.theServer = org.objectweb.proactive.api.PAActiveObject.lookupActive(Server.class,
                    urlAsString);
            logger.info("Client " + this.myName + " successfully found the server");
            // Registers myself with the server
            Client myself = (Client) org.objectweb.proactive.api.PAActiveObject.getStubOnThis();
            if (myself != null) {
                theServer.register(myself);
            } else {
                logger.info("Cannot get a stub on myself");
                return false;
            }

        } catch (IOException e) {
            logger.error("Server not found at " + urlAsString);
            return false;
        }
        return true;
    }

    public void interactWithServer() {
        // Gets the message from the server and prints it out
        //System.out.println (this.myName+": message is "+this.theServer.getMessageOfTheDay());
        // Sets a new message on the server
        theServer.setMessageOfTheDay(this.myName + " is connected (" +
            dateFormat.format(new java.util.Date()) + ")");
        //System.out.println (this.myName+": new message sent to the server");
    }

    public void messageChanged(String newMessage) {
        System.out.println(this.myName + ": message changed: " + newMessage);
    }

    public static void main(String[] args) throws Exception {
    	System.setProperty("proactive.configuration", Misc.getProActiveConfigurationFile());
    	Misc.createPolicyAndLoadIt();
        String clientName;
        String numberServer;
        
        clientName = "client";
        numberServer = "9598";
    

        try {
            // Creates an active object for the client
            Client theClient = org.objectweb.proactive.api.PAActiveObject.newActive(Client.class,
                    new Object[] { clientName, numberServer });
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