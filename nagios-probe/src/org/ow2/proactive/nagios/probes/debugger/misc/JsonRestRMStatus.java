package org.ow2.proactive.nagios.probes.debugger.misc;

import java.text.ParseException;
import java.util.regex.*;

public class JsonRestRMStatus {
	private int freeNodes;
	
	public JsonRestRMStatus(String json) throws ParseException{
		freeNodes = this.parse(json, "\"freeNodesNumber\"");
	}
	
	public int getFreeNodes(){
		return freeNodes;
		
	}
	
	private int parse(String text, String key) throws ParseException{
		// "freeNodesNumber":650		
		String tocompile = key + ":(\\d+)";
		Pattern strMatch = Pattern.compile(tocompile);
        Matcher m = strMatch.matcher(text);
        
        if(m.find()){
            String str = m.group(1);
            return Integer.valueOf(str);
        }
        else{
            throw new ParseException("Could not match the regex '" + tocompile + "' in text '" + text + "'.",0);
        }
	}
}
