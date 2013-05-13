package org.ow2.proactive.nagios.tests.testutils;
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
 * $PROACTIVE_INITIAL_DEV$
 */


import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import org.objectweb.proactive.api.PAFuture;
import org.objectweb.proactive.core.config.CentralPAPropertyRepository;
import org.objectweb.proactive.core.util.wrapper.BooleanWrapper;
import org.ow2.proactive.authentication.crypto.Credentials;
import org.ow2.proactive.resourcemanager.authentication.RMAuthentication;
import org.ow2.proactive.resourcemanager.common.event.RMInitialState;
import org.ow2.proactive.resourcemanager.core.properties.PAResourceManagerProperties;
import org.ow2.proactive.resourcemanager.frontend.RMConnection;
import org.ow2.proactive.resourcemanager.frontend.ResourceManager;
import org.ow2.proactive.resourcemanager.nodesource.infrastructure.LocalInfrastructure;
import org.ow2.proactive.resourcemanager.nodesource.policy.StaticPolicy;
import org.ow2.proactive.resourcemanager.utils.RMStarter;
import org.ow2.proactive.scheduler.common.Scheduler;
import org.ow2.proactive.scheduler.common.SchedulerAuthenticationInterface;
import org.ow2.proactive.scheduler.common.SchedulerConnection;
import org.ow2.proactive.scheduler.common.SchedulerConstants;
import org.ow2.proactive.scheduler.core.properties.PASchedulerProperties;
import org.ow2.proactive.scheduler.util.SchedulerStarter;


public class NagiosFuncTHelper {

    private static URL testConfigFilename = NagiosFuncTHelper.class.getResource("config/test.properties");
    
    private static URL serverJavaPolicy = NagiosFuncTHelper.class
            .getResource("config/server-java.security.policy");

    private static URL rmHibernateConfig = NagiosFuncTHelper.class.getResource("config/rmHibernateConfig.xml");

    private static URL schedHibernateConfig = NagiosFuncTHelper.class
            .getResource("config/schedHibernateConfig.xml");

    private static URL defaultPAConfigFile = NagiosFuncTHelper.class.getResource("config/defaultPAConfig.xml");

    private static URL rmLog4jConfig = NagiosFuncTHelper.class.getResource("config/rmLog4JConfig.properties");

    private static URL schedLog4JConfig = NagiosFuncTHelper.class
            .getResource("config/schedLog4JConfig.properties");

    private static URL forkedTaskLog4JConfig = NagiosFuncTHelper.class
            .getResource("config/forkedTaskLog4JConfig.properties");

    private static final String[] requiredJARs = { "script-js.jar", "gson-2.1.jar", "jruby-engine.jar",
            "jython-engine.jar", "commons-logging-1.1.1.jar", "ProActive_Scheduler-core.jar",
            "ProActive_SRM-common.jar", "ProActive_ResourceManager.jar", "ProActive_Scheduler-worker.jar",
            "ProActive_Scheduler-mapreduce.jar", "commons-httpclient-3.1.jar", "commons-codec-1.3.jar",
            "ProActive.jar" };

    private static final String defaultNodeSourceName = "_DEFAULT_NODE_SOURCE_";

    static final int defaultNumberOfNodes = 1;

    private static final long defaultNodeTimeout = 20 * 1000;

    private static Process rmProcess;

    private static Process schedProcess;

    private static Scheduler scheduler;
    
    private static ResourceManager rm;

    private static PublicKey schedulerPublicKey;


    private NagiosFuncTHelper() {
    }

