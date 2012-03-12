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

/**
 * It behaves as a structure where to put all the information that will be told to the administrator
 * who controls/monitors the plugin via Nagios. 
 * It contains 
 * - an error message, 
 * - an error code, 
 * - (possibly) a set of curve values and,
 * - (possibly) an exception.*/
public class NagiosReturnObject {
	
	private String errorMessage;						// Message to be told to Nagios.
	private int errorCode;								// Error code to be told to Nagios.
	private String curvesSection;						// Curves section for Nagios to use to generate curves. 
	private Throwable exception;						// Exception to be told to Nagios.

	
	/**
	 * Basic constructor of the NagiosReturnObject class. No exception is present.
	 * @param errorCode error code to be returned.
	 * @param errorMessage message to be returned to the user (through Nagios). */
	public NagiosReturnObject(Integer errorCode, String errorMessage){
		this(errorCode, errorMessage, null);
	}
	
	/**
	 * Full constructor of the NagiosReturnObject class.
	 * @param errorCode error code to be returned.
	 * @param errorMessage message to be returned to the user (through Nagios).
	 * @param exception exception thrown (if any) that caused the problem (if any). */
	public NagiosReturnObject(Integer errorCode, String errorMessage, Throwable exception){
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
		this.exception = exception;
		this.curvesSection = "";
	}
	
	/**
	 * Sets the error message.
	 * @param newerrormessage new error message. */
	public void setErrorMessage(String newerrormessage){
		this.errorMessage = newerrormessage;
	}
	
	/**
	 * Append to the section of curves (section after the pipe character in Nagios outputs) the given string.
	 * @param tst TimedStatusTracer containing the timing measurements done. 
	 * @param all_time_label label for the sum of all the measurements done (null if this measurement should not be added). */
	public void addCurvesSection(TimedStatusTracer tst, String all_time_label){
		curvesSection = tst.getMeasurementsSummary(all_time_label);
	}
	
	/**
	 * Get the whole message with the format 'errorMessage | curves_section_string'.
	 * If there is not curves section, then the format is 'errorMessage'.
	 * @return the whole message. */
	public String getWholeFirstLineMessage(){
		if (curvesSection.isEmpty() == true){
			return errorMessage;
		}else{
			return errorMessage + " | " + curvesSection;
		}
	}
	
	/**
	 * Return the error message.
	 * @return error message. */
	public String getErrorMessage(){
		return errorMessage;
	}
	
	/**
	 * Return the error code.
	 * @return error code. */
	public Integer getErrorCode(){
		return errorCode;
	}
	
	/**
	 * Return the exception (or null if not present).
	 * @return exception. */
	public Throwable getException(){
		return exception;
	}
}
