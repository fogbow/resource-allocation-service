package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.attachment.model.AttachVolumeRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.attachment.model.AttachmentJobStatusRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.attachment.model.DetachVolumeRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.compute.model.DeployVirtualMachineRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.compute.model.DestroyVirtualMachineRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.compute.model.GetVirtualMachineRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.image.model.GetAllImagesRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.network.model.CreateNetworkRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.network.model.DeleteNetworkRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.network.model.GetNetworkRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.publicip.model.AssociateIpAddressRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.publicip.model.CreateFirewallRuleRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.publicip.model.DisassociateIpAddressRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.publicip.model.EnableStaticNatRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.securityrule.model.DeleteFirewallRuleRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.volume.model.DeleteVolumeRequest;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.sdk.v4_9.volume.model.GetVolumeRequest;
import org.mockito.ArgumentMatcher;

public class RequestMatcher<T extends CloudStackRequest> extends ArgumentMatcher<T> {

    private CloudStackRequest internalRequest;

    private RequestMatcher(CloudStackRequest cloudStackRequest) {
        this.internalRequest = cloudStackRequest;
    }

    @Override
    public boolean matches(Object externalObj)  {
        try {
            CloudStackRequest externalRequest = (CloudStackRequest) externalObj;
            String internalURL = this.internalRequest.getUriBuilder().toString();
            String externalURL = externalRequest.getUriBuilder().toString();

            return internalURL.equals(externalURL);
        } catch (Exception e) {
            return false;
        }
    }

    public static class DeployVirtualMachine extends RequestMatcher<DeployVirtualMachineRequest> {
        public DeployVirtualMachine(CloudStackRequest cloudStackRequest) {
            super(cloudStackRequest);
        }
    }

    public static class GetVirtualMachine extends RequestMatcher<GetVirtualMachineRequest> {
        public GetVirtualMachine(CloudStackRequest cloudStackRequest) {
            super(cloudStackRequest);
        }
    }

    public static class DestroyVirtualMachine extends RequestMatcher<DestroyVirtualMachineRequest> {
        public DestroyVirtualMachine(CloudStackRequest cloudStackRequest) {
            super(cloudStackRequest);
        }
    }

    public static class CreateNetwork extends RequestMatcher<CreateNetworkRequest> {
        public CreateNetwork(CloudStackRequest cloudStackRequest) {
            super(cloudStackRequest);
        }
    }

    public static class DeleteNetwork extends RequestMatcher<DeleteNetworkRequest> {
        public DeleteNetwork(CloudStackRequest cloudStackRequest) {
            super(cloudStackRequest);
        }
    }

    public static class GetNetwork extends RequestMatcher<GetNetworkRequest> {
        public GetNetwork(CloudStackRequest cloudStackRequest) {
            super(cloudStackRequest);
        }
    }

    public static class AttachVolume extends RequestMatcher<AttachVolumeRequest> {
        public AttachVolume(CloudStackRequest cloudStackRequest) {
            super(cloudStackRequest);
        }
    }

    public static class DetachVolume extends RequestMatcher<DetachVolumeRequest> {
        public DetachVolume(CloudStackRequest cloudStackRequest) {
            super(cloudStackRequest);
        }
    }

    public static class AttachmentJobStatus extends RequestMatcher<AttachmentJobStatusRequest> {
        public AttachmentJobStatus(CloudStackRequest cloudStackRequest) {
            super(cloudStackRequest);
        }
    }

    public static class DeleteVolume extends RequestMatcher<DeleteVolumeRequest> {
        public DeleteVolume(CloudStackRequest cloudStackRequest) {
            super(cloudStackRequest);
        }
    }

    public static class CreateFirewallRule extends RequestMatcher<CreateFirewallRuleRequest> {
        public CreateFirewallRule(CloudStackRequest cloudStackRequest) {
            super(cloudStackRequest);
        }
    }

    public static class GetVolume extends RequestMatcher<GetVolumeRequest> {
        public GetVolume(CloudStackRequest cloudStackRequest) {
            super(cloudStackRequest);
        }
    }

    public static class DeleteFirewallRule extends RequestMatcher<DeleteFirewallRuleRequest> {
        public DeleteFirewallRule(CloudStackRequest cloudStackRequest) {
            super(cloudStackRequest);
        }
    }

    public static class GetAllImages extends RequestMatcher<GetAllImagesRequest> {
        public GetAllImages(CloudStackRequest cloudStackRequest) {
            super(cloudStackRequest);
        }
    }

    public static class EnableStaticNat extends RequestMatcher<EnableStaticNatRequest> {
        public EnableStaticNat(CloudStackRequest cloudStackRequest) {
            super(cloudStackRequest);
        }
    }

    public static class DisassociateIpAddress extends RequestMatcher<DisassociateIpAddressRequest> {
        public DisassociateIpAddress(CloudStackRequest cloudStackRequest) {
            super(cloudStackRequest);
        }
    }

    public static class AssociateIpAddress extends RequestMatcher<AssociateIpAddressRequest> {
        public AssociateIpAddress(CloudStackRequest cloudStackRequest) {
            super(cloudStackRequest);
        }
    }

}