    public static String startResourceManager(Map<String, String> rmNodeJvmArgs) throws Exception {
        List<String> commandList = new ArrayList<String>();
        String javaPath = NagiosFuncTUtils.getJavaPathFromSystemProperties();
        commandList.add(javaPath);
        commandList.add("-Djava.security.manager");
        
        commandList.add("-Djava.security.policy");
        commandList.add(CentralPAPropertyRepository.JAVA_SECURITY_POLICY.getCmdLine() +
            getServerSecurityPolicyPathname());
        
        String dropDB = System.getProperty("rm.deploy.dropDB", "true");
        commandList.add(PAResourceManagerProperties.RM_DB_HIBERNATE_DROPDB.getCmdLine() + dropDB);
        commandList.add(PAResourceManagerProperties.RM_DB_HIBERNATE_CONFIG.getCmdLine() +
            getRmHibernateConfigPathname());
        commandList.add(CentralPAPropertyRepository.PA_HOME.getCmdLine() + getRmHome());
        commandList.add(PAResourceManagerProperties.RM_HOME.getCmdLine() + getRmHome());
        commandList.add(CentralPAPropertyRepository.PA_CONFIGURATION_FILE.getCmdLine() +
            getDefaultPAConfigPathname());
        commandList
                .add(CentralPAPropertyRepository.LOG4J.getCmdLine() + "file:" + getRmLog4JConfigPathname());
        commandList.add("-cp");
        commandList.add(getClassPath());
        commandList.add(RMStarter.class.getName());
        
        System.out.println("Starting RM...");
        System.out.println("(RM START COMMAND: '");
        for (String c: commandList){
            System.out.print(c + " ");
        }
        System.out.println("')");
        System.out.println("Done.");
        
        ProcessBuilder processBuilder = new ProcessBuilder(commandList);
        processBuilder.redirectErrorStream(true);
        rmProcess = processBuilder.start();

        ProcessStreamReader out = new ProcessStreamReader("rm-process-output: ", rmProcess.getInputStream(),
            System.out);
        out.start();

        String port = CentralPAPropertyRepository.PA_RMI_PORT.getValueAsString();
        String url = "rmi://localhost:" + port + "/";

        System.out.println("Connecting to the RM at '" + url + "' to create a Node Source...");
        RMAuthentication rmAuth = RMConnection.waitAndJoin(url, TimeUnit.SECONDS.toMillis(120));
        System.out.println("Done.");

        Credentials rmCredentials = getRmCredentials();
        
        System.out.println("Logging to RM...");
        rm = rmAuth.login(rmCredentials);
        System.out.println("Done.");

        RMEventMonitor rmEventMonitor = new RMEventMonitor();
        RMEventListener eventListener = RMEventListener.createEventListener(rmEventMonitor);
        RMInitialState state = rm.getMonitoring().addRMEventListener(eventListener);
        PAFuture.waitFor(state);
        state.getNodeSource().size();
        
        System.out.println("Creating node source...");
        createNodeSource(rm, rmCredentials, rmEventMonitor, rmNodeJvmArgs);
        System.out.println("Done.");
        
        return url;
    }

    public static void createNodeSource(ResourceManager rm, Credentials rmCred, RMEventMonitor rmEventMonitor, Map<String, String> jvmArgs)
            throws Exception {
        String nodeSourceName = defaultNodeSourceName + System.currentTimeMillis();

        RMEventMonitor.RMNodesDeployedWaitCondition waitCondition = new RMEventMonitor.RMNodesDeployedWaitCondition(
            nodeSourceName, defaultNumberOfNodes);
        rmEventMonitor.addWaitCondition(waitCondition);
        
        Object[] infrastructureParams = new Object[] { "", rmCred.getBase64(), defaultNumberOfNodes,
                defaultNodeTimeout, NagiosFuncTUtils.buildJvmParameters(jvmArgs) };
        BooleanWrapper nodeSourceCreated = rm
                .createNodeSource(nodeSourceName, LocalInfrastructure.class.getName(), infrastructureParams,
                        StaticPolicy.class.getName(), null);
        if (!nodeSourceCreated.getBooleanValue()) {
            stopRm();
            throw new RuntimeException("Unable to create node sources.");
        }
        // wait until RM Nodes are deployed
        rmEventMonitor.waitFor(waitCondition, TimeUnit.SECONDS.toMillis(20));
    }

    public static String startScheduler(String rmUrl) throws Exception {
        List<String> commandList = new ArrayList<String>();
        String javaPath = NagiosFuncTUtils.getJavaPathFromSystemProperties();
        commandList.add(javaPath);
        commandList.add("-Djava.security.manager");
        commandList.add("-Djava.security.policy");
        commandList.add(CentralPAPropertyRepository.JAVA_SECURITY_POLICY.getCmdLine() +
            getServerSecurityPolicyPathname());
        String dropDB = System.getProperty("scheduler.deploy.dropDB", "true");
        commandList.add(PASchedulerProperties.SCHEDULER_DB_HIBERNATE_DROPDB.getCmdLine() + dropDB);
        commandList.add(PASchedulerProperties.SCHEDULER_DB_HIBERNATE_CONFIG.getCmdLine() +
            getShedHibernateConfigPathname());
        commandList.add(CentralPAPropertyRepository.PA_HOME.getCmdLine() + getSchedHome());
        commandList.add(PASchedulerProperties.SCHEDULER_HOME.getCmdLine() + getSchedHome());
        commandList.add(CentralPAPropertyRepository.PA_CONFIGURATION_FILE.getCmdLine() +
            getDefaultPAConfigPathname());
        commandList.add(CentralPAPropertyRepository.LOG4J.getCmdLine() + "file:" +
            getSchedLog4JConfigPathname());
        commandList.add(PASchedulerProperties.SCHEDULER_DEFAULT_FJT_LOG4J.getCmdLine() + "file:" +
            getForkedTaskLog4JConfigPathname());
        commandList.add(PASchedulerProperties.RESOURCE_MANAGER_CREDS.getCmdLine() +
            "config/authentication/rm.cred");
        commandList.add("-cp");
        commandList.add(getClassPath());
        commandList.add(SchedulerStarter.class.getName());
        commandList.add("-u");
        commandList.add(rmUrl);

        ProcessBuilder processBuilder = new ProcessBuilder(commandList);
        processBuilder.redirectErrorStream(true);
        schedProcess = processBuilder.start();

        ProcessStreamReader out = new ProcessStreamReader("scheduler-process-output: ",
            schedProcess.getInputStream(), System.out);
        out.start();

        String port = CentralPAPropertyRepository.PA_RMI_PORT.getValueAsString();
        String url = "rmi://localhost:" + port + "/";
        SchedulerAuthenticationInterface schedAuth = SchedulerConnection.waitAndJoin(url,
                TimeUnit.SECONDS.toMillis(60));
        schedulerPublicKey = schedAuth.getPublicKey();
        Credentials schedCred = NagiosFuncTUtils.createCredentials("admin", "admin", schedulerPublicKey);
        scheduler = schedAuth.login(schedCred);

        return url;
    }

