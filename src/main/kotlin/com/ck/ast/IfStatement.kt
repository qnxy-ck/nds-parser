package com.ck.ast

/**
 * @author 陈坤
 * 2023/10/5
 */
data class IfStatement(
    val test: ASTree,

    val consequent: ASTree,

    ) : ASTree
