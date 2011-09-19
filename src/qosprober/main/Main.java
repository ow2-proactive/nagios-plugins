package qosprober.main;

import jargs.gnu.CmdLineParser;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyException;
import java.util.Date;
import javax.security.auth.login.LoginException;
import qosprober.misc.Misc;
import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.core.config.ProActiveConfiguration;
import org.objectweb.proactive.core.node.NodeException;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobResult;
import qosprober.exceptions.ElementNotFoundException;
import qosprober.exceptions.InvalidProtocolException;

/** Main class. */
public class Main {

	/* Nagios exit codes. */
	private static final int RESULT_ERROR = -1; 	// Error not related to Nagios output (issue in the prober). 
	private static final int RESULT_OK = 0; 		// Nagios code. Execution successfully. 
	private static final int RESULT_WARNING = 1; 	// Nagios code. Warning. 
	private static final int RESULT_CRITICAL = 2; 	// Nagios code. Critical problem in the tested entity.
	private static final int RESULT_UNKNOWN = 3; 	// Nagios code. Unknown state of the tested entity.
	
	public static Logger logger = Logger.getLogger(Main.class.getName()); // Logger.
	
	public static void main(String[] args) throws Exception{
		
		/* Parsing of arguments. */
		CmdLineParser parser = new CmdLineParser();
		
		CmdLineParser.Option debugO = parser.addBooleanOption('d', "debug");
		CmdLineParser.Option userO = parser.addStringOption('u', "user");
		CmdLineParser.Option passO = parser.addStringOption('p', "pass");
		CmdLineParser.Option protocolO = parser.addStringOption("protocol");
		CmdLineParser.Option jobpathO = parser.addStringOption('j',"jobpath");
		CmdLineParser.Option urlO = parser.addStringOption("url");
		CmdLineParser.Option timeoutsecO = parser.addIntegerOption('t', "timeout");
		CmdLineParser.Option paconfO = parser.addStringOption('f', "paconf");
		CmdLineParser.Option logfileO = parser.addStringOption('l', "logfile");
		
		CmdLineParser.Option hostO = parser.addStringOption('H', "hostname");
		CmdLineParser.Option warningO = parser.addDoubleOption('w', "warning");
		CmdLineParser.Option criticalO = parser.addDoubleOption('c', "critical");

		try {
		    parser.parse(args);
		} catch ( CmdLineParser.OptionException e ) {
			/* In case something is not expected, print usage and exit. */
		    System.err.println(e.getMessage());
		    Main.printUsage();
		    System.exit(RESULT_ERROR);
		}
		
		Boolean debug = (Boolean)parser.getOptionValue(debugO, Boolean.FALSE); 	/* If false, only Nagios output. */ 
		String user = (String)parser.getOptionValue(userO, "demo"); 			/* User. */
		String pass = (String)parser.getOptionValue(passO, "demo"); 			/* Pass. */
		String protocol = (String)parser.getOptionValue(protocolO, "JAVAPA"); 	/* Protocol, either REST or JAVAPA. */
		String jobpath = (String)parser.getOptionValue(jobpathO, ""); 			/* Path of the job descriptor (xml). */
		String url = (String)parser.getOptionValue(urlO, "pamr://1"); 			/* Url of the Scheduler/RM. */
		Integer timeoutsec = (Integer)parser.getOptionValue(timeoutsecO, new Integer(60)); /* Timeout in seconds for the job to be executed. */
		String paconf = (String)parser.getOptionValue(paconfO); 				/* Path of the ProActive xml configuration file. */
		String logfile = (String)parser.getOptionValue(logfileO, null); 		/* Path of the logfile (if any). */
		String host = (String)parser.getOptionValue(hostO, "localhost"); 		/* Host to be tested. Ignored. */
		Double warning = (Double)parser.getOptionValue(warningO, new Double(100)); /* Warning level. Ignored. */
		Double critical = (Double)parser.getOptionValue(criticalO, new Double(100)); /* Critical level. Ignored. */ 
		
		PropertyConfigurator.configure("log4j.properties");
		
		/* If debug is true then we let print log4j messages in the stdout. */
		System.setProperty("log4j.defaultInitOverride", new Boolean(!debug).toString().toLowerCase());
		
		
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
				"\t logfile : " + logfile + "\n" +
				"\t host    : " + host + "\n" +
				"\t warning : " + warning  + "\n" +
				"\t critical: " + critical + "\n" 
				);
		
