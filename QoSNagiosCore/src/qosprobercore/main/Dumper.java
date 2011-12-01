package qosprobercore.main;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import org.apache.log4j.Logger;

public class Dumper {
	private static Logger logger = Logger.getLogger(Dumper.class.getName()); // Logger.
	private String scriptpath;
	private String source;
	
	public Dumper(String scriptpath, String source){
		this.scriptpath = scriptpath;
		this.source = source;
	}
	
	public void dump(NagiosReturnObject obj){
		String stacktrace = "";
		if (obj.getException() != null){
			stacktrace = getStackTraceAsString(obj.getException());
			logger.info("Exception obtained converted to '" + stacktrace + "'...");
		}
    	String[] command = {scriptpath,  "CODE:[" + obj.getErrorCode() + "]",  "SOURCE:[" + source + "] TIME:[" + (new Date()) + "] MESSAGE:[" + filter(obj.getWholeMessage()) + "] STACKTRACE:[" + filter(stacktrace) + "]"};
//		logger.info("Dumping with command >" + command + "<...");
		try {
			Process p = Runtime.getRuntime().exec(command);
			InputStream istr = p.getInputStream(); BufferedReader br = new BufferedReader( new InputStreamReader(istr)); String str; while ((str = br.readLine()) != null) logger.info("-         " + str);
		} catch (Exception e) {
			logger.warn(e.getMessage());
		}
	}
	
	private String filter(String source){
		return source.replace("\n", " /n ").replace("(", " ").replace(")", " ").replace("*", " /a ").replace("|", "  _  ");
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
