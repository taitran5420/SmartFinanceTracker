package com.seap.smartfinancetracker.security.annotation;

import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.lang.annotation.*;

@Target({ElementType.PARAMETER, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@SuppressWarnings("SpringElInspection")
@AuthenticationPrincipal(expression = "id")
public @interface CurrentUserId {
}
