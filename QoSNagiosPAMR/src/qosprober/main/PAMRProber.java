package qosprober.main;

import jargs.gnu.CmdLineParser;
import java.io.File;
import java.io.IOException;
import java.security.KeyException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.security.auth.login.LoginException;
import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.core.node.NodeException;
import org.ow2.proactive.resourcemanager.exception.RMException;
import qosprober.exceptions.ElementNotFoundException;
import qosprober.misc.Misc;


/** 
 * This is a general Nagios plugin class that performs a test on the PAMR Router of ProActive based grids.
 *  After that, a short summary regarding the result of the test is shown using Nagios format. */
public class PAMRProber {

	public static final String NAG_OUTPUT_PREFIX = "SERVICE STATUS: ";
	/** Nagios exit codes. */
	public static final int RESULT_OK       = 0; 			// Nagios code. Execution successfully. 
	public static final int RESULT_WARNING  = 1; 			// Nagios code. Warning. 
	public static final int RESULT_CRITICAL = 2; 			// Nagios code. Critical problem in the tested entity.
	public static final int RESULT_UNKNOWN  = 3; 			// Nagios code. Unknown state of the tested entity.
	
	public static final int DEBUG_LEVEL_0SILENT		= 0;	// Debug level, silent mode. 
	public static final int DEBUG_LEVEL_1EXTENDED 	= 1;	// Debug level, more than silent mode. Shows backtraces if error. 
	public static final int DEBUG_LEVEL_2VERBOSE	= 2;	// Debug level, similar to the previous one.
	public static final int DEBUG_LEVEL_3USER		= 3;	// Debug level, debugging only.
	
	public final static String SERVER_NAME = "server";		// Name that the server will use to register itself.
    public final static String PREFIX_URL = "pamr://";		// Prefix that is expected in a URL given by the registered server.
    
    public final static int MESSAGE_LENGTH = 1024;			// Length of the parameter passed when the client calls the server.
    
	private static String lastStatus;						// Holds a message representative of the current status of the test.
															// It is used in case of TIMEOUT, to help the administrator guess
															// where the problem is.
	
