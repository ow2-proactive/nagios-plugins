package qosprober.main;

import jargs.gnu.CmdLineParser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyException;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.security.auth.login.LoginException;
import qosprober.misc.Misc;
import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.core.config.ProActiveConfiguration;
import org.objectweb.proactive.core.node.NodeException;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import qosprober.exceptions.ElementNotFoundException;
import qosprober.exceptions.InvalidProtocolException;

/** 
 * This is a general Nagios plugin class that performs a test on the scheduler, by doing:
 *    -Job submission
 *    -Job result retrieval
 *    -Job result comparison 
 *  After that the result of the test is shown using Nagios format. */
public class JobProber {

	/** Nagios exit codes. */
	public static final int RESULT_OK = 0; 					// Nagios code. Execution successfully. 
	public static final int RESULT_WARNING = 1; 			// Nagios code. Warning. 
	public static final int RESULT_CRITICAL = 2; 			// Nagios code. Critical problem in the tested entity.
	public static final int RESULT_UNKNOWN = 3; 			// Nagios code. Unknown state of the tested entity.
	
	private static String lastStatus;						// Holds a message representative of the current status of the test.
															// It is used in case of TIMEOUT, to help the administrator guess
															// where the problem is.
	
	public static Logger logger = Logger.getLogger(JobProber.class.getName()); // Logger.
	
