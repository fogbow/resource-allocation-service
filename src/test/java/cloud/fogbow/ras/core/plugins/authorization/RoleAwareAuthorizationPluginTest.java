package cloud.fogbow.ras.core.plugins.authorization;

import static org.junit.Assert.assertTrue;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.UnauthorizedRequestException;
import cloud.fogbow.common.exceptions.WrongPolicyTypeException;
import cloud.fogbow.common.models.SystemUser;
import cloud.fogbow.common.models.policy.XMLRolePolicy;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.PolicyInstantiator;
import cloud.fogbow.ras.core.PropertiesHolder;
import cloud.fogbow.ras.core.models.Operation;
import cloud.fogbow.ras.core.models.RasOperation;
import cloud.fogbow.ras.core.models.ResourceType;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PropertiesHolder.class)
public class RoleAwareAuthorizationPluginTest {
    
    /*
     * user 1 can perform get, but not create or reload operations
     * user 2 can perform get and create, but not reload operations
     * userWithDefaultRole can perform get, but not create or reload operations
     */
    private String policyFileName = "policy.xml";
    private String policyFilePath = HomeDir.getPath() + policyFileName;
    
    private String newPolicyString = "policy";

    private String expectedPolicyType = "role";
    private String wrongPolicyType = "provider";
    
    private String identityProviderId = "provider";
    private String remoteProviderId = "remoteProvider";
    
    private String userId1 = "userId1";
    private String userId2 = "userId2";
    private String userIdWithDefaultRoles = "userIdWithDefaultRole";
    private String userIdRemoteProvider = "userIdRemoteProvider";
    
    private String userName1 = "user1";
    private String userName2 = "user2";
    private String userWithDefaultRole = "user3";
    private String userRemoteProvider = "userRemote";
        
    private String userId1Pair = String.format(RoleAwareAuthorizationPlugin.USER_NAME_PROVIDER_PAIR_CONFIGURATION_FORMAT, 
            userId1, identityProviderId);
    private String userId2Pair = String.format(RoleAwareAuthorizationPlugin.USER_NAME_PROVIDER_PAIR_CONFIGURATION_FORMAT, 
            userId2, identityProviderId);
    private String userIdDefaultRolesPair = String.format(RoleAwareAuthorizationPlugin.USER_NAME_PROVIDER_PAIR_CONFIGURATION_FORMAT, 
            userIdWithDefaultRoles, identityProviderId);
    private String userIdRemoteProviderPair = String.format(RoleAwareAuthorizationPlugin.USER_NAME_PROVIDER_PAIR_CONFIGURATION_FORMAT, 
            userIdRemoteProvider, remoteProviderId);
    
    private RoleAwareAuthorizationPlugin plugin;
    private PropertiesHolder propertiesHolder;

    private RasOperation operationGet;
    private RasOperation operationCreate;
    private RasOperation operationReload;
    
    private PolicyInstantiator policyInstantiator;
    private XMLRolePolicy<RasOperation> rolePolicy;
    private XMLRolePolicy<RasOperation> newRolePolicy;
    private XMLRolePolicy<RasOperation> updatedRolePolicy;
    
    @Before
    public void setUp() throws ConfigurationErrorException, WrongPolicyTypeException {
        // set up PropertiesHolder 
        PowerMockito.mockStatic(PropertiesHolder.class);
        this.propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.doReturn(policyFileName).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY);
        Mockito.doReturn(identityProviderId).when(propertiesHolder).getProperty(ConfigurationPropertyKeys.PROVIDER_ID_KEY);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        // set up PolicyInstantiator
        this.policyInstantiator = Mockito.mock(PolicyInstantiator.class);
        this.rolePolicy = Mockito.mock(XMLRolePolicy.class);
        Mockito.when(this.policyInstantiator.getRolePolicyInstanceFromFile(policyFilePath)).thenReturn(rolePolicy);
        
