package org.fogbowcloud.manager.util.connectivity;

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import net.schmizz.sshj.connection.channel.direct.Session.Command;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.PropertiesHolder;
import org.fogbowcloud.manager.core.constants.ConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;

public class SshConnectivityUtil {

    private static final int SUCCESSFUL_COMMAND_STATUS = 0;

    private static final String MESSAGE_ECHO_SEND = "echo HelloWorld";

    private static final Logger LOGGER = Logger.getLogger(SshConnectivityUtil.class);

    private SshClientPoolUtil sshClientPool = new SshClientPoolUtil();

    private static SshConnectivityUtil instance;

    private SshConnectivityUtil() {}

    public static SshConnectivityUtil getInstance() {
        if (instance == null) {
            instance = new SshConnectivityUtil();
        }
        return instance;
    }

    public boolean checkSSHConnectivity(SshTunnelConnectionData sshTunnelConnectionData) {
        String sshPublicAddress = sshTunnelConnectionData.getSshPublicAddress();
        if (sshTunnelConnectionData == null || sshPublicAddress == null) {
            return false;
        }
        try {
            Command sshOutput = execOnInstance(sshPublicAddress, MESSAGE_ECHO_SEND);
            if (sshOutput.getExitStatus() == SUCCESSFUL_COMMAND_STATUS) {
                return true;
            }
        } catch (Exception e) {
            LOGGER.error("Failure while checking connectivity.", e);
        }
        return false;
    }

    private Command execOnInstance(String sshPublicAddress, String cmd) throws Exception {
        SSHClient sshClient = sshClientPool.getClient(sshPublicAddress, SshCommonUserUtil.getSshCommonUser(),
                        getManagerSSHPrivateKey());
        Session session = sshClient.startSession();
        Command command = session.exec(cmd);
        command.join();
        return command;
    }

    private String getManagerSSHPrivateKey() throws FatalErrorException {
        String privateKey = PropertiesHolder.getInstance().
                getProperty(ConfigurationConstants.MANAGER_SSH_PRIVATE_KEY_FILE_PATH);
        if (privateKey == null || privateKey.isEmpty()) {
            return null;
        }
        return privateKey;
    }
}
