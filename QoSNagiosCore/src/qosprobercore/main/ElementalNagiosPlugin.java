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

package qosprobercore.main;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.*;

import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.log4j.Logger;
import qosprobercore.misc.Misc;

/**
 * Class to be inherited by any Elemental Nagios plugin. */
public abstract class ElementalNagiosPlugin {
	public static final String NAG_OUTPUT_PREFIX = "SERVICE STATUS: ";
	
	// Nagios exit codes.
	public static final Integer RESULT_0_OK = 0; 		// Nagios code. Execution successfully. 
	public static final Integer RESULT_1_WARNING = 1; 	// Nagios code. Warning. 
	public static final Integer RESULT_2_CRITICAL = 2; 	// Nagios code. Critical problem in the tested entity.
	public static final Integer RESULT_3_UNKNOWN = 3; 	// Nagios code. Unknown state of the tested entity.
	
	// Nagios debug levels.
	public static final int DEBUG_LEVEL_0_SILENT	= 0;	// Debug level, silent mode. 
	public static final int DEBUG_LEVEL_1_EXTENDED 	= 1;	// Debug level, more than silent mode. Shows backtraces if error. 
	public static final int DEBUG_LEVEL_2_VERBOSE	= 2;	// Debug level, similar to the previous one.
	public static final int DEBUG_LEVEL_3_USER		= 3;	// Debug level, debugging only.
	
	protected static Logger logger =						// Logger. 
			Logger.getLogger(ElementalNagiosPlugin.class.getName()); 
	protected Arguments arguments; 							// Arguments given to the prober. 
	protected String probeID;								// ID of the current probe (RM, Scheduler, etc.).
	
	
	/** 
	 * Constructor of the prober. The map contains all the arguments for the probe to be executed. 
	 * @param probeid id of the current prober (RM, Scheduler, etc.).
	 * @param args arguments to create this NagiosPlugin. */
	public ElementalNagiosPlugin(String probeid, Arguments args){
		this.arguments = args;
		this.probeID = probeid;
		args.addNewOption("h", "help", false);													// Help message.                                	
		args.addNewOption("V", "version", false);												// Prints the version of the plugin.
		args.addNewOption("v", "debug", true, new Integer(PANagiosPlugin.DEBUG_LEVEL_1_EXTENDED));// Level of verbosity.
		args.addNewOption("w", "warning", true);												// Timeout in seconds for the warning message to be thrown.
		args.addNewOption("c", "critical", true);												// Timeout in seconds for the job to be executed.
		args.addNewOption("S", "dump-script" , true);											// Script to be executed in case of any problem. 
		args.addNewOption("O", "logconf", true);												// Configuration file for log4j. 
	}
	
	/**
	 * Specific initialization for the probe. 
	 * This method can be overwritten, but must be called during initialization.
	 * @param arg arguments/parameters to initialize the probe. */
	public void initializeProber() throws Exception{ 
		try{
			getArgs().parseAll();
		}catch(MissingOptionException e){
			this.printMessageUsageAndExit(e.getMessage());
		}catch(UnrecognizedOptionException e){
			this.printMessageUsageAndExit(e.getMessage());
		}

		Misc.log4jConfiguration(getArgs().getInt("debug"), getArgs().getStr("logconf"));	// Loading log4j configuration. 
		if (getArgs().getBoo("help") == true)	
			this.printMessageUsageAndExit("");
		if (getArgs().getBoo("version") == true)
			ElementalNagiosPlugin.printVersionAndExit();
		this.validateArguments(getArgs());				// Validate its arguments. In case of problems, it throws an IllegalArgumentException.
		
		logger.info(">>>> Date: " + new Date());		// Print the date in the logs.
		getArgs().printArgumentsGiven();				// Print a list with the arguments given by the user. 
	}
	
	/** 
	 * Validate all the arguments given to this probe. 
	 * This method can be overwritten, but must be called during validation.
	 * @param args arguments to be validated.
	 * @throws IllegalArgumentException in case a non-valid argument is given. */
	public void validateArguments(Arguments args) throws IllegalArgumentException{
//		args.checkIsGiven("debug");
		args.checkIsValidInt("debug", 0, 3);
//		args.checkIsGiven("warning");								// Might not be given (there is a default value), so we don't check it.
		args.checkIsValidInt("warning", 0, Integer.MAX_VALUE);
		args.checkIsGiven("critical");
		args.checkIsValidInt("critical", 0, Integer.MAX_VALUE);
	}
	
