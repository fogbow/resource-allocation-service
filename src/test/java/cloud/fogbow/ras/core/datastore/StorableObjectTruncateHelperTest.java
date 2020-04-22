package cloud.fogbow.ras.core.datastore;

import cloud.fogbow.common.datastore.StorableObjectTruncateHelper;
import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.ras.core.models.UserData;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import cloud.fogbow.ras.core.models.orders.Order;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Random;

public class StorableObjectTruncateHelperTest {
    @Test
    public void testGetAllFields() throws UnexpectedException {
        Object object = new Object();
        StorableObjectTruncateHelper<Object> objectTruncateHelper = new StorableObjectTruncateHelper<>(object.getClass());

        Assert.assertEquals(0, objectTruncateHelper.getAllFields(object.getClass()).size());
    }

    @Test
    public void testTruncateOrder() throws UnexpectedException {
        String userId = "userId";
        String userName = "userName";
        String identityProviderId = "identityProviderId";
        SystemUser user = new SystemUser(userId, userName, identityProviderId);

        String requestingMember = "requestingMember";
        String providingMember = "providingMember";
        String name = "name";
        int vCPU = 4;
        int memory = 2048;
        int disk = 20;
        String imageId = "imageId";
        ArrayList<UserData> userData = new ArrayList<>();
        String publicKey = "publicKey";
        ArrayList<String> networkIds = new ArrayList<>();

        String cloudName = getRandomString(Order.FIELDS_MAX_SIZE + 1);

        ComputeOrder order = new ComputeOrder(user, requestingMember, providingMember, cloudName, name, vCPU,
                memory, disk, imageId, userData, publicKey, networkIds);

        StorableObjectTruncateHelper<ComputeOrder> objectTruncateHelper = new StorableObjectTruncateHelper<>(order.getClass());
        ComputeOrder truncated = objectTruncateHelper.truncate(order);

        Assert.assertEquals(order.getvCPU(), truncated.getvCPU());
        Assert.assertEquals(order.getRam(), truncated.getRam());
        Assert.assertEquals(order.getDisk(), truncated.getDisk());
        Assert.assertEquals(order.getUserData(), truncated.getUserData());
        Assert.assertEquals(order.getName(), truncated.getName());
        Assert.assertEquals(order.getImageId(), truncated.getImageId());
        Assert.assertEquals(order.getPublicKey(), truncated.getPublicKey());
        Assert.assertEquals(order.getActualAllocation(), truncated.getActualAllocation());
        Assert.assertEquals(order.getNetworkIds(), truncated.getNetworkIds());

        Assert.assertEquals(cloudName.substring(0, cloudName.length() - 1), truncated.getCloudName());
    }

    private String getRandomString(int stringSize) {
        int start = 48;
        int end = 122;
        byte[] byteArray = new byte[stringSize];
        for (int i = 0; i < byteArray.length; i++) {
            byteArray[i] = (byte) (new Random().nextInt(end - start) + start);
        }

        return new String(byteArray);
    }
}
