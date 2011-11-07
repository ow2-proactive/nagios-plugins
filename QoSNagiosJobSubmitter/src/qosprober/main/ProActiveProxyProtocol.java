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

/** 
 * Enum to establish the possible protocols to be used
 * when connected with the Scheduler. */
public enum ProActiveProxyProtocol  
{
	/** Unknown protocol. */
	UNKNOWN,	
	/** Java ProActive protocol. */
	JAVAPA,		
	/** RESTful protocol. */
	REST;		
	
	/** Representative Strings for each protocol. */
	protected static final String PROTOCOL_JAVAPA_STR = "JAVAPA";
	protected static final String PROTOCOL_REST_STR = "REST";
	
	/** 
	 * Convert a string to a value of this Enum. 
	 * @param string to convert, should be JAVAPA or REST (case non-sensitive). */
	public static ProActiveProxyProtocol parseProtocol(String str) throws IllegalArgumentException{
		str = str.trim().toUpperCase();
		if(str.equals(PROTOCOL_JAVAPA_STR)){
			return JAVAPA;
		}else if(str.equals(PROTOCOL_REST_STR)){
			return REST;
		}else{
			return UNKNOWN;
		}
	}
	
}
