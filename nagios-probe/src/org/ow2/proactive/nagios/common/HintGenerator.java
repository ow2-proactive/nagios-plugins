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

package org.ow2.proactive.nagios.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.ow2.proactive.nagios.misc.Misc;


/**
 * This class aims to help the Nagios administrator in 1. the interpretation of the probe's output and 2. the 
 * solution of the problem, if any.
 * If there is one exception thrown during the probe process, showing this exception to the administrator
 * is a straight pointer to the problem but it will not give them a clear suggestion about what might be wrong, specially 
 * if the administrator does not know anything about the software architecture of the software entity tested (Scheduler, RM, etc.). 
 * However we can give a possible cause of the problem, or a hint about the way to solve it. That's what this class is useful for.  
 * This class uses information regarding both the exception obtained during the probe procedure (if any) and the output of the
 * probe in order to give ideas to the administrator about the possible cause of the problem and its solution, something we define
 * as HINT. 
 * There is a hints resource file (file .txt inside the .jar generated by the packaging of this project) that helps 
 * mapping a particular problem (manifested through NagiosReturnObject) to a particular hint. This file has the following format:
 *  token1 token2 token3 : hint for the problem 
 * So if in the NagiosReturnObject all token1, token2, token3 are present as strings, then "cause of problem" will 
 * be told as one of the possible hints for the current problem. 
 *  */
public class HintGenerator {
	private static final String HINTS_PREFIX = "HINTS: "; 						// What to add as a prefix to the expression of the possible causes of problem. 
	protected static Logger logger =											// Logger. 
			Logger.getLogger(HintGenerator.class.getName()); 
	
	/**
	 * This method adds hints information to the NagiosReturnObject using the 
	 * guessing engine, so the Nagios administrator is more aware of what is going wrong, 
	 * having some more concrete information than rather only an exception. 
	 * @param o the raw (with no hints) NagiosReturnObject. 
	 * @return the enriched (with hints) NagiosReturnObject. */
	public static NagiosReturnObject getEnrichedNagiosReturnObject(NagiosReturnObject o, String hintfile){
		if (o.getErrorCode()!=ElementalNagiosPlugin.RESULT_0_OK){ // Something is wrong, we add hint information.
			logger.info("Performing processing of hints...");
			try {
				String possiblecauses = parseCauses(o, hintfile);
				o.setErrorMessage(o.getErrorMessage() + " (" + HINTS_PREFIX + possiblecauses + ")");
			} catch (Exception e) {
				logger.warn("Problem reading/interpreting " + hintfile, e);
				o.setErrorMessage(o.getErrorMessage() + " (" + HINTS_PREFIX + "can't read " + hintfile + ")");
			}
		}else{
			logger.info("Skipping the processing of hints (all right).");
		}
		return o;
	}
	
    /**
     * Perform the parsing of the NagiosReturnObject to detect a known problem and give hints on it. 
     * @param o object that represents all the information about the result of the probe, where to take problem's information (if any problem). 
     * @param resource path of the hints resource file to read.
     * @return a string containing hints for the administrator regarding the current problem. 
     * @throws IOException */
	private static String parseCauses(NagiosReturnObject o, String resourcename) throws IOException{
		ArrayList<String> listOfCauses = new ArrayList<String>();
		InputStream is = Misc.class.getResourceAsStream(resourcename);	// Get list of token1 token2 token3:hint for this problem.
	    InputStreamReader isr = new InputStreamReader(is);
	    BufferedReader br = new BufferedReader(isr);
	    String hintentry;
	    while ((hintentry = br.readLine()) != null){					// Read line by line the hints file getting hint entries.
	    	logger.info("Checking hintentry: '" + hintentry + "'");
	    	if (hintentry.startsWith("#")){									// Comment line.
	    		logger.info("Ignoring hint file comment line: '" + hintentry + "'");
	    	}else{
		    	if (thisHintMatches(o, hintentry)){							// If the current NagiosReturnObject matches the entry, add this hint.  
		    		listOfCauses.add(extractHint(hintentry));				// Add this hint to the list of hints for the given problem (specified by 'o'). 
		    		logger.info("DOES apply.");
		    	}else{
		    		logger.info("Does NOT apply.");
		    	}
	    	}
	    }
	    br.close();
	    isr.close();
	    is.close();
	    return generateStringListOfHints(listOfCauses);
	}
	
