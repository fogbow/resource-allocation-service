package cloud.fogbow.ras.core;

import cloud.fogbow.common.exceptions.FatalErrorException;
import cloud.fogbow.ras.core.stubs.StubClassForFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class ClassFactoryTest {

    private String className;
    ClassFactory classFactory;

    @Before
    public void setUp(){
        classFactory = new ClassFactory();
        className = "cloud.fogbow.ras.core.stubs.StubClassForFactory";
    }

    @Test
    public void createPluginInstanceNoParameters() {
        // exercise
        StubClassForFactory obj = (StubClassForFactory) classFactory.createPluginInstance(className);

        // verify
        Assert.assertNotEquals(null, obj);
    }

    @Test
    public void createPluginInstanceOneParameters() {
        // exercise
        String parameter1 = "parameter1";
        StubClassForFactory obj = (StubClassForFactory) classFactory.createPluginInstance(className, parameter1);

        // verify
        Assert.assertNotEquals(null, obj);
    }

    @Test
    public void createPluginInstanceTwoParameters() {
        // exercise
        String parameter1 = "parameter1";
        String parameter2 = "parameter2";
        StubClassForFactory obj = (StubClassForFactory) classFactory.createPluginInstance(className, parameter1, parameter2);

        // verify
        Assert.assertNotEquals(null, obj);
    }

    @Test(expected = FatalErrorException.class)
    public void createPluginInstanceWithNonExistingClass() {
        String invalidClassPath = "invalid.class.path.InvalidClass";
        classFactory.createPluginInstance(invalidClassPath);
    }
}