package cloud.fogbow.ras.core.models;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class StorableBeanTest {

    private StorableBean storableBean;

    @Before
    public void setUp() {
        this.storableBean = new StorableBean() {

            @Override
            protected Logger getLogger() {
                return Mockito.mock(Logger.class);
            }

        };
    }

    @Test
    public void testGetStorableValue() {
        Assert.assertEquals("abc", this.storableBean.treatValue("abcd", "fakename", 3));
        Assert.assertEquals("abcd", this.storableBean.treatValue("abcd", "fakename", 10));
        Assert.assertEquals("", this.storableBean.treatValue("abcd", "fakename", 0));
    }
}
