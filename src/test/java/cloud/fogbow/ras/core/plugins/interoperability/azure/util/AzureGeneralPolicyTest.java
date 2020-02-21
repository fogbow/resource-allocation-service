package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import cloud.fogbow.ras.constants.Messages;
import cloud.fogbow.ras.core.models.orders.ComputeOrder;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.util.regex.Pattern;

public class AzureGeneralPolicyTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // test case: When calling the getDisk method with disk value valid in the compute order,
    // it must verify if it returns the disk value.
    @Test
    public void testGetDiskSuccessfully() throws InvalidParameterException {
        // set up
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        int diskExpected = AzureGeneralPolicy.MINIMUM_DISK;
        Mockito.when(computeOrder.getDisk()).thenReturn(diskExpected);

        // exercise
        int disk = AzureGeneralPolicy.getDisk(computeOrder);

        // verify
        Assert.assertEquals(diskExpected, disk);
    }

    // test case: When calling the getDisk method with disk value invalid in the compute order,
    // it must verify if it throws an InvalidParameterException.
    @Test
    public void testGetDiskFail() throws InvalidParameterException {
        // set up
        ComputeOrder computeOrder = Mockito.mock(ComputeOrder.class);
        int diskInvalid = AzureGeneralPolicy.MINIMUM_DISK - 1;
        Mockito.when(computeOrder.getDisk()).thenReturn(diskInvalid);

        // verify
        this.expectedException.expect(InvalidParameterException.class);
        this.expectedException.expectMessage(String.format(
                Messages.Error.ERROR_DISK_PARAMETER_AZURE_POLICY, AzureGeneralPolicy.MINIMUM_DISK));

        // exercise
        AzureGeneralPolicy.getDisk(computeOrder);
    }

    // test case: When calling the generatePassword method , it must verify if it
    // returns a value as specified.
    @Test
    public void testGeneratePasswordSuccessfully() {
        // set up
        String regex = "(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.{1,})%s([a-z]|[A-Z]|[0-9])*";
        regex = String.format(regex, AzureGeneralPolicy.PASSWORD_PREFIX);
        Pattern pattern = Pattern.compile(regex);

        // exercise
        String password = AzureGeneralPolicy.generatePassword();

        // verify
        Assert.assertTrue(pattern.matcher(password).matches());
    }


}
