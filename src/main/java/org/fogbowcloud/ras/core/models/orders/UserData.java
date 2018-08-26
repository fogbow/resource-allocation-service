package org.fogbowcloud.ras.core.models.orders;

import org.fogbowcloud.ras.core.plugins.interoperability.util.CloudInitUserDataBuilder;

import javax.persistence.Embeddable;

@Embeddable
public class UserData {
    private String extraUserDataFileContent;

    private CloudInitUserDataBuilder.FileType extraUserDataFileType;

    public UserData() {
    }

    public UserData(String extraUserDataFile, CloudInitUserDataBuilder.FileType extraUserDataFileType) {
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
