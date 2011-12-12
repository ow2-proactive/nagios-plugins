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

import java.util.ArrayList;

/**
 * Class that builds a summary NagiosReturnObject from all the collected 
 * data about the current situation of the probe. */
public class NagiosReturnObjectSummaryMaker{
	
	private ArrayList<NagiosMiniStatus> miniStatusList;
	private ArrayList<String> facts;

	/**
	 * Main constructor. */
	public NagiosReturnObjectSummaryMaker(){
		miniStatusList = new ArrayList<NagiosMiniStatus>();
		facts = new ArrayList<String>();
	}
	
	/**
	 * Add information regarding one aspect of the current situation of the probe.
	 * It might be a warning regarding time, a warning regarding an expected result,
	 * a critical status, or even an unknown status.
	 * The messages of all NagiosReturnObjects given will be included in the output-line of the
	 * Nagios probe, and the error code used at the end will be the most weighted one.
	 * @param nrobj NagiosMiniStatus with the information about one aspect of the current probe. */
	public void addMiniStatus(NagiosMiniStatus nrobj){
		miniStatusList.add(nrobj);
	}
	
	/**
	 * Add one fact statement to the output of this Nagios probe.
	 * This will unconditionally appear on the output-line of the Nagios probe.
	 * This is intended to be used to tell the administrator information about the probe, 
	 * just for them to know some further details about the test.
	 * @param fact the information the administrator is supposed to know. */
	public void addFact(String fact){
		facts.add(fact);
	}
	
	/**
	 * Tell whether all the given NagiosReturnObjects were telling no problem.
	 * @return true if everything is okay. */
	public Boolean isAllOkay(){
		Integer code = ElementalNagiosPlugin.RESULT_0_OK;
		for (NagiosMiniStatus o: miniStatusList){
			code = mostRelevantNagiosCode(code, o.getErrorCode());
		}	
		return code.equals(ElementalNagiosPlugin.RESULT_0_OK);
	}
	
	/**
	 * Get a summary of all the given NagiosReturnObjects.
	 * @return the summary. */
	public NagiosReturnObject getSummaryOfAll(){
		String message = "";
		Integer code = ElementalNagiosPlugin.RESULT_0_OK;
		for (String o: facts){
			message = (message.isEmpty()?"":message + ", ") + o;
		}	
		for (NagiosMiniStatus o: miniStatusList){
			message = (message.isEmpty()?"":message + ", ") + o.getErrorMessage();
			code = mostRelevantNagiosCode(code, o.getErrorCode());
		}	
		return new NagiosReturnObject(code, message);
	}
	
	/**
	 * Get a summary of all the given NagiosReturnObjects.
	 * Also includes a time_all curve with the sum of all the times registered by the
	 * provided TimedStatusTracer.
	 * @return the summary. */
	public NagiosReturnObject getSummaryOfAllWithTimeAll(TimedStatusTracer tracer){
		String message = "";
		Integer code = ElementalNagiosPlugin.RESULT_0_OK;
		for (String o: facts){
			message = (message.isEmpty()?"":message + ", ") + o;
		}	
		for (NagiosMiniStatus o: miniStatusList){
			message = (message.isEmpty()?"":message + ", ") + o.getErrorMessage();
			code = mostRelevantNagiosCode(code, o.getErrorCode());
		}	
		NagiosReturnObject ret = new NagiosReturnObject(code, message);
		ret.addCurvesSection(tracer, "time_all");
		return ret;
	}
	/**
	 * Retrieve the most important NagiosCode between the two provided.
	 * The definition of important is in increasing order, as follows:
	 * ok, warning, critical, unknown
	 * So if mostRelevantNagiosCode(warning,unknown)=unknown 
	 * @param a NagiosCode1.
	 * @param b NagiosCode2.
	 * @return the most important between NagiosCode1 and NagiosCode2.
	 */
	private Integer mostRelevantNagiosCode(Integer a, Integer b){
		if (a.equals(b)){
			return a;
		}
		if (a < b){ // A >= B always.
			Integer aux = a;
			a = b; b = aux;
		}
		/* AB AB AB AB AB AB AB AB AB AB AB AB AB AB AB AB */
		/* 00 01 02 03 10 11 12 13 20 21 22 23 30 31 32 33 */
		/*    01 02 03 10    12 13 20 21    23 30 31 32    */
		/*             10          20 21       30 31 32    */
		/*             wo          co cw       uo uw uc    */
		if (a == ElementalNagiosPlugin.RESULT_1_WARNING && b == ElementalNagiosPlugin.RESULT_0_OK)          {return ElementalNagiosPlugin.RESULT_1_WARNING;}
		if (a == ElementalNagiosPlugin.RESULT_2_CRITICAL&& b == ElementalNagiosPlugin.RESULT_0_OK)          {return ElementalNagiosPlugin.RESULT_2_CRITICAL;}
		if (a == ElementalNagiosPlugin.RESULT_2_CRITICAL&& b == ElementalNagiosPlugin.RESULT_1_WARNING)     {return ElementalNagiosPlugin.RESULT_2_CRITICAL;}
		if (a == ElementalNagiosPlugin.RESULT_3_UNKNOWN && b == ElementalNagiosPlugin.RESULT_0_OK)          {return ElementalNagiosPlugin.RESULT_3_UNKNOWN;}
		if (a == ElementalNagiosPlugin.RESULT_3_UNKNOWN && b == ElementalNagiosPlugin.RESULT_1_WARNING)     {return ElementalNagiosPlugin.RESULT_3_UNKNOWN;}
		if (a == ElementalNagiosPlugin.RESULT_3_UNKNOWN && b == ElementalNagiosPlugin.RESULT_2_CRITICAL)    {return ElementalNagiosPlugin.RESULT_3_UNKNOWN;}
		throw new RuntimeException("Configuration not expected: " + a + ":" + b);
	}
}
