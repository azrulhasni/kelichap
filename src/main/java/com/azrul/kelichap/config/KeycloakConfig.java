/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.config;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 *
 * @author azrul
 */
@Configuration
public class KeycloakConfig {
    @Value("${kelichap.keycloak.username}")
    private String keycloakUsername;
    
    @Value("${kelichap.keycloak.password}")
    private String keycloakPassword;
    
    @Value("${kelichap.keycloak.url}")
    private String keycloakUrl;
    
    @Value("${kelichap.keycloak.client-id}")
    private String keycloakClientId;
    
    @Value("${kelichap.keycloak.realm}")
    private String keycloakRealm;

    @Bean
    Keycloak keycloak() {
        return KeycloakBuilder.builder()
                .serverUrl(keycloakUrl)
                .realm(keycloakRealm)
                .clientId(keycloakClientId)
                .grantType(OAuth2Constants.PASSWORD)
                .username(keycloakUsername)
                .password(keycloakPassword)
                .build();
    }
}
