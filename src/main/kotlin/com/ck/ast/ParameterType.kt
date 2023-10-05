package com.ck.ast

/**
 * @author 陈坤
 * 2023/10/5
 */
data class ParameterType(
    val multiple: Boolean,
    val value: String
) : ASTree
