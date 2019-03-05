package cloud.fogbow.ras.core.stubs;

import org.apache.log4j.Logger;

public class StubClassForFactory {
    private static final String NO_PARAMETER_MESSAGE = "Building instance with no parameter";
    private static final String ONE_PARAMETER_MESSAGE = "Building instance with one parameter: [%s]";
    private static final String TWO_PARAMETERS_MESSAGE = "Building instance with two parameters: [%s], [%s]";
    private Logger LOGGER = Logger.getLogger(StubClassForFactory.class);
    public StubClassForFactory() {
        LOGGER.debug(NO_PARAMETER_MESSAGE);
    }

    public StubClassForFactory(String parameter1) {
        LOGGER.debug(String.format(ONE_PARAMETER_MESSAGE, parameter1));
    }

    public StubClassForFactory(String parameter1, String parameter2) {
        LOGGER.debug(String.format(TWO_PARAMETERS_MESSAGE, parameter1, parameter2));
    }
}
