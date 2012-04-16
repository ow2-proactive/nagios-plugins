package org.ow2.proactive.nagios.common;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import org.apache.log4j.Logger;

/**
 * Class that deals with dumping script execution, if the user asked for it. */
public class Dumper {
	private static Logger logger = Logger.getLogger(Dumper.class.getName()); // Logger.
	private String scriptpath;	// Script to be executed in case of dump.
	private String source;		// Descriptive string telling what specific probe triggered this dump.
	
	/**
	 * Constructor method.
	 * @param scriptpath script to be executed for the dump.
	 * @param source descriptive string telling what specific probe triggered this dump. */
	public Dumper(String scriptpath, String source){
		this.scriptpath = scriptpath;
		this.source = source;
	}
	
	/**
	 * Start the dump script, giving as its arguments a NagiosReturnObject generated string.
	 * @param obj NagiosReturnObject to use to generate the arguments for the script. */
	public void dump(NagiosReturnObject obj){
		String stacktrace = "";
		if (obj.getException() != null){
			stacktrace = getStackTraceAsString(obj.getException());
			logger.info("Exception obtained converted to '" + stacktrace + "'...");
		}
    	String command = scriptpath + " SOURCE:[" + source + "]\nCODE:[" + obj.getErrorCode() + "]\nTIME:[" + (new Date()) + "]\nMESSAGE:[" + filter(obj.getWholeFirstLineMessage()) + "]\nSTACKTRACE:[" + filter(stacktrace) + "]";
    	command = filter(command);
		try {
			Process p = Runtime.getRuntime().exec(command);
			InputStream istr = p.getInputStream(); BufferedReader br = new BufferedReader( new InputStreamReader(istr)); String str; while ((str = br.readLine()) != null) logger.info("-         " + str);
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
	}
	
	private String filter(String source){
		return source.replace("\n", " /n").replace("(", "/lp").replace(")", "/rp").replace("*", "/as").replace("|", "/pipe");
	}
	
    private String getStackTraceAsString(Throwable exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        pw.print(exception.getClass().getName());
        exception.printStackTrace(pw);
        Throwable cause = exception.getCause();
        if (cause != null){
	        pw.print("Caused by:");
	        String str = getStackTraceAsString(cause);
	        pw.print(str);
        }
        return sw.toString();
    }

}
