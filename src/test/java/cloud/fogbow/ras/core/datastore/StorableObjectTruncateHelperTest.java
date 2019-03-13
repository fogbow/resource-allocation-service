package cloud.fogbow.ras.core.datastore;

import cloud.fogbow.common.exceptions.UnexpectedException;
import org.junit.Assert;
import org.junit.Test;

public class StorableObjectTruncateHelperTest {

    @Test
    public void testGetAllFields() throws UnexpectedException {
        Object object = new Object();
        StorableObjectTruncateHelper<Object> objectTruncateHelper = new StorableObjectTruncateHelper<>(object.getClass());

        Assert.assertEquals(0, objectTruncateHelper.getAllFields(object.getClass()).size());

        OneAttribute oneAttribute = new OneAttribute("someAttributeValue");
        StorableObjectTruncateHelper<Object> oneAttributeTruncateHelper = new StorableObjectTruncateHelper<>(oneAttribute.getClass());

        Assert.assertEquals(1, oneAttributeTruncateHelper.getAllFields(oneAttribute.getClass()).size());
    }

    class OneAttribute {
        private String attributeOne;

        public OneAttribute(String attributeOne) {
            this.attributeOne = attributeOne;
        }
    }
}
