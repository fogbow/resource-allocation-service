package cloud.fogbow.ras.core.plugins.interoperability.emulatedcloud;

import cloud.fogbow.common.util.GsonHolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class EmulatedPluginFileUtils {
    public static HashMap readJsonAsHashMap(String filePath) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(filePath));
        String fileContent = String.join("\n", lines);
        return GsonHolder.getInstance().fromJson(fileContent, HashMap.class);
    }
}
