package qosprober.main;


import java.io.IOException;
import java.net.ServerSocket;

import org.objectweb.proactive.core.config.CentralPAPropertyRepository;
import org.objectweb.proactive.core.xml.VariableContractImpl;
import org.objectweb.proactive.core.xml.VariableContractType;
import org.objectweb.proactive.extensions.pamr.PAMRConfig;
import org.objectweb.proactive.extensions.pamr.remoteobject.PAMRRemoteObjectFactory;
import org.objectweb.proactive.extensions.pamr.router.Router;
import org.objectweb.proactive.extensions.pamr.router.RouterConfig;
import org.objectweb.proactive.utils.OperatingSystem;


/**
 * ProActive configuration for tests
 *
 * @author ProActive team
 * @since  ProActive 5.2.0
 */
public class ProActiveSetup {
    /** Name of the variable used in gcmd to set the os type. */
    static final private String VAR_OS = "os";
    /** Name of the variable used in gcma to set the java parameters */
    static final private String VAR_JVM_PARAMETERS = "JVM_PARAMETERS";

    /** The parameters to pass to the forked JVMs */
    final private String jvmParameters;
    /** The variable contract to pass to GCMA. */
    final private VariableContractImpl vc;
    final private PAMRSetup pamrSetup;

    public ProActiveSetup() {
        this.pamrSetup = new PAMRSetup();
        this.jvmParameters = buildJvmParameters();
        this.vc = buildVariableContract(this.jvmParameters);
    }

    /**
     * Start everything required to run the tests
     *
     * @throws Exception
     *   If something goes wrong
     */
    final public void start() throws Exception {
        this.pamrSetup.start();
    }

    final public VariableContractImpl getVariableContract() {
        return this.vc;
    }

    final public String getJvmParameters() {
        return this.jvmParameters;
    }

    /**
     * Stop everything that had been started by {@link ProActiveSetup#start}
     *
     * @throws Exception
     *   If something goes wrong
     */
    final public void shutdown() {
        this.pamrSetup.stop();
    }

    private VariableContractImpl buildVariableContract(String jvmParameters) {
        VariableContractImpl vContract;
        vContract = new VariableContractImpl();
        vContract.setVariableFromProgram(VAR_OS, OperatingSystem.getOperatingSystem().name(),
                VariableContractType.DescriptorDefaultVariable);
        vContract.setVariableFromProgram(VAR_JVM_PARAMETERS, jvmParameters,
                VariableContractType.ProgramVariable);
        return vContract;
    }

    private String buildJvmParameters() {
        StringBuilder jvmParameters = new StringBuilder(" ");

        jvmParameters.append(CentralPAPropertyRepository.PA_TEST.getCmdLine());
        jvmParameters.append("true ");

        jvmParameters.append(" -Dproactive.test=true ");

        jvmParameters.append(CentralPAPropertyRepository.PA_COMMUNICATION_PROTOCOL.getCmdLine());
        jvmParameters.append(CentralPAPropertyRepository.PA_COMMUNICATION_PROTOCOL.getValue());
        jvmParameters.append(" ");

        if (PAMRRemoteObjectFactory.PROTOCOL_ID.equals(CentralPAPropertyRepository.PA_COMMUNICATION_PROTOCOL
                .getValue())) {
            jvmParameters.append(PAMRConfig.PA_NET_ROUTER_ADDRESS.getCmdLine());
            jvmParameters.append(this.pamrSetup.address);
            jvmParameters.append(" ");

            jvmParameters.append(PAMRConfig.PA_NET_ROUTER_PORT.getCmdLine());
            jvmParameters.append(this.pamrSetup.port);
            jvmParameters.append(" ");
        }

        return jvmParameters.toString();
    }

    static private class PAMRSetup {
        /** PAMR router when started or null. */
        volatile private Router router;
        /** Reversed port for PAMR router. */
        final int port;
        /** Reserve a port for the router if needed */
        volatile private ServerSocket reservedPort;
        /** Address of the PAMR router. */
        final String address;

        public PAMRSetup() {
            // Get router address
            if (PAMRConfig.PA_NET_ROUTER_ADDRESS.isSet()) {
                address = PAMRConfig.PA_NET_ROUTER_ADDRESS.getValue();
            } else {
                address = "localhost";
            }

            // Get router port (reserve a dynamic port if needed)
            if (PAMRConfig.PA_NET_ROUTER_PORT.isSet() && PAMRConfig.PA_NET_ROUTER_PORT.getValue() != 0) {
                port = PAMRConfig.PA_NET_ROUTER_PORT.getValue();
            } else {
                ServerSocket ss = null;
                try {
                    ss = new ServerSocket(0);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                reservedPort = ss;
                port = reservedPort.getLocalPort();
            }
            PAMRConfig.PA_NET_ROUTER_PORT.setValue(this.port);

        }

        synchronized public void start() throws Exception {
            if (this.router != null) {
                return;
            }

            if (this.reservedPort != null) {
                this.reservedPort.close();
            }

            if (PAMRRemoteObjectFactory.PROTOCOL_ID
                    .equals(CentralPAPropertyRepository.PA_COMMUNICATION_PROTOCOL.getValue())) {
                RouterConfig config = new RouterConfig();
                config.setPort(this.port);
                router = Router.createAndStart(config);
            }
        }

        public void stop() {
            if (this.router != null) {
                this.router.stop();
            }
        }
    }
}