		/* Security policy procedure. */
		logger.info("Setting security policies... ");
		Main.createPolicyAndLoadIt();
		logger.info("Done.");
		
		
		Boolean usepaconffile = false;
		if (paconf!=null){
			/* A ProActiveConf.xml file was given. If we find it, we use it. */
			if (new File(paconf).exists()==false){
				String msg = "The ProActive configuration file '"+paconf+"' was not found.";
				logger.fatal(msg);
				throw new ElementNotFoundException(msg);
			}
			System.setProperty("proactive.configuration", paconf);
			usepaconffile = true;
		}
		
		/* No need of other arguments. */
		//String[] otherArgs = parser.getRemainingArgs();
		
		/**********************/
		/*** Job submission ***/
		/**********************/
		
		/* Thread with timeout mechanism. 
		 * This kills the application after a huge effort trying to get connected to the Scheduler/RM. */
		TimeouterThread timeouter = new TimeouterThread(timeoutsec, Main.RESULT_CRITICAL, "TIMEOUT");
		timeouter.start();
		
		/* Probe agains the Scheduler. Job submission and Nagios-formatted output delivery. */
		try{
			Object[] rete = Main.probe(url, user, pass, protocol, jobpath, timeoutsec, usepaconffile);
			Main.printAndExit((Integer)rete[0], (String)rete[1]);
		}catch(Exception e){
			logger.fatal("Issue with the job submission. Error: '"+e.getMessage()+"'.", e);
		}
	}
	
	
	public static Object[] probe(String url, String user, String pass, String protocol, String jobpath, int timeoutsec, Boolean usepaconffile) throws IllegalArgumentException, LoginException, KeyException, ActiveObjectCreationException, NodeException, HttpException, SchedulerException, InvalidProtocolException, IOException{
		
		SchedulerStubProber schedulerstub = new SchedulerStubProber(); // We get connected to the Scheduler through this stub, later we submit a job, etc. 
		
		logger.info("Connecting... "); 					// Connecting to the Scheduler...
		schedulerstub.init(protocol, url, user, pass); 	// Login procedure...
		if (usepaconffile==true){
			logger.info("Loading ProActive configuration (xml) file... ");
			ProActiveConfiguration.load();
		}else{
			logger.info("Avoiding ProActive configuration (xml) file... ");
		}
		logger.info("Done.");
		
		
		int output_to_return = Main.RESULT_CRITICAL; 
		String output_to_print = "NO TEST PERFORMED"; 		// Default output (for Nagios).
		
		File f = new File(jobpath); 						// Path of the job descriptor file (xml) to submit to the Scheduler.
		String jobname = f.getName(); 						// We get the name of the jobdescriptor file to tell it in the logs.
		
		long start = (new Date()).getTime(); 				// Time counting... Start...
		
		logger.info("Submitting '" + jobname + "' job...");
		JobId jobId = schedulerstub.submitJob(jobpath); 	// Submission of the job.
		logger.info("Done.");
		
		logger.info("Waiting for " + jobname + ":" + jobId + " job...");
		schedulerstub.waitUntilJobFinishes(jobId, timeoutsec * 1000); // Wait up to 'timeoutsec' seconds until giving up.
		logger.info("Done.");
		
		long stop = (new Date()).getTime(); 				// Time counting. End.
		
		JobResult jr = schedulerstub.getJobResult(jobId); 	// Getting the result of the submitted job.
		
		float durationsec = ((float)(stop-start)/1000); 	// Calculation of the time elapsed between submission and arrival of result. 
		
		logger.info("Duration of submission+execution+retrieval: " + durationsec + " seconds.");
		
		if (jr==null){ 	// Timeout case. No results obtained.
			logger.info("Finished period for job  " + jobname + ":" + jobId + ". Result: NOT FINISHED");
			output_to_return = Main.RESULT_WARNING;
			output_to_print = "RESULT JOB " + jobname + " ID " + jobId + " - TIMEDOUT"; 
		}else{ 			// Non-timeout case. Result obtained.
			logger.info("Finished period for job  " + jobname + ":" + jobId + ". Result: " + jr.toString());
			
			try { 
				/* The result is saved beside the jobdescriptor (xml) file. 
				 * To have a better checking of the output of the job, just remove the extension '.tmp' of this output, (remaining a '.out' file).
				 * This '.out' file is the file used to match the output of the job.
				 */
				String ppath = jobpath + ".out.tmp";
				logger.info("Writing output in '" + ppath + "'...");
				Misc.writeAllFile(ppath, jr.toString());
				logger.info("Done.");
			} catch (Exception e1) {
				logger.warn("Could not write the output of the process. ", e1);
			}
			try{
				String expectedoutput = Misc.readAllFile(jobpath + ".out"); // Checking of the expected output '.out' file.
				if (jr.toString().equals(expectedoutput)){ 		// Checked file, all OK.
					output_to_return = Main.RESULT_OK;
					output_to_print = "RESULT JOB " + jobname + " ID " + jobId + " - OK ("+ durationsec +" sec)";
				}else{ 											// Outputs were different. 
					output_to_return = Main.RESULT_CRITICAL;
					output_to_print = "RESULT JOB " + jobname + " ID " + jobId + " - OUTPUT CHECK FAILED ("+ durationsec +" sec)";
				}
			}catch(IOException e){								// No 'output' reference point to do the checking.
				output_to_return = Main.RESULT_UNKNOWN; 		
				output_to_print = "RESULT JOB " + jobname + " ID " + jobId + " - OUTPUT NOT CHECKED ("+ durationsec +" sec)";
			}
		}
		
		logger.info("Removing job "+ jobname + ":" + jobId + "...");
		schedulerstub.removeJob(jobId);							// Job removed from the list of jobs in the Scheduler.
		logger.info("Done.");
		
		
		logger.info("Disconnecting...");
		schedulerstub.disconnect();								// Getting disconnected from the Scheduler.
		logger.info("Done.");
		
		Object [] ret = new Object[2];							// Both, error code and message are returned to be shown.
		ret[0] = new Integer(output_to_return);
		ret[1] = output_to_print;
			
		return ret;
	}

	/* Creates a java.policy file to grant permissions. */
	public static void createPolicyAndLoadIt() throws Exception{
		try{
			
			// Create temp file.
		    File temp = File.createTempFile("javapolicy", ".policy");
		    
		    // Delete temp file when program exits.
		    temp.deleteOnExit();

		    // Write to temp file.
		    BufferedWriter out = new BufferedWriter(new FileWriter(temp));
		    String policycontent = "grant {permission java.security.AllPermission;};";
		    out.write(policycontent);
		    out.close();

		    String policypath = temp.getAbsolutePath(); 
		    
		    // Load security policy. 
		    System.setProperty("java.security.policy", policypath);
		    
		}catch(Exception e){
			throw new Exception("Error while creating the security policy file. " + e.getMessage());
		}
	}
	
	/* Print a message in the stdout (for Nagios to use it) and return with the given error code. */
	public synchronized static void printAndExit(Integer ret, String str){
    	System.out.println(str);
    	System.exit(ret);
    }
    
	/* Print the usage of the application. */
	public static void printUsage(){
		String usage = null;
		try {
			usage = Misc.readAllTextResource("/resources/usage.txt");
			System.out.println(usage);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			logger.warn("Issue with usage message. Error: '"+e.getMessage()+"'.", e); 
		}
	}
	
}

