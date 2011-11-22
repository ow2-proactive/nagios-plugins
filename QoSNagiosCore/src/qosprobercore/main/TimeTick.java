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

/** 
 * Class to get the time elapsed between events/ticks. */
public class TimeTick {
	private long init;		// Timestamp of the last tick.

	/**
	 * Empty constructor. */
	public TimeTick(){
		tickSec();
	}
	
	/**
	 * Get the time elapsed between this tick/call and the previous one. */
	public double tickSec(){
		long now = (new Date()).getTime();
		double interval = ((double)((double)now - (double)init)) / 1000;
		init = gettime();
		return interval;
	}
	
	/**
	 * Get the current time in ms since the epoch.
	 * @return the value. */
	private long gettime(){
		return (new Date()).getTime();
	}
	
	/**
	 * Debugging purposes. */
	public static void main(String args[]) throws Exception{
		TimeTick tt = new TimeTick();
		System.out.println("tick " + tt.tickSec());
		Thread.sleep(150);
		System.out.println("tick " + tt.tickSec());
		Thread.sleep(250);
		System.out.println("tick " + tt.tickSec());
		Thread.sleep(350);
		System.out.println("tick " + tt.tickSec());
		Thread.sleep(1400);
		System.out.println("tick " + tt.tickSec());
	}
}