        // set up operations
        this.operationGet = new RasOperation(Operation.GET, ResourceType.ATTACHMENT, 
                identityProviderId, identityProviderId);
        this.operationCreate = new RasOperation(Operation.CREATE, ResourceType.ATTACHMENT, 
                identityProviderId, identityProviderId);
        this.operationReload = new RasOperation(Operation.RELOAD, ResourceType.CONFIGURATION, 
                identityProviderId, identityProviderId);
        
        // set up RolePolicy
        Mockito.when(this.rolePolicy.userIsAuthorized(userId1Pair, operationGet)).thenReturn(true);
        Mockito.when(this.rolePolicy.userIsAuthorized(userId1Pair, operationCreate)).thenReturn(false);
        Mockito.when(this.rolePolicy.userIsAuthorized(userId1Pair, operationReload)).thenReturn(false);
        
        Mockito.when(this.rolePolicy.userIsAuthorized(userId2Pair, operationGet)).thenReturn(true);
        Mockito.when(this.rolePolicy.userIsAuthorized(userId2Pair, operationCreate)).thenReturn(true);
        Mockito.when(this.rolePolicy.userIsAuthorized(userId2Pair, operationReload)).thenReturn(false);
        
        Mockito.when(this.rolePolicy.userIsAuthorized(userIdDefaultRolesPair, operationGet)).thenReturn(true);
        Mockito.when(this.rolePolicy.userIsAuthorized(userIdDefaultRolesPair, operationCreate)).thenReturn(false);
        Mockito.when(this.rolePolicy.userIsAuthorized(userIdDefaultRolesPair, operationReload)).thenReturn(false);
        
        Mockito.when(this.rolePolicy.userIsAuthorized(userIdRemoteProviderPair, operationGet)).thenReturn(true);
        Mockito.when(this.rolePolicy.userIsAuthorized(userIdRemoteProviderPair, operationCreate)).thenReturn(false);
        Mockito.when(this.rolePolicy.userIsAuthorized(userIdRemoteProviderPair, operationReload)).thenReturn(false);
        
