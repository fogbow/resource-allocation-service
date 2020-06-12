package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.regex.Pattern;

public class AzureGeneralPolicyTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

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
