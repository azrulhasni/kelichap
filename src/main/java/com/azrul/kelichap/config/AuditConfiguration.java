/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.config;

import com.azrul.kelichap.security.JwtAuthConverter;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

/**
 *
 * @author azrul
 */
@Configuration
public class AuditConfiguration {
    private final JwtAuthConverter jwtAuthConverter;
    
    public AuditConfiguration(@Autowired JwtAuthConverter jwtAuthConverter) {
        this.jwtAuthConverter=jwtAuthConverter;
    }
    
    
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            
            if (SecurityContextHolder.getContext().getAuthentication() instanceof JwtAuthenticationToken token){
                
                var o = Optional.ofNullable(jwtAuthConverter.convert(token.getToken()).getName());
                return o;
            }else{
                DefaultOidcUser oidcUser = (DefaultOidcUser) ((OAuth2AuthenticationToken) SecurityContextHolder.getContext().getAuthentication()).getPrincipal();
                return Optional.ofNullable(oidcUser.getGivenName() + " " + oidcUser.getFamilyName()+" ("+oidcUser.getPreferredUsername()+")");
            }
        };
    }
}
