package gov.usgs.aqcu.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableResourceServer;
import org.springframework.security.oauth2.config.annotation.web.configuration.ResourceServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configurers.ResourceServerSecurityConfigurer;

@Configuration
@EnableResourceServer
public class OAuth2ResourceServerConfig extends ResourceServerConfigurerAdapter {

	@Value("${security.oauth2.resource.id}")
	private String resourceId;

	@Override
	public void configure(HttpSecurity http) throws Exception {
		http
			.authorizeRequests()
				.antMatchers("/", "/swagger-ui.html", "/swagger-resources/**", "/webjars/**", "/v2/**").permitAll()
				.antMatchers("/actuator/health**", "/actuator/info**").permitAll()
				.anyRequest().fullyAuthenticated()
			.and()
				.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
			.and()
				.cors()
			.and()
				.csrf().disable()
		;
	}

	@Override
	public void configure(ResourceServerSecurityConfigurer resources) {
		resources.resourceId(resourceId);
	}

}
