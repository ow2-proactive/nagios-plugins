import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringReader;
import java.util.ArrayList;


/** This class is supposed to have multiple minor functionalities. */
public class Misc {

	private static boolean alltostdout = false;
	private static PrintStream stdout;
	private static PrintStream stdlog;
	
    private Misc(){}

    public static String printSpecified(Object o){
        String output = "";
        if (o instanceof ArrayList){
            ArrayList a = (ArrayList) o;
            for(Object i:a){
                output = output + i + " ";
            }
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
        }

        return output;
    }
    //public static int executeCall(String call) throws Exception {

     

    public static void deleteFile(String filename) throws Exception{
        File file = new File(filename);
        if(file.delete()){
            System.out.println("File '" + filename + "' deleted.");
        }else{
            System.out.println("Error deleting file '" + filename + "'...");
        }

    }



    public static void deleteFilesFrom(String extension, String tool_path) throws Exception{
        ArrayList<File> files = Misc.getListOfFiles(extension, tool_path);
        for (File f: files){
            if(f.delete()){
                System.out.println("File '" + f.getPath() + "' deleted.");
            }else{
                System.out.println("Error deleting file '" + f.getPath() + "'...");
            }
        }
    }

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
            e.printStackTrace();
        }
        return output;
    }

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
            e.printStackTrace();
        }
        return output;
    }


    public static String readAllFile(String filename) throws IOException{
        String str =  null;
        
        FileInputStream i = new FileInputStream(filename);
        byte buff[] = new byte[i.available()];
        i.read(buff);
        i.close();
        str = new String(buff);
    
        return str;
    }

    public static void writeAllFile(String filename, String content) throws Exception{
        try {
            FileOutputStream i = new FileOutputStream(filename);
            i.write(content.getBytes());
            i.close();
        }catch (Exception e){
            throw new Exception("Error writing file: " + filename);
        }
    }

    public static void appendToFile(String filename, String content) throws Exception{
        try {
            FileOutputStream appendedFile =
                    new FileOutputStream(filename, true);
            appendedFile.write(content.getBytes());
            appendedFile.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }


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
            e.printStackTrace();
        }
        return ret;
    }



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
    
    public static String getValueUsingKey(String key, ArrayList<String> set) throws ElementNotFoundException{
    	for (String s: set){
    		if (s.toUpperCase().startsWith(key.toUpperCase())){
    			return s.substring(key.length()).trim();
    		}
    	}
    	throw new ElementNotFoundException("The key " + key + " was not found.");
    }
    
    
    public static void redirectStdOut(boolean alltostdoutt) throws FileNotFoundException{
    	alltostdout = alltostdoutt;
	
    	if (stdout == null){
    		stdout = System.out;
    		stdlog = new PrintStream(new FileOutputStream("output.log"));
    	}
    	if (alltostdout==true){
    		System.setOut(stdout);
    	}else{
	    	System.setOut(stdlog);
    	}
    	
    }
    
    public static void print(String str){
    	PrintStream previous = System.out;
    	if (alltostdout==true){
    		System.setOut(stdout);
    	}else{
	    	System.setOut(stdout);
    	}
        System.out.println(str);
        System.setOut(previous);
        
    }
    public static void log(String str){
    	PrintStream previous = System.out;
    	if (alltostdout==true){
    		System.setOut(stdout);
    	}else{
    		System.setOut(stdlog);
    	}
    	System.out.println(str);
    	System.setOut(previous);
    }
}
