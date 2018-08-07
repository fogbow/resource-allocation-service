package org.fogbowcloud.manager.core.plugins.cloud.openstack;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeSet;
import java.util.UUID;

import org.apache.http.client.HttpResponseException;
import org.apache.log4j.Logger;
import org.fogbowcloud.manager.core.HomeDir;
import org.fogbowcloud.manager.core.constants.DefaultConfigurationConstants;
import org.fogbowcloud.manager.core.exceptions.FatalErrorException;
import org.fogbowcloud.manager.core.exceptions.FogbowManagerException;
import org.fogbowcloud.manager.core.exceptions.InvalidParameterException;
import org.fogbowcloud.manager.core.exceptions.NoAvailableResourcesException;
import org.fogbowcloud.manager.core.exceptions.UnavailableProviderException;
import org.fogbowcloud.manager.core.exceptions.UnexpectedException;
import org.fogbowcloud.manager.core.models.HardwareRequirements;
import org.fogbowcloud.manager.core.models.ResourceType;
import org.fogbowcloud.manager.core.models.instances.ComputeInstance;
import org.fogbowcloud.manager.core.models.instances.InstanceState;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.quotas.allocation.ComputeAllocation;
import org.fogbowcloud.manager.core.models.tokens.LocalUserAttributes;
import org.fogbowcloud.manager.core.models.tokens.OpenStackUserAttributes;
import org.fogbowcloud.manager.core.plugins.cloud.ComputePlugin;
import org.fogbowcloud.manager.core.plugins.cloud.util.DefaultLaunchCommandGenerator;
import org.fogbowcloud.manager.core.plugins.cloud.util.LaunchCommandGenerator;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.compute.v2.CreateComputeRequest;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.compute.v2.CreateComputeResponse;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.compute.v2.CreateOsKeypairRequest;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.compute.v2.GetAllFlavorsResponse;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.compute.v2.GetComputeResponse;
import org.fogbowcloud.manager.core.plugins.serialization.openstack.compute.v2.GetFlavorResponse;
import org.fogbowcloud.manager.util.PropertiesUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestClientUtil;
import org.fogbowcloud.manager.util.connectivity.HttpRequestUtil;

public class OpenStackNovaV2ComputePlugin implements ComputePlugin<OpenStackUserAttributes> {

	protected static final String COMPUTE_NOVAV2_URL_KEY = "openstack_nova_v2_url";
	protected static final String DEFAULT_NETWORK_ID_KEY = "default_network_id";

	protected static final String ID_JSON_FIELD = "id";
	protected static final String NAME_JSON_FIELD = "name";
	protected static final String SERVER_JSON_FIELD = "server";
	protected static final String FLAVOR_REF_JSON_FIELD = "flavorRef";
	protected static final String FLAVOR_JSON_FIELD = "flavor";
	protected static final String FLAVOR_ID_JSON_FIELD = "id";
	protected static final String IMAGE_JSON_FIELD = "imageRef";
	protected static final String USER_DATA_JSON_FIELD = "user_data";
	protected static final String NETWORK_JSON_FIELD = "networks";
	protected static final String STATUS_JSON_FIELD = "status";
	protected static final String DISK_JSON_FIELD = "disk";
	protected static final String VCPU_JSON_FIELD = "vcpus";
	protected static final String MEMORY_JSON_FIELD = "ram";
	protected static final String SECURITY_JSON_FIELD = "security_groups";
	protected static final String FLAVOR_JSON_OBJECT = "flavor";
	protected static final String FLAVOR_JSON_KEY = "flavors";
	protected static final String KEY_JSON_FIELD = "key_name";
	protected static final String PUBLIC_KEY_JSON_FIELD = "public_key";
	protected static final String KEYPAIR_JSON_FIELD = "keypair";
	protected static final String UUID_JSON_FIELD = "uuid";
	protected static final String FOGBOW_INSTANCE_NAME = "fogbow-instance-";
	protected static final String TENANT_ID = "tenantId";

