package org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.image;

import org.fogbowcloud.ras.core.exceptions.InvalidParameterException;
import org.fogbowcloud.ras.core.plugins.interoperability.cloudstack.CloudStackRequest;

public class GetAllImagesRequest extends CloudStackRequest {

    public static final String LIST_TEMPLATES_COMMAND = "listTemplates";

    public static final String TEMPLATE_FILTER_KEY = "templatefilter";
    public static final String ID_KEY = "id";

    public static final String EXECUTABLE_TEMPLATES_VALUE = "executable";

    protected GetAllImagesRequest(Builder builder) throws InvalidParameterException {
        addParameter(TEMPLATE_FILTER_KEY, EXECUTABLE_TEMPLATES_VALUE);
        addParameter(ID_KEY, builder.id);
    }

    @Override
    public String getCommand() {
        return LIST_TEMPLATES_COMMAND;
    }

    public static class Builder {

        private String id;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public GetAllImagesRequest build() throws InvalidParameterException {
            return new GetAllImagesRequest(this);
        }
    }

}