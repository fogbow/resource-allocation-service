package org.fogbowcloud.manager.core.controllers;

import org.fogbowcloud.manager.core.BaseUnitTests;
import org.fogbowcloud.manager.core.exceptions.OrderManagementException;
import org.fogbowcloud.manager.core.models.orders.ComputeOrder;
import org.fogbowcloud.manager.core.models.token.Token;
import org.fogbowcloud.manager.core.plugins.identity.exceptions.UnauthorizedException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class OrdersManagerControllerTest extends BaseUnitTests {

    private OrdersManagerController ordersManagerController;

    @Before
    public void setUp() {
        this.ordersManagerController = new OrdersManagerController();
    }

    @Test
    public void testNewOrderRequest() {
        try {
            OrdersManagerController ordersManagerController = new OrdersManagerController();
            ComputeOrder computeOrder = new ComputeOrder();
            Token localToken = getFakeToken();
            Token federationToken = getFakeToken();
            ordersManagerController.newOrderRequest(computeOrder, localToken, federationToken);
        } catch (OrderManagementException e) {
            Assert.fail();
        }
    }

    @Test
    public void testFailedNewOrderRequestOrderIsNull() {
        try {
            ComputeOrder computeOrder = null;
            Token localToken = getFakeToken();
            Token federationToken = getFakeToken();
            this.ordersManagerController.newOrderRequest(computeOrder, localToken, federationToken);
        } catch (OrderManagementException e) {
            String expectedErrorMessage = "Can't process new order request. Order reference is null.";
            Assert.assertEquals(e.getMessage(), expectedErrorMessage);
        } catch (Exception e) {
            Assert.fail();
        }
    }

    private Token getFakeToken() {
        String fakeAccessId = "0000";
        String fakeUserId = "userId";
        String fakeUserName = "userName";
        Token.User fakeUser = new Token.User(fakeUserId, fakeUserName);
        Date fakeExpirationTime = new Date();
        Map<String, String> fakeAttributes = new HashMap<>();
        return  new Token(fakeAccessId, fakeUser, fakeExpirationTime, fakeAttributes);
    }

}