	protected static final String SERVERS = "/servers";
	protected static final String SUFFIX_ENDPOINT_KEYPAIRS = "/os-keypairs";
	protected static final String SUFFIX_ENDPOINT_FLAVORS = "/flavors";
	protected static final String COMPUTE_V2_API_ENDPOINT = "/v2/";

	protected static final Logger LOGGER = Logger.getLogger(OpenStackNovaV2ComputePlugin.class);
	protected static final String ADDRESS_FIELD = "addresses";
	protected static final String PROVIDER_NETWORK_FIELD = "provider";
	protected static final String ADDR_FIELD = "addr";

	private TreeSet<HardwareRequirements> hardwareRequirementsList;
	private Properties properties;
	private HttpRequestClientUtil client;
	private LaunchCommandGenerator launchCommandGenerator;

	public OpenStackNovaV2ComputePlugin() throws FatalErrorException {
		HomeDir homeDir = HomeDir.getInstance();
		this.properties = PropertiesUtil.readProperties(homeDir.getPath() + File.separator
				+ DefaultConfigurationConstants.OPENSTACK_CONF_FILE_NAME);
		this.launchCommandGenerator = new DefaultLaunchCommandGenerator();
		instantiateOtherAttributes();
	}

	/** Constructor used for testing only */
	protected OpenStackNovaV2ComputePlugin(Properties properties, 
			LaunchCommandGenerator launchCommandGenerator,
			HttpRequestClientUtil client) {
		LOGGER.debug("Creating OpenStackNovaV2ComputePlugin with properties=" + properties.toString());
		this.properties = properties;
		this.launchCommandGenerator = launchCommandGenerator;
		this.client = client;
		this.hardwareRequirementsList = new TreeSet<HardwareRequirements>();
	}

	public String requestInstance(ComputeOrder computeOrder, OpenStackUserAttributes openStackUserAttributes)
			throws FogbowManagerException, UnexpectedException {
		LOGGER.debug("Requesting instance with tokens=" + openStackUserAttributes);

		HardwareRequirements hardwareRequirements = findSmallestFlavor(computeOrder, openStackUserAttributes);
		String flavorId = hardwareRequirements.getFlavorId();
		String tenantId = getTenantId(openStackUserAttributes);
		List<String> networksId = resolveNetworksId(computeOrder);
		String imageId = computeOrder.getImageId();
		String userData = this.launchCommandGenerator.createLaunchCommand(computeOrder);
		String keyName = getKeyName(tenantId, openStackUserAttributes, computeOrder.getPublicKey());
		String endpoint = getComputeEndpoint(tenantId, SERVERS);
		String instanceId = null;

		try {
			instanceId = doRequestInstance(openStackUserAttributes, flavorId, networksId, imageId, userData, keyName, endpoint);

			synchronized (computeOrder) {
				ComputeAllocation actualAllocation = new ComputeAllocation(
						hardwareRequirements.getCpu(),
						hardwareRequirements.getRam(), 
						1);
				// When the ComputeOrder is remote, this field must be copied into its local counterpart
				// that is updated when the requestingMember receives the reply from the providingMember
				// (see RemoteFacade.java)
				computeOrder.setActualAllocation(actualAllocation);
			}
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		} finally {
			if (keyName != null) {
				deleteKeyName(tenantId, openStackUserAttributes, keyName);
			}
		}
		return instanceId;
	}

	private String doRequestInstance(OpenStackUserAttributes openStackUserAttributes, String flavorId, List<String> networksId, String imageId,
									 String userData, String keyName, String endpoint) throws UnavailableProviderException, HttpResponseException {
		CreateComputeRequest createBody = getRequestBody(imageId, flavorId, userData, keyName, networksId);

		String body = createBody.toJson();
		String response = this.client.doPostRequest(endpoint, openStackUserAttributes, body);
		CreateComputeResponse createComputeResponse = CreateComputeResponse.fromJson(response);

		return createComputeResponse.getId();
	}

