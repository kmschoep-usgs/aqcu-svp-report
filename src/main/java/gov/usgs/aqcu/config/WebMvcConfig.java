package gov.usgs.aqcu.config;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.GsonHttpMessageConverter;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.google.gson.Gson;

import gov.usgs.aqcu.serializer.SwaggerGsonSerializer;
import gov.usgs.aqcu.util.AqcuGsonBuilderFactory;
import springfox.documentation.spring.web.json.Json;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
	@Override
	public void addCorsMappings(CorsRegistry registry) {
		registry.addMapping("/**");
	}

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/static/**")
				.addResourceLocations("/resources/")
				.setCacheControl(
						CacheControl.maxAge(30L, TimeUnit.DAYS).cachePublic())
				.resourceChain(true);
	}

	@Override
	public void configureMessageConverters(List<HttpMessageConverter < ? >> converters) {
		GsonHttpMessageConverter gsonHttpMessageConverter = new GsonHttpMessageConverter();
		gsonHttpMessageConverter.setGson(gson());
		converters.add(gsonHttpMessageConverter);
	}

	@Bean
	public Gson gson() {
		return AqcuGsonBuilderFactory.getConfiguredGsonBuilder()
			.registerTypeAdapter(Json.class, new SwaggerGsonSerializer())
			.create();
	}
}
