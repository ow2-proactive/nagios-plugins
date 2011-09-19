package main;
import jargs.gnu.CmdLineParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.KeyException;
import java.util.Date;
import java.util.Random;

import javax.security.auth.login.LoginException;

import misc.Misc;

import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.core.config.ProActiveConfiguration;
import org.objectweb.proactive.core.node.NodeException;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import org.ow2.proactive.scheduler.common.job.JobId;
import org.ow2.proactive.scheduler.common.job.JobResult;

import exceptions.ElementNotFoundException;
import exceptions.InvalidProtocolException;

public class Main {

	/* Nagios exit codes. */
	private static final int RESULT_ERROR = -1;
	private static final int RESULT_OK = 0;
	private static final int RESULT_WARNING = 1;
	private static final int RESULT_CRITICAL = 2;
	private static final int RESULT_UNKNOWN = 3;
	
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
		
		Boolean debug = (Boolean)parser.getOptionValue(debugO, Boolean.FALSE); /* If false, only Nagios output. */ 
		String user = (String)parser.getOptionValue(userO, "demo"); /* User. */
		String pass = (String)parser.getOptionValue(passO, "demo"); /* Pass. */
		String protocol = (String)parser.getOptionValue(protocolO, "JAVAPA"); /* Protocol, either REST or JAVAPA. */
		String jobpath = (String)parser.getOptionValue(jobpathO, ""); /* Path of the job descriptor (xml). */
		String url = (String)parser.getOptionValue(urlO, "pamr://1"); /* Url of the Scheduler/RM. */
		Integer timeoutsec = (Integer)parser.getOptionValue(timeoutsecO, new Integer(60)); /* Timeout in seconds for the job to be executed. */
		String paconf = (String)parser.getOptionValue(paconfO); /* Path of the ProActive xml configuration file. */
		String logfile = (String)parser.getOptionValue(logfileO, null); /* Path of the logfile (if any). */
		String host = (String)parser.getOptionValue(hostO, "localhost"); /* Host to be tested. Ignored. */
		Double warning = (Double)parser.getOptionValue(warningO, new Double(100)); /* Warning level. Ignored. */
		Double critical = (Double)parser.getOptionValue(criticalO, new Double(100)); /* Critical level. Ignored. */ 
		
		
		PropertyConfigurator.configure("log4j.properties");
		
		/* If debug is true then we let print log4j messages in the stdout. */
		System.setProperty("log4j.defaultInitOverride", new Boolean(!debug).toString().toLowerCase());
		
		/* Redirect the stdout depending on the 'debug' (boolean) and 'logfile' (String) flags. */
		//Misc.redirectStdOut(debug, logfile);
		
		/* Show all the arguments considered. */
		Logger.getRootLogger().info(
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
		Logger.getRootLogger().info("Setting security policies... ");
		Main.createPolicyAndLoadIt();
		Logger.getRootLogger().info("Done.");
		
		
		Boolean usepaconffile = false;
		if (paconf!=null){
			/* A ProActiveConf.xml file was given. If we find it, we use it. */
			if (new File(paconf).exists()==false){
				throw new ElementNotFoundException("The ProActive configuration file '"+paconf+"' was not found.");
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
		
		Object[] rete = Main.probe(url, user, pass, protocol, jobpath, timeoutsec, usepaconffile);
		Main.printAndExit((Integer)rete[0], (String)rete[1]);
	}
	
	public synchronized static void printAndExit(Integer ret, String str){
    	System.out.println(str);
    	System.exit(ret);
    }
    
	public static void printUsage(){
		String usage = null;
		try {
			usage = Misc.readAllTextResource("/resources/usage.txt");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(usage);
	}
	
	public static Object[] probe(String url, String user, String pass, String protocol, String jobpath, int timeoutsec, Boolean usepaconffile) throws IllegalArgumentException, LoginException, KeyException, ActiveObjectCreationException, NodeException, HttpException, SchedulerException, InvalidProtocolException, IOException{
		
		SchedulerStubProber schedulerstub = new SchedulerStubProber();
		
		Logger.getRootLogger().info("Connecting... ");
		schedulerstub.init(protocol, url, user, pass);
		if (usepaconffile==true){
			ProActiveConfiguration.load();
		}
		Logger.getRootLogger().info("Done.");
		
		int output_to_return = Main.RESULT_CRITICAL;
		String output_to_print = "NO TEST PERFORMED";
		
		File f = new File(jobpath);
		String jobname = f.getName();
		
		long start = (new Date()).getTime();
		
		Logger.getRootLogger().info("Submitting '" + jobname + "' job...");
		JobId jobId = schedulerstub.submitJob(jobpath);
		Logger.getRootLogger().info("Done.");
		
		Logger.getRootLogger().info("Waiting for " + jobname + ":" + jobId + " job...");
		schedulerstub.waitUntilJobFinishes(jobId, timeoutsec * 1000);
		Logger.getRootLogger().info("Done.");
		
		long stop = (new Date()).getTime();
		
		JobResult jr = schedulerstub.getJobResult(jobId);
		
		float durationsec = ((float)(stop-start)/1000); 
		
		Logger.getRootLogger().info("Duration of submission+execution+retrieval: " + durationsec + " seconds.");
		
		if (jr==null){
			Logger.getRootLogger().info("Finished period for job  " + jobname + ":" + jobId + ". Result: NOT FINISHED");
			output_to_return = Main.RESULT_WARNING;
			output_to_print = "RESULT JOB " + jobname + " ID " + jobId + " - TIMEDOUT"; 
		}else{
			Logger.getRootLogger().info("Finished period for job  " + jobname + ":" + jobId + ". Result: " + jr.toString());
			
			try {
				Misc.writeAllFile(jobpath + ".out.tmp", jr.toString());
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try{
				String expectedoutput = Misc.readAllFile(jobpath + ".out");
				if (jr.toString().equals(expectedoutput)){
					
					output_to_return = Main.RESULT_OK;
					output_to_print = "RESULT JOB " + jobname + " ID " + jobId + " - OK ("+ durationsec +" sec)";
				}else{
					output_to_return = Main.RESULT_CRITICAL;
					output_to_print = "RESULT JOB " + jobname + " ID " + jobId + " - OUTPUT CHECK FAILED ("+ durationsec +" sec)";
				}
			}catch(IOException e){
				output_to_return = Main.RESULT_UNKNOWN;
				output_to_print = "RESULT JOB " + jobname + " ID " + jobId + " - OUTPUT NOT CHECKED ("+ durationsec +" sec)";
			}
		}
		
		Logger.getRootLogger().info("Removing job "+ jobname + ":" + jobId + "...");
		schedulerstub.removeJob(jobId);
		Logger.getRootLogger().info("Done.");
		
		
		Logger.getRootLogger().info("Disconnecting...");
		schedulerstub.disconnect();
		Logger.getRootLogger().info("Done.");
		
		Object [] ret = new Object[2];
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
	
	
}