	/**
	 * Check if the given NagiosReturnObject matches the given hint or not. In other 
	 * words, whether it makes sense to tell this hint to the administrator given that it matches
	 * the current problem or not.  
	 * A NagiosReturnObject matches a given hint <=> this NagiosReturnObject matches all the tokens in the hint entry. 
	 * @param o NagiosReturnObject describing the current problem. 
	 * @param hintentry hint entry. 
	 * @return true if there is a matching between the NagiosReturnObject and the tokens of the hint entry. */
	private static Boolean thisHintMatches(NagiosReturnObject o, String hintentry){
		try{
			String[] tokens = extractSetOfTokensToMatch(hintentry);
			logger.info("Checking if this hint-entry applies to the current situation...");
			
			for (String token: tokens){
				if (thisTokenMatches(o, token)){
					logger.info(" - token " + token + " DOES apply to the current situation: we keep on evaluating...");
				}else{
					logger.info(" - token " + token + " does NOT apply to the current situation: avoiding remaining tokens...");
					return false;
				}
			}
			return true; // All of the tokens matched, otherwise we would have exited before with false. 
		}catch(ParseException e){
			logger.warn("Problem evaluating hint-entry '" + hintentry + "'... Message: '" + e.getMessage() + "'. So skipping entry.");
			return false;
		}
	}
	
	/**
	 * Check if the given NagiosReturnObject matches the given hint token or not.
	 * @param o NagiosReturnObject describing the current problem. 
	 * @param token hint token. 
	 * @return true if in the NagiosReturnObject the given token can be found. */
	private static Boolean thisTokenMatches(NagiosReturnObject o, String token){
		if (o.getErrorMessage().contains(token))
			return true;
		
		if (o.getException()!=null){
			if (Misc.getStackTrace(o.getException()).contains(token))
				return true;
		}
		return false;
	}
	
	/**
	 * Extract the Hint section of the hint entry. 
	 * @param entry hint entry.
	 * @return a String with the full hint. */
	private static String extractHint(String entry){
		String[] both = entry.split(":");
		if (both.length>2){
			logger.warn("Incorrect amount of colon symbols in causeofproblem entry: " + entry);
		}
		return both[1];
	}
	
	/**
	 * Extract the tokens section of the hint entry (the keywords to match). 
	 * @param entry hint entry.
	 * @return a list with all the tokens of the given hint entry. */
	private static String[] extractSetOfTokensToMatch(String hintentry) throws ParseException{
		String[] both = hintentry.split(":");
		if (both.length>2){
			logger.warn("Incorrect amount of colon symbols in the hint entry: '" + hintentry + "'");
			throw new ParseException("Incorrect amount of colon symbols in the hint entry: '" + hintentry + "'", 0);
		}
		String tokensStr = both[0];
		logger.info("Detected tokens: '" + tokensStr + "'");
		String[] ret = tokensStr.split(" ");
		for (String s: ret){
			logger.info("  - tok: '" + s + "'");
		}
		return ret;
	}
	
	/**
	 * Generate a pretty String with all the hints in it.
	 * @param list list to convert to String. 
	 * @return pretty String. */
    private static String generateStringListOfHints(ArrayList<String> list){
    	if (list.isEmpty()){
	    	return "no hint";
    	}else{
    		String ret="";
    		for (String s: list){
    			ret = (ret.isEmpty()?"":ret + "; ") + s.trim();
    		}
    		return ret;
    	}
    }
    
}
