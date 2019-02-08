package cloud.fogbow.ras.core.models;

import org.apache.log4j.Logger;

import static cloud.fogbow.ras.core.constants.Messages.Error.VALUE_TOO_LARGE_TO_STORE;

public abstract class StorableBean {

    protected String treatValue(String value, String columnName, int maxSize) {
        if (value == null || value.length() <= maxSize) {
            return value;
        } else {
            int endIndex = Math.min(value.length(), maxSize);
            String truncatedValue = value.substring(0, endIndex);

            getLogger().error(String.format(VALUE_TOO_LARGE_TO_STORE, value, columnName, getClass().getName(), truncatedValue));
            return truncatedValue;
        }
    }

    protected abstract Logger getLogger();

}
