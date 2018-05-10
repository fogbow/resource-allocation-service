package org.fogbowcloud.manager.core.rest.controllers;

import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

public class ComputeOrdersControllerTest {

    public final String ACESS_ID_HEADER = "accessId";
    public final String LOCAL_TOKEN_ID_HEADER = "localTokenId";

    @Test
    public void createdComputeTest() {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();

        headers.setAccept(Arrays.asList(new MediaType[] { MediaType.APPLICATION_JSON }));
        //headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.setContentType(MediaType.APPLICATION_JSON);
        String fakeAccessId = "llfdsfdsf";
        String fakeLocalTokenId = "37498327432";
        headers.set(ACESS_ID_HEADER, fakeAccessId);
        headers.set(LOCAL_TOKEN_ID_HEADER, fakeLocalTokenId);
        HttpEntity<String> entity = new HttpEntity<>("parameters", headers);
        String url = "";
        restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
    }

}