package cloud.fogbow.ras.core.models.permission;

import java.util.HashSet;
import java.util.Set;

import cloud.fogbow.common.models.policy.Permission;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;

public class AllowOnlyPermission implements Permission<RasOperation> {

	private String name;
    private Set<Operation> allowedOperationTypes;
    
    public AllowOnlyPermission(Set<Operation> allowedOperationTypes) {
        this.allowedOperationTypes = allowedOperationTypes;
    }
    
    public AllowOnlyPermission(String name, Set<Operation> allowedOperationTypes) {
    	this.name = name;
        this.allowedOperationTypes = allowedOperationTypes;
    }
    
    public AllowOnlyPermission(String permissionName) {
    	this.name = permissionName;
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
    
    @Override
    public boolean equals(Object o) {
    	if (o instanceof AllowOnlyPermission) {
    		return this.name.equals(((AllowOnlyPermission) o).name);
		}
    	
    	return false;
    }
    
    @Override
    public Set<String> getOperationsTypes() {
        HashSet<String> operationsStrings = new HashSet<String>(); 
        
        for (Operation operation : allowedOperationTypes) {
            operationsStrings.add(operation.getValue());
        }
        
        return operationsStrings;
    }

    // TODO test
    @Override
    public void setOperationTypes(Set<String> operations) {
        Set<Operation> rasOperations = new HashSet<Operation>();
        
        for (String operationName : operations) {
            rasOperations.add(Operation.fromString(operationName));
        }
        
        this.allowedOperationTypes = rasOperations;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
