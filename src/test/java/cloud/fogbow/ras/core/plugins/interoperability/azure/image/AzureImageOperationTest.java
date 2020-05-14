package cloud.fogbow.ras.core.plugins.interoperability.azure.image;

import cloud.fogbow.common.exceptions.UnexpectedException;
import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.core.TestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.AzureTestUtils;
import cloud.fogbow.ras.core.plugins.interoperability.azure.util.AzureImageOperationUtil;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.compute.VirtualMachineOffer;
import com.microsoft.azure.management.compute.VirtualMachinePublisher;
import com.microsoft.azure.management.compute.VirtualMachineSku;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AzureImageOperationUtil.class })
public class AzureImageOperationTest extends AzureTestUtils {

    private static final String AZURE_SELECTED_PUBLISHER = "Canonical";
    private AzureImageOperation operation;
    private Azure azure;

    @Before
    public void setUp() {
        String region = AzureTestUtils.DEFAULT_REGION_NAME;
        this.operation = Mockito.spy(new AzureImageOperation(region));
        this.azure = null;
    }

    // test case: When calling getImages method with all secondary method
    // mocked, it must return the right map of ImageSummary
    @Test
    public void testGetImagesSuccessfully() throws UnexpectedException {
        // set up
        VirtualMachinePublisher publisher = createPublisher(AZURE_SELECTED_PUBLISHER);
        PagedList<VirtualMachinePublisher> publishers = (PagedList<VirtualMachinePublisher>) Mockito.mock(PagedList.class);
        Mockito.when(publishers.iterator()).thenReturn(Arrays.asList(publisher).iterator());
        Mockito.doReturn(publishers).when(this.operation).getPublishers(Mockito.eq(this.azure));

        VirtualMachineOffer offer = createOffer("UbuntuServer");
        PagedList<VirtualMachineOffer> offers = (PagedList<VirtualMachineOffer>) Mockito.mock(PagedList.class);
        Mockito.when(offers.iterator()).thenReturn(Arrays.asList(offer).iterator());
        Mockito.doReturn(offers).when(this.operation).getOffersFrom(publisher);

        VirtualMachineSku sku = createSKU("Ubuntu-18.04");
        PagedList<VirtualMachineSku> skus = (PagedList<VirtualMachineSku>) Mockito.mock(PagedList.class);
        Mockito.when(skus.iterator()).thenReturn(Arrays.asList(sku).iterator());
        Mockito.doReturn(skus).when(this.operation).getSkusFrom(offer);

        List<String> selectedPublishers = Arrays.asList(AZURE_SELECTED_PUBLISHER);

        ImageSummary expectedImage;
        expectedImage = AzureImageOperationUtil.buildImageSummaryBy(publisher.name(), offer.name(), sku.name());

        // exercise
        Map<String, ImageSummary> images = this.operation.getImages(this.azure, selectedPublishers);

        // verify
        Mockito.verify(operation, Mockito.times(TestUtils.RUN_ONCE)).getPublishers(Mockito.eq(this.azure));
        Mockito.verify(operation, Mockito.times(TestUtils.RUN_ONCE)).getOffersFrom(Mockito.eq(publisher));
        Mockito.verify(operation, Mockito.times(TestUtils.RUN_ONCE)).getSkusFrom(Mockito.eq(offer));

        Assert.assertTrue(images.containsValue(expectedImage));
    }

    // test case: When calling getImages method if a UnexpectedException is thrown
    // when creating a id, the failed image must be not in the image map
    @Test
    public void testGetImagesFailedId() throws Exception {
        // set up
        PowerMockito.spy(AzureImageOperationUtil.class);

        VirtualMachinePublisher publisher = createPublisher(AZURE_SELECTED_PUBLISHER);
        PagedList<VirtualMachinePublisher> publishers = (PagedList<VirtualMachinePublisher>) Mockito.mock(PagedList.class);
        Mockito.when(publishers.iterator()).thenReturn(Arrays.asList(publisher).iterator());
        Mockito.doReturn(publishers).when(this.operation).getPublishers(Mockito.eq(this.azure));

        VirtualMachineOffer offer = createOffer("UbuntuServer");
        PagedList<VirtualMachineOffer> offers = (PagedList<VirtualMachineOffer>) Mockito.mock(PagedList.class);
        Mockito.when(offers.iterator()).thenReturn(Arrays.asList(offer).iterator());
        Mockito.doReturn(offers).when(this.operation).getOffersFrom(publisher);

        VirtualMachineSku sku = createSKU("Ubuntu-18.04");
        VirtualMachineSku sku2 = createSKU("FailedSKU");

        PagedList<VirtualMachineSku> skus = (PagedList<VirtualMachineSku>) Mockito.mock(PagedList.class);
        Mockito.when(skus.iterator()).thenReturn(Arrays.asList(sku, sku2).iterator());
        Mockito.doReturn(skus).when(this.operation).getSkusFrom(offer);

        List<String> selectedPublishers = Arrays.asList(AZURE_SELECTED_PUBLISHER);

        ImageSummary expectedImage;
        expectedImage = AzureImageOperationUtil.buildImageSummaryBy(publisher.name(), offer.name(), sku.name());

        ImageSummary failedImage;
        failedImage = AzureImageOperationUtil.buildImageSummaryBy(publisher.name(), offer.name(), sku2.name());

        PowerMockito.doThrow(new UnexpectedException())
                .when(AzureImageOperationUtil.class, "encode", Mockito.eq(failedImage.getId()));

        // exercise
        Map<String, ImageSummary> images = this.operation.getImages(this.azure, selectedPublishers);

        // verify
        Assert.assertTrue(images.containsValue(expectedImage));
        Assert.assertFalse(images.containsValue(failedImage));
    }

    private VirtualMachineSku createSKU(String name) {
        VirtualMachineSku sku = Mockito.mock(VirtualMachineSku.class);
        Mockito.when(sku.name()).thenReturn(name);
        return sku;
    }

    private VirtualMachineOffer createOffer(String name) {
        VirtualMachineOffer offer = Mockito.mock(VirtualMachineOffer.class);
        Mockito.when(offer.name()).thenReturn(name);
        return offer;
    }

    private VirtualMachinePublisher createPublisher(String name) {
        VirtualMachinePublisher publisher = Mockito.mock(VirtualMachinePublisher.class);
        Mockito.when(publisher.name()).thenReturn(name);
        return publisher;
    }
}
