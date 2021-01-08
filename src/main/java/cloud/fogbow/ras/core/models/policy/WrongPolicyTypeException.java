package cloud.fogbow.ras.core.models.policy;

public class WrongPolicyTypeException extends Exception {

    private static final long serialVersionUID = 1L;
    
    private String expectedType;
    private String currentType;
    
    public WrongPolicyTypeException(String expectedType, String currentType) {
        this.expectedType = expectedType;
        this.currentType = currentType;
    }
    
    public String getExpectedType() {
        return expectedType;
    }

    public String getCurrentType() {
        return currentType;
    }
}
