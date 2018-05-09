package org.fogbowcloud.manager.core.rest.services;

import org.fogbowcloud.manager.core.controllers.ApplicationController;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.orders.Order;
import org.fogbowcloud.manager.core.plugins.IdentityPlugin;
import org.fogbowcloud.manager.core.plugins.identity.ldap.LdapIdentityPlugin;
import org.fogbowcloud.manager.core.services.AuthenticationService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.junit.Assert;

import java.util.Properties;

@RunWith(SpringRunner.class)
@WebAppConfiguration
public class ComputeOrdersServiceTest {

   private ComputeOrdersService computeOrdersService;
   private ApplicationController applicationController;

    @Before
    public void setUp() {
        IdentityPlugin ldapIdentityPlugin = new LdapIdentityPlugin(new Properties());
        AuthenticationService authenticationController = new AuthenticationService(ldapIdentityPlugin);
        applicationController = ApplicationController.getInstance();
        applicationController.setAuthenticationController(authenticationController);
        computeOrdersService = new ComputeOrdersService();
    }

    @Test
    public void testCreatedComputeOrder() {
        ComputeOrder computeOrder = new ComputeOrder();
        String fakeAccessId = "99898";
        String fakeLocalTokenId = "85984034";
        ResponseEntity<Order> response = computeOrdersService.createCompute(computeOrder, fakeAccessId, fakeLocalTokenId);
        Assert.assertEquals(response.getStatusCode(), HttpStatus.CREATED);
    }

}