	/**
	 * Probe the entity.  
	 * This method needs to be implemented.
	 * Several calls are done against the entity, as needed to probe it. 
	 * @param tracer tracer that lets keep track of the last status, and the time each call took to be executed.
	 * @return NagiosReturnObject with Nagios code error and a descriptive message of the test. */	 
	public abstract NagiosReturnObject probe(TimedStatusTracer tracer) throws Exception;
	
	/**
	 * Get set of arguments given by the user.
	 * @return arguments. */
	final protected Arguments getArgs(){
		return arguments;
	}
	
	/**
	 * Start with the probing session.
	 * This method kills automatically the probe Java process when a conclusion/result about the entity
	 * tested has been obtained. 
	 * The timeout mechanism is automatically handled by this method. It means that the probe(...) method
	 * should not consider the case in which the test is taking longer than allowed by the timeout threshold. This
	 * method will execute the probe(...) in such a way that an execution longer than the threshold will be automatically 
	 * stopped and a TimeoutException will be thrown. */
	final public void startProbeAndExit(){
		/* We prepare now our probe to run it in a different thread. The probe consists in a job submission done to the Scheduler. */
		
		final TimedStatusTracer tracer = TimedStatusTracer.getInstance();	// We want to get last status memory, and timing measurements.
		
		ExecutorService executor = Executors.newFixedThreadPool(1);
		Callable<NagiosReturnObject> proberCallable = new Callable<NagiosReturnObject>(){
			public NagiosReturnObject call() throws Exception {
				return probe(tracer);
			}
		};

		// We submit to the executor the prober activity (and the prober will then submit a job to the scheduler in that activity). 
		Future<NagiosReturnObject> proberFuture = executor.submit(proberCallable); // We ask to execute the probe.
	
		NagiosReturnObject res = null;
		try{ 								// We execute the future using a timeout. 
			res = proberFuture.get(arguments.getInt("critical"), TimeUnit.SECONDS);
		}catch(TimeoutException e){
			logger.warn("Timeout Exception...");
			res = getNagiosReturnObjectForTimeoutException(arguments.getInt("critical"), tracer, e);
		}catch(ExecutionException e){ 		// There was a problem with the execution of the prober.
			logger.warn("Execution Exception: " + Misc.getStackTrace(e));
			res = getNagiosReturnObjectForExecutionException(tracer, e);
		}catch(Exception e){ 				// There was an unexpected critical exception not captured. 
			logger.warn("Unknown Exception:" + Misc.getStackTrace(e));
			res = new NagiosReturnObject(RESULT_2_CRITICAL, "CRITICAL ERROR: " + e.getMessage(), e);
			res.addCurvesSection(tracer, null);
		}
		printDumpAndExit(res, arguments.getInt("debug"), probeID);
	}
	
	/**
	 * Start with the probing session.
	 * This method kills automatically the probe Java process when a conclusion/result about the entity
	 * tested has been obtained. 
	 * The timeout mechanism is NOT handled by this method. It means that a probe(...) method
	 * taking longer than the timeout threshold should be stopped inside the probe(...) method itself and 
	 * a TimeoutException should be thrown. */
	final public void startProbeAndExitManualTimeout(){
		/* We prepare now our probe to run it in a different thread. The probe consists in a job submission done to the Scheduler. */
		
		final TimedStatusTracer tracer = TimedStatusTracer.getInstance();	// We want to get last status memory, and timing measurements.
		
		NagiosReturnObject res = null;

		try{ 								// We execute the future using a timeout. 
			res = this.probe(tracer);
		}catch(TimeoutException e){
			logger.warn("Timeout Exception...");
			res = getNagiosReturnObjectForTimeoutException(arguments.getInt("critical"), tracer, e);
		}catch(ExecutionException e){ 		// There was a problem with the execution of the prober.
			logger.warn("Execution Exception: " + Misc.getStackTrace(e));
			res = getNagiosReturnObjectForExecutionException(tracer, e);
		}catch(Exception e){ 				// There was an unexpected critical exception not captured. 
			logger.warn("Unknown Exception:" + Misc.getStackTrace(e));
			res = new NagiosReturnObject(RESULT_2_CRITICAL, "CRITICAL ERROR: " + e.getMessage(), e);
			res.addCurvesSection(tracer, null);
		}
		printDumpAndExit(res, arguments.getInt("debug"), probeID);
	}
	
