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

package qosprober.main;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.ow2.proactive.scheduler.common.job.*;
import java.util.Vector;

public class SchedulerThroughSingleThread {

	private final ExecutorService THREAD_POOL = Executors.newSingleThreadExecutor(); 	// The rights to manage the RM nodes will be given to this thread.
	
	SchedulerStubProberJava schedstub;
	
	public SchedulerThroughSingleThread(){
		schedstub = new SchedulerStubProberJava();
	}
	
	
	public void init(final String url, final String user, final String pass, final boolean polling, long timeoutms) throws InterruptedException, ExecutionException, TimeoutException{
		Callable<Object> callab = new Callable<Object>(){                                                    
			public Object call() throws Exception{                            
				schedstub.init(url, user, pass, polling);                           
				return null;
			}                                                                    
		};                                                                       
		FutureTask<Object> task = new FutureTask<Object>(callab);                                                                                   
		THREAD_POOL.execute(task);
		task.get(timeoutms, TimeUnit.MILLISECONDS);
	}
	
	public String submitJob(final String name, final String taskname, final Boolean highpriority, long timeoutms) throws InterruptedException, ExecutionException, TimeoutException{
		Callable<String> callab = new Callable<String>(){                                                    
			public String call() throws Exception{                            
				String ret = schedstub.submitJob(name, taskname, highpriority);                           
				return ret;
			}                                                                    
		};                                                                       
		FutureTask<String> task = new FutureTask<String>(callab);                                                                                   
		THREAD_POOL.execute(task);
		String ret = task.get(timeoutms, TimeUnit.MILLISECONDS);
		return ret; 
	}
	
	public String getJobResult(final String jobId, long timeoutms) throws InterruptedException, ExecutionException, TimeoutException{
		Callable<String> callab = new Callable<String>(){                                                    
			public String call() throws Exception{                            
				String ret = schedstub.getJobResult(jobId);                           
				return ret;
			}                                                                    
		};                                                                       
		FutureTask<String> task = new FutureTask<String>(callab);                                                                                   
		THREAD_POOL.execute(task);
		String ret = task.get(timeoutms, TimeUnit.MILLISECONDS);
		return ret; 
	}
	
	public void waitUntilJobFinishes(final String jobId, long timeoutms) throws InterruptedException, ExecutionException, TimeoutException{
		Callable<Object> callab = new Callable<Object>(){                                                    
			public Object call() throws Exception{                            
				schedstub.waitUntilJobFinishes(jobId);                           
				return null;
			}                                                                    
		};                                                                       
		FutureTask<Object> task = new FutureTask<Object>(callab);                                                                                   
		THREAD_POOL.execute(task);
		task.get(timeoutms, TimeUnit.MILLISECONDS);
	}
	
	public void waitUntilJobIsCleanedFinishes(final String jobId, long timeoutms) throws InterruptedException, ExecutionException, TimeoutException{
		Callable<Object> callab = new Callable<Object>(){                                                    
			public Object call() throws Exception{                            
				schedstub.waitUntilJobIsCleaned(jobId);                           
				return null;
			}                                                                    
		};                                                                       
		FutureTask<Object> task = new FutureTask<Object>(callab);                                                                                   
		THREAD_POOL.execute(task);
		task.get(timeoutms, TimeUnit.MILLISECONDS);
	}

	public JobStatus getJobStatus(final String jobId, long timeoutms) throws InterruptedException, ExecutionException, TimeoutException{
		Callable<JobStatus> callab = new Callable<JobStatus>(){                                                    
			public JobStatus call() throws Exception{                            
				JobStatus ret = schedstub.getJobStatus(jobId);                           
				return ret;
			}                                                                    
		};                                                                       
		FutureTask<JobStatus> task = new FutureTask<JobStatus>(callab);                                                                                   
		THREAD_POOL.execute(task);
		JobStatus ret = task.get(timeoutms, TimeUnit.MILLISECONDS);
		return ret; 
	}
	
	public void forceJobKillingAndRemoval(final String jobId, long timeoutms) throws InterruptedException, ExecutionException, TimeoutException{
		Callable<Object> callab = new Callable<Object>(){                                                    
			public Object call() throws Exception{                            
				schedstub.forceJobKillingAndRemoval(jobId);                           
				return null;
			}                                                                    
		};                                                                       
		FutureTask<Object> task = new FutureTask<Object>(callab);                                                                                   
		THREAD_POOL.execute(task);
		task.get(timeoutms, TimeUnit.MILLISECONDS);
	}

	public void removeJob(final String jobId, long timeoutms) throws InterruptedException, ExecutionException, TimeoutException{
		Callable<Object> callab = new Callable<Object>(){                                                    
			public Object call() throws Exception{                            
				schedstub.removeJob(jobId);                           
				return null;
			}                                                                    
		};                                                                       
		FutureTask<Object> task = new FutureTask<Object>(callab);                                                                                   
		THREAD_POOL.execute(task);
		task.get(timeoutms, TimeUnit.MILLISECONDS);
	}
	
	public void disconnect(long timeoutms) throws InterruptedException, ExecutionException, TimeoutException{
		Callable<Object> callab = new Callable<Object>(){                                                    
			public Object call() throws Exception{                            
				schedstub.disconnect();                           
				return null;
			}                                                                    
		};                                                                       
		FutureTask<Object> task = new FutureTask<Object>(callab);                                                                                   
		THREAD_POOL.execute(task);
		task.get(timeoutms, TimeUnit.MILLISECONDS);
	}
	
	public Vector<String> getAllCurrentJobsList(final String jobname, long timeoutms) throws InterruptedException, ExecutionException, TimeoutException{
		Callable<Vector<String>> callab = new Callable<Vector<String>>(){                                                    
			public Vector<String> call() throws Exception{                            
				Vector<String> ret = schedstub.getAllCurrentJobsList(jobname);                           
				return ret;
			}                                                                    
		};                                                                       
		FutureTask<Vector<String>> task = new FutureTask<Vector<String>>(callab);                                                                                   
		THREAD_POOL.execute(task);
		Vector<String> ret = task.get(timeoutms, TimeUnit.MILLISECONDS);
		return ret; 
	}
	
	public void removeOldProbeJobs(final String jobname, final boolean deleteallold, long timeoutms) throws InterruptedException, ExecutionException, TimeoutException{
		Callable<Object> callab = new Callable<Object>(){                                                    
			public Object call() throws Exception{                            
				schedstub.removeOldProbeJobs(jobname, deleteallold);                           
				return null;
			}                                                                    
		};                                                                       
		FutureTask<Object> task = new FutureTask<Object>(callab);                                                                                   
		THREAD_POOL.execute(task);
		task.get(timeoutms, TimeUnit.MILLISECONDS);
	}
}