    public static Scheduler getScheduler() {
        return scheduler;
    }

    public static ResourceManager getResourceManager() {
        return rm;
    }
    
    public static PublicKey getSchedulerPublicKey() {
        return schedulerPublicKey;
    }

    public static void stopRm() {
        if (rmProcess != null) {
            System.out.println("Shutting down resource manager process.");
            try {
                NagiosFuncTUtils.destory(rmProcess);
            } catch (Throwable error) {
                System.err.println("An error occurred while shutting down resource manager process:");
                error.printStackTrace();
            } finally {
                try {
                    NagiosFuncTUtils.cleanupRMActiveObjectRegistry();
                } catch (Throwable error) {
                    System.err.println("An error occurred while cleaning up:");
                    error.printStackTrace();
                }
            }
        }
    }

    public static void stopScheduler() {
        System.out.println("Shutting down scheduler process.");
        if (schedProcess != null) {
            try {
                NagiosFuncTUtils.destory(schedProcess);
            } catch (Throwable error) {
                System.err.println("An error occurred while shutting down scheduler process:");
                error.printStackTrace();
            } finally {
                try {
                    NagiosFuncTUtils.cleanupActiveObjectRegistry(SchedulerConstants.SCHEDULER_DEFAULT_NAME);
                } catch (Throwable error) {

                }
            }
        }
    }

    public static String getString(InputStream is) {
        return new Scanner(is).useDelimiter("\\A").next();
    }

    private static String getServerSecurityPolicyPathname() throws Exception {
        return (new File(serverJavaPolicy.toURI())).getAbsolutePath();
    }

    private static String getRmHibernateConfigPathname() throws Exception {
        return (new File(rmHibernateConfig.toURI())).getAbsolutePath();
    }

    private static String getShedHibernateConfigPathname() throws Exception {
        return (new File(schedHibernateConfig.toURI()).getAbsolutePath());
    }

    private static String getDefaultPAConfigPathname() throws Exception {
        return (new File(defaultPAConfigFile.toURI())).getAbsolutePath();
    }

    private static String getRmLog4JConfigPathname() throws Exception {
        return (new File(rmLog4jConfig.toURI())).getAbsolutePath();
    }

    private static String getSchedLog4JConfigPathname() throws Exception {
        return (new File(schedLog4JConfig.toURI())).getAbsolutePath();
    }

    private static String getForkedTaskLog4JConfigPathname() throws Exception {
        return (new File(forkedTaskLog4JConfig.toURI())).getAbsolutePath();
    }

    private static String getRmHome() throws Exception {
        return NagiosFuncTConfig.getInstance().getProperty(NagiosFuncTConfig.TEST_SCHEDULER_HOME);
    }

    private static String getSchedHome() throws Exception {
        return NagiosFuncTConfig.getInstance().getProperty(NagiosFuncTConfig.TEST_SCHEDULER_HOME);
    }

    public static Credentials getRmCredentials() throws Exception {
        File rmCredentails = new File(getRmHome(), "config/authentication/rm.cred");
        return Credentials.getCredentials(new FileInputStream(rmCredentails));
    }
    
    
    public static String getTestConfigPathname() throws Exception {
        return (new File(testConfigFilename.toURI())).getAbsolutePath();
    }

    private static String getClassPath() throws Exception {
        StringBuilder classpath = new StringBuilder();
        String libPath = getRmHome() + File.separator + "dist" + File.separator + "lib";
        for (String jar : requiredJARs) {
            classpath.append(libPath).append(File.separator).append(jar).append(File.pathSeparatorChar);
        }

        return classpath.toString();
    }

}
