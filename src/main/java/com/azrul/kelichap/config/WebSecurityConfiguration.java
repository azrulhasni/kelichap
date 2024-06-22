package com.azrul.kelichap.config;

import com.azrul.kelichap.security.JwtAuthConverter;
import com.azrul.kelichap.security.JwtAuthForAPIConverter;
import com.azrul.kelichap.security.KeycloakLogoutHandler;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.spring.security.VaadinWebSecurity;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.authentication.session.RegisterSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;

@EnableWebSecurity
@Configuration
public class WebSecurityConfiguration extends VaadinWebSecurity {

    private final KeycloakLogoutHandler keycloakLogoutHandler;
    private final JwtAuthConverter jwtAuthConverter;
    private final JwtAuthForAPIConverter jwtAuthForAPIConverter;

    public WebSecurityConfiguration(
            @Autowired JwtAuthConverter jwtAuthConverter,
            @Autowired JwtAuthForAPIConverter jwtAuthForAPIConverter,
            @Autowired KeycloakLogoutHandler keycloakLogoutHandler) {
        this.keycloakLogoutHandler = keycloakLogoutHandler;
        this.jwtAuthForAPIConverter = jwtAuthForAPIConverter;
        this.jwtAuthConverter = jwtAuthConverter;
    }

    @Bean
    protected SessionAuthenticationStrategy sessionAuthenticationStrategy() {
        return new RegisterSessionAuthenticationStrategy(new SessionRegistryImpl());
    }

    private OAuth2UserService<OidcUserRequest, OidcUser> oidcUserService() {
        final OidcUserService delegate = new OidcUserService();

        return (userRequest) -> {
            OidcUser oidcUser = delegate.loadUser(userRequest);

            OAuth2AccessToken accessToken = userRequest.getAccessToken();

            JwtDecoder jwtDecoder = NimbusJwtDecoder.withIssuerLocation(oidcUser.getIssuer().toString()).jwsAlgorithm(SignatureAlgorithm.RS256).build();
            Jwt jwt = jwtDecoder.decode(accessToken.getTokenValue());

            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
            mappedAuthorities.addAll(jwtAuthConverter.extractResourceRoles(jwt));

            oidcUser = new DefaultOidcUser(mappedAuthorities, oidcUser.getIdToken());

            return oidcUser;
        };
    }

    @Bean
    public GrantedAuthoritiesMapper userAuthoritiesMapper() {
        return (authorities) -> {
            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();

            authorities.forEach(authority -> {
                mappedAuthorities.add(new SimpleGrantedAuthority(authority.getAuthority()));
                if (OidcUserAuthority.class.isInstance(authority)) {
                    OidcUserAuthority oidcUserAuthority = (OidcUserAuthority) authority;
                    OidcIdToken idToken = oidcUserAuthority.getIdToken();
                    OidcUserInfo userInfo = oidcUserAuthority.getUserInfo();
                } else if (OAuth2UserAuthority.class.isInstance(authority)) {
                    OAuth2UserAuthority oauth2UserAuthority = (OAuth2UserAuthority) authority;
                    Map<String, Object> userAttributes = oauth2UserAuthority.getAttributes();
                }
            });
            return mappedAuthorities;
        };
    }

//    @Bean
//    public GrantedAuthoritiesMapper userAuthoritiesMapperForKeycloak() {
//        return authorities -> {
//            Set<GrantedAuthority> mappedAuthorities = new HashSet<>();
//            var authority = authorities.iterator().next();
//
//            if (authority instanceof OidcUserAuthority oidcUserAuthority) {
//                var userInfo = oidcUserAuthority.getUserInfo();
//
//                if (userInfo.hasClaim("realm_access")) {
//                    var realmAccess = userInfo.getClaimAsMap("realm_access");
//                    var roles = (Collection<String>) realmAccess.get("roles");
//                    mappedAuthorities.addAll(roles.stream()
//                            .map(role -> new SimpleGrantedAuthority("OIDC_" + role.toUpperCase()))
//                            .toList());
//                }
//            }
//            return mappedAuthorities;
//        };
//    }
    @Override
    protected void configure(HttpSecurity http) throws Exception {

        http.authorizeHttpRequests(
                authorize -> authorize.requestMatchers(new AntPathRequestMatcher("/images/*.png")).permitAll());
        http.authorizeHttpRequests(
                authorize -> authorize.requestMatchers(new AntPathRequestMatcher("/images/*.svg")).permitAll());
         http.authorizeHttpRequests(
                authorize -> authorize.requestMatchers(new AntPathRequestMatcher("/icons/*.ico")).permitAll());

        // Icons from the line-awesome addon
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(new AntPathRequestMatcher("/line-awesome/**/*.svg")).permitAll()
        );
        
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(new AntPathRequestMatcher("/swagger-ui/**")).permitAll()
        );
        
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(new AntPathRequestMatcher("/v3/api-docs/**")).permitAll()
        );

        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(new AntPathRequestMatcher("/folder")).authenticated()
        );

        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(new AntPathRequestMatcher("/document")).authenticated()
        );
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(new AntPathRequestMatcher("/access")).authenticated()
        );
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(new AntPathRequestMatcher("/note")).authenticated()
        );

        http.oauth2Login((oauth2Login) -> oauth2Login.userInfoEndpoint(u -> u.oidcUserService(this.oidcUserService())));//standard grant
        http.oauth2ResourceServer((oauth2) -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthForAPIConverter)));//direct access grant
        http.logout(logout -> {
            logout.addLogoutHandler(keycloakLogoutHandler);
        });
        super.configure(http);

    }

}
