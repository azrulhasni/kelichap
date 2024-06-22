/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.azrul.kelichap.security;



import com.azrul.kelichap.config.JwtAuthConverterProperties;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;

/**
 *
 * @author azrul
 */
@Component
public class JwtAuthConverter implements Converter<Jwt, AbstractAuthenticationToken> {

    private final JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();

    private final JwtAuthConverterProperties properties;

    public JwtAuthConverter(JwtAuthConverterProperties properties) {
        this.properties = properties;
    }

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = Stream.concat(
                jwtGrantedAuthoritiesConverter.convert(jwt).stream(),
                extractResourceRoles(jwt).stream()).collect(Collectors.toSet());
        return new JwtAuthenticationToken(jwt, authorities, getPrincipalClaimName(jwt));
    }

    private String getPrincipalClaimName(Jwt jwt) {
        String claimName = JwtClaimNames.SUB;
        if (properties.getPrincipalAttribute() != null) {
            claimName = properties.getPrincipalAttribute();
        }
        return jwt.getClaim(claimName);
    }

    public Collection<? extends GrantedAuthority> extractResourceRoles(Jwt jwt) {

        Map<String, Object> resource;
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        Set<SimpleGrantedAuthority> roles = new HashSet<>();
        
        if (realmAccess!=null
             && realmAccess.containsKey("roles")){
            roles.addAll(((Collection<String>)realmAccess.get("roles"))
                    .stream()
                .map(role -> new SimpleGrantedAuthority("OIDC_" + role.toUpperCase()))
                .collect(Collectors.toSet())
            );
        }
        
        
        if (resourceAccess != null
                && (resource = (Map<String, Object>) resourceAccess.get(properties.getResourceId())) != null
                && resource.containsKey("roles")) {
       
            
            roles.addAll(((Collection<String>) resource.get("roles"))
                .stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_"+role.toUpperCase()))
                .collect(Collectors.toSet())
            );
        }
        
        return roles;
        
    }
}
