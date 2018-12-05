package org.fogbowcloud.ras;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.rest.RepositoryRestMvcAutoConfiguration;

@SpringBootApplication(exclude = RepositoryRestMvcAutoConfiguration.class)
public class FogbowRasApplication {

    public static void main(String[] args) {
        SpringApplication.run(FogbowRasApplication.class, args);
    }
}
