package com.tripsy.config;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.CorsConfigurer;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
class SecurityConfig {

	public static final String PARTICIPATE_IN_TRAINING = "Participate in Training";
	
    public interface Jwt2AuthoritiesConverter extends Converter<Jwt, Collection<? extends GrantedAuthority>> {}

    @SuppressWarnings("unchecked")
    @Bean
    public Jwt2AuthoritiesConverter authoritiesConverter() {
    	System.out.println("authoritiesConverter...");
        return jwt -> {

        	System.out.println("Jwt: " + jwt.getTokenValue());
            // realm roles
            final var realmAccess = (Map<String, Object>) jwt.getClaims().getOrDefault("realm_access", Map.of());
            final var realmRoles = (Collection<String>) realmAccess.getOrDefault("roles", List.of());
            
            final var resourceAccess = (Map<String, Object>) jwt.getClaims().getOrDefault("resource_access", Map.of());
            final var apiResource = (Map<String, Object>) resourceAccess.getOrDefault("my-client", Map.of());
            final var apiRoles = (Collection<String>) apiResource.getOrDefault("roles", List.of());
     
            System.out.println("realmAccess..."+realmAccess);
            System.out.println("realmRoles..."+realmRoles);
            System.out.println("apiRoles..."+apiRoles);
            
            return Stream.concat(realmRoles.stream(), apiRoles.stream())
                    .map(SimpleGrantedAuthority::new).toList();
        };
    }

    public interface Jwt2AuthenticationConverter extends Converter<Jwt, AbstractAuthenticationToken> { }

    @Bean
    public Jwt2AuthenticationConverter authenticationConverter(Jwt2AuthoritiesConverter authoritiesConverter) {
        return jwt -> new JwtAuthenticationToken(
                jwt,
                authoritiesConverter.convert(jwt),
                (String) jwt.getClaims().getOrDefault("preferred_username", "")
        );
    }



    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, Converter<Jwt, AbstractAuthenticationToken> authenticationConverter) throws Exception {
        http.oauth2ResourceServer(oauth2ResourceServer -> oauth2ResourceServer.jwt(jwt -> jwt.jwtAuthenticationConverter(authenticationConverter)));

        http.cors(CorsConfigurer::disable);

        http.sessionManagement(sessionManagement -> sessionManagement.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

       

        // don't redirect to login page if unauthorized. just return a 401.
        http.exceptionHandling(authenticationEntryPoint ->
                authenticationEntryPoint.authenticationEntryPoint((request, response, authException) -> {
                    response.addHeader(HttpHeaders.WWW_AUTHENTICATE, "Basic realm=\"Restricted Content\"");
                    response.sendError(HttpStatus.UNAUTHORIZED.value(), HttpStatus.UNAUTHORIZED.getReasonPhrase());
                })
        );

        // end point security config
        http.authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET, "/info", "/health", "/metrics", "/prometheus", "/v3/api-docs.yaml").permitAll()
                
                .requestMatchers(HttpMethod.GET, "/customers").hasAnyAuthority("user")
                .requestMatchers(HttpMethod.GET, "/users").hasAnyAuthority("user")
                
             //   .requestMatchers(HttpMethod.GET, "/customers").permitAll()
             //   .requestMatchers(HttpMethod.GET, "/users").permitAll()

                .anyRequest().denyAll()
        );

        return http.build();
    }
}
