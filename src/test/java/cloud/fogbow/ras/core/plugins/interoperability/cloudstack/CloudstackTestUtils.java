package cloud.fogbow.ras.core.plugins.interoperability.cloudstack;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.common.models.CloudStackUser;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.common.util.connectivity.cloud.cloudstack.CloudStackUrlUtil;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.compute.v4_9.GetVirtualMachineResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CloudstackTestUtils {

    private static final String LIST_SERVICE_OFFERINGS_RESPONSE = "listserviceofferingsresponse.json";
    private static final String LIST_SERVICE_OFFERINGS_EMPTY_RESPONSE =
            "listserviceofferingsresponse_empty.json";
    private static final String LIST_SERVICE_OFFERINGS_ERROR_RESPONSE =
            "listserviceofferingsresponse_error.json";
    private static final String LIST_DISK_OFFERINGS_RESPONSE = "listdiskofferingsresponse.json";
    private static final String LIST_DISK_OFFERINGS_EMPTY_RESPONSE = "listdiskofferingsresponse_empty.json";
    private static final String LIST_DISK_OFFERINGS_ERROR_RESPONSE = "listdiskofferingsresponse_error.json";
    private static final String DEPLOY_VIRTUAL_MACHINE_RESPONSE = "deployvirtualmachineresponse.json";
    private static final String DEPLOY_VIRTUAL_MACHINE_ERROR_RESPONSE = "deployvirtualmachineresponse_error.json";
    private static final String LIST_VIRTUAL_MACHINE_RESPONSE = "listvirtualmachinesresponse.json";
    private static final String LIST_VIRTUAL_MACHINE_EMPTY_RESPONSE =
            "listvirtualmachinesresponse_empty.json";
    private static final String LIST_VIRTUAL_MACHINE_ERROR_RESPONSE =
            "listvirtualmachinesresponse_error.json";
    private static final String NIC_VIRTUAL_MACHINE_RESPONSE = "nic.json";
    private static final String LIST_VOLUMES_RESPONSE = "listvolumesresponse.json";
    private static final String LIST_VOLUMES_EMPTY_RESPONSE = "listvolumesresponse_empty.json";
    private static final String LIST_VOLUMES_ERROR_RESPONSE = "listvolumesresponse_error.json";
    private static final String CREATE_NETWORK_RESPONSE = "createnetworkresponse.json";
    private static final String CREATE_NETWORK_EMPTY_RESPONSE = "createnetworkresponse_empty.json" ;
    private static final String CREATE_NETWORK_ERROR_RESPONSE = "createnetworkresponse_error.json";
    private static final String LIST_NETWORKS_RESPONSE = "listnetworksresponse.json";
    private static final String LIST_NETWORKS_EMPTY_RESPONSE = "listnetworksresponse_empty.json";
    private static final String LIST_NETWORKS_ERROR_RESPONSE = "listnetworksresponse_error.json";
    private static final String ATTACH_VOLUME_RESPONSE = "attachvolumeresponse.json";
    private static final String ATTACH_VOLUME_ERROR_RESPONSE = "attachvolumeresponse_error.json";

    public static final CloudStackUser CLOUD_STACK_USER =
            new CloudStackUser("id", "", "", "", new HashMap<>());

    public static final String BAD_REQUEST_MSG = "Bad Request";
    public static final String CLOUD_NAME = "cloudstack";
    private static final String CLOUDSTACK_RESOURCE_PATH = "cloud" + File.separator +
            "plugins" + File.separator + "interoperability" + File.separator +
            "cloudstack" + File.separator;
    public static String CLOUDSTACK_CONF_FILE_PATH = HomeDir.getPath() +
            SystemConstants.CLOUDS_CONFIGURATION_DIRECTORY_NAME + File.separator +
            CLOUD_NAME + File.separator + SystemConstants.CLOUD_SPECIFICITY_CONF_FILE_NAME;

    public static final String AND_OPERATION_URL_PARAMETER = "&";
    public static final String CLOUDSTACK_MULTIPLE_TAGS_SEPARATOR = ",";

    public static final String CLOUDSTACK_URL_DEFAULT = "http://localhost";

    public static String createGetAllServiceOfferingsResponseJson(
            String id, String name, int cpuNumber, int memory, String tags) throws IOException {

        String rawJson = readFileAsString(getPathCloudstackFile() + LIST_SERVICE_OFFERINGS_RESPONSE);

        return String.format(rawJson, id, name, cpuNumber, memory, tags);
    }

    public static String createGetAllServiceOfferingsErrotResponseJson(int errorCode, String errorText)
            throws IOException {

        String rawJson = readFileAsString(
                getPathCloudstackFile() + LIST_SERVICE_OFFERINGS_ERROR_RESPONSE);

        return String.format(rawJson, errorCode, errorText);
    }

    public static String createGetAllServiceOfferingsEmptyResponseJson() throws IOException {
        String rawJson = readFileAsString(getPathCloudstackFile()
                + LIST_SERVICE_OFFERINGS_EMPTY_RESPONSE);

        return String.format(rawJson);
    }

    public static String createGetAllDiskOfferingsResponseJson(
            String id, int disk, boolean customized, String tags) throws IOException {

        String rawJson = readFileAsString(getPathCloudstackFile()
                + LIST_DISK_OFFERINGS_RESPONSE);

        return String.format(rawJson, id, disk, customized, tags);
    }

    public static String createGetAllDiskOfferingsErrorResponseJson(int errorCode, String errorText)
            throws IOException {

        String rawJson = readFileAsString(getPathCloudstackFile()
                + LIST_DISK_OFFERINGS_ERROR_RESPONSE);

        return String.format(rawJson, errorCode, errorText);
    }

    public static String createGetAllDiskOfferingsEmptyResponseJson() throws IOException {
        String rawJson = readFileAsString(getPathCloudstackFile()
                + LIST_DISK_OFFERINGS_EMPTY_RESPONSE);

        return String.format(rawJson);
    }

    public static String createDeployVirtualMachineResponseJson(String id) throws IOException {
        String rawJson = readFileAsString(getPathCloudstackFile() + DEPLOY_VIRTUAL_MACHINE_RESPONSE);

        return String.format(rawJson, id);
    }

    public static String createDeployVirtualMachineErrorResponseJson(
            int errorCode, String errorText) throws IOException {

        String rawJson = readFileAsString(getPathCloudstackFile()
                + DEPLOY_VIRTUAL_MACHINE_ERROR_RESPONSE);

        return String.format(rawJson, errorCode, errorText);
    }

    public static String createEmptyGetVolumesResponseJson() throws IOException {
        String rawJson = readFileAsString(getPathCloudstackFile() + LIST_VOLUMES_EMPTY_RESPONSE);

        return String.format(rawJson);
    }

    public static String createGetVolumesResponseJson(
            String id, String name, double size, String state) throws IOException {

        String rawJson = readFileAsString(getPathCloudstackFile() + LIST_VOLUMES_RESPONSE);

        return String.format(rawJson, id, name, size, state);
    }

    public static String createGetVolumesErrorResponseJson(int errorCode, String errorText) throws IOException {

        String rawJson = readFileAsString(getPathCloudstackFile() + LIST_VOLUMES_ERROR_RESPONSE);

        return String.format(rawJson, errorCode, errorText);
    }

    public static String createGetVirtualMachineResponseJson(
            String id, String name, String state, int memory,
            int cpuNumber, List<GetVirtualMachineResponse.Nic> nics) throws IOException {

        String rawJson = readFileAsString(getPathCloudstackFile() + LIST_VIRTUAL_MACHINE_RESPONSE);

        ArrayList<String> nicsStrs = new ArrayList<>();
        nics.stream().forEach(nic -> {
            try {
                nicsStrs.add(createNicJson(nic.getIpAddress()));
            } catch (IOException e) {
                throw new Error();
            }
        });
        String[] nicsStrsArr = new String[nicsStrs.size()];
        nicsStrs.toArray(nicsStrsArr);
        String nicsFullStr = String.join(",", nicsStrsArr);

        return String.format(rawJson, id, name, state, memory, cpuNumber, nicsFullStr);
    }

    public static String createGetVirtualMachineEmptyResponseJson() throws IOException {
        String rawJson = readFileAsString(getPathCloudstackFile()
                + LIST_VIRTUAL_MACHINE_EMPTY_RESPONSE);

        return String.format(rawJson);
    }

    public static String createGetVirtualMachineErrorResponseJson(
            int errorCode, String errorText) throws IOException {

        String rawJson = readFileAsString(getPathCloudstackFile()
                + LIST_VIRTUAL_MACHINE_ERROR_RESPONSE);

        return String.format(rawJson, errorCode, errorText);
    }

    static String createNicJson(String idAddress) throws IOException {
        String rawJson = readFileAsString(getPathCloudstackFile() + NIC_VIRTUAL_MACHINE_RESPONSE);

        return String.format(rawJson, idAddress);
    }

    public static String createNetworkResponseJson(String idNetwork) throws IOException {
        String rawJson = readFileAsString(getPathCloudstackFile() + CREATE_NETWORK_RESPONSE);

        return String.format(rawJson, idNetwork);
    }

    public static String createCreateNetworkEmptyResponseJson() throws IOException {
        String rawJson = readFileAsString(getPathCloudstackFile() + CREATE_NETWORK_EMPTY_RESPONSE);

        return String.format(rawJson);
    }

    public static String createCreateNetworkErrorResponseJson(int errorCode, String errorText)
            throws IOException {

        String rawJson = readFileAsString(getPathCloudstackFile()
                + CREATE_NETWORK_ERROR_RESPONSE);

        return String.format(rawJson, errorCode, errorText);
    }

    public static String createGetNetworkResponseJson(String id, String name, String gateway,
                                                      String cird, String state)
            throws IOException {

        String rawJson = readFileAsString(getPathCloudstackFile()
                + LIST_NETWORKS_RESPONSE);

        return String.format(rawJson, id, name, gateway, cird, state);
    }

    public static String createGetNetworkEmptyResponseJson() throws IOException {
        String rawJson = readFileAsString(getPathCloudstackFile()
                + LIST_NETWORKS_EMPTY_RESPONSE);

        return String.format(rawJson);
    }

    public static String createGetNetworkErrorResponseJson(int errorCode, String errorText)
            throws IOException {

        String rawJson = readFileAsString(getPathCloudstackFile()
                + LIST_NETWORKS_ERROR_RESPONSE);

        return String.format(rawJson, errorCode, errorText);
    }

    public static String attachVolumeResponseJson(String jobId) throws IOException {
        String rawJson = readFileAsString(getPathCloudstackFile()
                + ATTACH_VOLUME_RESPONSE);

        return String.format(rawJson, jobId);
    }

    public static String attachVolumeErrorResponseJson(int errorCode, String errorText)
            throws IOException {

        String rawJson = readFileAsString(getPathCloudstackFile()
                + ATTACH_VOLUME_ERROR_RESPONSE);

        return String.format(rawJson, errorCode, errorText);
    }

    private static String readFileAsString(final String fileName) throws IOException {
        Path path = Paths.get(fileName);
        byte[] bytes = Files.readAllBytes(path);
        String data = new String(bytes);
        return data;
    }

    private static String getPathCloudstackFile() {
        String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
        return rootPath + CLOUDSTACK_RESOURCE_PATH;
    }

    // TODO(chico) use in the cloudstack compute
    public static void ignoringCloudStackUrl() throws InvalidParameterException {
        PowerMockito.mockStatic(CloudStackUrlUtil.class);
        PowerMockito.when(CloudStackUrlUtil.createURIBuilder(Mockito.anyString(),
                Mockito.anyString())).thenCallRealMethod();
    }

    // TODO(chico) use in the cloudstack compute
    public static HttpResponseException createBadRequestHttpResponse() {
        return new HttpResponseException(HttpStatus.SC_BAD_REQUEST, BAD_REQUEST_MSG);
    }

}