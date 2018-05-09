package org.fogbowcloud.manager.core.models.orders;

public class UserData {

	private String extraUserDataFile;
    
    private String extraUserDataFileType;

    public UserData(String extraUserDataFile, String extraUserDataFileType) {
        this.extraUserDataFile = extraUserDataFile;
        this.extraUserDataFileType = extraUserDataFileType;
    }

    public String getExtraUserDataFile() {
		return extraUserDataFile;
	}

    public String getExtraUserDataFileType() {
		return extraUserDataFileType;
	}
    
}
