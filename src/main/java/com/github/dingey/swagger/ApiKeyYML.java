package com.github.dingey.swagger;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@SuppressWarnings("unused")
class ApiKeyYML {
    /**
     * 令牌名称
     */
    private String name;
    /**
     * 令牌编码
     */
    private String keyname;
    /**
     * header/cookie
     */
    private String passAs;
}
