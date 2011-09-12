
package misc;

import java.io.*;
import java.net.*;
import java.util.HashMap;


public class RestMisc {
	private static String serverRestAPI = null;
	
	public static String get(String id, String restcommand, String ... args) throws Exception {
        URL url = new URL(serverRestAPI + restcommand + "?" + transf(args));
        URLConnection yc = url.openConnection();

        if (id!=null){
        	yc.setRequestProperty("sessionid", id);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
        
        String inputLine;
        String acum = "";

        while ((inputLine = in.readLine()) != null){ 
            System.out.println(inputLine);
            acum = acum + inputLine + "\n";
        }
        in.close();
        return acum;
    }

    public static String put(String id, String restcommand, String ... args) throws Exception {
    	
		URL url = new URL(serverRestAPI + restcommand);
		URLConnection connection = url.openConnection();
		connection.setDoOutput(true);
		

		if (id!=null){
			connection.setRequestProperty("sessionid", id);
        }
		
		OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
		
		String acum = "";
		int i = 0;
		if (args.length%2!=0){
			throw new IllegalArgumentException("Invalid number of arguments, should be even.");
		}
		
		for(String val:args){
			if (i%2==0){
				acum = acum + val + "=";
			}else{
				acum = acum + val + "&";
			}
			i++;
    	}
		
		//acum = "username=demo&password=demo";
		
	//	out.write(URLEncoder.encode(acum,"UTF-8"));
		out.write(acum);
		out.close();

		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		
		String decodedString;
		String acume = "";
		while ((decodedString = in.readLine()) != null) {
		    System.out.println(decodedString);
		    acume = acume + decodedString;
		}
		
		in.close();
		return acume;
    }
    
 public static String post(String id, String restcommand, String ... args) throws Exception {
    	
		URL url = new URL(serverRestAPI + restcommand);
		URLConnection connection = url.openConnection();
		connection.setDoOutput(true);

		if (id!=null){
			connection.setRequestProperty("sessionid", id);
        }
		
		OutputStreamWriter out = new OutputStreamWriter(connection.getOutputStream());
		
		String acum = "";
		int i = 0;
		if (args.length%2!=0){
			throw new IllegalArgumentException("Invalid number of arguments, should be even.");
		}
		
		for(String val:args){
			if (i%2==0){
				acum = acum + val + "=";
			}else{
				acum = acum + val + "&";
			}
			i++;
    	}
		
		//acum = "username=demo&password=demo";
		
		//out.write(URLEncoder.encode(acum,"UTF-8"));
		out.write(acum);
		out.close();

		BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
		
		String decodedString;
		String acume = "";
		while ((decodedString = in.readLine()) != null) {
		    System.out.println(decodedString);
		    acume = acume + decodedString;
		}
		
		in.close();
		return acume;
    }
    
    public static void setRestAPIServerURL(String url){
    	RestMisc.serverRestAPI = url;
    }

    public static String transf(String[] args){
    	String acum = "";
    	int i = 0;
    	for(String val:args){
			if (i%2==0){
				acum = acum + val + "=";
			}else{
				acum = acum + val + "&";
			}
			i++;
    	}
    	return acum;
    }
    
    public static void main(String argsssssss[]){
    	                              
    	RestMisc.setRestAPIServerURL("http://localhost:8080/SchedulingRest-1.0.0/rest");
   
    	String id = null;
		try {
			id = RestMisc.post(null, "/scheduler/login", "username", "demo", "password", "demo");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	try {
			RestMisc.get(id, "/scheduler/jobs", "index", "1" , "range", "1");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		try {
			RestMisc.get(id, "/scheduler/isconnected");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		

		try {
			RestMisc.get(id, "/scheduler/status");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    	try {
			RestMisc.put(id, "/scheduler/disconnect", "1", "1");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
		
    	try {
			RestMisc.get(id, "/scheduler/jobs", "index", "1" , "range", "1");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		try {
			RestMisc.get(id, "/scheduler/isconnected");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
    }

}

