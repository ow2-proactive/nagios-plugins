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

package org.ow2.proactive.nagios.history;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

import org.apache.log4j.Logger;
import org.ow2.proactive.nagios.exceptions.InvalidFileContentException;


/**
 * Class that manages historical information regarding previous executions of the current probe.
 * It can save a serialized object containing attributes regarding the data obtained during the current execution of the probe.
 * It can also load the information obtained before by another execution of the probe. 
 * The storage entity is a file which content is a serialized object. */
public class HistoryDataManager <T>{
	
	protected static Logger logger =						// Logger. 
			Logger.getLogger(HistoryDataManager.class.getName()); 
	private static final int MAX_BUFFER_SIZE = 1024 * 8;
	
	private FileLock lock = null;
	private FileChannel channel = null; 
	
	/**
	 * Take the lock of the given file.
	 * If the file does not exist, it will be created.
	 * @param filename file to use as a lock.
	 * @throws Exception in case the lock was already taken. */
	public HistoryDataManager(String filename) throws Exception{
	    // Get a file channel for the file.
	    File file = new File(filename);
	    channel = new RandomAccessFile(file, "rw").getChannel();
	
	    // Try acquiring the lock without blocking. This method returns
	    // null or throws an exception if the file is already locked.
	    try{ 
	        lock = channel.tryLock();
	    }catch(Exception e){
			logger.info("Error while locking the file...");
	    }
	    
	    if (lock == null){ // If any problem getting the lock...
			logger.info("File not locked... There was a problem...");
	    	channel.close();
	    	throw new Exception("Lock file '" + filename + "' already locked by another process...");
	    }
		logger.info("Done.");
	}

	/**
	 * Dump a serialized object to the file already given (which works as a lock as well).
	 * @param obj object to dump.
	 * @throws IOException if a problem while writing the file. */
	public void set(T obj) throws IOException {
		// Serialize to a byte array.
	    ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
		ObjectOutput out;
	    out = new ObjectOutputStream(bos) ;
	    out.writeObject(obj);
	    out.close();

	    // Get the bytes of the serialized object and dump it into the file.
	    byte[] buf = bos.toByteArray();
		channel.write(ByteBuffer.wrap(buf));
	}
	
	/**
	 * Get the object from the already given file.
	 * @return the object representing the history of the probe.
	 * @throws InvalidFileContentException if any problem with the file's content. */
	@SuppressWarnings("unchecked")
	public T get(T defaultObject) throws InvalidFileContentException {
		T ret;
		ByteBuffer buffer = ByteBuffer.allocate(MAX_BUFFER_SIZE);
		try{
			channel.read(buffer,0);
			byte[] bytes = buffer.array();

			ObjectInputStream in;
		    // Deserialize from a byte array.
		    in = new ObjectInputStream(new ByteArrayInputStream(bytes));
		    ret = (T)in.readObject();
		    in.close();
		    return ret;
		}catch(Exception e){
			return defaultObject;
		}
	}
	
	/**
	 * Release the token.
	 * @throws IOException if any problem. */
	public void release() throws IOException{
	    // Release the lock.
	    lock.release();
	    // Close the file.
	    channel.close();
	}
	
	public static void main(String[] args) throws Exception{
		// Test.
		System.out.println("Creating...");
		HistoryDataManager<Integer> h = new HistoryDataManager<Integer>("data");
		System.out.println("Setting...");
//		h.set(new Integer(1));
		System.out.println("Getting...");
		System.out.println((Integer)h.get(1));
		System.out.println("Releasing...");
		h.release();
		System.out.println("Done.");
	}
}
