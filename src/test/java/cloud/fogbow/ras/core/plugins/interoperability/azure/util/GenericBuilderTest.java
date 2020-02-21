package cloud.fogbow.ras.core.plugins.interoperability.azure.util;

import cloud.fogbow.common.exceptions.InvalidParameterException;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.function.Supplier;

public class GenericBuilderTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    // test case: When calling the build method with all attributes filled and it does not call
    // check parameters method, it must verify if the Object was filled with the right values.
    @Test
    public void testBuildSuccessfullyWhenAllAttributesAreFilledAndWithoutChecking() {
        // set up
        String attributeRequired = "attributeRequired";
        String attributeOptional = "attributeOptional";

        // exercise
        GenericObject genericObject = GenericObject.builder()
                .atributeRequired(attributeRequired)
                .atributeOptional(attributeOptional)
                .build();

        // verify
        Assert.assertEquals(attributeOptional, genericObject.getAtributeOptional());
        Assert.assertEquals(attributeRequired, genericObject.getAtributeRequired());
    }

    // test case: When calling the build method with all "set" methods filled and it calls
    // check parameters method, it must verify if the Object was filled with the right values and
    // does not throw the exception.
    @Test
    public void testBuildSuccessfullyWhenAllAttributesAreFilledAndWithChecking()
            throws InvalidParameterException {
        // set up
        String attributeRequired = "attributeRequired";
        String attributeOptional = "attributeOptional";

        // exercise
        GenericObject genericObject = GenericObject.builder()
                .atributeRequired(attributeRequired)
                .atributeOptional(attributeOptional)
                .checkAndBuild();

        // verify
        Assert.assertEquals(attributeOptional, genericObject.getAtributeOptional());
        Assert.assertEquals(attributeRequired, genericObject.getAtributeRequired());
    }

    // test case: When calling the build method with a required attribute not filled and it does not call
    // check parameters method, it must verify if the Object was filled with the right values.
    @Test
    public void testBuildSuccessfullyWhenARequiredAttributeIsNotFieldAndWithoutChecking() {
        // set up
        String attributeOptional = "attributeOptional";

        // exercise
        GenericObject genericObject = GenericObject.builder()
                .atributeOptional(attributeOptional)
                .build();

        // verify
        Assert.assertEquals(attributeOptional, genericObject.getAtributeOptional());
        Assert.assertNull(genericObject.getAtributeRequired());
    }

    // test case: When calling the build method with a required attribute not filled and it does not call
    // check parameters method, it must verify if it throws an exception.
    @Test
    public void testBuildFailWhenARequiredAttributeIsNotFieldAndWithChecking() {

        // set up
        String attributeOptional = "attributeOptional";

        // exercise
        try {
            GenericObject.builder()
                    .atributeOptional(attributeOptional)
                    .checkAndBuild();
        } catch (InvalidParameterException e) {
            // verify
            String exceptionMessageExpected = generateExceptionMessage();
            Assert.assertEquals(exceptionMessageExpected, e.getMessage());
        }
    }

    // test case: When calling the build method with a required attribute not filled and it does not call
    // check parameters method, it must verify if it the Object was filled with the right values.
    @Test
    public void testBuildSuccessfullyWhenAnOpitionalAttributeIsNotFieldAndWithChecking()
            throws InvalidParameterException {

        // set up
        String attributeRequired = "attributeRequired";

        // exercise
        GenericObject genericObject = GenericObject.builder()
                .atributeRequired(attributeRequired)
                .checkAndBuild();

        // verify
        Assert.assertEquals(attributeRequired, genericObject.getAtributeRequired());
        Assert.assertNull(attributeRequired, genericObject.getAtributeOptional());
    }

    private String generateExceptionMessage() {
        String requiredAttribute = GenericObject.class.getDeclaredFields()[0].getName();
        return String.format(GenericBuilder.FIELD_REQUIRED_MESSAGE,
                requiredAttribute, GenericObject.class.getSimpleName());
    }

    private static class GenericObject {

        @GenericBuilder.Required
        private String atributeRequired;
        private String atributeOptional;

        private static Builder builder() {
            return new Builder(GenericObject::new);
        }

        private String getAtributeRequired() {
            return atributeRequired;
        }

        private void setAtributeRequired(String atributeRequired) {
            this.atributeRequired = atributeRequired;
        }

        private String getAtributeOptional() {
            return atributeOptional;
        }

        private void setAtributeOptional(String atributeOptional) {
            this.atributeOptional = atributeOptional;
        }

        private static class Builder extends GenericBuilder<GenericObject> {

            private Builder(Supplier<GenericObject> instantiator) {
                super(instantiator);
            }

            private Builder atributeRequired(String atributeRequired) {
                with(GenericObject::setAtributeRequired, atributeRequired);
                return this;
            }

            private Builder atributeOptional(String atributeOptional) {
                with(GenericObject::setAtributeOptional, atributeOptional);
                return this;
            }

        }

    }
}

