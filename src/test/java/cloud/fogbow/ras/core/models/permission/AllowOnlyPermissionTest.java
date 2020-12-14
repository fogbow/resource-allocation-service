package cloud.fogbow.ras.core.models.permission;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class})
public class AllowOnlyPermissionTest {

    private AllowOnlyPermission permission;
    private String permissionName = "permission1";
    private String operationsNamesString;
    private List<Operation> allowedOperations = Arrays.asList(Operation.GET, 
                                                                  Operation.CREATE);
    private List<Operation> noOperation = new ArrayList<Operation>();
    
    private String provider = "member1";
    
    private void setUpTest(List<Operation> allowedOperations) {
        operationsNamesString = generateOperationNamesString(allowedOperations);
        
        // set up PropertiesHolder 
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.doReturn(operationsNamesString).when(propertiesHolder).getProperty(permissionName + 
                SystemConstants.OPERATIONS_LIST_KEY_SUFFIX);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        permission = new AllowOnlyPermission(permissionName);
    }
    
    private String generateOperationNamesString(List<Operation> operations) {
        ArrayList<String> operationsNames = new ArrayList<String>();
        for (Operation type : operations) {
            operationsNames.add(type.getValue());
        }
        return String.join(SystemConstants.OPERATION_NAME_SEPARATOR, operationsNames);
    }
    
    // test case: if the list of the allowed operations types contains 
    // the type of the operation passed as argument, the method isAuthorized must
    // return true. Otherwise, it must return false.
    @Test
    public void testIsAuthorized() {
        setUpTest(allowedOperations);

        for (Operation type : Operation.values()) {
            RasOperation operation = new RasOperation(type, ResourceType.ATTACHMENT, provider, provider);
            
            if (allowedOperations.contains(type)) {
                assertTrue(permission.isAuthorized(operation));                
            } else {
                assertFalse(permission.isAuthorized(operation));
            }
        }
    }
    
    // test case: if the list of the allowed operations is empty,
    // the method isAuthorized must always return false.
    @Test
    public void testIsAuthorizedNoAuthorizedOperation() {
        setUpTest(noOperation);
        
        for (Operation type : Operation.values()) {
            RasOperation operation = new RasOperation(type, ResourceType.ATTACHMENT, provider, provider);
            
            assertFalse(permission.isAuthorized(operation));
        }
    }
}

