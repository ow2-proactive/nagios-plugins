package qosprober.main;
import java.io.Serializable;

import qosprober.misc.Misc;

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
		logger.debug("\tLast Events List: " + Misc.getDescriptiveString((Object)lastEventsList));
	}


	
	/** 
	 * Interface RMEventListener
	 * Notification of events that happen in the RM.  
	 */

	@Override
	public void nodeEvent(RMNodeEvent event) {
		logger.info(">> nodeEvent " + event.getEventType());
		addEvent(event.getEventType().toString());
		synchronized(RMEventsListener.class){
			RMEventsListener.class.notifyAll();
		}
				
	}

	@Override
	public void nodeSourceEvent(RMNodeSourceEvent event) {
		// TODO Auto-generated method stub
		logger.info(">> nodeSourceEvent " + event.getEventType());
		addEvent(event.getEventType().toString());
		synchronized(RMEventsListener.class){
			RMEventsListener.class.notifyAll();
		}
	}


	@Override
	public void rmEvent(RMEvent event) {
		// TODO Auto-generated method stub
		logger.info(">> rmEvent " + event.getEventType());
		addEvent(event.getEventType().toString());
		synchronized(RMEventsListener.class){
			RMEventsListener.class.notifyAll();
		}
	}


}
