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

import org.ow2.proactive.resourcemanager.common.RMState;
import org.ow2.proactive.utils.NodeSet;

/**
 * This class deals with the fact that:
 * 1. Once a session is started with the RM, only the Thread that started the connection can make calls to the RM. 
 * 2. We need to ensure that in case of timeout, the quickDisconnection method should be called in such a way that if any resource
 *    was already obtained, by the quickDisconnection it will be released automatically. */
public class RMThroughSingleThread {
	private final ExecutorService THREAD_POOL = Executors.newSingleThreadExecutor(); 	// The rights to manage the RM nodes will be given to this thread.
	
	RMStubProber rmstub;
	
	public RMThroughSingleThread(){
		rmstub = new RMStubProber();
	}
	
	public void init(final String url, final String user, final String pass, long timeoutms) throws InterruptedException, ExecutionException, TimeoutException{
		Callable<Object> callab = new Callable<Object>(){                                                    
			public Object call() throws Exception{                            
				rmstub.init(url, user, pass);                           
				return null;
			}                                                                    
		};                                                                       
		FutureTask<Object> task = new FutureTask<Object>(callab);                                                                                   
		THREAD_POOL.execute(task);
		task.get(timeoutms, TimeUnit.MILLISECONDS);
	}
	
	public NodeSet getNodes(final int amountOfNodesRequired, long timeoutms) throws InterruptedException, ExecutionException, TimeoutException{
		Callable<NodeSet> callab = new Callable<NodeSet>(){                                                    
			public NodeSet call() throws Exception{                            
				NodeSet ret = rmstub.getNodes(amountOfNodesRequired);                           
				return ret;
			}                                                                    
		};                                                                       
		FutureTask<NodeSet> task = new FutureTask<NodeSet>(callab);                                                                                   
		THREAD_POOL.execute(task);
		NodeSet ret = task.get(timeoutms, TimeUnit.MILLISECONDS);
		return ret;
	}
	
	public void releaseNodes(final NodeSet setOfNodesToRelease, long timeoutms) throws InterruptedException, ExecutionException, TimeoutException{
		Callable<Object> callab = new Callable<Object>(){                                                    
			public Object call() throws Exception{                            
				rmstub.releaseNodes(setOfNodesToRelease);                           
				return null;
			}                                                                    
		};                                                                       
		FutureTask<Object> task = new FutureTask<Object>(callab);                                                                                   
		THREAD_POOL.execute(task);
		task.get(timeoutms, TimeUnit.MILLISECONDS);
	}
	
	public RMState getRMState(long timeoutms) throws InterruptedException, ExecutionException, TimeoutException{
		Callable<RMState> callab = new Callable<RMState>(){                                                    
			public RMState call() throws Exception{                            
				RMState ret = rmstub.getRMState();                           
				return ret;
			}                                                                    
		};                                                                       
		FutureTask<RMState> task = new FutureTask<RMState>(callab);                                                                                   
		THREAD_POOL.execute(task);
		RMState ret = task.get(timeoutms, TimeUnit.MILLISECONDS);
		return ret; 
	}
	
	public Boolean disconnect(long timeoutms) throws InterruptedException, ExecutionException, TimeoutException{
		Callable<Boolean> callab = new Callable<Boolean>(){                                                    
			public Boolean call() throws Exception{                            
				Boolean ret = rmstub.disconnect();                           
				return ret;
			}                                                                    
		};                                                                       
		FutureTask<Boolean> task = new FutureTask<Boolean>(callab);                                                                                   
		THREAD_POOL.execute(task);
		Boolean ret = task.get(timeoutms, TimeUnit.MILLISECONDS);
		return ret;
	}
	
	public void quickDisconnect(long timeoutms) throws InterruptedException, ExecutionException, TimeoutException{
		Callable<Object> callab = new Callable<Object>(){                                                    
			public Object call() throws Exception{                            
				rmstub.quickDisconnect();                           
				return null;
			}                                                                    
		};                                                                       
		FutureTask<Object> task = new FutureTask<Object>(callab);                                                                                   
		THREAD_POOL.execute(task);
		task.get(timeoutms, TimeUnit.MILLISECONDS);
	}
}
