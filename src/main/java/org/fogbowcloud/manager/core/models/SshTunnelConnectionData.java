package org.fogbowcloud.manager.core.models;

public class SshTunnelConnectionData {

    private String sshPublicAddress;
    private String sshUserName;
    private String sshExtraPorts;

    public SshTunnelConnectionData(String sshPublicAddress, String sshUserName,
        String sshExtraPorts) {
        this.sshPublicAddress = sshPublicAddress;
        this.sshUserName = sshUserName;
        this.sshExtraPorts = sshExtraPorts;
    }

    public String getSshPublicAddress() {
        return sshPublicAddress;
    }

    public void setSshPublicAddress(String sshPublicAddress) {
        this.sshPublicAddress = sshPublicAddress;
    }

    public String getSshUserName() {
        return sshUserName;
    }

    public void setSshUserName(String sshUserName) {
        this.sshUserName = sshUserName;
    }

    public String getSshExtraPorts() {
        return sshExtraPorts;
    }

    public void setSshExtraPorts(String sshExtraPorts) {
        this.sshExtraPorts = sshExtraPorts;
    }
}
