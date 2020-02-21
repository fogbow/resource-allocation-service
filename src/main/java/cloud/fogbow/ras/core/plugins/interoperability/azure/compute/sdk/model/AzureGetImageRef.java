package cloud.fogbow.ras.core.plugins.interoperability.azure.compute.sdk.model;

import java.util.Objects;

public class AzureGetImageRef {

    private String publisher;
    private String offer;
    private String sku;

    public AzureGetImageRef(String publisher, String offer, String sku) {
        this.publisher = publisher;
        this.offer = offer;
        this.sku = sku;
    }

    public String getOffer() {
        return this.offer;
    }

    public String getPublisher() {
        return this.publisher;
    }

    public String getSku() {
        return this.sku;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AzureGetImageRef that = (AzureGetImageRef) o;
        return Objects.equals(this.publisher, that.publisher) &&
                Objects.equals(this.offer, that.offer) &&
                Objects.equals(this.sku, that.sku);
    }

}
