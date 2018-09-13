package org.fogbowcloud.ras.core.models.orders;

import org.fogbowcloud.ras.core.plugins.interoperability.util.CloudInitUserDataBuilder;

import javax.persistence.Embeddable;
import javax.persistence.Column;

@Embeddable
public class UserData {
    @Column(length=2048)
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
