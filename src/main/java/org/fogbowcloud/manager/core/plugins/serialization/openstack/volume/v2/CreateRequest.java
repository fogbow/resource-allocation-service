package org.fogbowcloud.manager.core.plugins.serialization.openstack.volume.v2;

import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;
import org.fogbowcloud.manager.core.plugins.serialization.JsonSerializable;

import com.google.gson.annotations.SerializedName;

/**
 * Documentation: https://developer.openstack.org/api-ref/block-storage/v2/
 */
public class CreateRequest implements JsonSerializable {

	@SerializedName(OpenstackApiConstants.Volume.VOLUME_KEY_JSON)
	private VolumeParameters volumeParameters;
	
	public CreateRequest() {}
	
	private CreateRequest(VolumeParameters volume) {
		this.volumeParameters = volume;
	}
	
	public class VolumeParameters {
		
		@SerializedName(OpenstackApiConstants.Volume.NAME_KEY_JSON)
		private final String name;
		@SerializedName(OpenstackApiConstants.Volume.SIZE_KEY_JSON)
		private final String size;

		public VolumeParameters(Builder builder) {
			super();
			this.name = builder.name;
			this.size = builder.size;
		}
		
	}
	
	public class Builder {
		
		private String name;
		private String size;
		
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder size(String size) {
            this.size = size;
            return this;
        }        
        
        public CreateRequest build() {
            VolumeParameters volumeParameters = new VolumeParameters(this);
			return new CreateRequest(volumeParameters);
        }        
		
	}
	
	@Override
	public String toJson() {
		return GsonHolder.getInstance().toJson(this);
	}

}
