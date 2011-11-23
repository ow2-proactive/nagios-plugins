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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.log4j.Logger;
import qosprobercore.misc.Misc;

/**
 * Set of useful definitions and methods for any Nagios plugin. */
public abstract class NagiosPlugin {
	public static final String NAG_OUTPUT_PREFIX = "SERVICE STATUS: ";
	
	public static final int DEBUG_LEVEL_0_SILENT	= 0;	// Debug level, silent mode. 
	public static final int DEBUG_LEVEL_1_EXTENDED 	= 1;	// Debug level, more than silent mode. Shows backtraces if error. 
	public static final int DEBUG_LEVEL_2_VERBOSE	= 2;	// Debug level, similar to the previous one.
	public static final int DEBUG_LEVEL_3_USER		= 3;	// Debug level, debugging only.
	
	protected static Logger logger =						// Logger. 
			Logger.getLogger(NagiosPlugin.class.getName()); 
	private Arguments arguments; 							// Arguments given to the prober. 
	
	
	/** 
	 * Constructor of the prober. The map contains all the arguments for the probe to be executed. 
	 * @param args arguments to create this NagiosPlugin. */
	public NagiosPlugin(Arguments args){
		this.arguments = args;
		
		args.addNewOption("h", "help", false);													// Help message.                                	
		args.addNewOption("V", "version", false);												// Prints the version of the plugin.
		args.addNewOption("v", "debug", true, new Integer(NagiosPlugin.DEBUG_LEVEL_1_EXTENDED));// Level of verbosity.
		args.addNewOption("w", "warning", true);												// Timeout in seconds for the warning message to be thrown.
		args.addNewOption("c", "critical", true);												// Timeout in seconds for the job to be executed.
		
		args.addNewOption("f", "paconf", true);													// Path of the ProActive xml configuration file.
		args.addNewOption("H", "hostname", true);												// Host to be tested. 
		args.addNewOption("x", "port"    , true);												// Port of the host to be tested. 
		
	}
	
	final protected Arguments getArgs(){
		return arguments;
	}
	
	final public void initializeAll() throws Exception{
		initializeBasics(arguments);
		initializeProber(arguments);
	}
	
	final private void initializeBasics(Arguments ars) throws Exception{
		ars.parseAll();

		if (ars.getBoo("help") == true)	
			NagiosPlugin.printMessageUsageAndExit("");
		
		if (ars.getBoo("version") == true)
			NagiosPlugin.printVersionAndExit();
		
		this.validateArguments(ars);					// Validate its arguments. In case of problems, it throws an IllegalArgumentException.
	
		Misc.log4jConfiguration(ars.getInt("debug"));	// Loading log4j configuration. 
		
		ars.printArgumentsGiven();						// Print a list with the arguments given by the user. 
	}
	
	/**
	 * Initialize this probe. */
	protected abstract void initializeProber(Arguments arg) throws Exception; 
	
	/** 
	 * Validate all the arguments given to this probe. 
	 * @throws IllegalArgumentException in case a non-valid argument is given. */
	public void validateArguments(Arguments args) throws IllegalArgumentException{
		args.checkIsGiven("debug");
		args.checkIsValidInt("debug", 0, 3);
		
//		args.checkIsGiven("warning");								// Might not be given (there is a default value), so we don't check it.
		args.checkIsValidInt("warning", 0, Integer.MAX_VALUE);
		
		args.checkIsGiven("critical");
		args.checkIsValidInt("critical", 0, Integer.MAX_VALUE);
		
//		args.checkIsGiven("port");									// Might not be given (there is a default value), so we don't check it.
		args.checkIsValidInt("port", 0, 65535);
	}
	
	/**
	 * Probe the entity. 
	 * Several calls are done against the entity, as needed to probe it. 
	 * @param tracer tracer that lets keep track of the last status, and the time each call took to be executed.
	 * @return NagiosReturnObject with Nagios code error and a descriptive message of the test. */	 
	public abstract NagiosReturnObject probe(TimedStatusTracer tracer) throws Exception;
	
	
	public void startProbeAndExit() throws Exception{
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
			res.addCurvesSection(tracer, "time_all");
		}catch(TimeoutException e){
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "TIMEOUT OF " + arguments.getInt("critical")+ " SEC. (last status: " + tracer.getLastStatusDescription() + ")", e);
			res.addCurvesSection(tracer, null);
		}catch(ExecutionException e){ 		// There was a problem with the execution of the prober.
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "FAILURE: " + e.getMessage(), e);
			res.addCurvesSection(tracer, null);
		}catch(Exception e){ 				// There was an unexpected critical exception not captured. 
			res = new NagiosReturnObject(NagiosReturnObject.RESULT_2_CRITICAL, "CRITICAL ERROR: " + e.getMessage(), e);
			res.addCurvesSection(tracer, null);
		}
		NagiosPlugin.printAndExit(res, arguments.getInt("debug"));
	}
	
    /** 
     * Print a message in the stdout (for Nagios to use it) and return with the given error code. 
     * Print a back-trace later only if the debug-level is appropriate. 
     * @param obj object to take the information from.
     * @param debuglevel level of verbosity. */
    public synchronized static void printAndExit(NagiosReturnObject obj, int debuglevel){
    	Throwable exc = obj.getException();
        switch(debuglevel){
            case DEBUG_LEVEL_0_SILENT:
                System.out.println(NAG_OUTPUT_PREFIX + obj.getWholeMessage());
                break;
            default:
                System.out.println(NAG_OUTPUT_PREFIX + obj.getWholeMessage());
                if (exc!=null){
	                exc.printStackTrace(System.out);
                }
                break;
        }
        System.exit(obj.getErrorCode());
    }
    
	/**
	 * Used when a parameter given by the user is wrong. 
	 * Print a message, then the usage of the application, and the exits the application. 
	 * @param mainmessage message to be shown to the user (though Nagios). */
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
