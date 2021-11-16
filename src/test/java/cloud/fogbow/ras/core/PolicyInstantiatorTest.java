package cloud.fogbow.ras.core;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.BDDMockito;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import cloud.fogbow.common.exceptions.ConfigurationErrorException;
import cloud.fogbow.common.exceptions.WrongPolicyTypeException;
import cloud.fogbow.common.models.policy.PermissionInstantiator;
import cloud.fogbow.common.models.policy.RolePolicy;
import cloud.fogbow.common.util.HomeDir;
import cloud.fogbow.ras.constants.ConfigurationPropertyKeys;
import cloud.fogbow.ras.core.models.RasOperation;


@RunWith(PowerMockRunner.class)
@PrepareForTest({PropertiesHolder.class, HomeDir.class})
public class PolicyInstantiatorTest {

    private static final String ADMIN_ROLE = "admin";
    private static final String POLICY_FILE = "policy_file";
    private static final String POLICY_CLASS = "policy_class";
    private static final String HOME_DIR_PATH = "home_dir_path/";
    private static final String POLICY_STRING = "policy";
    private RasClassFactory classFactory;
    private Properties properties;
    private PropertiesHolder propertiesHolder;
    private PermissionInstantiator<RasOperation> permissionInstantiator;
    private RolePolicy<RasOperation> policy;

    // TODO documentation
    @Test
    public void testConstructorReadsConfiguration() throws ConfigurationErrorException {
        setUpConfiguration();
        

        new PolicyInstantiator(classFactory, permissionInstantiator);
        
        
        Mockito.verify(propertiesHolder).getProperty(ConfigurationPropertyKeys.ADMIN_ROLE);
        Mockito.verify(propertiesHolder).getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY);
        Mockito.verify(propertiesHolder).getProperty(ConfigurationPropertyKeys.POLICY_CLASS_KEY);
        
        PowerMockito.verifyStatic(HomeDir.class);
        HomeDir.getPath();
    }

    // TODO documentation
    @Test(expected = ConfigurationErrorException.class)
    public void testConstructorConfigurationDoesNotSpecifyAdminRole() throws ConfigurationErrorException {
        setUpConfiguration();
        
        Mockito.when(properties.containsKey(ConfigurationPropertyKeys.ADMIN_ROLE)).thenReturn(false);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.ADMIN_ROLE)).thenReturn(null);

        new PolicyInstantiator(classFactory, permissionInstantiator);
    }
    
    // TODO documentation
    @Test(expected = ConfigurationErrorException.class)
    public void testConstructorConfigurationDoesNotSpecifyPolicyFile() throws ConfigurationErrorException {
        setUpConfiguration();
        
        Mockito.when(properties.containsKey(ConfigurationPropertyKeys.POLICY_FILE_KEY)).thenReturn(false);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY)).thenReturn(null);

        new PolicyInstantiator(classFactory, permissionInstantiator);
    }
    
    // TODO documentation
    @Test(expected = ConfigurationErrorException.class)
    public void testConstructorConfigurationDoesNotSpecifyPolicyClass() throws ConfigurationErrorException {
        setUpConfiguration();
        
        Mockito.when(properties.containsKey(ConfigurationPropertyKeys.POLICY_CLASS_KEY)).thenReturn(false);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.POLICY_CLASS_KEY)).thenReturn(null);

        new PolicyInstantiator(classFactory, permissionInstantiator);
    }
    
    // TODO documentation
    @Test
    public void testGetRolePolicyInstance() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpConfiguration();

        Mockito.when(classFactory.createPluginInstance(POLICY_CLASS, permissionInstantiator, POLICY_STRING,
                ADMIN_ROLE, HOME_DIR_PATH + POLICY_FILE)).thenReturn(policy);
        PolicyInstantiator policyInstantiator = new PolicyInstantiator(classFactory, permissionInstantiator);
        
        
        RolePolicy<RasOperation> returnedPolicy = policyInstantiator.getRolePolicyInstance(POLICY_STRING);
        
        
        assertEquals(policy, returnedPolicy);
        Mockito.verify(classFactory).createPluginInstance(POLICY_CLASS, permissionInstantiator, POLICY_STRING,
                ADMIN_ROLE, HOME_DIR_PATH + POLICY_FILE);
    }
    
    // TODO documentation
    @Test
    public void testGetRolePolicyInstanceFromFile() throws ConfigurationErrorException, WrongPolicyTypeException {
        setUpConfiguration();
        
        Mockito.when(classFactory.createPluginInstance(POLICY_CLASS, permissionInstantiator, 
                new File(HOME_DIR_PATH + POLICY_FILE), ADMIN_ROLE, HOME_DIR_PATH + POLICY_FILE)).thenReturn(policy);
        PolicyInstantiator policyInstantiator = new PolicyInstantiator(classFactory, permissionInstantiator);
        
        
        RolePolicy<RasOperation> returnedPolicy = policyInstantiator.getRolePolicyInstanceFromFile(HOME_DIR_PATH + POLICY_FILE);
        
        
        assertEquals(policy, returnedPolicy);
        Mockito.verify(classFactory).createPluginInstance(POLICY_CLASS, permissionInstantiator, new File(HOME_DIR_PATH + POLICY_FILE),
                ADMIN_ROLE, HOME_DIR_PATH + POLICY_FILE);
    }
    
    private void setUpConfiguration() {
        properties = Mockito.mock(Properties.class);
        Mockito.when(properties.containsKey(ConfigurationPropertyKeys.ADMIN_ROLE)).thenReturn(true);
        Mockito.when(properties.containsKey(ConfigurationPropertyKeys.POLICY_FILE_KEY)).thenReturn(true);
        Mockito.when(properties.containsKey(ConfigurationPropertyKeys.POLICY_CLASS_KEY)).thenReturn(true);
        
        propertiesHolder = Mockito.mock(PropertiesHolder.class);
        Mockito.when(propertiesHolder.getProperties()).thenReturn(properties);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.ADMIN_ROLE)).thenReturn(ADMIN_ROLE);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.POLICY_FILE_KEY)).thenReturn(POLICY_FILE);
        Mockito.when(propertiesHolder.getProperty(ConfigurationPropertyKeys.POLICY_CLASS_KEY)).thenReturn(POLICY_CLASS);
        
        PowerMockito.mockStatic(PropertiesHolder.class);
        BDDMockito.given(PropertiesHolder.getInstance()).willReturn(propertiesHolder);
        
        PowerMockito.mockStatic(HomeDir.class);
        BDDMockito.given(HomeDir.getPath()).willReturn(HOME_DIR_PATH);
        
        permissionInstantiator = Mockito.mock(PermissionInstantiator.class);
        
        policy = Mockito.mock(RolePolicy.class);
        classFactory = Mockito.mock(RasClassFactory.class);
    }

}
