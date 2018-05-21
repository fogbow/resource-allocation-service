package org.fogbowcloud.manager.core.models.orders;

import org.fogbowcloud.manager.core.manager.plugins.compute.util.CloudInitUserDataBuilder;

public class UserData {

    private String extraUserDataFileContent;

    private CloudInitUserDataBuilder.FileType extraUserDataFileType;

    public UserData() {}

    public UserData(
            String extraUserDataFile, CloudInitUserDataBuilder.FileType extraUserDataFileType) {
        this.extraUserDataFileContent = extraUserDataFile;
        this.extraUserDataFileType = extraUserDataFileType;
    }

    public String getExtraUserDataFileContent() {
        return extraUserDataFileContent;
    }

    public CloudInitUserDataBuilder.FileType getExtraUserDataFileType() {
        return extraUserDataFileType;
    }
}
