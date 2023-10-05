package com.ck.ast

/**
 * @author 陈坤
 * 2023/10/3
 */
data class MemberExpression(
    val obj: ASTree,
    val property: ASTree?
) : ASTree