	@Override
	public ComputeInstance getInstance(String instanceId, OpenStackUserAttributes openStackUserAttributes)
			throws FogbowManagerException, UnexpectedException {
		LOGGER.info("Getting instance " + instanceId + " with tokens " + openStackUserAttributes);

		String tenantId = getTenantId(openStackUserAttributes);
		String requestEndpoint = getComputeEndpoint(tenantId, SERVERS + "/" + instanceId);

		String jsonResponse = null;
		try {
			jsonResponse = this.client.doGetRequest(requestEndpoint, openStackUserAttributes);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}

		LOGGER.debug("Getting instance from json: " + jsonResponse);

		ComputeInstance computeInstance = instanceFromJson(jsonResponse, openStackUserAttributes);
		return computeInstance;
	}

	@Override
	public void deleteInstance(String instanceId, OpenStackUserAttributes openStackUserAttributes) throws FogbowManagerException, UnexpectedException {
		LOGGER.info("Deleting instance " + instanceId + " with tokens " + openStackUserAttributes);
		String endpoint = getComputeEndpoint(getTenantId(openStackUserAttributes), SERVERS + "/" + instanceId);
		try {
			this.client.doDeleteRequest(endpoint, openStackUserAttributes);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}
	}

	private void instantiateOtherAttributes() {
		this.hardwareRequirementsList = new TreeSet<HardwareRequirements>();
		this.initClient();
	}

	private void initClient() {
		HttpRequestUtil.init();
		this.client = new HttpRequestClientUtil();
	}

	private String getTenantId(OpenStackUserAttributes openStackUserAttributes) throws InvalidParameterException {
		String tenantId = openStackUserAttributes.getTenantId();
		if (tenantId == null) {
			throw new InvalidParameterException("No tenantId in local token.");
		}
		return tenantId;
	}

	private List<String> resolveNetworksId(ComputeOrder computeOrder) {
		List<String> requestedNetworksId = new ArrayList<>();
		String defaultNetworkId = this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);

