package com.ck.ast


/**
 * 函数调用
 *
 * @author 陈坤
 * 2023/10/5
 */
data class CallExpression(
    /**
     * 调用者
     */
    val callee: MemberExpression,

    /**
     * 调用方法
     */
    val funName: String,

    /**
     * 方法参数
     */
    val arguments: List<ASTree>
) : ASTree
