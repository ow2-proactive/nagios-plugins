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

import javax.security.auth.login.LoginException;
import org.apache.commons.httpclient.HttpException;
import org.apache.log4j.Logger;
import org.objectweb.proactive.ActiveObjectCreationException;
import org.objectweb.proactive.core.node.NodeException;
import org.ow2.proactive.scheduler.common.SchedulerConnection;
import org.ow2.proactive.scheduler.common.SchedulerState;
import org.ow2.proactive.scheduler.common.exception.NotConnectedException;
import org.ow2.proactive.scheduler.common.exception.PermissionException;
import org.ow2.proactive.scheduler.common.exception.SchedulerException;
import org.ow2.proactive.scheduler.common.exception.UnknownJobException;
import org.ow2.proactive.scheduler.common.job.*;
import org.ow2.proactive.scheduler.common.Scheduler;
import org.ow2.proactive.authentication.crypto.CredData;
import org.ow2.proactive.scheduler.common.SchedulerAuthenticationInterface;
import java.io.IOException;
import java.security.KeyException;
import java.util.Vector;
import org.ow2.proactive.authentication.crypto.Credentials;

/** 
 * Class that connects the test with the real scheduler, works as a stub. 
 * This is our interface to the remote Scheduler.
 * This class is specific for JAVAPA protocol. */
public class SchedulerStubProber {
	private static Logger logger =
			Logger.getLogger(SchedulerStubProber.class.getName()); 		// Logger.
	private Scheduler schedulerStub; 									// Stub to the scheduler.
	
	/**
	 * Constructor method. */
	public SchedulerStubProber(){}
	
	/** 
	 * Initialize the connection/session with the scheduler.
	 * @param url url of the scheduler. 
	 * @param user username to access the scheduler.
	 * @param pass password to access the scheduler. */
	public void init(String url, String user, String pass) throws IllegalArgumentException, LoginException, SchedulerException, KeyException, ActiveObjectCreationException, NodeException, HttpException, IOException{
		logger.info("Joining the scheduler at '" + url + "'...");
        SchedulerAuthenticationInterface auth = SchedulerConnection.join(url);
        logger.info("Done.");
        logger.info("Logging in...");
        Credentials cred = Credentials.createCredentials(new CredData(user, pass), auth.getPublicKey());
        schedulerStub = auth.login(cred);
        logger.info("Done.");
	}
	
	/** 
	 * Return the status of the job (running, finished, etc.). 
	 * @param jobId the ID of the job. 
	 * @return the status of the job. */
	public JobStatus getJobStatus(String jobId) throws NotConnectedException, PermissionException, UnknownJobException, HttpException, IOException{
		return schedulerStub.getJobState(jobId).getStatus();
	}  
	
	/** 
	 * Disconnect from the Scheduler. */
	public void disconnect() throws NotConnectedException, PermissionException, HttpException, IOException{		
		logger.info("Disconnecting...");
		schedulerStub.disconnect();
		logger.info("Done.");
	}
	
	/**
	 * Get a list of all jobs that are in running queue 
	 * of the scheduler (all jobs, including my jobs). 
	 * @return the list with all the running jobs. */
	public Vector<JobState> getAllRunningJobsList() throws NotConnectedException, PermissionException{
		logger.info("Getting list of jobs...");
		SchedulerState st = schedulerStub.getState(false); // Get not only my jobs but all of them.
		Vector<JobState> vector = st.getRunningJobs();
		for(JobState js: vector){
			logger.info("\t" + js.getId() + ":" + js.getName() + " tasks_running=" + js.getNumberOfRunningTasks());
		}
		logger.info("Done.");
		return vector;
	}
}
