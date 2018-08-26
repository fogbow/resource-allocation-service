package org.fogbowcloud.ras.core.plugins.cloud.cloudstack;

import org.mockito.ArgumentMatcher;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CloudStackUrlMatcher extends ArgumentMatcher<String> {

    public static final String URL_SEPARATOR = "?";
    public static final String PARAMS_SEPARATOR = "&";
    public static final String EQUAL_SIGN = "=";

    private Map<String, String> expectedParams;
    private List<String> ignoredKeys;

    public CloudStackUrlMatcher(Map<String, String> expectedParams, String... ignoredKeys) {
        this.expectedParams = expectedParams;
        this.ignoredKeys = Arrays.asList(ignoredKeys);
    }

    @Override
    public boolean matches(Object o) {
        if (o instanceof String) {
            String url = (String) o;
            return compareWith(getUrlParams(url));
        }

        return false;
    }

    private boolean compareWith(Map<String, String> map) {
        for (String key : this.expectedParams.keySet()) {
            if (!this.ignoredKeys.contains(key) &&
                    !this.expectedParams.get(key).equals(map.get(key))) {
                return false;
            }
        }
        return true;
    }

    private Map<String, String> getUrlParams(String url) {
        String params = url.substring(url.indexOf(URL_SEPARATOR) + 1);
        String[] keysAndValues = params.split(PARAMS_SEPARATOR);

        Map<String, String> urlParams = new HashMap<>();
        for (int i = 0; i < keysAndValues.length; i++) {
            String[] keyAndValue = keysAndValues[i].split(EQUAL_SIGN);
            urlParams.put(keyAndValue[0], keyAndValue[1]);
        }
        return urlParams;
    }

}