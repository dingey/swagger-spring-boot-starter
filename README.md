# swagger-spring-boot-starter
swagger自动配置，默认引入依赖即可无需配置任何属性。支持zuul网关和spring gateway网关。
```
<dependency>
    <groupId>com.github.dingey</groupId>
    <artifactId>swagger-spring-boot-starter</artifactId>
    <version>1.1</version>
</dependency>
```
###配置示例
```yaml
swagger:
  enable: true #默认启用
  title: 标题 #为null,取spring.application.name值，为空字符则不展示标题
  description: 这是文档说明 #title和description都为空字符，则隐藏页面头元素
  api-keys: #配置鉴权信息
    - name: 鉴权
      keyname: Auth
      pass-as: header # header/cookie
  resources: #网关层可配置聚合各服务的文档地址,未配置取网关的配置
    - name: 文档1
      url: /v2/api-docs
    - name: 文档2
      url: /v2/api-docs2
  ignore-types: #忽略方法指定类型的参数
    - ch.qos.logback.classic.AsyncAppender
    - org.springframework.web.multipart.MultipartFile
spring: #对spring gateway网关的支持
  cloud:
    gateway:
      routes:
        - id:  user
          uri: lb://sample-user
          predicates:
          - Path=/user/**
          filters:
          - StripPrefix=1
zuul: #对对spring zuul网关的支持
  routes:
    user:
      path: /user/**
      serviceId: user-service
```