package com.ck.ast

/**
 * @author 陈坤
 * 2023/10/3
 */
data class Parameter(
    val varName: String,
    val varType: String
) : ASTree
