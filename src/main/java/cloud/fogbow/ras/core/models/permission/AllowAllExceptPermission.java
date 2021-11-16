package cloud.fogbow.ras.core.models.permission;

import java.util.HashSet;
import java.util.Set;

import cloud.fogbow.common.models.policy.Permission;
import cloud.fogbow.ras.constants.SystemConstants;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;

public class AllowAllExceptPermission implements Permission<RasOperation> {

	private String name;
    private Set<Operation> notAllowedOperationTypes;
    
    public AllowAllExceptPermission() {
        
    }
    
    public AllowAllExceptPermission(Set<Operation> notAllowedOperationTypes) {
        this.notAllowedOperationTypes = notAllowedOperationTypes;
    }
    
    public AllowAllExceptPermission(String name, Set<Operation> notAllowedOperationTypes) {
    	this.name = name;
        this.notAllowedOperationTypes = notAllowedOperationTypes;
    }
    
    public AllowAllExceptPermission(String permissionName) {
        this.notAllowedOperationTypes = new HashSet<Operation>();
        this.name = permissionName;
        
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
    
    @Override
    public boolean equals(Object o) {
    	if (o instanceof AllowAllExceptPermission) {
    		return this.name.equals(((AllowAllExceptPermission) o).name);
		}
    	
    	return false;
    }
    
    @Override
    public Set<String> getOperationsTypes() {
        HashSet<String> operationsStrings = new HashSet<String>(); 
        
        for (Operation operation : notAllowedOperationTypes) {
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

        this.notAllowedOperationTypes = rasOperations;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }
}