        this.plugin = new RoleAwareAuthorizationPlugin(this.policyInstantiator);
    }

    @Test
    public void constructorReadsConfigurationCorrectly() throws ConfigurationErrorException {
        Mockito.verify(this.rolePolicy, Mockito.atLeastOnce()).validate();
        
        Mockito.verify(propertiesHolder, Mockito.times(1)).getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY);
        PowerMockito.verifyStatic(PropertiesHolder.class, Mockito.atLeastOnce());
    }

    @Test
    public void testIsAuthorized() throws UnauthorizedRequestException {
        SystemUser user1 = new SystemUser(userId1, userName1, identityProviderId);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGet));
        assertIsAuthorizedThrowsException(user1, operationCreate);
        assertIsAuthorizedThrowsException(user1, operationReload);

        SystemUser user2 = new SystemUser(userId2, userName2, identityProviderId);
        
        assertTrue(this.plugin.isAuthorized(user2, operationGet));
        assertTrue(this.plugin.isAuthorized(user2, operationCreate));
        assertIsAuthorizedThrowsException(user2, operationReload);
        
        SystemUser userWithDefaultRoles = new SystemUser(userIdWithDefaultRoles, userWithDefaultRole, identityProviderId);

        assertTrue(this.plugin.isAuthorized(userWithDefaultRoles, operationGet));
        assertIsAuthorizedThrowsException(userWithDefaultRoles, operationCreate);
        assertIsAuthorizedThrowsException(userWithDefaultRoles, operationReload);
    }
    
    @Test
    public void testRemoteOperationsAreAlwaysAuthorized() throws UnauthorizedRequestException {
        this.operationGet = new RasOperation(Operation.GET, ResourceType.ATTACHMENT, 
                this.identityProviderId, remoteProviderId);
        this.operationCreate = new RasOperation(Operation.CREATE, ResourceType.ATTACHMENT, 
                this.identityProviderId, remoteProviderId);
        this.operationReload = new RasOperation(Operation.RELOAD, ResourceType.CONFIGURATION, 
                this.identityProviderId, remoteProviderId);
        
        SystemUser user1 = new SystemUser(userId1, userName1, identityProviderId);
        SystemUser user2 = new SystemUser(userId2, userName2, identityProviderId);
        SystemUser remoteUser = new SystemUser(userIdRemoteProvider, userRemoteProvider, remoteProviderId);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGet));
        assertTrue(this.plugin.isAuthorized(user1, operationCreate));
        assertTrue(this.plugin.isAuthorized(user1, operationReload));
        
        assertTrue(this.plugin.isAuthorized(user2, operationGet));
        assertTrue(this.plugin.isAuthorized(user2, operationCreate));
        assertTrue(this.plugin.isAuthorized(user2, operationReload));
        
        assertTrue(this.plugin.isAuthorized(remoteUser, operationGet));
        assertTrue(this.plugin.isAuthorized(remoteUser, operationCreate));
        assertTrue(this.plugin.isAuthorized(remoteUser, operationReload));
    }
    
    // test case: when calling the setPolicy method with a valid policy string, 
    // it must call PolicyInstantiator to create a new policy instance, validate
    // and persist the new instance and use this new instance to authorize operations.
    @Test
    public void testSetPolicy() throws ConfigurationErrorException, WrongPolicyTypeException, UnauthorizedRequestException {
        this.newRolePolicy = Mockito.mock(XMLRolePolicy.class);
        Mockito.when(this.newRolePolicy.userIsAuthorized(userId1Pair, operationGet)).thenReturn(false);
        Mockito.when(this.policyInstantiator.getRolePolicyInstance(newPolicyString)).thenReturn(newRolePolicy);
        
        SystemUser user1 = new SystemUser(userId1, userName1, identityProviderId);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGet));
        
        this.plugin.setPolicy(newPolicyString);
        
        assertIsAuthorizedThrowsException(user1, operationGet);
        Mockito.verify(this.newRolePolicy, Mockito.atLeastOnce()).validate();
        Mockito.verify(this.newRolePolicy, Mockito.atLeastOnce()).save();
    }
    
    // test case: when calling the setPolicy method with a wrong policy type string,
    // it must handle the WrongPolicyTypeException and not change the policy it uses
    // to authorize operations.
    @Test
    public void testSetPolicyWrongPolicyType() throws ConfigurationErrorException, WrongPolicyTypeException, UnauthorizedRequestException {
        this.newRolePolicy = Mockito.mock(XMLRolePolicy.class);
        WrongPolicyTypeException exception = new WrongPolicyTypeException(expectedPolicyType, wrongPolicyType);
        Mockito.when(this.policyInstantiator.getRolePolicyInstance(newPolicyString)).thenThrow(exception);
        
        SystemUser user1 = new SystemUser(userId1, userName1, identityProviderId);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGet));
        
        this.plugin.setPolicy(newPolicyString);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGet));
        Mockito.verify(this.newRolePolicy, Mockito.never()).validate();
    }
    
    // test case: when calling the setPolicy method and the validation
    // fails, it must throw a ConfigurationErrorException. 
    @Test(expected = ConfigurationErrorException.class)
    public void testSetInvalidPolicy() throws ConfigurationErrorException, WrongPolicyTypeException, UnauthorizedRequestException {
        this.newRolePolicy = Mockito.mock(XMLRolePolicy.class);
        ConfigurationErrorException exception = new ConfigurationErrorException();
        Mockito.when(this.newRolePolicy.userIsAuthorized(userId1Pair, operationGet)).thenReturn(false);
        Mockito.when(this.policyInstantiator.getRolePolicyInstance(newPolicyString)).thenReturn(newRolePolicy);
        Mockito.doThrow(exception).when(this.newRolePolicy).validate();
        
        SystemUser user1 = new SystemUser(userId1, userName1, identityProviderId);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGet));
        
        this.plugin.setPolicy(newPolicyString);
    }
    
    // test case: when calling the updatePolicy method with a valid policy string, 
    // it must call PolicyInstantiator to create a new policy instance from the policy string, 
    // create a copy of the policy it uses, update, validate and save this copy. Then, use this
    // new instance to authorize operations.
    @Test
    public void testUpdatePolicy() throws ConfigurationErrorException, WrongPolicyTypeException, UnauthorizedRequestException {
        this.newRolePolicy = Mockito.mock(XMLRolePolicy.class);
        this.updatedRolePolicy = Mockito.mock(XMLRolePolicy.class);
        Mockito.when(this.rolePolicy.copy()).thenReturn(updatedRolePolicy);
        Mockito.when(this.updatedRolePolicy.userIsAuthorized(userId1Pair, operationGet)).thenReturn(false);
        
        Mockito.when(this.policyInstantiator.getRolePolicyInstance(newPolicyString)).thenReturn(this.newRolePolicy);
        
        SystemUser user1 = new SystemUser(userId1, userName1, identityProviderId);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGet));
        
        this.plugin.updatePolicy(newPolicyString);
        
        assertIsAuthorizedThrowsException(user1, operationGet);

        Mockito.verify(this.updatedRolePolicy, Mockito.atLeastOnce()).update(this.newRolePolicy);
        Mockito.verify(this.updatedRolePolicy, Mockito.atLeastOnce()).validate();
        Mockito.verify(this.updatedRolePolicy, Mockito.atLeastOnce()).save();
    }
    
    // test case: when calling the updatePolicy method with a wrong policy type string,
    // it must handle the WrongPolicyTypeException and not change the policy it uses
    // to authorize operations.
    @Test
    public void testUpdatePolicyWrongPolicyType() throws ConfigurationErrorException, WrongPolicyTypeException, UnauthorizedRequestException {
        this.newRolePolicy = Mockito.mock(XMLRolePolicy.class);
        WrongPolicyTypeException exception = new WrongPolicyTypeException(expectedPolicyType, wrongPolicyType);
        Mockito.when(this.policyInstantiator.getRolePolicyInstance(newPolicyString)).thenThrow(exception);
        
        SystemUser user1 = new SystemUser(userId1, userName1, identityProviderId);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGet));
        
        this.plugin.updatePolicy(newPolicyString);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGet));
    }
    
    // test case: when calling the updatePolicy method and the validation
    // fails, it must throw a ConfigurationErrorException. 
    @Test(expected = ConfigurationErrorException.class)
    public void testUpdateInvalidPolicy() throws ConfigurationErrorException, WrongPolicyTypeException, UnauthorizedRequestException {
        this.newRolePolicy = Mockito.mock(XMLRolePolicy.class);
        this.updatedRolePolicy = Mockito.mock(XMLRolePolicy.class);
        ConfigurationErrorException exception = new ConfigurationErrorException();
        
        Mockito.when(this.rolePolicy.copy()).thenReturn(updatedRolePolicy);
        Mockito.doThrow(exception).when(this.updatedRolePolicy).validate();
        
        Mockito.when(this.policyInstantiator.getRolePolicyInstance(newPolicyString)).thenReturn(this.newRolePolicy);
        
        SystemUser user1 = new SystemUser(userId1, userName1, identityProviderId);
        
        assertTrue(this.plugin.isAuthorized(user1, operationGet));
        
        this.plugin.updatePolicy(newPolicyString);
    }
    
    private void assertIsAuthorizedThrowsException(SystemUser user, RasOperation operation) {
        try {
            this.plugin.isAuthorized(user, operation);
            Assert.fail("isAuthorized call should fail.");
        } catch (UnauthorizedRequestException e) {

        }
    }
}
