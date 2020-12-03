package cloud.fogbow.ras.core.models.permission;

import java.util.HashSet;
import java.util.Set;

import cloud.fogbow.common.models.Permission;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;

public class AllowOnlyPermission implements Permission<RasOperation> {

    private Set<Operation> allowedOperationTypes;
    
    public AllowOnlyPermission(Set<Operation> allowedOperationTypes) {
        this.allowedOperationTypes = allowedOperationTypes;
    }
    
    public AllowOnlyPermission(String permissionName) {
        this.allowedOperationTypes = new HashSet<Operation>();
        
        String operationTypesString = PropertiesHolder.getInstance().getProperty(permissionName + 
                SystemConstants.OPERATIONS_LIST_KEY_SUFFIX).trim();
        
        if (!operationTypesString.isEmpty()) {
            for (String operationString : operationTypesString.split(SystemConstants.OPERATION_NAME_SEPARATOR)) {
                this.allowedOperationTypes.add(Operation.fromString(operationString.trim()));
            }
        }
    }

    @Override
    public boolean isAuthorized(RasOperation operation) {
        return this.allowedOperationTypes.contains(operation.getOperationType());
    }
}
