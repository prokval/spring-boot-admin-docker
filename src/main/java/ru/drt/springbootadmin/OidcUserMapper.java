package ru.drt.springbootadmin;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.authentication.OidcIdTokenDecoderFactory;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUserAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoderFactory;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;

import static java.util.Optional.ofNullable;
import static java.util.function.Predicate.not;
import static org.springframework.security.oauth2.jwt.JwtValidators.createDefaultWithValidators;

public class OidcUserMapper implements BiFunction<OidcUserRequest, OidcUserInfo, OidcUser> {

    private final JwtDecoderFactory<ClientRegistration> jwtDecoderFactory = new OidcIdTokenDecoderFactory();

    @Override
    public OidcUser apply(OidcUserRequest userRequest, OidcUserInfo userInfo) {
        Set<GrantedAuthority> authorities = new LinkedHashSet<>();
        ClientRegistration.ProviderDetails providerDetails = userRequest.getClientRegistration().getProviderDetails();
        String userNameAttributeName = providerDetails.getUserInfoEndpoint().getUserNameAttributeName();
        if (StringUtils.hasText(userNameAttributeName)) {
            authorities.add(new OidcUserAuthority(userRequest.getIdToken(), userInfo, userNameAttributeName));
        }
        else {
            authorities.add(new OidcUserAuthority(userRequest.getIdToken(), userInfo));
        }
        OAuth2AccessToken token = userRequest.getAccessToken();
        for (String scope : token.getScopes()) {
            authorities.add(new SimpleGrantedAuthority("SCOPE_" + scope));
        }

        NimbusJwtDecoder jwtDecoder = (NimbusJwtDecoder) jwtDecoderFactory.createDecoder(userRequest.getClientRegistration());
        jwtDecoder.setJwtValidator(createDefaultWithValidators(new JwtTimestampValidator()));
        Jwt jwt = jwtDecoder.decode(token.getTokenValue());

        ofNullable(jwt.getClaimAsMap("realm_access")).map(c -> (List<String>) c.get("roles")).orElse(List.of())
                .stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(not(String::isEmpty))
                .map(String::toUpperCase)
                .distinct()
                .map(s -> "ROLE_" + s)
                .map(SimpleGrantedAuthority::new)
                .forEach(authorities::add);

        if (StringUtils.hasText(userNameAttributeName)) {
            return new DefaultOidcUser(authorities, userRequest.getIdToken(), userInfo, userNameAttributeName);
        }
        return new DefaultOidcUser(authorities, userRequest.getIdToken(), userInfo);
    }
}
