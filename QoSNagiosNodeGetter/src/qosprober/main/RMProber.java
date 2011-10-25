package qosprober.main;

import jargs.gnu.CmdLineParser;
import java.io.File;
import java.security.KeyException;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.security.auth.login.LoginException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.objectweb.proactive.core.config.ProActiveConfiguration;
import org.objectweb.proactive.core.node.Node;
import org.ow2.proactive.resourcemanager.exception.RMException;
import org.ow2.proactive.utils.NodeSet;
import qosprober.misc.Misc;


/** 
 * This is a general Nagios plugin class that performs a test on the RM, by doing:
 *    -Node obtaining
 *    -Node retrieval 
 *  After that, a short summary regarding the result of the test is shown using Nagios format. */
public class RMProber {

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
	
	public static final String COMMUNICATION_PROTOCOL =
			"pamr";											// Default protocol to be used to get connected to the RM. 
	private static String lastStatus;						// Holds a message representative of the current status of the test.
															// It is used in case of TIMEOUT, to help the administrator guess
															// where the problem is.
	
	public static Logger logger = Logger.getLogger(RMProber.class.getName()); // Logger.
	
	/**
	 * Starting point.
	 * The arguments/parameters are specified in the file /resources/usage.txt
	 * @return Nagios error code. */
	public static void main(String[] args) throws Exception{
		
		RMProber.setLastStatuss("started, parsing arguments and basic initialization...");
		
		/* Parsing of arguments. */
		CmdLineParser parser = new CmdLineParser();
	
		/* Defining the command line parameters interpretation. */
		CmdLineParser.Option debugO = parser.addIntegerOption('v', "debug");
		CmdLineParser.Option userO = parser.addStringOption('u', "user");
		CmdLineParser.Option passO = parser.addStringOption('p', "pass");
		CmdLineParser.Option urlO = parser.addStringOption("url");
		CmdLineParser.Option nodesrequiredO = parser.addIntegerOption('r', "nodes");
		CmdLineParser.Option nodeswarningO = parser.addIntegerOption("nodeswarning");
		CmdLineParser.Option nodescriticalO = parser.addIntegerOption("nodescritical");
		CmdLineParser.Option timeoutsecO = parser.addIntegerOption('t', "timeout");
		CmdLineParser.Option timeoutwarnsecO = parser.addIntegerOption('n', "timeoutwarning");
		CmdLineParser.Option paconfO = parser.addStringOption('f', "paconf");
		CmdLineParser.Option hostO = parser.addStringOption('H', "hostname");
		CmdLineParser.Option portO = parser.addStringOption("port");
		CmdLineParser.Option warningO = parser.addStringOption('w', "warning");
		CmdLineParser.Option criticalO = parser.addStringOption('c', "critical");

		try {
		    parser.parse(args);
		} catch ( CmdLineParser.OptionException e ) {
			/* In case something is not expected, print usage and exit. */
		    Misc.printMessageUsageAndExit(e.getMessage());
		}
		
		final Integer debug =
			(Integer)parser.getOptionValue(debugO, RMProber.DEBUG_LEVEL_1EXTENDED); 	// Level of verbosity.
		final String user = (String)parser.getOptionValue(userO);			 			// User.
		final String pass = (String)parser.getOptionValue(passO); 						// Pass.
		final String url = (String)parser.getOptionValue(urlO); 						// Url of the Scheduler/RM.
		final Integer nodesrequired = (Integer)parser.getOptionValue(nodesrequiredO,1); // Amount of nodes to be asked to the Resource Manager.
		final Integer nodeswarning =
				(Integer)parser.getOptionValue(nodeswarningO,nodesrequired);   			// Obtaining fewer nodes than this, a warning message will be thrown. 
		final Integer nodescritical =
				(Integer)parser.getOptionValue(nodescriticalO,nodesrequired); 			// Obtaining fewer nodes than this, a critical message will be thrown. 
		final Integer timeoutsec = (Integer)parser.getOptionValue(timeoutsecO); 		// Timeout in seconds for the job to be executed.
		final Integer timeoutwarnsec = 
			(Integer)parser.getOptionValue(timeoutwarnsecO,timeoutsec); 				// Timeout in seconds for the warning message to be thrown.
		final String paconf = (String)parser.getOptionValue(paconfO); 					// Path of the ProActive xml configuration file.
		final String host = (String)parser.getOptionValue(hostO);			 			// Host to be tested. 
		final String port = (String)parser.getOptionValue(portO);						// Port of the host to be tested. 
		final String warning = (String)parser.getOptionValue(warningO, "ignored");		// Warning level. Ignored.
		final String critical = (String)parser.getOptionValue(criticalO, "ignored"); 	// Critical level. Ignored. 
		
		
		/* Checking if the mandatory parameters are provided. */
		String errorMessage = "";
		Boolean errorParam = false;
		if (user == null)		{errorParam=true; errorMessage+="'user' not defined... ";}
		if (pass == null)		{errorParam=true; errorMessage+="'pass' not defined... ";}
		if (timeoutsec == null)	{errorParam=true; errorMessage+="'timeout' (sec) not defined... ";}
		if (errorParam==true) {
			/* In case something is not expected, print usage and exit. */
			Misc.printMessageUsageAndExit("There are some missing mandatory parameters: " + errorMessage);
		}
		
		/* Loading log4j configuration. */
		log4jConfiguration(debug);
		
		/* Show all the arguments considered. */
		logger.info(
				"Configuration: \n" +
				"\t debug              : " + debug + "\n" +
				"\t user               : " + user + "\n" +
				"\t pass               : " + pass + "\n" +
				"\t url                : " + url + "\n" +
				"\t nodes required     : " + nodesrequired + "\n" +
				"\t min. nodes warning : " + nodeswarning + "\n" +
				"\t min. nodes critical: " + nodescritical + "\n" +
				"\t timeout            : " + timeoutsec + "\n" +
				"\t warning timeout    : " + timeoutwarnsec + "\n" +
				"\t paconf             : " + paconf + "\n" +
				"\t host               : " + host + "\n" +
				"\t port               : " + port + "\n" +
				"\t warning            : " + warning  + "\n" +
				"\t critical           : " + critical + "\n" 
				);
		
		RMProber.setLastStatuss("basic initialization done, loading security policy...");
		
		/* Security policy procedure. */
		logger.info("Setting security policies... ");
		Misc.createPolicyAndLoadIt();
		logger.info("Done.");
		
		RMProber.setLastStatuss("security policy loaded, loading proactive configuration (if needed)...");
		
		/* Load ProActive configuration. */
		boolean usepaconffilee = false;
		/* Check whether to use or not the ProActive configuration file. */
		if (paconf!=null){
			/* A ProActiveConf.xml file was given. If we find it, we use it. */
			if (new File(paconf).exists()==true){
				System.setProperty("proactive.configuration", paconf);
				usepaconffilee = true;
			}else{
				logger.warn("The ProActive configuration file '"+paconf+"' was not found. Using default configuration.");
			}
		}
		
		if (usepaconffilee == false){
			logger.info("Avoiding ProActive configuration file...");
			ProActiveConfiguration pac = ProActiveConfiguration.getInstance();	
			pac.setProperty("proactive.communication.protocol", COMMUNICATION_PROTOCOL, false);
			if (host==null || port==null){
				Misc.printMessageUsageAndExit("Parameters 'hostname' and 'port' must be given.\n");
			}
			pac.setProperty("proactive.net.router.address", host, false);
			pac.setProperty("proactive.net.router.port", port, false);
		}
		RMProber.setLastStatuss("proactive configuration loaded, initializing probe module...");
		
		/* Now we prepare our probe to run it in a different thread. */
		/* The probe consists in a node obtaining done from the Resource Manager. */
		ExecutorService executor = Executors.newFixedThreadPool(1);
		
		Callable<Object[]> proberCallable = new Callable<Object[]>(){
			public Object[] call() throws Exception {
				return RMProber.probe(url, user, pass, timeoutsec, timeoutwarnsec, nodesrequired, nodeswarning, nodescritical);
			}
		};

		/* We submit to the executor the prober activity (and the prober will then 
		 * obtain a node from the RM in that activity). */
		Future<Object[]> proberFuture = executor.submit(proberCallable); // We ask to execute the probe.
		
		try{
			/* We execute the future using a timeout. */
			Object[] res = proberFuture.get(timeoutsec, TimeUnit.SECONDS);
			/* At this point all went okay. */ 
			RMProber.printAndExit((Integer)res[0], (String)res[1]);
		}catch(TimeoutException e){
			/* The execution took more time than expected. */
			RMProber.printAndExit(
					RMProber.RESULT_CRITICAL, 
					NAG_OUTPUT_PREFIX + "TIMEOUT OF "+timeoutsec+ "s (last status was '" + RMProber.getLastStatus() + "')", 
					debug, 
					e);
		}catch(ExecutionException e){
			/* There was an unexpected problem with the execution of the prober. */
			RMProber.printAndExit(
					RMProber.RESULT_CRITICAL, 
					NAG_OUTPUT_PREFIX + "FAILURE: " + e.getMessage(), 
					debug, 
					e);
		}catch(Exception e){
			/* There was an unexpected critical exception not captured. */
			RMProber.printAndExit(
					RMProber.RESULT_CRITICAL, 
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
	 * @throws KeyException */	 
	public static Object[] probe(String url, String user, String pass, int timeoutsec, int timeoutwarnsec, int nodesRequired, int nodesminimumwarning, int nodesminimumcritical) throws KeyException, LoginException, RMException{
		
		TimeTick timer = new TimeTick(); // We want to get time durations of each operation.
		
		/* We get connected to the Scheduler through this stub, later we submit a job, etc. */
		RMStubProber rmstub; 
		
		rmstub = new RMStubProber();
		
		double time_initializing = timer.tickSec();
		
		RMProber.setLastStatuss("RM stub created, connecting to RM...");
		logger.info("Connecting to Resource Manager... "); 	// Connecting to RM...
		rmstub.init(url, user, pass); 						// Login procedure...
		logger.info("Done.");
	
		RMProber.setLastStatuss("connected to RM, getting nodes...");
		
		double time_connection = timer.tickSec();
		
		int output_to_return = RMProber.RESULT_CRITICAL; 
		String output_to_print = 
			NAG_OUTPUT_PREFIX + "NO TEST PERFORMED"; 		// Default output (for Nagios).
		
		
		logger.info("Getting nodes...");
		NodeSet nodes = rmstub.getNodes(nodesRequired); 	// Request some nodes.
		
		
		double time_getting_nodes = timer.tickSec();
		
		int obtainednodes = nodes.size();
		logger.info("\tListing nodes...");					// List the nodes obtained.
    	for(Node n:nodes){
    		logger.info("\t - " + n.getNodeInformation().getName());
    	}
    	logger.info("Done.");
    	
    	
    	RMProber.setLastStatuss("obtained nodes, releasing nodes...");
    	
    	logger.info("Releasing nodes...");					// Release the nodes obtained.
    	rmstub.releaseNodes(nodes);
    	logger.info("Done.");
    	
    	double time_releasing_nodes = timer.tickSec();		
    	
    	RMProber.setLastStatuss("released nodes, disconnecting...");
    	
    	logger.info("Disconnecting...");					// Disconnecting from RM.
    	rmstub.disconnect();
    	logger.info("Done.");
    				
		double time_disconn = timer.tickSec();
		
		double time_all = time_initializing+time_connection+time_getting_nodes+time_releasing_nodes+time_disconn;
		
		String timesummary =
			"nodes_required=" + (nodesRequired) + " " +
			"nodes_obtained=" + (obtainednodes) + " " +
			"time_connection=" + String.format(Locale.ENGLISH, "%1.03f", time_connection)   + "s " +
			"time_getting_nodes=" + String.format(Locale.ENGLISH, "%1.03f", time_getting_nodes) + "s " +
			"time_releasing_nodes=" + String.format(Locale.ENGLISH, "%1.03f", time_releasing_nodes) + "s " +
			"time_disconnection=" + String.format(Locale.ENGLISH, "%1.03f", time_disconn   )   + "s " +
			"timeout_threshold=" + String.format(Locale.ENGLISH, "%1.03f", (float)timeoutsec)   + "s " +
			"time_all_warning_threshold=" + String.format(Locale.ENGLISH, "%1.03f", (float)timeoutwarnsec)   + "s " +
			"time_all="   + String.format(Locale.ENGLISH, "%1.03f", time_all) + "s"; 
		
		String summary = "(obtained/required/critical/warning)=(" + obtainednodes + "/" + nodesRequired + "/" + nodesminimumcritical + "/" + nodesminimumwarning + ")";
		
		if (obtainednodes < nodesminimumcritical){	// Else everything was okay.
			output_to_return = RMProber.RESULT_CRITICAL;
			output_to_print = 
				NAG_OUTPUT_PREFIX + "CRITICAL STATE, TOO FEW NODES OBTAINED "+ summary + " | " + timesummary;
		}else if (obtainednodes < nodesminimumwarning){		// Else everything was okay.
			output_to_return = RMProber.RESULT_WARNING;
			output_to_print = 
				NAG_OUTPUT_PREFIX + "WARNING STATE, TOO FEW NODES OBTAINED "+ summary + " | " + timesummary;
		}else if (time_all > timeoutwarnsec){						// If longer than timeoutwarnsec, warning message.
			output_to_return = RMProber.RESULT_WARNING;
			output_to_print = 
				NAG_OUTPUT_PREFIX + "WARNING STATE, " + obtainednodes + " NODE/S OBTAINED TOO SLOWLY | " + timesummary;
		}else{												// Else everything was okay.
			output_to_return = RMProber.RESULT_OK;
			output_to_print = 
				NAG_OUTPUT_PREFIX + obtainednodes + " NODE/S OBTAINED OK | " + timesummary;
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
		RMProber.lastStatus = laststatus;
	}
	
	/** 
	 * Get a message regarding the last status of the probe. 
	 * This last status will be used in case of timeout to tell Nagios up to which point
	 * (logging, job submission, job retrieval, etc.) the probe arrived. 
	 * @return the last status of the test. */
	public synchronized static String getLastStatus(){
		return RMProber.lastStatus;
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
			case RMProber.DEBUG_LEVEL_0SILENT:
				System.out.println(str);
				break;
			default:
				System.out.println(str);
				e.printStackTrace(System.out);
				break;
			
		}
    	System.exit(ret);
    }
	
	/**
	 * Creates a default set of properties for the log4j logging module. */
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
	 * Configures de log4j module for logging. */
	public static void log4jConfiguration(int debuglevel){
		System.setProperty("log4j.configuration", "");
		if (debuglevel == RMProber.DEBUG_LEVEL_3USER){
			/* We load the log4j.properties file. */
			PropertyConfigurator.configure("log4j.properties");
		}else {
			/* We do the log4j configuration on the fly. */
			Properties properties = RMProber.getSilentLoggingProperties();
			PropertyConfigurator.configure(properties);
		}
	}
	
}

