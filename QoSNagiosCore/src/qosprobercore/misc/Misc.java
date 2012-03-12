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

package qosprobercore.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Properties;
import org.apache.log4j.PropertyConfigurator;
import qosprobercore.main.PANagiosPlugin;

/** 
 * This class is supposed to have multiple minor functionalities. */
public class Misc {
	
    private Misc(){}
    
    /**
     * Read all the content of a resource file. 
     * @param resource path of the resource to read.
     * @return string that the resource contains. */
    public static String readAllTextResource(String resource) throws Exception{
		InputStream is = Misc.class.getResourceAsStream(resource);
	    InputStreamReader isr = new InputStreamReader(is);
	    BufferedReader br = new BufferedReader(isr);
	    String line;
	    String ret = "";
	    while ((line = br.readLine()) != null){
	      ret = ret + line + "\n";
	    }
	    br.close();
	    isr.close();
	    is.close();
	    return ret;
	}
    
	/**
	 * Creates a default set of properties for the log4j logging module.
	 * @return properties. */
	public static Properties getSilentLoggingProperties(){
		Properties properties = new Properties();
		properties.put("log4j.rootLogger",				"ERROR,NULL"); 	// By default, do not show anything.
		properties.put("log4j.logger.org",				"ERROR,STDOUT");	// For this module, show warning messages in stdout.
		properties.put("log4j.logger.proactive", 		"ERROR,STDOUT");
		properties.put("log4j.logger.qosprober", 		"ERROR,STDOUT");
		/* NULL Appender. */
		properties.put("log4j.appender.NULL",			"org.apache.log4j.varia.NullAppender");
		/* STDOUT Appender. */
		properties.put("log4j.appender.STDOUT",			"org.apache.log4j.ConsoleAppender");
		properties.put("log4j.appender.STDOUT.Target",	"System.out");
		properties.put("log4j.appender.STDOUT.layout",	"org.apache.log4j.PatternLayout");
		properties.put("log4j.appender.STDOUT.layout.ConversionPattern","%-4r [%t] %-5p %c %x - %m%n");
		return properties;
	}
	
	/**
	 * Creates a default set of properties for the log4j logging module.
	 * @return properties. */
	public static Properties getVerboseLoggingProperties(){
		Properties properties = new Properties();
		properties.put("log4j.rootLogger",				"INFO,STDOUT"); 		// By default, show everything.
		/* STDOUT Appender. */
		properties.put("log4j.appender.STDOUT",			"org.apache.log4j.ConsoleAppender");
		properties.put("log4j.appender.STDOUT.Target",	"System.out");
		properties.put("log4j.appender.STDOUT.layout",	"org.apache.log4j.PatternLayout");
		properties.put("log4j.appender.STDOUT.layout.ConversionPattern","[%20.20c] %5p -     %m%n");
		return properties;
	}
	
	/**
	 * Configures de log4j module for logging.
	 * @param debuglevel debug level (or verbosity level) to be used when loading log4j properties. 
	 * @param givenlogfile path of the file where to put the logs, if null then log4j.properties will be used as default. */
	public static void log4jConfiguration(int debuglevel, String givenlogfile){
		System.setProperty("log4j.configuration", "");
		String defaultlogfile = "log4j.properties";
		if (debuglevel == PANagiosPlugin.DEBUG_LEVEL_3_USER){
			// We load the log4j.properties file. 
			if (givenlogfile != null && new File(givenlogfile).exists() == true){
				PropertyConfigurator.configure(givenlogfile);
			}else if (new File(defaultlogfile).exists() == true){
				PropertyConfigurator.configure(defaultlogfile);
			}else{
				Properties properties = Misc.getVerboseLoggingProperties();
				PropertyConfigurator.configure(properties);
			}
		}else {
			// We do the log4j configuration on the fly. 
			Properties properties = Misc.getSilentLoggingProperties();
			PropertyConfigurator.configure(properties);
		}
	}

	
	/**
	 * Parse the corresponding value, and if any problem, return the default value given. 
	 * @param o string to parse.
	 * @param defaultvalue default value in case of exception parsing the string.
	 * @return the result of the parsing as an Integer. */
	public static Integer parseInteger(String o, Integer defaultvalue){
		Integer ret;
		try{
			ret = Integer.parseInt(o);
		}catch(Exception e){
			ret = defaultvalue;
		}
		return ret;
	}
	
	/**
	 * Remove characters that are difficult to handle by the user when using them in bash or any other shell.
	 * @param argument string that needs to be checked out.
	 * @return the string given without any conflict character. */
	public static String removeConflictCharacters(String argument){
		String aux;
		if (argument == null){
			return null;
		}
		aux = argument;
		aux = aux.replace("'", "");
		aux = aux.replace("\"","");
		return aux;
	}
	
	/**
	 * Convert the given throwable's stack trace in a String. 
	 * @param aThrowable throwable to convert.
	 * @return the string. */
    public static String getStackTrace(Throwable aThrowable) {
	    final Writer result = new StringWriter();
	    final PrintWriter printWriter = new PrintWriter(result);
	    aThrowable.printStackTrace(printWriter);
	    return result.toString();
    }
}
