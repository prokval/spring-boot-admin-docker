package ru.drt.springbootadmin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.client.RestTemplate;

import java.net.URI;

import static org.springframework.http.HttpHeaders.HOST;

@Configuration
@EnableWebSecurity
@Profile("keycloak")
public class SecurityConfig {

    @Autowired
    private ClientRegistrationRepository clientRegistrationRepository;

    private String keycloakAuthHost;

    @Value("${spring.security.oauth2.client.provider.keycloak.logout-uri}")
    private String logoutUri;

    @Value("${spring.security.oauth2.client.provider.keycloak.authorization-uri}")
    void setAuthorizationUri(final String authorizationUri) {
        URI uri = URI.create(authorizationUri);
        int port = uri.getPort();
        if (port != 0) {
            this.keycloakAuthHost = uri.getHost() + ":" + port;
        } else {
            this.keycloakAuthHost = uri.getHost();
        }
    }

    // Web Security (OAuth2 Client)
    @Bean
    public SecurityFilterChain webFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .anyRequest().hasRole("ADMIN")
                )
                /*
                  To make in work
                  - Go to Clients -> <clientId> -> Settings, turn on "Standard Flow Enabled", specify "Valid Redirect URIs" so that it
                  include defaultSuccessUrl. Typically just put "*" there.

                  - Go to Clients -> <clientId> -> Scope, assign roles you want to see in your app, or just enable "Full Scope Allowed"

                 */
                .oauth2Login(oauth2 -> oauth2
                        .defaultSuccessUrl("/", true))
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler())
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID"))
                .csrf(csrf -> csrf.ignoringRequestMatchers("/logout"));
        return http.build();
    }

    private LogoutSuccessHandler oidcLogoutSuccessHandler() {
        var logoutSuccessHandler = new ConfiguredOidcClientInitiatedLogoutSuccessHandler(clientRegistrationRepository);
        logoutSuccessHandler.setEndSessionEndpointUri(logoutUri);
        logoutSuccessHandler.setPostLogoutRedirectUri("{baseUrl}/");
        return logoutSuccessHandler;
    }

    @Bean
    public DefaultOAuth2UserService oauth2UserService() {
        DefaultOAuth2UserService oauth2UserService = new DefaultOAuth2UserService();

        RestTemplate restTemplate = new RestTemplateBuilder(
                c -> c.setErrorHandler(new OAuth2ErrorResponseErrorHandler()),
/*
                c -> c.getInterceptors().add((request, body, execution) -> {
                    System.out.println(request.getURI());
                    System.out.println(request.getHeaders());
                    return execution.execute(request, body);
                }),
*/
                c -> c.getClientHttpRequestInitializers().add(request -> {
                    // Looks like a dirty hack, but I didn't find better solution
                    // user-info-uri (openid-connect/userinfo) request is authorized with Bearer token, which has iss host
                    // from authorization-uri as it goes through client's browser.
                    // Inside Docker, we connect to Keycloak via direct connection inside Docker network. The host is
                    // different from what client's browser use, thus to make Bearer token accepted by the Keycloak,
                    // we need to replace the Host http header.
                    request.getHeaders().set(HOST, keycloakAuthHost);
                })
        ).build();

        oauth2UserService.setRestOperations(restTemplate);
        return oauth2UserService;
    }

    @Bean
    public OidcUserService oidcUserService() {
        OidcUserService oidcUserService = new OidcUserService();
        oidcUserService.setOauth2UserService(oauth2UserService());
        oidcUserService.setOidcUserMapper(new OidcUserMapper());
        return oidcUserService;
    }

}
