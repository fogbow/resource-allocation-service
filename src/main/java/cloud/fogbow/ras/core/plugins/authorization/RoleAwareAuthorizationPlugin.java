package cloud.fogbow.ras.core.plugins.authorization;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.plugins.authorization.AuthorizationPlugin;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.PolicyInstantiator;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.RolePolicy;
import cloud.fogbow.ras.core.models.policy.WrongPolicyTypeException;

public class RoleAwareAuthorizationPlugin implements AuthorizationPlugin<RasOperation> {

	// TODO documentation
    public static final String USER_NAME_PROVIDER_PAIR_CONFIGURATION_FORMAT = "%s.%s";
    private PolicyInstantiator policyInstantiator;
    private RolePolicy rolePolicy;
    
    public RoleAwareAuthorizationPlugin() throws ConfigurationErrorException {
        this(new PolicyInstantiator());
    }
    
    public RoleAwareAuthorizationPlugin(PolicyInstantiator policyInstantiator) throws ConfigurationErrorException {
        this.policyInstantiator = policyInstantiator;
        String policyFileName = PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY);
        try {
            this.rolePolicy = policyInstantiator.getRolePolicyInstanceFromFile(policyFileName);
        } catch (WrongPolicyTypeException e) {
            // TODO add message
            throw new ConfigurationErrorException();
        }
        this.rolePolicy.validate();
    }
    
    @Override
    public boolean isAuthorized(SystemUser systemUser, RasOperation operation) throws UnauthorizedRequestException {
        // check permissions only for local operations
        if (operationIsLocal(operation)) {
            if (!this.rolePolicy.userIsAuthorized(getUserConfigurationString(systemUser), operation)) {
                throw new UnauthorizedRequestException(Messages.Exception.USER_DOES_NOT_HAVE_ENOUGH_PERMISSION);                
            }
        }
        
        return true;
    }

    private boolean operationIsLocal(RasOperation operation) {
        return operation.getProvider().equals(PropertiesHolder.getInstance().getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY));
    }

    private String getUserConfigurationString(SystemUser systemUser) {
        return String.format(USER_NAME_PROVIDER_PAIR_CONFIGURATION_FORMAT, systemUser.getId(), systemUser.getIdentityProviderId());
    }

	@Override
	public void setPolicy(String policyString) throws ConfigurationErrorException {
		try {
		    RolePolicy policy = policyInstantiator.getRolePolicyInstance(policyString);
			policy.validate();

			this.rolePolicy = policy;
		} catch (WrongPolicyTypeException e) {
			e.printStackTrace();
		}
	}

    @Override
    public void updatePolicy(String policyString) throws ConfigurationErrorException {
        try {
            RolePolicy policy = policyInstantiator.getRolePolicyInstance(policyString);
            RolePolicy base = this.rolePolicy.copy();

            base.update(policy);
            base.validate();

            this.rolePolicy = base;
        } catch (WrongPolicyTypeException e) {
            e.printStackTrace();
        }
    }
}