	/**
	 * Starting point.
	 * The arguments/parameters are specified in the file /resources/usage.txt
	 * @return Nagios error code. */
	public static void main(String[] args) throws Exception{
		
		JobProber.setLastStatuss("started, parsing arguments...");
		
		/* Parsing of arguments. */
		CmdLineParser parser = new CmdLineParser();
		
		CmdLineParser.Option debugO = parser.addBooleanOption('v', "debug");
		CmdLineParser.Option userO = parser.addStringOption('u', "user");
		CmdLineParser.Option passO = parser.addStringOption('p', "pass");
		CmdLineParser.Option protocolO = parser.addStringOption("protocol");
		CmdLineParser.Option jobpathO = parser.addStringOption('j',"jobpath");
		CmdLineParser.Option urlO = parser.addStringOption("url");
		CmdLineParser.Option timeoutsecO = parser.addIntegerOption('t', "timeout");
		CmdLineParser.Option paconfO = parser.addStringOption('f', "paconf");
		CmdLineParser.Option hostO = parser.addStringOption('H', "hostname");
		CmdLineParser.Option warningO = parser.addStringOption('w', "warning");
		CmdLineParser.Option criticalO = parser.addStringOption('c', "critical");

		try {
		    parser.parse(args);
		} catch ( CmdLineParser.OptionException e ) {
			/* In case something is not expected, print usage and exit. */
		    System.err.println(e.getMessage());
		    JobProber.printUsage();
		    System.exit(RESULT_CRITICAL);
		}
		
		final Boolean debug = (Boolean)parser.getOptionValue(debugO, Boolean.FALSE); 	// If false, only Nagios output.
		final String user = (String)parser.getOptionValue(userO);			 			// User.
		final String pass = (String)parser.getOptionValue(passO); 						// Pass.
		final String protocol = (String)parser.getOptionValue(protocolO);			 	// Protocol, either REST or JAVAPA.
		final String jobpath = (String)parser.getOptionValue(jobpathO); 				// Path of the job descriptor (xml).
		final String url = (String)parser.getOptionValue(urlO); 						// Url of the Scheduler/RM.
		final Integer timeoutsec = (Integer)parser.getOptionValue(timeoutsecO,60); 		// Timeout in seconds for the job to be executed.
		final String paconf = (String)parser.getOptionValue(paconfO); 					// Path of the ProActive xml configuration file.
		final String host = (String)parser.getOptionValue(hostO); 						// Host to be tested. Ignored.
		final String warning = (String)parser.getOptionValue(warningO, "ignored");		// Warning level. Ignored.
		final String critical = (String)parser.getOptionValue(criticalO, "ignored"); 	// Critical level. Ignored. 
		
		
		if (jobpath == null || user == null || pass == null || protocol == null || jobpath == null || timeoutsec == null){
			/* In case something is not expected, print usage and exit. */
		    logger.fatal("There are some missing parameters.");
		    JobProber.printUsage();
		    System.exit(RESULT_CRITICAL);
		}
		
		if (debug == true){
			/* We load the log4j.properties file. */
			PropertyConfigurator.configure("log4j.properties");
		}else{
			/* We do the log4j configuration on the fly. */
			Properties properties = JobProber.getMainLoggingProperties();
			PropertyConfigurator.configure(properties);
		}
		
		JobProber.setLastStatuss("parameters parsed, doing log4j configuration...");
		
		/* Testing of the log4j behavior. */
		//logger.debug("DEBUG TEST MESSAGE");
		//logger.info("INFO TEST MESSAGE");
		//logger.warn("WARN TEST MESSAGE");
		//logger.error("ERROR TEST MESSAGE");
		
		/* Show all the arguments considered. */
		logger.info(
				"Configuration: \n" +
				"\t debug   : " + debug + "\n" +
				"\t user    : " + user + "\n" +
				"\t pass    : " + pass + "\n" +
				"\t prot    : " + protocol + "\n" +
				"\t jobpath : " + jobpath + "\n" +
				"\t url     : " + url + "\n" +
				"\t timeout : " + timeoutsec + "\n" +
				"\t paconf  : " + paconf + "\n" +
				"\t host    : " + host + "\n" +
				"\t warning : " + warning  + "\n" +
				"\t critical: " + critical + "\n" 
				);
		
		JobProber.setLastStatuss("log4j configuration done, loading security policy...");
		
		/* Security policy procedure. */
		logger.info("Setting security policies... ");
		JobProber.createPolicyAndLoadIt();
		logger.info("Done.");
		
		JobProber.setLastStatuss("security policy loaded, loading proactive configuration (if needed)...");
		
		/* Check whether to use or not the ProActive configuration file. */
		Boolean usepaconffilee = false;
		if (paconf!=null){
			/* A ProActiveConf.xml file was given. If we find it, we use it. */
			if (new File(paconf).exists()==false){
				String msg = "The ProActive configuration file '"+paconf+"' was not found.";
				logger.fatal(msg);
				throw new ElementNotFoundException(msg);
			}
			System.setProperty("proactive.configuration", paconf);
			usepaconffilee = true;
		}
		
		JobProber.setLastStatuss("proactive configuration loaded, initializing probe module...");
		
		final Boolean usepaconffile = usepaconffilee;
		
		/* No need of other arguments. */
		//String[] otherArgs = parser.getRemainingArgs();
		
		/* We prepare our probe to run it in a different thread. */
		/* The probe consists in a job submission done to the Scheduler. */
		ExecutorService executor = Executors.newFixedThreadPool(1);
		
		Callable<Object[]> proberCallable = new Callable<Object[]>(){
			public Object[] call() throws Exception {
				return JobProber.probe(url, user, pass, protocol, jobpath, timeoutsec, usepaconffile);
			}
		};

		/* We submit to the executor the prober activity (and the prober will then 
		 * submit a job to the scheduler in that activity). */
		Future<Object[]> proberFuture = executor.submit(proberCallable); // We ask to execute the probe.
		
		try{
			/* We execute the future using a timeout. */
			Object[] res = proberFuture.get(timeoutsec, TimeUnit.SECONDS);
			/* At this point all went okay. */ 
			JobProber.printAndExit((Integer)res[0], (String)res[1]);
		}catch(TimeoutException e){
			/* The execution took more time than expected. */
			JobProber.printAndExit(JobProber.RESULT_CRITICAL, "JOB RESULT - TIMEOUT ("+timeoutsec+" seconds, last status was '" + JobProber.getLastStatus() + "')");
		}catch(ExecutionException e){
			/* There was an unexpected problem with the execution of the prober. */
			JobProber.printAndExit(JobProber.RESULT_CRITICAL, "JOB RESULT - FAILURE: " + e.getMessage());
		}catch(Exception e){
			/* There was an unexpected critical exception not captured. */
			JobProber.printAndExit(JobProber.RESULT_CRITICAL, "JOB RESULT - CRITICAL ERROR: " + e.getMessage());
		}
	}
	
	
	/**
	 * Probe the scheduler
	 * Several calls are done against the scheduler:
	 *   - join
	 *   - submit job
	 *   - get job status (or event registering, depends on the protocol chosen)
	 *   - get job result
	 *   - remove job
	 *   - disconnect
	 *  After a correct disconnection call, the output of the job is compared with a 
	 *  given correct output, and the result of the test is told. 
	 * @return Object[Integer, String] with Nagios code error and a descriptive message of the test. */	 
	public static Object[] probe(String url, String user, String pass, String protocol, String jobpath, int timeoutsec, Boolean usepaconffile) throws IllegalArgumentException, LoginException, KeyException, ActiveObjectCreationException, NodeException, HttpException, SchedulerException, InvalidProtocolException, IOException, Exception{
		
		
		/* We get connected to the Scheduler through this stub, later we submit a job, etc. */
		SchedulerStubProber schedulerstub; 
		
		ProActiveProxyProtocol papp = ProActiveProxyProtocol.parseProtocol(protocol);
		
		if (papp.equals(ProActiveProxyProtocol.JAVAPA)){
			schedulerstub = new SchedulerStubProberJava();
		}else if (papp.equals(ProActiveProxyProtocol.REST)){
			schedulerstub = new SchedulerStubProberRest();
		}else{
			throw new InvalidProtocolException("Unknown protocol '"+protocol+"'");
		}
		
		
		JobProber.setLastStatuss("scheduler stub created, connecting to shceduler...");
		
		logger.info("Connecting... "); 					// Connecting to the Scheduler...
		schedulerstub.init(protocol, url, user, pass); 	// Login procedure...
		
		JobProber.setLastStatuss("connected to scheduler, loading proactive configuration...");
		
		if (usepaconffile==true){
			logger.info("Loading ProActive configuration (xml) file... ");
			ProActiveConfiguration.load(); // Load the ProActive configuration file.
		}else{
			logger.info("Avoiding ProActive configuration (xml) file... ");
		}
		logger.info("Done.");
		
		JobProber.setLastStatuss("proactive configuration loaded, submitting job...");
		
		int output_to_return = JobProber.RESULT_CRITICAL; 
		String output_to_print = "NO TEST PERFORMED"; 		// Default output (for Nagios).
		
		File f = new File(jobpath); 						// Path of the job descriptor file (xml) to submit to the Scheduler.
		String jobname = f.getName(); 						// We get the name of the jobdescriptor file to tell it in the logs.
		
		long start = (new Date()).getTime(); 				// Time counting... Start...
		
		logger.info("Submitting '" + jobname + "' job...");
		String jobId = schedulerstub.submitJob(jobpath); 	// Submission of the job.
		logger.info("Done.");
		
		JobProber.setLastStatuss("job "+jobId+" submitted, waiting for it...");
		
		logger.info("Waiting for " + jobname + ":" + jobId + " job...");
		schedulerstub.waitUntilJobFinishes(jobId, timeoutsec * 1000); // Wait up to 'timeoutsec' seconds until giving up.
		logger.info("Done.");
		
		JobProber.setLastStatuss("job "+jobId+" finished, getting its result...");
		
		long stop = (new Date()).getTime(); 					// Time counting. End.
		
		String jresult = schedulerstub.getJobResult(jobId); 	// Getting the result of the submitted job.
		
		float durationsec = ((float)(stop-start)/1000); 		// Calculation of the time elapsed between submission and arrival of result. 
		
		JobProber.setLastStatuss("job "+jobId+" result retrieved, checking it...");
		logger.info("Duration of submission+execution+retrieval: " + durationsec + " seconds.");
		
		logger.info("Checking output...");
		if (jresult==null){ 		// Timeout case. No results obtained.
			logger.info("Finished period for job  " + jobname + ":" + jobId + ". Result: NOT FINISHED");
			output_to_print = "RESULT JOB " + jobname + " ID " + jobId + " - ERROR (job told to be finished, but no result obtained)";
			output_to_return = JobProber.RESULT_CRITICAL;
		}else{ 						// Non-timeout case. Result obtained.
			logger.info("Finished period for job  " + jobname + ":" + jobId + ". Result: " + jresult.toString());
			
			try { 
				/* The result is saved beside the jobdescriptor (xml) file. 
				 * To have a better checking of the output of the job, just remove the extension '.tmp' of this output, (remaining a '.out' file).
				 * This '.out' file is the file used to match the output of the job.
				 */
				String ppath = jobpath + ".out.tmp";
				logger.info("Writing output in '" + ppath + "'...");
				Misc.writeAllFile(ppath, jresult.toString());
				logger.info("Done.");
			} catch (Exception e1) {
				logger.warn("Could not write the output of the process.", e1);
			}
			
			try{
				String expectedoutput = Misc.readAllFile(jobpath + ".out"); // Checking of the expected output '.out' file.
				if (jresult.toString().equals(expectedoutput)){ 			// Checked file, all OK.
					output_to_return = JobProber.RESULT_OK;
					output_to_print = "RESULT JOB " + jobname + " ID " + jobId + " - OK ("+ durationsec +" sec)";
				}else{ 														// Outputs were different. 
					output_to_return = JobProber.RESULT_CRITICAL;
					output_to_print = "RESULT JOB " + jobname + " ID " + jobId + " - OUTPUT CHECK FAILED ("+ durationsec +" sec)";
				}
			}catch(IOException e){											// No 'output' reference point to do the checking.
				output_to_return = JobProber.RESULT_UNKNOWN; 		
				output_to_print = "RESULT JOB " + jobname + " ID " + jobId + " - OUTPUT NOT CHECKED ("+ durationsec +" sec)";
			}
		}
		logger.info("Done.");
		
		JobProber.setLastStatuss("job "+jobId+" result checked, removing job from scheduler...");
		
		logger.info("Removing job "+ jobname + ":" + jobId + "...");
		schedulerstub.removeJob(jobId);							// Job removed from the list of jobs in the Scheduler.
		logger.info("Done.");
		
		JobProber.setLastStatuss("job "+jobId+" removed from scheduler, disconnecting...");
		
		logger.info("Disconnecting...");
		schedulerstub.disconnect();								// Getting disconnected from the Scheduler.
		logger.info("Done.");
		
		JobProber.setLastStatuss("disconnected from scheduler, retrieving Nagios result...");
		
		Object [] ret = new Object[2];							// Both, error code and message are returned to be shown.
		ret[0] = new Integer(output_to_return);
		ret[1] = output_to_print;
			
		return ret;
	}

