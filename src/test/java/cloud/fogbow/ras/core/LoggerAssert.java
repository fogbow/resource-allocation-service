package cloud.fogbow.ras.core;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.Assert;
import org.slf4j.LoggerFactory;

import java.util.List;

public class LoggerAssert {

    private Logger fooLogger;
    private ListAppender<ILoggingEvent> listAppender;

    public LoggerAssert(Class classToTest) {
        this.fooLogger = (Logger) LoggerFactory.getLogger(classToTest);
        this.listAppender = new ListAppender<>();
        this.listAppender.start();

        this.fooLogger.addAppender(this.listAppender);
    }

    public void assertEquals(int logPosition, Level level, String message) {
        List<ILoggingEvent> list = this.listAppender.list;
        ILoggingEvent iLoggingEvent = list.get(getPositionList(logPosition));
        Assert.assertEquals(iLoggingEvent.getMessage(), message);
        Assert.assertEquals(iLoggingEvent.getLevel(), level);
    }

    private int getPositionList(int logPosition) {
        return logPosition - 1;
    }

}