package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud;

import cloud.fogbow.common.util.GsonHolder;
import cloud.fogbow.ras.constants.SystemConstants;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class EmulatedCloudUtils {
    public static HashMap readJsonAsHashMap(String filePath) throws IOException {
        String fileContent = EmulatedCloudUtils.getFileContent(filePath);

        return GsonHolder.getInstance().fromJson(fileContent, HashMap.class);
    }

    public static String getFileContent(String filePath) throws IOException {

        List<String> lines = Files.readAllLines(Paths.get(filePath));
        String fileContent = String.join("\n", lines);

        return fileContent;
    }

    public static void saveHashMapAsJson(String path, HashMap hashMap) throws IOException {
        deleteFile(path);

        String jsonContent = GsonHolder.getInstance().toJson(hashMap);
        EmulatedCloudUtils.saveFileContent(path, jsonContent);
    }

    public static void saveFileContent(String path, String content) throws IOException {
        FileUtils.writeStringToFile(new File(path), content);
    }

    public static String getResourcePath(Properties properties, String path){
        String resourcesPath = properties.getProperty(EmulatedCloudConstants.Conf.RESOURCES_FOLDER);
        return resourcesPath + "/" + path;
    }

    public static void deleteFile(String path){
        File file = new File(path);

        if(file.exists()){
            file.delete();
        }
    }

    public static String getFileContentById(Properties properties, String id) throws IOException {
        String path = EmulatedCloudUtils.getResourcePath(properties, id);
        return EmulatedCloudUtils.getFileContent(path);
    }

    public static String getName(String name){
        return (name == null ? SystemConstants.FOGBOW_INSTANCE_NAME_PREFIX + EmulatedCloudUtils.getRandomUUID() : name);
    }

    public static String getRandomUUID() {
        return UUID.randomUUID().toString();
    }
}
