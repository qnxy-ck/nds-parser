package com.ck.ast

/**
 * ?. 控制语句
 *
 * @author 陈坤
 * 2023/10/7
 */
data class SimpleIfStatement(
    val test: ASTree,
    val consequent: ASTree
) : ASTree
