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

import java.util.Date;
import java.util.concurrent.TimeoutException;

/** 
 * Tells how much time is remaining until a particular timeout limit is
 * reached. */
public class RemainingTime{
	long start;							// Start time (ms). 
	long timeout;						// Timeout time (ms).
	
	/**
	 * Constructor. 
	 * @param timeoutms timeout in milliseconds from now. */
	public RemainingTime(long timeoutms){
		start = getTimeNow();
		timeout = timeoutms;
	}
	
	/**
	 * Return how long remaining until reaching the timeout value, 
	 * considering as beginning the moment in which this object was created.
	 * @return time (in ms.) remaining until reaching timeout. Positive value means
	 * that the timeout has not been yet reached, while a negative value means that
	 * the timeout has been already overrun. */
	public long getRemainingTime(){
		return start + timeout - getTimeNow();
	}
	
	/**
	 * Return how long remaining until reaching the timeout value, 
	 * considering as beginning the moment in which this object was created.
	 * @throws TimeoutException in case the timeout has been overrun.
	 * @return time (in ms.) remaining until reaching timeout. */
	public long getRemainingTimeWE() throws TimeoutException{
		long rt = start + timeout - getTimeNow();
		if (rt < 0){
			throw new TimeoutException("Timeout of " + (-rt) + " ms.");
		}else{
			return rt; 
		}
	}
	
	private long getTimeNow(){
		return (new Date()).getTime();
	}
}
