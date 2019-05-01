package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud;

import cloud.fogbow.common.util.GsonHolder;
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
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        String fileContent = String.join("\n", lines);

        return GsonHolder.getInstance().fromJson(fileContent, HashMap.class);
    }

    public static void saveHashMapAsJson(String path, HashMap hashMap) throws IOException {
        deleteFile(path);

        String jsonContent = GsonHolder.getInstance().toJson(hashMap);
        FileUtils.writeStringToFile(new File(path), jsonContent);
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

    public static String getRandomUUID() {
        return UUID.randomUUID().toString();
    }
}