package com.github.dingey.swagger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.config.GatewayAutoConfiguration;
import org.springframework.cloud.gateway.config.GatewayProperties;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.support.NameUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import springfox.documentation.swagger.web.*;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Configuration
@ConditionalOnClass(RouteLocator.class)
@AutoConfigureAfter(GatewayAutoConfiguration.class)
@ConditionalOnProperty(value = "swagger.enable", matchIfMissing = true)
@EnableConfigurationProperties({SwaggerProperties.class})
public class GatewaySwaggerAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(GatewaySwaggerAutoConfiguration.class);
    private final SwaggerProperties swaggerProperties;

    public GatewaySwaggerAutoConfiguration(SwaggerProperties swaggerProperties) {
        this.swaggerProperties = swaggerProperties;
    }

    @PostConstruct
    public void init() {
        if (log.isDebugEnabled()) {
            log.debug("Initializing Spring cloud Gateway Swagger2");
        }
    }

    @Bean
    @Primary
    public GatewaySwaggerResourcesProvider gatewaySwaggerResourcesProvider(RouteLocator routeLocator, GatewayProperties gatewayProperties) {
        return new GatewaySwaggerResourcesProvider(routeLocator, gatewayProperties);
    }

    @Bean
    public SwaggerHeaderFilter swaggerHeaderFilter() {
        return new SwaggerHeaderFilter();
    }

    @SuppressWarnings("unused")
    @RestController
    @RequestMapping("/swagger-resources")
    class SwaggerHandler {
        private final SecurityConfiguration securityConfiguration;
        private final UiConfiguration uiConfiguration;
        private final SwaggerResourcesProvider swaggerResources;

        @Autowired
        public SwaggerHandler(SwaggerResourcesProvider swaggerResources, @Autowired(required = false) SecurityConfiguration securityConfiguration, @Autowired(required = false) UiConfiguration uiConfiguration) {
            this.swaggerResources = swaggerResources;
            this.securityConfiguration = securityConfiguration;
            this.uiConfiguration = uiConfiguration;
        }


        @GetMapping("/configuration/security")
        public Mono<ResponseEntity<SecurityConfiguration>> securityConfiguration() {
            return Mono.just(new ResponseEntity<>(
                    Optional.ofNullable(securityConfiguration).orElse(SecurityConfigurationBuilder.builder().build()), HttpStatus.OK));
        }

        @GetMapping("/configuration/ui")
        public Mono<ResponseEntity<UiConfiguration>> uiConfiguration() {
            return Mono.just(new ResponseEntity<>(
                    Optional.ofNullable(uiConfiguration).orElse(UiConfigurationBuilder.builder().build()), HttpStatus.OK));
        }

        @GetMapping("")
        public Mono<ResponseEntity> swaggerResources() {
            return Mono.just((new ResponseEntity<>(swaggerResources.get(), HttpStatus.OK)));
        }
    }

    @Primary
    class GatewaySwaggerResourcesProvider implements SwaggerResourcesProvider {
        static final String API_URI = "/v2/api-docs";
        private final RouteLocator routeLocator;
        private final GatewayProperties gatewayProperties;

        GatewaySwaggerResourcesProvider(RouteLocator routeLocator, GatewayProperties gatewayProperties) {
            this.routeLocator = routeLocator;
            this.gatewayProperties = gatewayProperties;
        }

        @Override
        public List<SwaggerResource> get() {
            if (swaggerProperties.getResources() != null) {
                return swaggerProperties.getResources();
            }
            List<SwaggerResource> resources = new ArrayList<>();
            List<String> routes = new ArrayList<>();
            routeLocator.getRoutes().subscribe(route -> routes.add(route.getId()));
            gatewayProperties.getRoutes().stream().filter(routeDefinition -> routes.contains(routeDefinition.getId()))
                    .forEach(routeDefinition -> routeDefinition.getPredicates().stream()
                            .filter(predicateDefinition -> ("Path").equalsIgnoreCase(predicateDefinition.getName()))
                            .forEach(predicateDefinition -> resources.add(swaggerResource(routeDefinition.getId(),
                                    predicateDefinition.getArgs().get(NameUtils.GENERATED_NAME_PREFIX + "0")
                                            .replace("/**", API_URI)))));
            return resources;
        }

        private SwaggerResource swaggerResource(String name, String location) {
            SwaggerResource swaggerResource = new SwaggerResource();
            swaggerResource.setName(name);
            swaggerResource.setLocation(location);
            return swaggerResource;
        }
    }

    class SwaggerHeaderFilter extends AbstractGatewayFilterFactory {
        private static final String HEADER_NAME = "X-Forwarded-Prefix";

        @Override
        public GatewayFilter apply(Object config) {
            return (exchange, chain) -> {
                ServerHttpRequest request = exchange.getRequest();
                String path = request.getURI().getPath();
                if (!StringUtils.endsWithIgnoreCase(path, GatewaySwaggerResourcesProvider.API_URI)) {
                    return chain.filter(exchange);
                }

                String basePath = path.substring(0, path.lastIndexOf(GatewaySwaggerResourcesProvider.API_URI));

                ServerHttpRequest newRequest = request.mutate().header(HEADER_NAME, basePath).build();
                ServerWebExchange newExchange = exchange.mutate().request(newRequest).build();
                return chain.filter(newExchange);
            };
        }
    }
}
