package qosprober.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.io.StringReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashSet;

import org.apache.log4j.Logger;
import org.ow2.proactive.scheduler.common.job.Job;
import org.ow2.proactive.scheduler.common.job.factories.JobFactory;

import qosprober.exceptions.ElementNotFoundException;
import qosprober.main.RMProber;


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
            @SuppressWarnings("rawtypes")
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
        }else if(o instanceof HashSet){
        	
        	HashSet<Object> rr = (HashSet<Object>)o;
            for(Object i:rr){
                output = output + i + " ";
            }
        	
        }else{
        	output = "<UNKNOWN FORMAT>: " + o.getClass().getName();
        }

        return output;
    }

    /* Delete a given file. */
    public static void deleteFile(String filename) throws Exception{
        File file = new File(filename);
        if(file.delete()){
            //logger.info("File '" + filename + "' deleted.");
        }else{
        	logger.info("Error deleting file '" + filename + "'...");
        }

    }

    /* Delete a set of files matching the given extension. */
    public static void deleteFilesFrom(String extension, String tool_path) throws Exception{
        ArrayList<File> files = Misc.getListOfFiles(extension, tool_path);
        for (File f: files){
            if(f.delete()){
            	logger.info("File '" + f.getPath() + "' deleted.");
            }else{
            	logger.info("Error deleting file '" + f.getPath() + "'...");
            }
        }
    }

    /* Gets a list of files matching the extension given. */
    public static ArrayList<File> getListOfFiles(String extension, String tool_path) throws Exception{
        final String exten = extension;

        if (tool_path==null){
            tool_path = ".";
        }

        File dir = new File(tool_path);

        FilenameFilter fnf;
        if (extension!=null){
            fnf = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return (name.endsWith("." + exten));
                }
            };
        }else{
            fnf = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return true;
                }
            };
        }

        ArrayList<File> output = new ArrayList<File>();
        try{
            File[] list_of_log = dir.listFiles(fnf);

            for (int i=0; i<list_of_log.length; i++)
            {
                output.add(list_of_log[i]);
            }
        }catch(Exception e){
        	logger.warn("Not supposed to happen...", e);
        }
        return output;
    }

    /* Gets a list of files with particular suffix (including extension). */
    public static ArrayList<File> getListOfFilesEndingWith(String ending, String tool_path) throws Exception{
        final String ending_final = ending;

        if (tool_path==null){
            tool_path = ".";
        }

        File dir = new File(tool_path);

        FilenameFilter fnf;
        if (ending!=null){
            fnf = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return (name.endsWith(ending_final));
                }
            };
        }else{
            fnf = new FilenameFilter() {
                public boolean accept(File dir, String name) {
                    return true;
                }
            };
        }

        ArrayList<File> output = new ArrayList<File>();
        try{
            File[] list_of_log = dir.listFiles(fnf);

            for (int i=0; i<list_of_log.length; i++)
            {
                output.add(list_of_log[i]);
            }
        }catch(Exception e){
        	logger.warn("Not supposed to happen...", e);
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

    /* Append a content into a file. */
    public static void appendToFile(String filename, String content) throws Exception{
        try {
            FileOutputStream appendedFile =
                    new FileOutputStream(filename, true);
            appendedFile.write(content.getBytes());
            appendedFile.close();

        } catch (Exception e) {
        	logger.warn("Not supposed to happen...", e);
        }

    }

    /* Split a String into many Strings with the lines of the first one. */
    public static ArrayList<String> getLines(String input){
        String str;
        ArrayList<String> ret = new ArrayList<String>();
        BufferedReader reader = new BufferedReader(new StringReader(input));
        try{
            while ((str = reader.readLine()) != null){
                if (str.length() > 0){
                    ret.add(str);
                }
            }
        }catch(Exception e){
        	logger.warn("Not supposed to happen...", e);
        }
        return ret;
    }


    /* Filter out empty Strings from the set. */
    public static ArrayList<String> filterEmptyLines(ArrayList<String> inp){
        ArrayList<String> ret = new ArrayList<String>();
        for(String l:inp){
            if (l.trim().length()>0){
                ret.add(l);
            }
        }
        return ret;
    }

    public static String getLineThatContains(ArrayList<String> set, String string){
        for (String line: set){
            if (line.contains(string)){
                return line;
            }
        }
        return null;
    }
    
    /* A key-value Strings set is given. This method gets the value section using the key. */
    public static String getValueUsingKey(String key, ArrayList<String> set) throws ElementNotFoundException{
    	for (String s: set){
    		if (s.toUpperCase().startsWith(key.toUpperCase())){
    			return s.substring(key.length()).trim();
    		}
    	}
    	throw new ElementNotFoundException("The key '" + key + "' was not found.");
    }

    /* Print the classpath (used for debug only). */
    public static String getClasspath() {
        // Get the System Classloader
    	String ret = "";
        ClassLoader sysClassLoader = ClassLoader.getSystemClassLoader();
        ret = "Classpath [";
        // Get the URLs
        URL[] urls = ((URLClassLoader)sysClassLoader).getURLs();

        for(int i=0; i< urls.length; i++)
        {
            ret = ret + "\t-" + urls[i].getFile() + "\n";
        }       
        ret = ret + "]";
        return ret;
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
		    
		    temp.deleteOnExit(); // Delete temp. file when program exits.

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
	 * Used when a parameter given by the user is wrong. 
	 * Print a message, then the usage of the application, and the exits the application. */
	public static void printMessageUsageAndExit(String mainmessage){
		System.out.println(mainmessage);
	    Misc.printUsage();
	    System.exit(RMProber.RESULT_CRITICAL);
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
	 * Print the version of the plugin and the exits the application. */
	public static void printVersionAndExit(){
		String usage = null;
		try {
			usage = Misc.readAllTextResource("/resources/version.txt");
			System.err.println(usage);
		} catch (IOException e) {
			logger.warn("Issue with usage message. Error: '"+e.getMessage()+"'.", e); 
		}
	    System.exit(RMProber.RESULT_OK);
	}
}
