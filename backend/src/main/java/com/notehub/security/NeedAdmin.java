package com.notehub.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记接口需要管理员权限 (含登录校验, 见 AuthAspect)
 * 所有 /api/admin/** 接口必须标注 —— 原项目缺失的鉴权在本项目补全
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface NeedAdmin {
}
