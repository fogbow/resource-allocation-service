package org.fogbowcloud.manager.core.models.orders;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import org.fogbowcloud.manager.core.plugins.cloud.util.CloudInitUserDataBuilder;

@Entity
public class UserData implements Serializable{
	
	private static final long serialVersionUID = 1L;
	
	@Id
	@GeneratedValue(strategy=GenerationType.IDENTITY)
	@Column
	private long id;
	
	@Column
    private String extraUserDataFileContent;
	
	@Column
	@Enumerated(EnumType.STRING)
    private CloudInitUserDataBuilder.FileType extraUserDataFileType;

    public UserData() {}

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
