package gov.va.ocp.framework.security.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationProvider;

import gov.va.ocp.framework.security.jwt.JwtAuthenticationProperties;
import gov.va.ocp.framework.security.jwt.JwtAuthenticationProvider;
import gov.va.ocp.framework.security.jwt.JwtParser;

@Configuration
@ComponentScan(basePackages = { "gov.va.ocp.framework.security", "gov.va.ocp.framework.security.jwt" })
public class OcpSecurityTestConfig {
	@Bean
	JwtAuthenticationProperties jwtAuthenticationProperties() {
		return new JwtAuthenticationProperties();
	}

	@Bean
	protected AuthenticationProvider jwtAuthenticationProvider() {
		return new JwtAuthenticationProvider(new JwtParser(jwtAuthenticationProperties()));
	}
}