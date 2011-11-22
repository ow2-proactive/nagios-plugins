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

package qosprobercore.main;

import java.util.Locale;
import java.util.Properties;
import org.apache.log4j.Logger;
import qosprobercore.misc.Misc;

/** 
 * Class that provides support for measuring execution time of certain calls, and, for saving last status information
 * which is shown in case of timeout (to let the user know where the process got stuck before the timeout). */
public class TimedStatusTracer {
	private static TimedStatusTracer instance;				// Singleton.
	private TimeTick timetick;								// Timing measurement object.
	private Properties timingMeasurements;					// Entries with measurements.
	private Properties timingMeasurementsReference;			// Entries with references. 
	private String lastStatusDescription = null;			// Holds a message representative of the current status of the test. 
															// It is used in case of TIMEOUT, to help the administrator guess 
															// where the problem is.
	private String lastLabel = null;						// Holds a message representative of the current status of the test. 
															// It is used in case of TIMEOUT, to help the administrator guess. 
															// where the problem is.
	public static Logger logger =							// Logger. 
			Logger.getLogger(TimedStatusTracer.class.getName()); 

	/** 
	 * Testing purposes. */
	public static void main(String args[]) throws Exception{
		Misc.log4jConfiguration(3);
		TimedStatusTracer st = new TimedStatusTracer();
		st.finishLastMeasurementAndStartNewOne("first", "status1 here");
		Thread.sleep(1000); // Initializing part.
		st.finishLastMeasurementAndStartNewOne("second", "status2 here");
		Thread.sleep(2000); // Middle part.
		st.finishLastMeasurementAndStartNewOne("third", "status3 here");
		Thread.sleep(3000); // Disconnection part.
		st.finishLastMeasurement();
		st.addNewReference("warning", 10.0);
		System.out.println(st.getMeasurementsSummary("all"));
	}
	
	/** 
	 * This class follows a singleton pattern. */
	public static TimedStatusTracer getInstance(){
		if (instance==null){
			instance = new TimedStatusTracer();
		}
		return instance;
	}

	/** 
	 * Private class constructor. */
	private TimedStatusTracer(){
		timingMeasurements = new Properties();
		timingMeasurementsReference = new Properties();
		timetick = new TimeTick();
	}
	
	/**
	 * Start a new time measurement. It will be added with the label provided.
	 * The time measurement finishes once finishLastMeasurement is executed. 
	 * @param label label to be used for this measurement. */
	public synchronized void startNewMeasurement(String label){
		lastLabel = label;
		timetick.tickSec();
		logger.info("Started new measurement '"+label+"'.");
	}
	
	/**
	 * Finish previous started measurement. It adds to the list of measurements
	 * done an entry following the format label=time. */
	public synchronized void finishLastMeasurement(){
		if (lastLabel != null){
			Double time = new Double(timetick.tickSec());
			timingMeasurements.put(lastLabel, time);
			logger.info("Finished last measurement '"+lastLabel+"'.");
		}	
	}
	
	/**
	 * Finish previous started measurement and start a new one.
	 * @param newlabel label to be used for the new measurement. */
	public synchronized void finishLastMeasurementAndStartNewOne(String newlabel){
		finishLastMeasurement();
		startNewMeasurement(newlabel);
	}
	
	/**
	 * Finish previous started measurement and start a new one.
	 * It also adds a current status, so in case of timeout we can retrieve it and tell the user where the process got stuck.
	 * @param newlabel new label to be used with the new measurement.
	 * @param currentStatus	current status to be set from now on. */
	public synchronized void finishLastMeasurementAndStartNewOne(String newlabel, String currentStatus){
		finishLastMeasurementAndStartNewOne(newlabel);
		setLastStatusDescription(currentStatus);
	}
	
	/**
	 * Add a new "hardcoded" entry to the set of measurements. This is to show reference values such as "timeout value". 
	 * @param label label of the new entry.
	 * @param time_sec value of the new entry. */
	public synchronized void addNewReference(String label, Number time_sec){
		timingMeasurementsReference.put((String)label, time_sec);
	}
	
	/**
	 * Get a string with the summary of all the entries with the format 'key1=value1 key2=value2 ...'
	 * @param totalLabel if not null, it adds a new entry with the totalLabel and the value given
	 * by the sum of all the measurements (not including the references).
	 * @return a string with the summary. */
	public synchronized String getMeasurementsSummary(String totalLabel){
		String ret_measurements = "";
		String ret_total = "";
		String ret_references = "";
		Double total = new Double(0.0);
		
		for (Object key: timingMeasurements.keySet()){
			Double timing = (Double)timingMeasurements.get(key.toString());
			total += timing;
			ret_measurements = key + "=" + String.format(Locale.ENGLISH, "%1.03f", timing) + " " + ret_measurements;
		}
		
		if (totalLabel == null){
			ret_total = "";
		}else{
			ret_total = totalLabel + "=" + String.format(Locale.ENGLISH, "%1.03f", total) + " ";
		}
		
		for (Object key: timingMeasurementsReference.keySet()){
			ret_references = key + "=" + String.format(Locale.ENGLISH, "%1.03f", timingMeasurementsReference.get(key.toString())) + " " + ret_references;
		}
		
		return ret_total + ret_measurements + ret_references;
	}

	/**
	 * Get the sum of all the measurements (not including references added).
	 * @return the sum. */
	public synchronized Double getTotal(){
		Double total = new Double(0.0);
		for (Object key: timingMeasurements.keySet()){
			Double timing = (Double)timingMeasurements.get(key.toString());
			total += timing;
		}
		return total;
	}
	
	/** 
	 * Save a message regarding the last status of the probe. 
	 * This last status will be used in case of timeout to tell Nagios up to which point
	 * (logging, job submission, job retrieval, etc.) the probe arrived. */
	public synchronized void setLastStatusDescription(String laststatus){
		lastStatusDescription = laststatus;
		logger.info("Last status description set to: '"+ laststatus +"'.");
	}
	
	/** 
	 * Get a message regarding the last status of the probe. 
	 * This last status will be used in case of timeout to tell Nagios up to which point
	 * (logging, job submission, job retrieval, etc.) the probe arrived. 
	 * @return the last status of the test. */
	public synchronized String getLastStatusDescription(){
		return lastStatusDescription;
	}
}
