/*
 * ################################################################
 *
 * ProActive Parallel Suite(TM): The Java(TM) library for
 *    Parallel, Distributed, Multi-Core Computing for
 *    Enterprise Grids & Clouds
 *
 * Copyright (C) 1997-2011 INRIA/University of
 *                 Nice-Sophia Antipolis/ActiveEon
 * Contact: proactive@ow2.org or contact@activeeon.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; version 3 of
 * the License.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
 * USA
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 *
 *  Initial developer(s):               The ProActive Team
 *                        http://proactive.inria.fr/team_members.htm
 *  Contributor(s):
 *
 * ################################################################
 * $$PROACTIVE_INITIAL_DEV$$
 */

package org.ow2.proactive.nagios.probes.pamr;

import org.apache.log4j.Logger;


/**
 * Server class. It is instantiated as an Active object within the plugin process. Then a secondary 
 * process having a client is launched. Both client and server contact the same PAMR router to become Active objects.
 * Then the client will contact the server sending a predefined message.
 * If that message is okay, then the server (and consequently the plugin in which it is embedded) tell that the PAMR router behaved
 * correctly during this process. */
public class Server {
    
	public static Logger logger = Logger.getLogger(Server.class.getName()); // Logger.
    private boolean done = false;											// This flag tells whether the call from the client was already done or not.
    private static final String MY_MESSAGE =
    		PAMRMisc.generateFibString(PAMRProber.MESSAGE_LENGTH);				// This message should be received from the client. 
    																		// If not, there was a problem.
    
    public Server() {}														// Needed by ProActive to initialize the Active object.

    /**
     * This method is supposed to be called by the client (through PAMR router).
     * If the argument passed equals MY_MESSAGE, then everything went okay. */
    public synchronized boolean putMessageAndCheckIt(String message){
    	boolean bothequal = false;
    	
    	logger.info("Client is putting a message in Server...");
    	logger.info("Checking message of length '" + message.length() + "' against message of length '" + MY_MESSAGE.length() + "'....");
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