	/** 
	 * Create a java.policy file to grant permissions, and load it for the current JVM. */
	public static void createPolicyAndLoadIt() throws Exception{
		try{
			
			
		    File temp = File.createTempFile("javapolicy", ".policy"); // Create temp file.
		    

		    temp.deleteOnExit(); // Delete temp file when program exits.

		    // Write to temp file.
		    BufferedWriter out = new BufferedWriter(new FileWriter(temp));
		    String policycontent = "grant {permission java.security.AllPermission;};";
		    out.write(policycontent);
		    out.close();

		    String policypath = temp.getAbsolutePath(); 
		    
		    System.setProperty("java.security.policy", policypath); // Load security policy.
		    
		}catch(Exception e){
			throw new Exception("Error while creating the security policy file. " + e.getMessage());
		}
	}
	
	/** 
	 * Save a message regarding the last status of the probe. 
	 * This last status will be used in case of timeout to tell Nagios up to which point
	 * (logging, job submission, job retrieval, etc.) the probe arrived. */
	public synchronized static void setLastStatuss(String laststatus){
		JobProber.lastStatus = laststatus;
	}
	
	/** 
	 * Get a message regarding the last status of the probe. 
	 * This last status will be used in case of timeout to tell Nagios up to which point
	 * (logging, job submission, job retrieval, etc.) the probe arrived. 
	 * @return the last status of the test. */
	public synchronized static String getLastStatus(){
		return JobProber.lastStatus;
	}
	
	/** 
	 * Print a message in the stdout (for Nagios to use it) and return with the given error code. */
	public synchronized static void printAndExit(Integer ret, String str){
    	System.out.println(str);
    	System.exit(ret);
    }
    
	/**
	 * Print the usage of the application. */
	public static void printUsage(){
		String usage = null;
		try {
			usage = Misc.readAllTextResource("/resources/usage.txt");
			System.out.println(usage);
		} catch (IOException e) {
			logger.warn("Issue with usage message. Error: '"+e.getMessage()+"'.", e); 
		}
	}
	
	/**
	 * Creates a default set of properties for the log4j logging module. */
	public static Properties getMainLoggingProperties(){
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
	
}