	/**
	 * Get the NagiosReturnObject with the format to be shown according to a Timeout problem arisen when probing. 
	 * @param timeout given by the user as useful data to generate the message.
	 * @param tracer as useful data to generate the message.
	 * @param e as useful data to generate the message.
	 * @return the NagiosReturnObject generated. */
	protected NagiosReturnObject getNagiosReturnObjectForTimeoutException(Integer timeout, TimedStatusTracer tracer, Exception e){
		NagiosReturnObject ret = new NagiosReturnObject(RESULT_2_CRITICAL, "TIMEOUT OF " + arguments.getInt("critical")+ " SEC. (last status: " + tracer.getLastStatusDescription() + ")", e);
		ret.addCurvesSection(tracer, null);
		return ret;
	}
	
	/**
	 * Get the NagiosReturnObject with the format to be shown according to an Execution problem arisen when probing. 
	 * @param tracer as data useful to generate the message.
	 * @param e as data useful to generate the message.
	 * @return the NagiosReturnObject generated. */
	protected NagiosReturnObject getNagiosReturnObjectForExecutionException(TimedStatusTracer tracer, Exception e){
		NagiosReturnObject ret = new NagiosReturnObject(RESULT_2_CRITICAL, "FAILURE: " + e.getMessage(), e);
		ret.addCurvesSection(tracer, null);
		return ret;
	}
	
    /** 
     * Print a message in the stdout (for Nagios to use it) and return with the given error code. 
     * Print a back-trace later only if the debug-level is appropriate. 
     * @param obj object to take the information from.
     * @param debuglevel level of verbosity. 
     * @param source id of the specific caller probe (RM, Scheduler, etc.). */
    private synchronized void printDumpAndExit(NagiosReturnObject obj, int debuglevel, String source){
    	String ret = "";
    	Throwable exc = obj.getException();
        ret = ret + NAG_OUTPUT_PREFIX + obj.getWholeFirstLineMessage() + "\n";
        
        if (obj.getErrorCode()!= RESULT_0_OK && arguments.isGiven("dump-script")){
        	logger.info("Dumping...");
	        Dumper du = new Dumper(arguments.getStr("dump-script"), source);
	        du.dump(obj);
        }
        
        switch(debuglevel){
            case DEBUG_LEVEL_0_SILENT:
                break;
            default:
                if (exc!=null){
	                String st = Misc.getStackTrace(exc);
	                ret = ret + st;
                }
                break;
        }
        
        ret = Misc.removeConflictCharacters(ret);
        
        logger.info(ret);
        logger.info(">>>> Return code: " + obj.getErrorCode());
        System.out.print(ret);
        System.exit(obj.getErrorCode());
    }
    
	/**
	 * Print the version of the plugin and the exits the application. */
	private static void printVersionAndExit(){
		String usage = null;
		try {
			usage = Misc.readAllTextResource("/resources/version.txt");
			System.err.println(usage);
		} catch (IOException e) {
			logger.warn("Issue with usage message. Error: '"+e.getMessage()+"'.", e); 
		}
	    System.exit(RESULT_0_OK);
	}
	
	/**
	 * Used when a parameter given by the user is wrong. 
	 * Print a message, then the usage of the application, and the exits the application. 
	 * @param errormessage message of error to be shown to the user (through Nagios). */
	public void printMessageUsageAndExit(String errormessage){
		if (errormessage!=null){
			System.out.println(errormessage);
		}
		String usage = ""; 
		try {
			usage = usage + Misc.readAllTextResource("/resources/usage.txt");
			usage = usage + Misc.readAllTextResource("/resources/core/usage.txt");
			System.err.println(usage);
		} catch (IOException e) {
			logger.warn("Issue with usage message. Error: '"+e.getMessage()+"'.", e); 
		}
	
	    System.exit(RESULT_2_CRITICAL);
	}
	
}
