package com.github.dingey.swagger;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import io.swagger.annotations.ApiOperation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.*;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.ApiSelectorBuilder;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author d
 */
@EnableSwagger2
@Configuration
@ConditionalOnClass(name = "org.springframework.web.servlet.HandlerMapping")
@ConditionalOnProperty(value = "swagger.enable", matchIfMissing = true)
@EnableConfigurationProperties({SwaggerProperties.class})
public class SwaggerAutoConfiguration {
    private static final Logger log = LoggerFactory.getLogger(SwaggerAutoConfiguration.class);

    private final SwaggerProperties swaggerProperties;
    private final Environment env;

    public SwaggerAutoConfiguration(SwaggerProperties swaggerProperties, Environment env) {
        this.swaggerProperties = swaggerProperties;
        this.env = env;
    }

    @PostConstruct
    public void init() {
        if (log.isDebugEnabled()) {
            log.debug("Initializing Swagger2");
        }
    }

    @Bean
    public Docket createRestApi() {
        Docket docket = new Docket(DocumentationType.SWAGGER_2)
                .ignoredParameterTypes(HttpServletRequest.class, HttpServletResponse.class, HttpSession.class, Model.class)
                .apiInfo(apiInfo());
        if (swaggerProperties.getIgnoreTypes() != null) {
            for (Class clazz : swaggerProperties.getIgnoreTypes()) {
                docket.ignoredParameterTypes(clazz);
            }
        }
        ApiSelectorBuilder builder = docket.select().paths(PathSelectors.any());
        if (StringUtils.hasText(swaggerProperties.getBasePackage())) {
            builder.apis(RequestHandlerSelectors.basePackage(swaggerProperties.getBasePackage()));
        } else if (swaggerProperties.getPaths() != null && swaggerProperties.getPaths().length > 0) {
            List<Predicate<String>> predicates = Stream.of(swaggerProperties.getPaths()).map(PathSelectors::regex).collect(Collectors.toList());
            builder.paths(Predicates.or(predicates));
        } else if (!swaggerProperties.isAll()) {
            builder.apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class));
        }
        docket = builder.build();
        if (swaggerProperties.getApiKeys() != null && !swaggerProperties.getApiKeys().isEmpty()) {
            docket.securitySchemes(securitySchemes());
            docket.securityContexts(securityContexts());
        }
        return docket;
    }

    private ApiInfo apiInfo() {
        ApiInfoBuilder apiInfoBuilder = new ApiInfoBuilder()
                .description(swaggerProperties.getDescription())
                .version(swaggerProperties.getVersion());
        if (swaggerProperties.getTitle() == null && StringUtils.hasText(env.getProperty("spring.application.name"))) {
            apiInfoBuilder.title(env.getProperty("spring.application.name"));
        } else {
            apiInfoBuilder.title(swaggerProperties.getTitle());
        }
        return apiInfoBuilder.build();
    }

    private List<ApiKey> securitySchemes() {
        List<ApiKey> list = new ArrayList<>(swaggerProperties.getApiKeys().size());
        for (ApiKeyYML key : swaggerProperties.getApiKeys()) {
            ApiKey apiKey = new ApiKey(key.getName(), key.getKeyname(), key.getPassAs());
            list.add(apiKey);
        }
        return list;
    }

    private List<SecurityContext> securityContexts() {
        SecurityContext securityContext = SecurityContext.builder()
                .securityReferences(defaultAuth())
                .forPaths(PathSelectors.any())
                .build();
        return Collections.singletonList(securityContext);
    }

    private List<SecurityReference> defaultAuth() {
        AuthorizationScope authorizationScope = new AuthorizationScope("global", "accessEverything");
        List<SecurityReference> references = new ArrayList<>(swaggerProperties.getApiKeys().size());

        AuthorizationScope[] authorizationScopes = new AuthorizationScope[1];
        authorizationScopes[0] = authorizationScope;

        for (ApiKeyYML keyYML : swaggerProperties.getApiKeys()) {
            references.add(new SecurityReference(keyYML.getName(), authorizationScopes));
        }
        return references;
    }
}
