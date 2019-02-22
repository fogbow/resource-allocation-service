package cloud.fogbow.ras.api.http.response;

import cloud.fogbow.ras.api.http.request.Cloud;

import java.util.List;

public class CloudList {
    private List<String> clouds;

    private CloudList() {}

    public CloudList(List<String> clouds) {
        this.clouds = clouds;
    }

    public List<String> getClouds() {
        return clouds;
    }

    public void setClouds(List<String> clouds) {
        this.clouds = clouds;
    }
}