		//We add the default network before any other network, because the order is very important to Openstack
		//request. Openstack will configure the routes to the external network by the first network found on request body.
		requestedNetworksId.add(defaultNetworkId);
		requestedNetworksId.addAll(computeOrder.getNetworksId());
		computeOrder.setNetworksId(requestedNetworksId);
		return requestedNetworksId;
	}

	private String getKeyName(String tenantId, OpenStackUserAttributes openStackUserAttributes, String publicKey)
			throws FogbowManagerException, UnexpectedException {
		String keyName = null;

		if (publicKey != null && !publicKey.isEmpty()) {
			String osKeypairEndpoint = getComputeEndpoint(tenantId, SUFFIX_ENDPOINT_KEYPAIRS);

			keyName = getRandomUUID();
			CreateOsKeypairRequest request = new CreateOsKeypairRequest.Builder()
					.name(keyName)
					.publicKey(publicKey)
					.build();

			String body = request.toJson();
			try {
				this.client.doPostRequest(osKeypairEndpoint, openStackUserAttributes, body);
			} catch (HttpResponseException e) {
				OpenStackHttpToFogbowManagerExceptionMapper.map(e);
			}
		}

		return keyName;
	}

	private String getComputeEndpoint(String tenantId, String suffix) {
		return this.properties.getProperty(COMPUTE_NOVAV2_URL_KEY) + COMPUTE_V2_API_ENDPOINT + tenantId + suffix;
	}

	private void deleteKeyName(String tenantId, OpenStackUserAttributes openStackUserAttributes, String keyName) throws FogbowManagerException, UnexpectedException {
		String suffixEndpoint = SUFFIX_ENDPOINT_KEYPAIRS + "/" + keyName;
		String keyNameEndpoint = getComputeEndpoint(tenantId, suffixEndpoint);
		
		try {
			this.client.doDeleteRequest(keyNameEndpoint, openStackUserAttributes);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}
	}

	private CreateComputeRequest getRequestBody(String imageRef, String flavorRef, String userdata, String keyName,
												List<String> networksIds) {
		List<CreateComputeRequest.Network> networks = new ArrayList<>();
		List<CreateComputeRequest.SecurityGroup> securityGroups = new ArrayList<>();
		for (String networkId : networksIds) {
			networks.add(new CreateComputeRequest.Network(networkId));

			String defaultNetworkId = this.properties.getProperty(DEFAULT_NETWORK_ID_KEY);
			if (!networkId.equals(defaultNetworkId)) {
				String prefix = OpenStackV2NetworkPlugin.SECURITY_GROUP_PREFIX;
				String securityGroupName = prefix + "-" + networkId;
				securityGroups.add(new CreateComputeRequest.SecurityGroup(securityGroupName));
			}
		}

		// do not specify security groups if no additional network was given
		securityGroups = securityGroups.size() == 0 ? null : securityGroups;

		String name = FOGBOW_INSTANCE_NAME + getRandomUUID();
		CreateComputeRequest createComputeRequest = new CreateComputeRequest.Builder()
				.name(name)
				.imageReference(imageRef)
				.flavorReference(flavorRef)
				.userData(userdata)
				.keyName(keyName)
				.networks(networks)
				.securityGroups(securityGroups)
				.build();

		return createComputeRequest;
	}

	private HardwareRequirements findSmallestFlavor(ComputeOrder computeOrder, OpenStackUserAttributes openStackUserAttributes)
			throws UnexpectedException, FogbowManagerException {
		HardwareRequirements bestFlavor = getBestFlavor(computeOrder, openStackUserAttributes);
		if (bestFlavor == null) {
			throw new NoAvailableResourcesException();
		}
		return bestFlavor;
	}

	private HardwareRequirements getBestFlavor(ComputeOrder computeOrder, OpenStackUserAttributes openStackUserAttributes) throws FogbowManagerException, UnexpectedException {
		updateFlavors(openStackUserAttributes);
		TreeSet<HardwareRequirements> hardwareRequirementsList = getHardwareRequirementsList();
		for (HardwareRequirements hardwareRequirements : hardwareRequirementsList) {
			if (hardwareRequirements.getCpu() >= computeOrder.getvCPU()
					&& hardwareRequirements.getRam() >= computeOrder.getMemory()
					&& hardwareRequirements.getDisk() >= computeOrder.getDisk()) {
				return hardwareRequirements;
			}
		}
		return null;
	}

	private void updateFlavors(OpenStackUserAttributes openStackUserAttributes) throws FogbowManagerException, UnexpectedException {
		LOGGER.debug("Updating hardwareRequirements from OpenStack");

		String tenantId = getTenantId(openStackUserAttributes);
		String flavorsEndpoint = getComputeEndpoint(tenantId, SUFFIX_ENDPOINT_FLAVORS);

		try {
			String jsonResponse = this.client.doGetRequest(flavorsEndpoint, openStackUserAttributes);
			GetAllFlavorsResponse getAllFlavorsResponse = GetAllFlavorsResponse.fromJson(jsonResponse);

			List<String> flavorsIds = new ArrayList<>();
			for (GetAllFlavorsResponse.Flavor flavor : getAllFlavorsResponse.getFlavors()) {
				flavorsIds.add(flavor.getId());
			}

			TreeSet<HardwareRequirements> newHardwareRequirements = detailFlavors(flavorsEndpoint, openStackUserAttributes, flavorsIds);
			setHardwareRequirementsList(newHardwareRequirements);
		} catch (HttpResponseException e) {
			OpenStackHttpToFogbowManagerExceptionMapper.map(e);
		}
	}

	private TreeSet<HardwareRequirements> detailFlavors(String endpoint, OpenStackUserAttributes openStackUserAttributes, List<String> flavorsIds)
			throws FogbowManagerException, UnexpectedException 
			{
		TreeSet<HardwareRequirements> newHardwareRequirements = new TreeSet<>();
		TreeSet<HardwareRequirements> flavorsCopy = new TreeSet<>(getHardwareRequirementsList());

		for (String flavorId : flavorsIds) {
			boolean containsFlavorForCaching = false;

			for (HardwareRequirements flavor : flavorsCopy) {
				if (flavor.getFlavorId().equals(flavorId)) {
					containsFlavorForCaching = true;
					newHardwareRequirements.add(flavor);
					break;
				}
			}

			if (!containsFlavorForCaching) {
				String newEndpoint = endpoint + "/" + flavorId;
				
				String getJsonResponse = null;
				
				try {
					getJsonResponse = this.client.doGetRequest(newEndpoint, openStackUserAttributes);
				} catch (HttpResponseException e) {
					OpenStackHttpToFogbowManagerExceptionMapper.map(e);
				}

				GetFlavorResponse getFlavorResponse = GetFlavorResponse.fromJson(getJsonResponse);

				String id = getFlavorResponse.getId();
				String name = getFlavorResponse.getName();
				int disk = getFlavorResponse.getDisk();
				int memory = getFlavorResponse.getMemory();
				int vcpusCount = getFlavorResponse.getVcpusCount();

				newHardwareRequirements.add(new HardwareRequirements(name, id, vcpusCount, memory, disk));
			}
		}

		return newHardwareRequirements;
	}

	private ComputeInstance instanceFromJson(String getRawResponse, OpenStackUserAttributes openStackUserAttributes) throws FogbowManagerException, UnexpectedException {
		GetComputeResponse getComputeResponse = GetComputeResponse.fromJson(getRawResponse);

		String flavorId = getComputeResponse.getFlavor().getId();
		HardwareRequirements hardwareRequirements = getFlavorById(flavorId, openStackUserAttributes);

		if (hardwareRequirements == null) {
			throw new NoAvailableResourcesException("No matching flavor");
		}

		int vcpusCount = hardwareRequirements.getCpu();
		int memory = hardwareRequirements.getRam();
		int disk = hardwareRequirements.getDisk();

		String openStackState = getComputeResponse.getStatus();
		InstanceState fogbowState = OpenStackStateMapper.map(ResourceType.COMPUTE, openStackState);

		String instanceId = getComputeResponse.getId();
		String hostName = getComputeResponse.getName();

		GetComputeResponse.Addresses addressesContainer = getComputeResponse.getAddresses();

		String address = "";
		if (addressesContainer != null) {
			GetComputeResponse.Address[] addresses = addressesContainer.getProviderAddresses();
			boolean firstAddressEmpty = addresses == null || addresses.length == 0 || addresses[0].getAddress() == null;
			address = firstAddressEmpty ? "" : addresses[0].getAddress();
		}

		ComputeInstance computeInstance = new ComputeInstance(instanceId,
				fogbowState, hostName, vcpusCount, memory, disk, address);
		return computeInstance;
	}

	private HardwareRequirements getFlavorById(String id, OpenStackUserAttributes openStackUserAttributes) throws FogbowManagerException, UnexpectedException {
		updateFlavors(openStackUserAttributes);
		TreeSet<HardwareRequirements> flavorsCopy = new TreeSet<>(getHardwareRequirementsList());
		for (HardwareRequirements hardwareRequirements : flavorsCopy) {
			if (hardwareRequirements.getFlavorId().equals(id)) {
				return hardwareRequirements;
			}
		}
		return null;
	}

	protected String getRandomUUID() {
		return UUID.randomUUID().toString();
	}
	
	private TreeSet<HardwareRequirements> getHardwareRequirementsList() {
		synchronized (this.hardwareRequirementsList) {
			return new TreeSet<HardwareRequirements>(this.hardwareRequirementsList);
		}
	}
	
	private void setHardwareRequirementsList(TreeSet<HardwareRequirements> hardwareRequirementsList) {
		synchronized (this.hardwareRequirementsList) {
			this.hardwareRequirementsList = hardwareRequirementsList;
		}
	}
}
