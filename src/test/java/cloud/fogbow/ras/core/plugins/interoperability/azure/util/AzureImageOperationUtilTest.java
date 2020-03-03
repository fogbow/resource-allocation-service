package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.ras.api.http.response.ImageSummary;
import cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model.AzureGetImageRef;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

public class AzureImageOperationUtilTest {

    // test case: When calling the buildImageSummaryBy method,
    // it must verify if It return the right ImageSummary.
    @Test
    public void testBuildImageSummaryBySuccessfully() {
        // set up
        String publisherExpected = "publisherExpected";
        String offerExpected = "offerExpected";
        String skuExpected = "skuExpected";
        AzureGetImageRef azureVirtualMachineImage = Mockito.mock(AzureGetImageRef.class);
        Mockito.when(azureVirtualMachineImage.getPublisher()).thenReturn(publisherExpected);
        Mockito.when(azureVirtualMachineImage.getOffer()).thenReturn(offerExpected);
        Mockito.when(azureVirtualMachineImage.getSku()).thenReturn(skuExpected);

        String summaryIdExpected = new StringBuilder()
                .append(publisherExpected)
                .append(AzureImageOperationUtil.IMAGE_SUMMARY_ID_SEPARATOR)
                .append(offerExpected)
                .append(AzureImageOperationUtil.IMAGE_SUMMARY_ID_SEPARATOR)
                .append(skuExpected)
                .toString();

        String summaryNameExpected = new StringBuilder()
                .append(offerExpected)
                .append(AzureImageOperationUtil.IMAGE_SUMMARY_NAME_SEPARATOR)
                .append(skuExpected)
                .toString();

        // execute
        ImageSummary imageSummary = AzureImageOperationUtil.buildImageSummaryBy(azureVirtualMachineImage);

        // verify
        Assert.assertEquals(summaryIdExpected, imageSummary.getId());
        Assert.assertEquals(summaryNameExpected, imageSummary.getName());
    }

    // test case: When calling the buildAzureVirtualMachineImageBy method,
    // it must verify if It return the right VirtualMachineImage.
    @Test
    public void testBuildAzureVirtualMachineImageBySuccessfully() {
        // set up
        String publisherExpected = "publisherExpected";
        String offerExpected = "offerExpected";
        String skuExpected = "skuExpected";

        String summaryIdExpected = new StringBuilder()
                .append(publisherExpected)
                .append(AzureImageOperationUtil.IMAGE_SUMMARY_ID_SEPARATOR)
                .append(offerExpected)
                .append(AzureImageOperationUtil.IMAGE_SUMMARY_ID_SEPARATOR)
                .append(skuExpected)
                .toString();

        // execute
        AzureGetImageRef azureVirtualMachineImage =
                AzureImageOperationUtil.buildAzureVirtualMachineImageBy(summaryIdExpected);

        // verify
        Assert.assertEquals(publisherExpected, azureVirtualMachineImage.getPublisher());
        Assert.assertEquals(offerExpected, azureVirtualMachineImage.getOffer());
        Assert.assertEquals(skuExpected, azureVirtualMachineImage.getSku());
    }


}
