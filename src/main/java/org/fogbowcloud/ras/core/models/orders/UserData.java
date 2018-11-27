package org.fogbowcloud.ras.core.models.orders;

import org.fogbowcloud.ras.core.plugins.interoperability.util.CloudInitUserDataBuilder;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class UserData {
    public static final int MAX_EXTRA_USER_DATA_FILE_CONTENT = 4096;

    @Column(length = MAX_EXTRA_USER_DATA_FILE_CONTENT)
    private String extraUserDataFileContent;

    private CloudInitUserDataBuilder.FileType extraUserDataFileType;

    private String tag;

    public UserData() {
    }

    public UserData(String extraUserDataFile, CloudInitUserDataBuilder.FileType extraUserDataFileType) {
        this(extraUserDataFile, extraUserDataFileType, "");
    }

    public UserData(String extraUserDataFile, CloudInitUserDataBuilder.FileType extraUserDataFileType, String tag) {
        this.extraUserDataFileContent = extraUserDataFile;
        this.extraUserDataFileType = extraUserDataFileType;
        this.tag = tag;
    }

    public String getExtraUserDataFileContent() {
        return extraUserDataFileContent;
    }

    public void setExtraUserDataFileContent(String extraUserDataFileContent) {
        this.extraUserDataFileContent = extraUserDataFileContent;
    }

    public CloudInitUserDataBuilder.FileType getExtraUserDataFileType() {
        return extraUserDataFileType;
    }

    public String getTag() {
        return tag;
    }

}
