package cloud.fogbow.ras.core;

import org.apache.log4j.Appender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;
import org.junit.Assert;

import java.util.ArrayList;
import java.util.List;

public class LoggerAssert {

    public static final int SECOND_POSITION = 2;
    public static final int FIRST_POSITION = 1;

    private List<LoggingEvent> list;
    private int globalPosition = 1;

    public LoggerAssert(Class classToTest) {
        Logger logger = Logger.getLogger(classToTest);
        MemoryListAppender memoryListAppender = new MemoryListAppender();
        logger.addAppender(memoryListAppender);

        this.list = memoryListAppender.getLoggingEvents();
    }

    public void assertEquals(int logPosition, Level level, String message) {
        LoggingEvent loggingEvent = this.list.get(getPositionList(logPosition));
        Assert.assertEquals(loggingEvent.getMessage(), message);
        Assert.assertEquals(loggingEvent.getLevel(), level);
    }

    public LoggerAssert assertEqualsInOrder(Level level, String message) {
        assertEquals(this.globalPosition++, level, message);
        return this;
    }

    private int getPositionList(int logPosition) {
        return logPosition - 1;
    }

    class MemoryListAppender implements Appender {

        private List<LoggingEvent> loggingEvents = new ArrayList<>();

        public List<LoggingEvent> getLoggingEvents() {
            return this.loggingEvents;
        }

        @Override
        public void addFilter(Filter filter) {

        }

        @Override
        public Filter getFilter() {
            return null;
        }

        @Override
        public void clearFilters() {

        }

        @Override
        public void close() {

        }

        @Override
        public void doAppend(LoggingEvent loggingEvent) {
            this.loggingEvents.add(loggingEvent);
        }

        @Override
        public String getName() {
            return null;
        }

        @Override
        public void setErrorHandler(ErrorHandler errorHandler) {

        }

        @Override
        public ErrorHandler getErrorHandler() {
            return null;
        }

        @Override
        public void setLayout(Layout layout) {

        }

        @Override
        public Layout getLayout() {
            return null;
        }

        @Override
        public void setName(String s) {

        }

        @Override
        public boolean requiresLayout() {
            return false;
        }
    }

}