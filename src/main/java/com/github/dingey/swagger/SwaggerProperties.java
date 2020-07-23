package com.github.dingey.swagger;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import springfox.documentation.swagger.web.SwaggerResource;

import java.util.List;

@Getter
@Setter
@RefreshScope
@ConfigurationProperties(prefix = "swagger")
class SwaggerProperties {
    /**
     * enable for swagger auto configuration
     */
    private boolean enable = true;
    /**
     * title for swagger
     */
    private String title;
    /**
     * description for swagger
     */
    private String description = "";
    /**
     * 版本
     */
    private String version = "";
    /**
     * Scan path
     */
    private String basePackage = "";
    /**
     * url path add to swagger
     */
    private String[] paths;
    /**
     * Authentication list
     * <br>
     * swagger: <br>
     * api-keys: <br>
     * - name: authentication name <br>
     * keyname: authentication code <br>
     * pass-as: header/cookie <br>
     */
    private List<ApiKeyYML> apiKeys;
    /**
     * Document aggregation, cooperate with gateway
     * <br>
     * swagger: <br>
     * resources: <br>
     * - name: 文档1 <br>
     * url: /v2/api-docs <br>
     * - name: 文档2 <br>
     * url: /v2/api-docs2 <br>
     */
    private List<SwaggerResource> resources;

    /**
     * which class type ignore in method parameter
     */
    private List<Class> ignoreTypes;
}
