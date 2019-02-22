package cloud.fogbow.ras.api.http.response;

public class PublicKey {
    private String publicKey;

    public PublicKey() {}

    public PublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }
}
