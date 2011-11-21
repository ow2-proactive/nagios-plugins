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

import java.util.HashMap;
import java.util.Set;

/**
 * An Arguments object contains all the arguments (entries) that a probe class might need to perform the test.
 * We control that each get has a valid key. */
public class Arguments {
	private HashMap<String, Object> args;	// Set of arguments given.
	
	/** 
	 * Constructor. */
	public Arguments(){
		args = new HashMap<String, Object>();
	}
	
	/**
	 * Simple put method.
	 * @param key key to be used for the new entry.
	 * @param value value to be used for the new entry. */
	public void put(String key, Object value){
		args.put(key, value);
	}
	
	/**
	 * Simple get method. 
	 * It controls that the given key has been 'put' before by a 'put' method.
	 * @param key key to get the right entry.
	 * @return returns the value of the entry/argument. */
	public Object get(String key){
		if (args.containsKey(key)==false)
			throw new RuntimeException("Problem trying to use key '" + key + "'.");
		return args.get(key);
	}
	
	/**
	 * Get the whole key set.
	 * @return key set.
	 */
	public Set<String> keySet(){
		return args.keySet();
	}
	
	/**
	 * Testing purposes. */
	public static void main(String[] args){
		Arguments ar = new Arguments();
		ar.put("hey1", "hey");
		ar.put("hey2", null);
		System.out.println(ar.get("hey1"));
		System.out.println(ar.get("hey2"));
		System.out.println(ar.get("hey3"));
	}
	
}
