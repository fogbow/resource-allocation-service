package cloud.fogbow.ras.core.plugins.interoperability.cloudstack.securityrule.v4_9;

import cloud.fogbow.common.exceptions.FogbowException;
import cloud.fogbow.ras.core.plugins.interoperability.cloudstack.CloudstackTestUtils;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;

public class ListFirewallRulesResponseTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // test case: When calling the fromJson method, it must verify if It returns the right ListFirewallRulesResponse.
    @Test
    public void testFromJsonSuccessfully() throws Exception {
        // set up
        String instanceId = "1";
        String protocol = "protocol";
        int startPort = 1;
        int endPort = 2;
        String ipaddress = "ip";
        String cird = "cird";
        String listFirewallRulesResponseJson = CloudstackTestUtils.createListFirewallRulesResponseJson(
                instanceId, protocol, startPort, endPort, ipaddress, cird);

        // execute
        ListFirewallRulesResponse listFirewallRulesResponse =
                ListFirewallRulesResponse.fromJson(listFirewallRulesResponseJson);

        // verify
        ListFirewallRulesResponse.SecurityRuleResponse firstSecurityRule =
                listFirewallRulesResponse.getSecurityRulesResponse().listIterator().next();
        Assert.assertEquals(instanceId, firstSecurityRule.getInstanceId());
        Assert.assertEquals(ipaddress, firstSecurityRule.getIpAddress());
        Assert.assertEquals(protocol, firstSecurityRule.getProtocol());
        Assert.assertEquals(startPort, firstSecurityRule.getPortFrom());
        Assert.assertEquals(endPort, firstSecurityRule.getPortTo());
        Assert.assertEquals(cird, firstSecurityRule.getCidr());
    }

    // test case: When calling the fromJson method with empty json response,
    // it must verify if It returns the ListFirewallRulesResponse with null values.
    @Test
    public void testFromJsonWithEmptyValue() throws Exception {
        // set up
        String listFirewallRulesEmptyResponseJson = CloudstackTestUtils
                .createListFirewallRulesEmptyResponseJson();

        // execute
        ListFirewallRulesResponse listFirewallRulesResponse =
                ListFirewallRulesResponse.fromJson(listFirewallRulesEmptyResponseJson);

        // verify
        Assert.assertTrue(listFirewallRulesResponse.getSecurityRulesResponse().isEmpty());
    }

    // test case: When calling the fromJson method with error json response,
    // it must verify if It throws a FogbowException.
    @Test
    public void testFromJsonFail() throws IOException, FogbowException {
        // set up
        String errorText = "anyString";
        int errorCode = HttpStatus.SC_BAD_REQUEST;
        String volumesErrorResponseJson = CloudstackTestUtils
                .createListFirewallRulesErrorResponseJson(errorCode, errorText);

        // verify
        this.expectedException.expect(FogbowException.class);
        this.expectedException.expectMessage(errorText);

        // execute
        ListFirewallRulesResponse.fromJson(volumesErrorResponseJson);
    }

}
