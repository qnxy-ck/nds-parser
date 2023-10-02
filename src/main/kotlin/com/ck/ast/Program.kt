package com.ck.ast

/**
 * @author 陈坤
 * 2023/10/2
 */
data class Program(
    val body: List<ASTree>
) : ASTree
