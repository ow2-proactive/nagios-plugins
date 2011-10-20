package qosprober.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Properties;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.ow2.proactive.scheduler.common.job.Job;
import org.ow2.proactive.scheduler.common.job.factories.JobFactory;
import qosprober.main.JobProber;


/** This class is supposed to have multiple minor functionalities. */
public class Misc {

	private static Logger logger = Logger.getLogger(Misc.class.getName()); // Logger.
	private static final long MAX_BUFFER = 1024 * 1024 * 2; // 2 MiB the maximum size of a file to get loaded completely. 
	
	
    private Misc(){}

    /* Get a descriptive string from the given object. 
     * Particularly useful if the argument is an ArrayList, or just an array. */
	@SuppressWarnings("unchecked")
	public static String getDescriptiveString(Object o){
        String output = "";
        if (o instanceof ArrayList){
            ArrayList a = (ArrayList) o;
            for(Object i:a){
                output = output + i.toString() + " ";
            }
        }else if(o instanceof Object[]){
            Object[] a = (Object[]) o;
            output = "[";
            for(Object i:a){
            	if (i==null){
            		//output = output + "null" + " ";
            	}else{
            		output = output + i.toString() + " ";
            	}
            }
            output = output + "]";
        }else if(o instanceof int[]){
            int[] a = (int[]) o;
            for (int i=0;i<a.length;i++){
                output = output + a[i] + " ";
            }
        }else if(o instanceof float[]){
            float[] a = (float[]) o;
            for (int i=0;i<a.length;i++){
                output = output + a[i] + " ";
            }
        }else{
        	output = "<UNKNOWN FORMAT>";
        }

        return output;
    }



    /* Put the content of a file into a String. */
    public static String readAllFile(String filename) throws IOException{
        RandomAccessFile file = new RandomAccessFile (filename, "r");
        if (file.length() > MAX_BUFFER){
        	throw new IOException("The file '"+filename+"' is too big to load it completely in memory.");
        }
        byte buff[] = new byte[(int) file.length()];
        file.readFully(buff);
        String str = new String(buff);
        file.close();
        return str;
    }

    /* Write a content into a file. */
    public static void writeAllFile(String filename, String content) throws Exception{
        try {
            FileOutputStream i = new FileOutputStream(filename);
            i.write(content.getBytes());
            i.close();
        }catch (Exception e){
            throw new Exception("Error writing file: '" + filename + "'.");
        }
    }

    /* Read all the content of a resource file. */
    public static String readAllTextResource(String resource) throws IOException{
		InputStream is = Misc.class.getResourceAsStream(resource);
	    InputStreamReader isr = new InputStreamReader(is);
	    BufferedReader br = new BufferedReader(isr);
	    String line;
	    String ret = "";
	    while ((line = br.readLine()) != null){
	      ret = ret + line + "\n";
	    }
	    br.close();
	    isr.close();
	    is.close();
	    return ret;
	}
    
    /* Return the name of a job by reading its job file descriptor. */
    public static String getJobNameFromJobDescriptor(String jobdescxmlpath) throws Exception{
    	Job job = JobFactory.getFactory().createJob(jobdescxmlpath);
    	return job.getName();
    }
    
    /** 
	 * Create a java.policy file to grant permissions, and load it for the current JVM. */
	public static void createPolicyAndLoadIt() throws Exception{
		try{
			
			
		    File temp = File.createTempFile("javapolicy", ".policy"); // Create temp file.
		    

		    temp.deleteOnExit(); // Delete temp file when program exits.

		    // Write to temp file.
		    BufferedWriter out = new BufferedWriter(new FileWriter(temp));
		    String policycontent = "grant {permission java.security.AllPermission;};";
		    out.write(policycontent);
		    out.close();

		    String policypath = temp.getAbsolutePath(); 
		    
		    System.setProperty("java.security.policy", policypath); // Load security policy.
		    
		}catch(Exception e){
			throw new Exception("Error while creating the security policy file. " + e.getMessage());
		}
	}

    
	
	/**
	 * Print the usage of the application. */
	public static void printUsage(){
		String usage = null;
		try {
			usage = Misc.readAllTextResource("/resources/usage.txt");
			System.err.println(usage);
		} catch (IOException e) {
			logger.warn("Issue with usage message. Error: '"+e.getMessage()+"'.", e); 
		}
	}
	
	/**
	 * Creates a default set of properties for the log4j logging module. */
	public static Properties getSilentLoggingProperties(){
		Properties properties = new Properties();
		properties.put("log4j.rootLogger",				"ERROR,NULL"); 	// By default, do not show anything.
		properties.put("log4j.logger.org",				"ERROR,STDOUT");	// For this module, show warning messages in stdout.
		properties.put("log4j.logger.proactive", 		"ERROR,STDOUT");
		properties.put("log4j.logger.qosprober", 		"ERROR,STDOUT");
		/* NULL Appender. */
		properties.put("log4j.appender.NULL",			"org.apache.log4j.varia.NullAppender");
		/* STDOUT Appender. */
		properties.put("log4j.appender.STDOUT",			"org.apache.log4j.ConsoleAppender");
		properties.put("log4j.appender.STDOUT.Target",	"System.out");
		properties.put("log4j.appender.STDOUT.layout",	"org.apache.log4j.PatternLayout");
		properties.put("log4j.appender.STDOUT.layout.ConversionPattern","%-4r [%t] %-5p %c %x - %m%n");
		return properties;
	}
	
	/**
	 * Configures de log4j module for logging. */
	public static void log4jConfiguration(int debuglevel){
		System.setProperty("log4j.configuration", "");
		if (debuglevel == JobProber.DEBUG_LEVEL_3USER){
			/* We load the log4j.properties file. */
			PropertyConfigurator.configure("log4j.properties");
		}else {
			/* We do the log4j configuration on the fly. */
			Properties properties = Misc.getSilentLoggingProperties();
			PropertyConfigurator.configure(properties);
		}
	}

	
}
