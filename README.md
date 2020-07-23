# swagger-spring-boot-starter
swagger自动配置
```yaml
swagger:
  enable: true #默认启用
  title: 标题 #为null,取spring.application.name值，为空字符则不展示标题
  description: 这是文档说明 #title和description都为空字符，则隐藏文档说明页面元素
  api-keys: #配置鉴权信息
    - name: 鉴权
      keyname: Auth
      pass-as: header # header/cookie
  resources: #网关层可配置聚合各服务的文档地址
    - name: 文档1
      url: /v2/api-docs
    - name: 文档2
      url: /v2/api-docs2
  ignore-types: #忽略方法指定类型的参数
    - ch.qos.logback.classic.AsyncAppender
    - org.springframework.web.multipart.MultipartFile
```