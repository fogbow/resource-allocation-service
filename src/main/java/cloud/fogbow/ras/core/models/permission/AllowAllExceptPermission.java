package cloud.fogbow.ras.core.models.permission;

import java.util.HashSet;
import java.util.Set;

import cloud.fogbow.common.models.Permission;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;

public class AllowAllExceptPermission implements Permission<RasOperation> {

    private Set<Operation> notAllowedOperationTypes;
    
    public AllowAllExceptPermission(Set<Operation> notAllowedOperationTypes) {
        this.notAllowedOperationTypes = notAllowedOperationTypes;
    }
    
    public AllowAllExceptPermission(String permissionName) {
        this.notAllowedOperationTypes = new HashSet<Operation>();
        
        String operationTypesString = PropertiesHolder.getInstance().getProperty(permissionName + 
                SystemConstants.OPERATIONS_LIST_KEY_SUFFIX).trim();
        
        if (!operationTypesString.isEmpty()) {
            for (String operationString : operationTypesString.split(SystemConstants.OPERATION_NAME_SEPARATOR)) {
                this.notAllowedOperationTypes.add(Operation.fromString(operationString.trim()));
            }            
        }
    }

    @Override
    public boolean isAuthorized(RasOperation operation) {
        return !this.notAllowedOperationTypes.contains(operation.getOperationType());
    }
}
