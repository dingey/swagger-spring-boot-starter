package com.github.dingey.swagger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import springfox.documentation.swagger.web.SwaggerResource;
import springfox.documentation.swagger.web.SwaggerResourcesProvider;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@EnableSwagger2
@Configuration
@ConditionalOnClass(ZuulProperties.class)
@ConditionalOnProperty(value = "swagger.enable", matchIfMissing = true)
@EnableConfigurationProperties({SwaggerProperties.class})
public class ZuulGatewayAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(GatewaySwaggerAutoConfiguration.class);
    SwaggerProperties swaggerProperties;

    public ZuulGatewayAutoConfiguration(SwaggerProperties swaggerProperties) {
        this.swaggerProperties = swaggerProperties;
    }

    @PostConstruct
    public void init() {
        if (log.isDebugEnabled()) {
            log.debug("Initializing Spring cloud Zuul Swagger2");
        }
    }


    @Bean
    @Primary
    public ZuulSwaggerResourcesProvider zuulSwaggerResourcesProvider(ZuulProperties zuulProperties) {
        return new ZuulSwaggerResourcesProvider(zuulProperties);
    }

    class ZuulSwaggerResourcesProvider implements SwaggerResourcesProvider {
        private final ZuulProperties zuulProperties;

        ZuulSwaggerResourcesProvider(ZuulProperties zuulProperties) {
            this.zuulProperties = zuulProperties;
        }

        @Override
        public List<SwaggerResource> get() {
            if (swaggerProperties.getResources() != null) {
                return swaggerProperties.getResources();
            }
            List<SwaggerResource> resources = new ArrayList<>(zuulProperties.getRoutes().size());
            for (Map.Entry<String, ZuulProperties.ZuulRoute> entry : zuulProperties.getRoutes().entrySet()) {
                SwaggerResource swaggerResource = new SwaggerResource();
                swaggerResource.setName(entry.getValue().getServiceId());
                swaggerResource.setLocation(entry.getValue().getPath().replace("/**", "/v2/api-docs"));
                resources.add(swaggerResource);
            }
            return resources;
        }
    }
}