	public static Logger logger = Logger.getLogger(PAMRProber.class.getName()); // Logger.
	
	
	/**
	 * Starting point.
	 * The arguments/parameters are specified in the file /resources/usage.txt
	 * @return Nagios error code. */
	public static void main(String[] args) throws Exception{
		
		PAMRProber.setLastStatuss("started, parsing arguments...");
		
		/* Parsing of arguments. */
		CmdLineParser parser = new CmdLineParser();
		
		CmdLineParser.Option debugO = parser.addIntegerOption('v', "debug");			// Debug mode, 0 is silent, 3 is verbose (log4j file loaded).
		CmdLineParser.Option timeoutsecO = parser.addIntegerOption('t', "timeout");
		CmdLineParser.Option timeoutwarnsecO = parser.addIntegerOption('n', "timeoutwarning");
		CmdLineParser.Option paconfO = parser.addStringOption('f', "paconf");
		CmdLineParser.Option hostO = parser.addStringOption('H', "hostname");
		CmdLineParser.Option warningO = parser.addStringOption('w', "warning");
		CmdLineParser.Option criticalO = parser.addStringOption('c', "critical");

		try {
		    parser.parse(args);
		} catch ( CmdLineParser.OptionException e ) {
			/* In case something is not expected, print usage and exit. */
		    System.out.println(e.getMessage());
		    Misc.printUsage();
		    System.exit(RESULT_CRITICAL);
		}
		
		final Integer debug = (Integer)parser.getOptionValue(debugO, 0); 				// If false, only Nagios output.
		final Integer timeoutsec = (Integer)parser.getOptionValue(timeoutsecO); 		// Timeout in seconds for the job to be executed.
		final Integer timeoutwarnsec = 
			(Integer)parser.getOptionValue(timeoutwarnsecO,timeoutsec); 				// Timeout in seconds for the warning message to be thrown.
		final String paconf = (String)parser.getOptionValue(paconfO); 					// Path of the ProActive xml configuration file.
		final String host = (String)parser.getOptionValue(hostO); 						// Host to be tested. Ignored.
		final String warning = (String)parser.getOptionValue(warningO, "ignored");		// Warning level. Ignored.
		final String critical = (String)parser.getOptionValue(criticalO, "ignored"); 	// Critical level. Ignored. 
		
		
		/* Validating the arguments. */
		
		String errorMessage = "";
		Boolean errorParam = false;
		if (timeoutsec == null)	{errorParam=true; errorMessage+="'Timeout' (sec) not defined... ";}
		if (paconf == null)		{errorParam=true; errorMessage+="'ProActiveConfiguration path' not defined... ";}	
		if (errorParam==true)
		{
			/* In case something is not expected, print usage and exit. */
		    System.out.println("There are some missing mandatory parameters: " + errorMessage);
		    Misc.printUsage();
		    System.exit(RESULT_CRITICAL);
		}
		
		
		Misc.log4jConfiguration(debug);							// Load log4j configuration file.
		
		PAMRProber.setLastStatuss("parameters parsed, doing log4j configuration...");
		
		/* Show all the arguments considered. */
		logger.info(
				"Configuration: \n" +
				"\t debug              : " + debug + "\n" +
				"\t timeout            : " + timeoutsec + "\n" +
				"\t warning timeout    : " + timeoutwarnsec + "\n" +
				"\t paconf             : " + paconf + "\n" +
				"\t host               : " + host + "\n" +
				"\t warning            : " + warning  + "\n" +
				"\t critical           : " + critical + "\n" 
				);
		
		PAMRProber.setLastStatuss("log4j configuration done, loading security policy...");
		
		/* Security policy procedure. */
		logger.info("Setting security policies... ");
		Misc.createPolicyAndLoadIt();
		logger.info("Done.");
		
		PAMRProber.setLastStatuss("security policy loaded, loading proactive configuration (if needed)...");
		
		/* Check whether to use or not the ProActive configuration file. */
		if (paconf!=null){
			/* A ProActiveConf.xml file was given. If we find it, we use it. */
			if (new File(paconf).exists()==false){
				String msg = "The ProActive configuration file '"+paconf+"' was not found.";
				logger.fatal(msg);
				throw new ElementNotFoundException(msg);
			}
			System.setProperty("proactive.configuration", paconf);
		}
		
		PAMRProber.setLastStatuss("proactive configuration loaded, initializing probe module...");
		
		/* No need of other arguments. */
		//String[] otherArgs = parser.getRemainingArgs();
		
		/* We prepare our probe to run it in a different thread. */
		/* The probe consists in a node obtaining done from the Resource Manager. */
		ExecutorService executor = Executors.newFixedThreadPool(1);
		
		Callable<Object[]> proberCallable = new Callable<Object[]>(){
			public Object[] call() throws Exception {
				return PAMRProber.probe(timeoutsec, paconf, timeoutwarnsec);
			}
		};

		/* We submit to the executor the prober activity. */
		Future<Object[]> proberFuture = executor.submit(proberCallable); // We ask to execute the probe.
		
		try{
			/* We execute the future using a timeout. */
			Object[] res = proberFuture.get(timeoutsec, TimeUnit.SECONDS);
			/* At this point all went okay. */ 
			PAMRProber.printAndExit((Integer)res[0], (String)res[1]);
		}catch(TimeoutException e){
			/* The execution took more time than expected. */
			PAMRProber.printAndExit(
					PAMRProber.RESULT_CRITICAL, 
					NAG_OUTPUT_PREFIX + "TIMEOUT OF "+timeoutsec+ "s (last status was '" + PAMRProber.getLastStatus() + "')", 
					debug, 
					e);
		}catch(ExecutionException e){
			/* There was an unexpected problem with the execution of the prober. */
			PAMRProber.printAndExit(
					PAMRProber.RESULT_CRITICAL, 
					NAG_OUTPUT_PREFIX + "FAILURE: " + e.getMessage(), 
					debug, 
					e);
		}catch(Exception e){
			/* There was an unexpected critical exception not captured. */
			PAMRProber.printAndExit(
					PAMRProber.RESULT_CRITICAL, 
					NAG_OUTPUT_PREFIX + "CRITICAL ERROR: " + e.getMessage(), 
					debug, 
					e);
		}
	}
	
	
	/**
	 * Probe the scheduler
	 * A few calls are done against the Resource Manager (RM):
	 *   - join
	 *   - get node/s
	 *   - release node/s
	 *   - disconnect
	 * @return Object[Integer, String] with Nagios code error and a descriptive message of the test. 
	 * @throws RMException 
	 * @throws LoginException 
	 * @throws KeyException 
	 * @throws IOException 
	 * @throws NodeException, Exception 
	 * @throws ActiveObjectCreationException */	 
	public static Object[] probe(int timeoutsec, String paconffile, int timeoutwarnsec) throws KeyException, LoginException, RMException, IOException, ActiveObjectCreationException, NodeException, Exception{
		// This is done.
		TimeTick timing = new TimeTick();
		
    	String serverurl = null;
    	Server server = null;
    
    	PAMRProber.setLastStatuss("initializing the probe...");
    	
        // Creates an active object for the server
    	logger.info("Initializing the server...");
        server = org.objectweb.proactive.api.PAActiveObject.newActive(Server.class, null);
        logger.info("Done.");
        
        double time_initializing = timing.tickSec();
        
        PAMRProber.setLastStatuss("initialized the server, registering the server...");
        
        logger.info("Registering server...");
        org.objectweb.proactive.api.PAActiveObject.registerByName(server, SERVER_NAME);
        String url = org.objectweb.proactive.api.PAActiveObject.getActiveObjectNodeUrl(server);
        logger.info("Done.");
        
        serverurl = PREFIX_URL + Misc.getResourceNumberFromURL(url) + "/" + SERVER_NAME;
        logger.info(">>> Server standard URL: "+ serverurl);
        
        PAMRProber.setLastStatuss("registered the server, running the client...");
        double time_registering_server = timing.tickSec();
        
        logger.info("Running the client...");
        Misc.runNewJVM(Client.class.getName(), paconffile + " " + serverurl);
        logger.info("Done.");
        
        double time_executing_client = timing.tickSec();
        
        logger.info("Waiting for the client's message...");
        
        PAMRProber.setLastStatuss("executed the client, waiting for its message...");
   
    	while(server.isDone()==false){
    		Thread.sleep(300);
    	}
    
    	PAMRProber.setLastStatuss("received the message from client, finishing...");
    	
    	double time_waiting_message = timing.tickSec();
    	logger.info("Done!");
    	
    	
		int output_to_return;
		String output_to_print = null;
		
		double time_all = time_initializing+time_registering_server+time_executing_client+time_waiting_message;
		
		String timesummary =
			"time_initializing=" + String.format(Locale.ENGLISH, "%1.03f", time_initializing)   + "s " +
			"time_registering_server=" + String.format(Locale.ENGLISH, "%1.03f", time_registering_server) + "s " +
			"time_executing_client=" + String.format(Locale.ENGLISH, "%1.03f", time_executing_client) + "s " +
			"time_waiting_message=" + String.format(Locale.ENGLISH, "%1.03f", time_waiting_message   )   + "s " +
			"timeout_threshold=" + String.format(Locale.ENGLISH, "%1.03f", (float)timeoutsec)   + "s " +
			"time_all_warning_threshold=" + String.format(Locale.ENGLISH, "%1.03f", (float)timeoutwarnsec)   + "s " +
			"time_all="   + String.format(Locale.ENGLISH, "%1.03f", time_all) + "s"; 
		

		if (time_all > timeoutwarnsec){						// If longer than timeoutwarnsec, warning message.
			output_to_return = PAMRProber.RESULT_WARNING;
			output_to_print = 
				NAG_OUTPUT_PREFIX + "WARNING STATE, SLOW PROBE | " + timesummary;
		}else{												// Else everything was okay.
			output_to_return = PAMRProber.RESULT_OK;
			output_to_print = 
				NAG_OUTPUT_PREFIX + "OK | " + timesummary;
		}
		
		Object [] ret = new Object[2];							// Both, error code and message are returned to be shown.
		ret[0] = new Integer(output_to_return);
		ret[1] = output_to_print;
			
		return ret;
	}
	
