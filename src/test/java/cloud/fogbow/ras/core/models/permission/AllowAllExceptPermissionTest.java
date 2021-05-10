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
public class AllowAllExceptPermissionTest {

    private AllowAllExceptPermission permission;
    private String permissionName = "permission1";
    private String operationsNamesString;
    private List<Operation> notAllowedOperations = Arrays.asList(Operation.RELOAD, 
                                                                  Operation.CREATE);
    private List<Operation> noOperation = new ArrayList<Operation>();
    
    private String provider = "member1";
    
    private void setUpVariables(List<Operation> operations) {
        operationsNamesString = generateOperationNamesString(operations);
        
        // set up PropertiesHolder 
        PowerMockito.mockStatic(PropertiesHolder.class);
        PropertiesHolder propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.doReturn(operationsNamesString).when(propertiesHolder).getProperty(permissionName + 
                SystemConstants.OPERATIONS_LIST_KEY_SUFFIX);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        permission = new AllowAllExceptPermission(permissionName);
    }
    
    private String generateOperationNamesString(List<Operation> operations) {
        ArrayList<String> operationsNames = new ArrayList<String>();
        for (Operation type : operations) {
            operationsNames.add(type.getValue());
        }
        return String.join(SystemConstants.OPERATION_NAME_SEPARATOR, operationsNames);
    }
    
    // test case: if the list of the not allowed operations types contains 
    // the type of the operation passed as argument, the method isAuthorized must
    // return false. Otherwise, it must return true.
    @Test
    public void testIsAuthorized() {
        setUpVariables(notAllowedOperations);
        
        for (Operation type : Operation.values()) {
            RasOperation operation = new RasOperation(type, ResourceType.ATTACHMENT, provider, provider);
            
            if (notAllowedOperations.contains(type)) {
                assertFalse(permission.isAuthorized(operation));                
            } else {
                assertTrue(permission.isAuthorized(operation));
            }
        }
    }
    
    // test case: if the list of the not allowed operations is empty,
    // the method isAuthorized must always return true.
    @Test
    public void testIsAuthorizedAllOperationsAreAuthorized() {
        setUpVariables(noOperation);
        
        for (Operation type : Operation.values()) {
            RasOperation operation = new RasOperation(type, ResourceType.ATTACHMENT, provider, provider);
            assertTrue(permission.isAuthorized(operation));
        }
    }
}

