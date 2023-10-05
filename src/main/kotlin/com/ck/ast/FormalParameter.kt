package com.ck.ast

/**
 * 形参信息
 * 
 * @author 陈坤
 * 2023/10/3
 */
data class FormalParameter(
    /**
     * 参数名称
     */
    val parameterName: String,

    /**
     * 返回类型是否为多个
     */
    val multiple: Boolean,

    /**
     * 返回类型
     */
    val parameterType: String
) : ASTree