	/** 
	 * Save a message regarding the last status of the probe. 
	 * This last status will be used in case of timeout to tell Nagios up to which point
	 * (logging, job submission, job retrieval, etc.) the probe arrived. */
	public synchronized static void setLastStatuss(String laststatus){
		PAMRProber.lastStatus = laststatus;
	}
	
	/** 
	 * Get a message regarding the last status of the probe. 
	 * This last status will be used in case of timeout to tell Nagios up to which point
	 * (logging, job submission, job retrieval, etc.) the probe arrived. 
	 * @return the last status of the test. */
	public synchronized static String getLastStatus(){
		return PAMRProber.lastStatus;
	}
	
	/** 
	 * Print a message in the stdout (for Nagios to use it) and return with the given error code. */
	public synchronized static void printAndExit(Integer ret, String str){
    	System.out.println(str);
    	System.exit(ret);
    }
    
	/** 
	 * Print a message in the stdout (for Nagios to use it) and return with the given error code. 
	 * Print a backtrace only if the debuglevel is appropriate. */
	public synchronized static void printAndExit(Integer ret, String str, int debuglevel, Throwable e){
		switch(debuglevel){
			case PAMRProber.DEBUG_LEVEL_0SILENT:
				System.out.println(str);
				break;
			default:
				System.out.println(str);
				e.printStackTrace(System.out);
				break;
			
		}
    	System.exit(ret);
    }
}

