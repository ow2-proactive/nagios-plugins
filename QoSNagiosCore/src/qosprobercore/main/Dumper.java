package qosprobercore.main;

import java.io.IOException;

import org.apache.log4j.Logger;

public class Dumper {
	private static Logger logger = Logger.getLogger(Dumper.class.getName()); // Logger.
	private String scriptpath;
	
	public Dumper(String scriptpath){
		this.scriptpath = scriptpath;
	}
	
	public void dump(NagiosReturnObject obj){
    	String command = " " + scriptpath + " " + obj.getErrorCode();
		logger.info("Dumping with command '" + command + "'...");
		try {
			Runtime.getRuntime().exec(command);
		} catch (IOException e) {
			logger.warn(e.getMessage());
		}
	}
}