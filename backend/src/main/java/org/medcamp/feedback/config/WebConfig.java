package org.medcamp.feedback.config;

import java.io.IOException;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

/**
 * Serves the built React app from {@code classpath:/static}. Any path that is not an API call
 * and not a real file (for example {@code /users} after a browser refresh) gets
 * {@code index.html}, so React Router can handle it.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/**")
			.addResourceLocations("classpath:/static/")
			.resourceChain(true)
			.addResolver(new SpaFallbackResourceResolver());
	}

	private static final class SpaFallbackResourceResolver extends PathResourceResolver {

		@Override
		protected Resource getResource(String resourcePath, Resource location) throws IOException {
			if (resourcePath.startsWith("api/")) {
				return null;
			}
			if (!resourcePath.isEmpty()) {
				Resource requested = location.createRelative(resourcePath);
				if (requested.exists() && requested.isReadable()) {
					return requested;
				}
				if (resourcePath.contains(".")) {
					return null; // a missing asset should be a 404, not the app shell
				}
			}
			Resource index = location.createRelative("index.html");
			return index.exists() && index.isReadable() ? index : null;
		}

	}

}
