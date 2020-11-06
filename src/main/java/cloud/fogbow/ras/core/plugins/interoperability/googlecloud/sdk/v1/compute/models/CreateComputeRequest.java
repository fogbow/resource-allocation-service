package cloud.fogbow.ras.core.plugins.interoperability.googlecloud.sdk.v1.compute.models;

import cloud.fogbow.common.constants.GoogleCloudConstants;
import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.common.util.JsonSerializable;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/*
* Documentation reference: https://cloud.google.com/compute/docs/reference/rest/v1/instances/insert
* Request example:
*
* {
*   "name":"new-virtual-machine",
*   "machineType":"zones/us-central1-f/machineTypes/custom-2-2048",
*   "disks":[
*       {
*           "initializeParams": {
*               "sourceImage": "projects/debian-cloud/global/images/1938182592388592611",
*               "diskSizeGb": 100
*           },
*           "boot": true
*       }
*   ],
*   "networkInterfaces":[
*       {
*           "network":"projects/us-central1-f/global/networks/default"
*        }
*   ],
*   "metadata":{
*       "items": [
*           {
*               "key":"ssh-keys",
*               "value":"ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABgQCnzMbrs3PKigPAF0tpIqJqlEWx0dsZ9TV5VnuBRNPUmD64rzjnSGYmSGTwPggE0uPGFw9p4xEKRN8srJxhRCA5VaraBljTZXlqe43WeP61Iz2+P4r69rqzzqxvY316VBDECz47HvL0KGrAsHHbcAgnuT/nJBpsgB8h3OW+fCo2YJ90KjgY/gULTELukBmDInIeIRz9rGR09YU/PEHhqTIdvMX9PfP9u6ol6vpny7w7z4IkplC0drsRpS+2gPDP7Unw8Q0OFhtojPA0GHy+BNEh4o7ZG42DpAOgYmc2m/0tKuiwVH+hr2OfxT1vrFa0SP7WE9FrVsVHWf62ZWReMt5M+MeD5cYO1wkZiCKYrgBf5Ij8Oie0QNe0PXHlt+3F2l2xiKbQ6AJtPiBqrG1FqTwrdcbjqMdwmuyw1yzbZoeGDc/ZqQwrg6cZikx9Al9xs+NE+xO7PDvC/Nc0zkXbZAg6qOiMkEntZ0/BUZR5pgiNE/IVrfHwoiGn4i5JZ/O3B1U= user123"
*           },
*           {
*               "key":"startup-script",
*               "value":"#! /bin/bash\n\n# Installs apache and a custom homepage\napt update\napt -y install apache2\ncat <<EOF > /var/www/html/index.html\n<html><body><h1>Hello World</h1>\n<p>This page was created from a start up script.</p>\n</body></html>"
*           }
*       ]
*   }
* }
*/

public class CreateComputeRequest implements JsonSerializable {

    @SerializedName(GoogleCloudConstants.Compute.NAME_KEY_JSON)
    private String name;
    @SerializedName(GoogleCloudConstants.Compute.FLAVOR_KEY_JSON)
    private String flavorId;
    @SerializedName(GoogleCloudConstants.Compute.DISKS_KEY_JSON)
    private List<Disk> disks;
    @SerializedName(GoogleCloudConstants.Compute.NETWORKS_KEY_JSON)
    private List<Network> networks;
    @SerializedName(GoogleCloudConstants.Compute.METADATA_KEY_JSON)
    private MetaData metaData;

    public CreateComputeRequest(Builder builder) {
        this.name = builder.name;
        this.flavorId = builder.flavorId;
        this.disks = builder.disks;
        this.networks = builder.networks;
        this.metaData = builder.metaData;
    }

    @Override
    public String toJson() {
        return GsonHolder.getInstance().toJson(this);
    }

    public static class Disk {
        @SerializedName(GoogleCloudConstants.Compute.Disk.INITIAL_PARAMS_KEY_JSON)
        private InicialeParams inicialeParams;
        @SerializedName(GoogleCloudConstants.Compute.Disk.BOOT_KEY_JSON)
        private boolean boot;
        @SerializedName(GoogleCloudConstants.Compute.Disk.AUTO_DELETE_KEY_JSON)
        private boolean autoDelete;

        public Disk(boolean boot, boolean autoDelete, InicialeParams inicialeParams) {
            this.boot = boot;
            this.autoDelete = autoDelete;
            this.inicialeParams = inicialeParams;
        }
    }

    public static class InicialeParams {
        @SerializedName(GoogleCloudConstants.Compute.Disk.InitializeParams.IMAGE_KEY_JSON)
        private String sourceImageId;
        @SerializedName(GoogleCloudConstants.Compute.Disk.InitializeParams.DISK_SIZE_KEY_JSON)
        private int diskSizeGb;

        public InicialeParams(String sourceImageId, int diskSizeGb) {
            this.sourceImageId = sourceImageId;
            this.diskSizeGb = diskSizeGb;
        }
    }

    public static class MetaData {
        @SerializedName(GoogleCloudConstants.Compute.Metadata.ITEMS_KEY_JSON)
        private List<Item> items;

        public MetaData(List<Item> items) {
            this.items = items;
        }
    }

    public static class Item {
        @SerializedName(GoogleCloudConstants.Compute.Metadata.KEY_ITEM_KEY_JSON)
        private String key;
        @SerializedName(GoogleCloudConstants.Compute.Metadata.VALUE_ITEM_KEY_JSON)
        private String value;

        public Item(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    public static class Network {
        @SerializedName(GoogleCloudConstants.Network.NETWORK_KEY_JSON)
        private String network;

        public Network(String network) {
            this.network = network;
        }
    }

    public static class Builder {
        private String name;
        private String flavorId;
        private List<Disk> disks;
        private MetaData metaData;
        private List<Network> networks;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder flavorId(String flavorId) {
            this.flavorId = flavorId;
            return this;
        }

        public Builder disks(List<Disk> disks) {
            this.disks = disks;
            return this;
        }

        public Builder networks(List<Network> networks) {
            this.networks = networks;
            return this;
        }

        public Builder metaData(MetaData metaData) {
            this.metaData = metaData;
            return this;
        }

        public CreateComputeRequest build() {
            return new CreateComputeRequest(this);
        }
    }
}
