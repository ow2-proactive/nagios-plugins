package qosprober.misc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Random;

import qosprober.main.PAMRProber;


/** This class is supposed to have multiple minor functionalities. */
public class Misc {
	 
	private static Random random = new Random();
    private Misc(){}

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
    
    public static  String getResourceNumberFromURL(String url) throws Exception{
    	// Typical one is pamr://9607/Node1807777269
    	
    	if (!url.startsWith(PAMRProber.PREFIX_URL)){
    		throw new Exception("Expected '" + PAMRProber.PREFIX_URL + "' at the beginning but found '" + url + "'.");
    	}
    	String rem = url.substring(PAMRProber.PREFIX_URL.length());
    	rem = rem.substring(0,rem.indexOf('/'));
    	return rem;
    }
    
    public static String generateRandomString(int length){
    	System.out.println("Generating random string...");
    	char[] buffer = new char[length];
    	for(int i=0;i<length;i++){
    		buffer[i] = generateRandomChar();
    	}
		return new String(buffer);
    	
    }
    
    private static char generateRandomChar(){
    	char c = (char)(random.nextInt(50)+32);
    	//char c = ' ';
    	return c;
    }
    
    
    public static String generateFibString(int length) { 
	
    	System.out.println("Generating Fibonacci...");
		int f0 = 0; 
		int f1 = 1; 
		
		char [] ret = new char[length];
		
		for (int i = 0; i < length; i++) { 
			char c = (char)(f0 % 256);
			
			ret[i] = c;
			
			final int temp = f1; 
			f1 += f0; 
			f0 = temp; 
		} 
		return new String(ret);
    }


}
