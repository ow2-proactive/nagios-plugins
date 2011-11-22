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

import java.io.IOException;
import org.apache.log4j.Logger;
import qosprobercore.misc.Misc;

/**
 * Set of useful definitions and methods for any Nagios plugin. */
public class NagiosPlugin {
	public static final String NAG_OUTPUT_PREFIX = "SERVICE STATUS: ";
	
	public static final int DEBUG_LEVEL_0_SILENT	= 0;	// Debug level, silent mode. 
	public static final int DEBUG_LEVEL_1_EXTENDED 	= 1;	// Debug level, more than silent mode. Shows backtraces if error. 
	public static final int DEBUG_LEVEL_2_VERBOSE	= 2;	// Debug level, similar to the previous one.
	public static final int DEBUG_LEVEL_3_USER		= 3;	// Debug level, debugging only.
	
	private static Logger logger = Logger.getLogger(NagiosPlugin.class.getName()); // Logger.
	
    /** 
     * Print a message in the stdout (for Nagios to use it) and return with the given error code. 
     * Print a back-trace later only if the debug-level is appropriate. 
     * @param obj object to take the information from.
     * @param debuglevel level of verbosity. */
    public synchronized static void printAndExit(NagiosReturnObject obj, int debuglevel){
    	Throwable exc = obj.getException();
        switch(debuglevel){
            case DEBUG_LEVEL_0_SILENT:
                System.out.println(NAG_OUTPUT_PREFIX + obj.getWholeMessage());
                break;
            default:
                System.out.println(NAG_OUTPUT_PREFIX + obj.getWholeMessage());
                if (exc!=null){
	                exc.printStackTrace(System.out);
                }
                break;
        }
        System.exit(obj.getErrorCode());
    }
    
	/**
	 * Used when a parameter given by the user is wrong. 
	 * Print a message, then the usage of the application, and the exits the application. 
	 * @param mainmessage message to be shown to the user (though Nagios). */
	public static void printMessageUsageAndExit(String mainmessage){
		if (mainmessage!=null){
			System.out.println(mainmessage);
		}
	    Misc.printUsage();
	    System.exit(NagiosReturnObject.RESULT_2_CRITICAL);
	}
	
	/**
	 * Print the version of the plugin and the exits the application. */
	public static void printVersionAndExit(){
		String usage = null;
		try {
			usage = Misc.readAllTextResource("/resources/version.txt");
			System.err.println(usage);
		} catch (IOException e) {
			logger.warn("Issue with usage message. Error: '"+e.getMessage()+"'.", e); 
		}
	    System.exit(NagiosReturnObject.RESULT_0_OK);
	}
}
