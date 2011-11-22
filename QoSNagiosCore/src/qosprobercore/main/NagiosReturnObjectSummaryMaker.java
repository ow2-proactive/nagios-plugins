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

public class NagiosReturnObjectSummaryMaker{
	
	private ArrayList<NagiosReturnObject> list;
	private ArrayList<String> facts;

	public NagiosReturnObjectSummaryMaker(){
		list = new ArrayList<NagiosReturnObject>();
		facts = new ArrayList<String>();
	}
	
	public void addNagiosReturnObject(NagiosReturnObject nro){
		if (nro.getException()!=null)
			throw new RuntimeException("NagiosReturnObjects with Exceptions are not supposed to be added here.");
		list.add(nro);
	}
	
	public void addFact(String fact){
		facts.add(fact);
	}
	
	public Boolean isAllOkay(){
		Integer code = NagiosReturnObject.RESULT_0_OK;
		for (NagiosReturnObject o: list){
			code = mostRelevantNagiosCode(code, o.getErrorCode());
		}	
		return code.equals(NagiosReturnObject.RESULT_0_OK);
	}
	
	public NagiosReturnObject getSummaryOfAll(){
		String message = "";
		Integer code = NagiosReturnObject.RESULT_0_OK;
		for (String o: facts){
			message = (message.isEmpty()?"":message + ", ") + o;
		}	
		for (NagiosReturnObject o: list){
			message = (message.isEmpty()?"":message + ", ") + o.getErrorMessage();
			code = mostRelevantNagiosCode(code, o.getErrorCode());
		}	
		return new NagiosReturnObject(code, message);
	}
	
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
		if (a == NagiosReturnObject.RESULT_1_WARNING && b == NagiosReturnObject.RESULT_0_OK)          {return NagiosReturnObject.RESULT_1_WARNING;}
		if (a == NagiosReturnObject.RESULT_2_CRITICAL&& b == NagiosReturnObject.RESULT_0_OK)          {return NagiosReturnObject.RESULT_2_CRITICAL;}
		if (a == NagiosReturnObject.RESULT_2_CRITICAL&& b == NagiosReturnObject.RESULT_1_WARNING)     {return NagiosReturnObject.RESULT_2_CRITICAL;}
		if (a == NagiosReturnObject.RESULT_3_UNKNOWN && b == NagiosReturnObject.RESULT_0_OK)          {return NagiosReturnObject.RESULT_3_UNKNOWN;}
		if (a == NagiosReturnObject.RESULT_3_UNKNOWN && b == NagiosReturnObject.RESULT_1_WARNING)     {return NagiosReturnObject.RESULT_3_UNKNOWN;}
		if (a == NagiosReturnObject.RESULT_3_UNKNOWN && b == NagiosReturnObject.RESULT_2_CRITICAL)    {return NagiosReturnObject.RESULT_3_UNKNOWN;}
		throw new RuntimeException("Configuration not expected: " + a + ":" + b);
	}
}
