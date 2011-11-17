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
 * This is a general Nagios plugin class that performs a test on the scheduler, by doing:
 *    -Job submission
 *    -Job result retrieval
 *    -Job result comparison 
 *  After that, a short summary regarding the result of the test is shown using Nagios format. */
public class TimedStatusTracer {
	private static TimedStatusTracer instance;				// Singleton.
	private TimeTick timetick;
	private Properties timingMeasurements;
	private Properties timingMeasurementsReference;
	private String lastStatusDescription = null;			// Holds a message representative of the current status of the test. 
															// It is used in case of TIMEOUT, to help the administrator guess 
															// where the problem is.
	
	private String lastLabel = null;						// Holds a message representative of the current status of the test. 
															// It is used in case of TIMEOUT, to help the administrator guess 
															// where the problem is.
	public static Logger logger = Logger.getLogger(TimedStatusTracer.class.getName()); // Logger.
	
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
	
	public static TimedStatusTracer getInstance(){
		if (instance==null){
			instance = new TimedStatusTracer();
		}
		return instance;
	}
	
	private TimedStatusTracer(){
		timingMeasurements = new Properties();
		timingMeasurementsReference = new Properties();
		timetick = new TimeTick();
	}
	
	public synchronized void startNewMeasurement(String label){
		lastLabel = label;
		timetick.tickSec();
		logger.info("Started new measurement '"+label+"'.");
	}
	
	public synchronized void finishLastMeasurement(){
		if (lastLabel != null){
			Double time = new Double(timetick.tickSec());
			timingMeasurements.put(lastLabel, time);
			logger.info("Finished last measurement '"+lastLabel+"'.");
		}	
	}
	
	public synchronized void finishLastMeasurementAndStartNewOne(String newlabel){
		finishLastMeasurement();
		startNewMeasurement(newlabel);
	}
	
	public synchronized void finishLastMeasurementAndStartNewOne(String newlabel, String currentStatus){
		finishLastMeasurementAndStartNewOne(newlabel);
		setLastStatusDescription(currentStatus);
	}
	
	public synchronized void addNewReference(String label, Double time_sec){
		timingMeasurementsReference.put((String)label, time_sec);
	}
	
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
