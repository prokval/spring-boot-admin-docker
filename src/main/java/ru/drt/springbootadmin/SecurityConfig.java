package ru.drt.springbootadmin;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.authority.mapping.GrantedAuthoritiesMapper;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.function.Predicate.not;

@Configuration
@EnableWebSecurity
@Profile("keycloak")
public class SecurityConfig {

    // Web Security (OAuth2 Client)
    @Bean
    public SecurityFilterChain webFilterChain(HttpSecurity http, ClientRegistrationRepository clientRegistrationRepository) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasRole("ADMIN")
                )
                /*
                  To make in work
                  Go to Clients -> <clientId> -> Settings, turn on "Standard Flow Enabled", specify "Valid Redirect URIs" so that it
                  include defaultSuccessUrl. Typically just put "*" there.
                 */
                .oauth2Login(oauth2 -> oauth2.defaultSuccessUrl("/", true))
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(clientRegistrationRepository))
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/logout"));
        return http.build();
    }

    private static LogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository clientRegistrationRepository) {
        var logoutSuccessHandler = new OidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/");
        return logoutSuccessHandler;
    }


    @Bean
    GrantedAuthoritiesMapper authenticationConverter() {
        /*
        To make it work
        - Go to Clients -> <clientId> -> Scope, assign roles you want to see in your app, or just enable "Full Scope Allowed"

        Option 1:
        - Go to Clients -> <clientId> -> Client Scopes, ensure it has 'roles' in Assigned Default Client Scopes
        - Go to Client Scopes -> roles -> Mappers -> realm roles, enable "Add to ID token"

        Option 2:
        - Go to Clients -> <clientId> -> Mappers, add "realm roles" mapper
        - Click Edit on it and enable "Add to ID token"

         */
        return (authorities) -> authorities.stream()
                .filter(authority -> authority instanceof OidcUserAuthority)
                .map(OidcUserAuthority.class::cast)
                .map(OidcUserAuthority::getIdToken)
                .map(OidcIdToken::getClaims)
                .map(realmRolesAuthoritiesConverter())
                .flatMap(roles -> roles.stream())
                .collect(Collectors.toSet());
    }

    private static Function<Map<String, Object>, Collection<GrantedAuthority>> realmRolesAuthoritiesConverter() {
        return claims -> {
            var realmAccess = Optional.ofNullable((Map<String, Object>) claims.get("realm_access"));
            var roles = realmAccess.flatMap(map -> Optional.ofNullable((List<String>) map.get("roles")));
            return roles.map(List::stream)
                    .orElse(Stream.empty())
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(not(String::isEmpty))
                    .map(String::toUpperCase)
                    .distinct()
                    .map(s -> "ROLE_" + s)
                    .map(SimpleGrantedAuthority::new)
                    .map(GrantedAuthority.class::cast)
                    .toList();
        };
    }
}
