package org.fogbowcloud.manager.core.plugins.serialization.openstack.volume.v2;

import org.fogbowcloud.manager.core.plugins.serialization.GsonHolder;

import com.google.gson.annotations.SerializedName;

/**
 * Documentation: https://developer.openstack.org/api-ref/block-storage/v2/
 */
public class VolumeResponse {

	@SerializedName(OpenstackApiConstants.Volume.VOLUME_KEY_JSON)
	private VolumeParameters volumeParameters;
	
	public VolumeResponse() {}
	
	private VolumeResponse(VolumeParameters volume) {
		this.volumeParameters = volume;
	}	
	
	public class VolumeParameters {
		
		@SerializedName(OpenstackApiConstants.Volume.ID_KEY_JSON)
		private final String id;
		@SerializedName(OpenstackApiConstants.Volume.NAME_KEY_JSON)
		private final String name;
		@SerializedName(OpenstackApiConstants.Volume.SIZE_KEY_JSON)
		private final Integer size;
		@SerializedName(OpenstackApiConstants.Volume.STATUS_KEY_JSON)
		private final String status;		

		public VolumeParameters(Builder builder) {
			super();
			this.id = builder.id;
			this.name = builder.name;
			this.size = builder.size;
			this.status = builder.status;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Integer getSize() {
			return size;
		}

		public String getStatus() {
			return status;
		}

	}
		
	public class Builder {
		
		private String id;
		private String name;
		private Integer size;
		private String status;
		
		public Builder id(String id) {
            this.id = id;
            return this;
        }
		
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        public Builder size(Integer size) {
            this.size = size;
            return this;
        }        
        
        public VolumeResponse build() {
            VolumeParameters volumeParameters = new VolumeParameters(this);
			return new VolumeResponse(volumeParameters);
        }        
		
	}
	
	public VolumeParameters getVolumeParameters() {
		return this.volumeParameters;
	}
	
	public VolumeResponse fromJson(String jsonStr) {
		return GsonHolder.getInstance().fromJson(jsonStr, VolumeResponse.class);
	}
	
}
