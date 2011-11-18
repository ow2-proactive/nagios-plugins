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

package qosprober.misc;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import org.apache.log4j.Logger;
import qosprober.main.PAMRProber;

/** This class is supposed to have multiple minor functionalities. */
public class PAMRMisc {
	 
	private static Logger logger = Logger.getLogger(PAMRMisc.class.getName()); // Logger.
    private PAMRMisc(){}

    /** 
     * Return the classpath being used by this JVM. */
    public static String getClasspath() {
        // Get the System Classloader
    	String ret = "";
        ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
        ret = "";
        // Get the URLs
        URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();

        for(int i=0; i< urls.length; i++)
        {
            ret = ret + "" + urls[i].getFile() + ":";
        }       
        return ret;
    }
    
    /**
     * Extract the number present in the server's url, which is used later to contact the server. */
    public static  String getResourceNumberFromURL(String url) throws Exception{
    	// Typical one is pamr://9607/Node1807777269
    	if (!url.startsWith(PAMRProber.PREFIX_URL)){
    		throw new Exception("Expected '" + PAMRProber.PREFIX_URL + "' at the beginning but found '" + url + "'.");
    	}
    	String rem = url.substring(PAMRProber.PREFIX_URL.length());
    	rem = rem.substring(0,rem.indexOf('/'));
    	return rem;
    }
    
    /**
     * Generate a non-trivial String with the given amount of characters. */
    public static String generateFibString(int length) { 
    	//System.out.println("Generating Fibonacci...");
		int f0 = 0; 
		int f1 = 1; 
		
		char [] ret = new char[length];
		
		for (int i = 0; i < length; i++) { 
			char c = (char)(f0 % 256);
			
			ret[i] = c;
			
			final int temp = f1; 
			f1 += f0; 
			f0 = temp; 
		} 
		return new String(ret);
    }

    /**
     * Get the java home (so we can use it to run a differentd JVM). */
    public static String getJavaHome(){
    	return System.getProperty("java.home");
    }
    
    /**
     * Run a new JVM with the given arguments. */
    public static void runNewJVM(String classname, String arguments) throws IOException{
    	String command = PAMRMisc.getJavaHome() + "/bin/java" 	+ " " + 
    						"-cp " + PAMRMisc.getClasspath() 	+ " " + 
    						classname 						+ " " + 
    						arguments;
    	// The command would be something like:
    	//	/tmp/JDK/bin/java -cp bin:../otherbins package.ClassToExecute [arguments]
    	logger.info("Running command: '" + command + "'...");
		Runtime.getRuntime().exec(command);
    }
}
