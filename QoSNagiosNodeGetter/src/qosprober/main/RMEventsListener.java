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

package qosprober.main;
import java.io.Serializable;
import org.apache.log4j.Logger;
import org.ow2.proactive.resourcemanager.common.event.RMEvent;
import org.ow2.proactive.resourcemanager.common.event.RMNodeEvent;
import org.ow2.proactive.resourcemanager.common.event.RMNodeSourceEvent;
import org.ow2.proactive.resourcemanager.frontend.RMEventListener;

/** 
 * Class that listens to events that happen in the remote RM. */ 
public class RMEventsListener implements RMEventListener, Serializable{
	private static final long serialVersionUID = 1L;
	private static Logger logger = Logger.getLogger(RMEventsListener.class.getName());// Logger.	
	private static final int lastSomethingRMBufferSize = 200; 							 // Maximum amount of elements in the lastFinished/RemovedJobs array.
	public static String[] lastEventsList = new String[lastSomethingRMBufferSize]; 		 // List of last finished jobs.
	public static int currentCounterFinished = 0; 											 // Circular index.
	
	/** 
	 * Check if a given event has already happened. */
	public static synchronized boolean checkIfEventHappened(String event){
		printList();
		for (String j:lastEventsList){
			if (j!=null && j.equals(event)){
				logger.debug("\t\tyes!");
				return true;
			}
		}
		logger.debug("\t\tno...");
		return false;
	}

	
	/**
	 * Add an event to the list of last events. */
	public static synchronized void addEvent(String event){
		lastEventsList[currentCounterFinished] = event;
		currentCounterFinished = (currentCounterFinished + 1) % lastSomethingRMBufferSize ;
		printList();
	}
	
	/**
	 * Print the list of last events. */
	private static synchronized void printList(){
		//logger.debug("\tLast Events List: " + Misc.getDescriptiveString((Object)lastEventsList));
	}


	
	/** 
	 * Interface RMEventListener
	 * Notification of events that happen in the RM.  
	 */

	public void nodeEvent(RMNodeEvent event) {
		logger.info(">> nodeEvent " + event.getEventType());
		addEvent(event.getEventType().toString());
		synchronized(RMEventsListener.class){
			RMEventsListener.class.notifyAll();
		}
				
	}

	public void nodeSourceEvent(RMNodeSourceEvent event) {
		// TODO Auto-generated method stub
		logger.info(">> nodeSourceEvent " + event.getEventType());
		addEvent(event.getEventType().toString());
		synchronized(RMEventsListener.class){
			RMEventsListener.class.notifyAll();
		}
	}


	public void rmEvent(RMEvent event) {
		// TODO Auto-generated method stub
		logger.info(">> rmEvent " + event.getEventType());
		addEvent(event.getEventType().toString());
		synchronized(RMEventsListener.class){
			RMEventsListener.class.notifyAll();
		}
	}


}
