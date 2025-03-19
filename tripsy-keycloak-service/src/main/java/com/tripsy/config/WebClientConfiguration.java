package com.tripsy.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.InMemoryOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfiguration {

    private static final String REGISTRATION_ID = "ws_safety_training_participation_api";
    

    private String traineeUrl = "http://localhost:8081";
    
    private String traineeTokenUrl = "http://localhost:8080/realms/SpringBootKeycloak/protocol/openid-connect/token";

    private String clientId = "my-client";

    private String clientSecret = "zv0FG8baWYYOilLU6wCt8ZqE2adp0XQU";

    @Bean(name= "apiClient")
    public WebClient apiClient(WebClient.Builder clientBuilder) {
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth =
                buildClientRegistrationRepo(clientId, clientSecret, traineeTokenUrl, REGISTRATION_ID);
        return clientBuilder
                .baseUrl(traineeUrl)
                .apply(oauth.oauth2Configuration())
                .build();
    }


    private ServletOAuth2AuthorizedClientExchangeFilterFunction buildClientRegistrationRepo(String clientId, String clientSecret,
                                                                                            String tokenUrl, String registrationId) {
        ClientRegistrationRepository clientRegistrationRepo = new InMemoryClientRegistrationRepository(
                ClientRegistration.withRegistrationId(registrationId)
                        .tokenUri(tokenUrl)
                        .clientSecret(clientSecret)
                        .clientId(clientId)
                        .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                        .build());

        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth = new ServletOAuth2AuthorizedClientExchangeFilterFunction(
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(clientRegistrationRepo, new InMemoryOAuth2AuthorizedClientService(clientRegistrationRepo)));
        oauth.setDefaultClientRegistrationId(registrationId);
        return oauth;
    }

}
