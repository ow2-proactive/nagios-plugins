package qosprobercore.main;

import java.io.IOException;

import org.apache.log4j.Logger;

import qosprobercore.misc.Misc;

public class NagiosPlugin {
	public static final String NAG_OUTPUT_PREFIX = "SERVICE STATUS: ";
	
	public static final int DEBUG_LEVEL_0_SILENT	= 0;	// Debug level, silent mode. 
	public static final int DEBUG_LEVEL_1_EXTENDED 	= 1;	// Debug level, more than silent mode. Shows backtraces if error. 
	public static final int DEBUG_LEVEL_2_VERBOSE	= 2;	// Debug level, similar to the previous one.
	public static final int DEBUG_LEVEL_3_USER		= 3;	// Debug level, debugging only.
	
	private static Logger logger = Logger.getLogger(NagiosPlugin.class.getName()); // Logger.
	
	 /** 
     * Print a message in the stdout (for Nagios to use it) and return with the given error code. */
    public synchronized static void printAndExit(Integer ret, String str){
        System.out.println(NAG_OUTPUT_PREFIX + str);
        System.exit(ret);
    }

    /** 
     * Print a message in the stdout (for Nagios to use it) and return with the given error code. 
     * Print a back-trace later only if the debug-level is appropriate. */
    public synchronized static void printAndExit(Integer ret, String str, int debuglevel, Throwable e){
        switch(debuglevel){
            case DEBUG_LEVEL_0_SILENT:
                System.out.println(NAG_OUTPUT_PREFIX + str);
                break;
            default:
                System.out.println(NAG_OUTPUT_PREFIX + str);
                e.printStackTrace(System.out);
                break;
        }
        System.exit(ret);
    }
    
	/**
	 * Used when a parameter given by the user is wrong. 
	 * Print a message, then the usage of the application, and the exits the application. */
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
