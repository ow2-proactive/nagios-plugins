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
 * It behaves as a structure where to put information about a particular item
 * that we would like the administrator to know. 
 * It contains 
 * - an error message, 
 * - an error code. */
public class NagiosMiniStatus {
	
	private String errorMessage;						// Message to be told to Nagios.
	private int errorCode;								// Error code to be told to Nagios.
	
	/**
	 * Full constructor of the NagiosReturnObject class.
	 * @param errorCode error code to be returned.
	 * @param errorMessage message to be returned to the user (through Nagios). */
	public NagiosMiniStatus(Integer errorCode, String errorMessage){
		this.errorCode = errorCode;
		this.errorMessage = errorMessage;
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
}
