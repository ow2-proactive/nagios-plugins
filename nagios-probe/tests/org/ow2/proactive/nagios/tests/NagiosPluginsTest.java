/*
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
 *  * $$PROACTIVE_INITIAL_DEV$$
 */

package org.ow2.proactive.nagios.tests;

import java.util.Map;
import org.junit.Test;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Before;
import java.util.HashMap;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.ow2.proactive.nagios.common.Arguments;
import org.ow2.proactive.nagios.probes.rm.RMProber;
import org.ow2.proactive.scheduler.common.Scheduler;
import org.ow2.proactive.nagios.common.NagiosReturnObject;
import org.ow2.proactive.nagios.probes.scheduler.JobProber;
import org.ow2.proactive.nagios.common.ElementalNagiosPlugin;
import org.ow2.proactive.nagios.tests.testutils.NagiosFuncTHelper;


@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class NagiosPluginsTest {

    /**
     * Url of the RM.
     */
    private static String rmurl;

    /**
     * Url of the Scheduler.
     */
    private static String schedurl;

    /**
     * Flag to tell if the last test was executed (so RM and Scheduler can be shut down).
     */
    private static boolean lastTestExecuted = false;

    //@Before
    public void startRMandScheduler() throws Exception {

        if (rmurl == null) {

            // Start the RM. 
            try {
                System.out.println("Starting RM (make sure no other RM is running)...");
                Map<String, String> rmNodeJvmArgs = new HashMap<String, String>();

                rmurl = NagiosFuncTHelper.startResourceManager(rmNodeJvmArgs);
                schedurl = NagiosFuncTHelper.startScheduler(rmurl);
            } catch (Exception e) {
                System.out.println("Error while initializing the RM and its Node Source...");
                e.printStackTrace();
                rmurl = "<error>";
            }
        }
    }

    @Test
    public void Test_Probe_TimeoutCritical() throws Exception {
        String[] args = new String[] { "--url", rmurl, "--critical", "0", "--user", "admin", "--pass",
                "admin" };
        final Arguments options = new Arguments(args);
        RMProber prob = new RMProber(options); // Create the prober.
        prob.initializeProber();
        NagiosReturnObject ret = prob.startProbeAndExitManualTimeout(true); // Start the probe.
        if (ret.getErrorCode() != ElementalNagiosPlugin.RESULT_2_CRITICAL) {
            throw new Exception("Fail.");
        }
    }

    @Test
    public void Test_Probe_TimeoutWarning() throws Exception {
        String[] args = new String[] { "--url", rmurl, "--critical", "10", "--user", "admin", "--pass",
                "admin", "--warning", "0" };
        final Arguments options = new Arguments(args);
        RMProber prob = new RMProber(options); // Create the prober.
        prob.initializeProber();
        NagiosReturnObject ret = prob.startProbeAndExitManualTimeout(true); // Start the probe.
        if (ret.getErrorCode() != ElementalNagiosPlugin.RESULT_1_WARNING) {
            throw new Exception("Fail.");
        }
    }

    @Test
    public void Test_JobProber() throws Exception {
        startRMandScheduler();
        String[] args = new String[] { "--url", schedurl, "--critical", "10", "--user", "admin", "--pass",
                "admin" };
        final Arguments options = new Arguments(args);
        JobProber prob = new JobProber(options); // Create the prober.
        prob.initializeProber();
        NagiosReturnObject ret = prob.startProbeAndExitManualTimeout(true); // Start the probe.
        if (ret.getErrorCode() != ElementalNagiosPlugin.RESULT_0_OK) {
            throw new Exception("Fail.");
        }
    }

    @Test
    public void Test_JobProberRemoval() throws Exception {
        // Some job will remain in the scheduler, without being erased.
        for (int i = 5; i > 0; i--) {
            String[] args = new String[] { "--url", schedurl, "--critical", "" + i, "--user", "admin",
                    "--pass", "admin" };
            final Arguments options = new Arguments(args);
            JobProber prob = new JobProber(options); // Create the prober.
            prob.initializeProber();
            prob.startProbeAndExitManualTimeout(true); // Start the probe.
        }

        Scheduler sched = NagiosFuncTHelper.getScheduler();

        int jobs;

        jobs = sched.getState().getFinishedJobs().size() + sched.getState().getPendingJobs().size() +
            sched.getState().getRunningJobs().size();

        System.out.println(jobs);

        // Execution with time enough to clean rubish in the scheduler.
        String[] args = new String[] { "--url", schedurl, "--critical", "10", "--user", "admin", "--pass",
                "admin" };
        final Arguments options = new Arguments(args);
        JobProber prob = new JobProber(options); // Create the prober.
        prob.initializeProber();
        NagiosReturnObject ret = prob.startProbeAndExitManualTimeout(true); // Start the probe.
        if (ret.getErrorCode() != ElementalNagiosPlugin.RESULT_0_OK) {
            throw new Exception("Fail.");
        }

        jobs = sched.getState().getFinishedJobs().size() + sched.getState().getPendingJobs().size() +
            sched.getState().getRunningJobs().size();

        if (jobs != 0) {
            throw new Exception("Fail.");
        }
    }

    @Test
    public void Test_RMProber_Basic() throws Exception {
        String[] args = new String[] { "--url", rmurl, "--critical", "10", "--user", "admin", "--pass",
                "admin" };
        final Arguments options = new Arguments(args);
        RMProber prob = new RMProber(options); // Create the prober.
        prob.initializeProber();
        NagiosReturnObject ret = prob.startProbeAndExitManualTimeout(true); // Start the probe.
        if (ret.getErrorCode() != ElementalNagiosPlugin.RESULT_0_OK) {
            throw new Exception("Fail.");
        }
    }

    @Test
    public void Test_RMProber_NodesCritical() throws Exception {
        String[] args = new String[] { "--url", rmurl, "--critical", "10", "--user", "admin", "--pass",
                "admin", "--nodesrequired", "2", "--nodescritical", "2" };
        final Arguments options = new Arguments(args);
        RMProber prob = new RMProber(options); // Create the prober.
        prob.initializeProber();
        NagiosReturnObject ret = prob.startProbeAndExitManualTimeout(true); // Start the probe.
        if (ret.getErrorCode() != ElementalNagiosPlugin.RESULT_2_CRITICAL) {
            throw new Exception("Fail.");
        }
    }

    @Test
    public void Test_RMProber_NodesWarning() throws Exception {
        String[] args = new String[] { "--url", rmurl, "--critical", "10", "--user", "admin", "--pass",
                "admin", "--nodesrequired", "2", "--nodeswarning", "2", "--nodescritical", "1" };
        final Arguments options = new Arguments(args);
        RMProber prob = new RMProber(options); // Create the prober.
        prob.initializeProber();
        NagiosReturnObject ret = prob.startProbeAndExitManualTimeout(true); // Start the probe.
        if (ret.getErrorCode() != ElementalNagiosPlugin.RESULT_1_WARNING) {
            throw new Exception("Fail.");
        }
    }

    @Test
    public void zz_lastTest() throws Exception {
        lastTestExecuted = true;
    }

    @After
    public void shutDownScheduler() throws Exception {
        if (lastTestExecuted) {
            System.out.println("RM and Scheduler not needed anymore. Shutting it down...");
            rmurl = null;
            schedurl = null;
            NagiosFuncTHelper.stopRm();
            NagiosFuncTHelper.stopScheduler();
        } else {
            System.out.println("RM and Scheduler still needed... Keeping them alive.");
        }
    }
}
