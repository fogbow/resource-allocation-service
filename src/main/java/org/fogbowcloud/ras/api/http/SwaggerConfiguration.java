package org.fogbowcloud.ras.api.http;

import org.fogbowcloud.ras.core.ApplicationFacade;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@EnableSwagger2
@Configuration
public class SwaggerConfiguration {

    public static final String BASE_PACKAGE = "org.fogbowcloud.ras";

    public static final String API_TITLE = "Fogbow API";
    public static final String API_DESCRIPTION = "This API introduces readers to Fogbow REST API, "
        + "provides guidelines on how to use it, and describe the available features accessible from it.";

    public static final String CONTACT_NAME = "Fogbow";
    public static final String CONTACT_URL = "https://www.fogbowcloud.org";
    public static final String CONTACT_EMAIL = "contact@fogbowcloud.org";
    public static final Contact CONTACT = new Contact(
        CONTACT_NAME,
        CONTACT_URL,
        CONTACT_EMAIL);

    @Bean
    public Docket apiDetails() {
        Docket docket = new Docket(DocumentationType.SWAGGER_2);

        docket.select()
            .apis(RequestHandlerSelectors.basePackage(BASE_PACKAGE))
            .paths(PathSelectors.any())
            .build()
            .apiInfo(this.apiInfo().build());

        return docket;
    }

    private ApiInfoBuilder apiInfo() {
        String versionNumber = ApplicationFacade.getInstance().getVersionNumber();

        ApiInfoBuilder apiInfoBuilder = new ApiInfoBuilder();

        // TODO Add License to the documentation?
        apiInfoBuilder.title(API_TITLE);
        apiInfoBuilder.description(API_DESCRIPTION);
        apiInfoBuilder.version(versionNumber);
        apiInfoBuilder.contact(CONTACT);

        return apiInfoBuilder;

    }

}