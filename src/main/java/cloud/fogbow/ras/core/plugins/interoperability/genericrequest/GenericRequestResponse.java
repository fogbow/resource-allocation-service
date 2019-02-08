package cloud.fogbow.ras.core.plugins.interoperability.genericrequest;

import java.util.Objects;

public class GenericRequestResponse {
    private String content;

    public GenericRequestResponse(String content) {
        this.content = content;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GenericRequestResponse that = (GenericRequestResponse) o;
        return Objects.equals(content, that.content);
    }

    @Override
    public int hashCode() {
        return Objects.hash(content);
    }
}
