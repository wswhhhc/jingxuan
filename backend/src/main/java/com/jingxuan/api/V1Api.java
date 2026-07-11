package com.jingxuan.api;

import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** 标记 v1 REST Controller，使其使用 RFC Problem Details 错误契约。 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@RequestMapping("/api/v1")
public @interface V1Api {
}
