package com.ck.ast

/**
 * @author 陈坤
 * 2023/10/3
 */
data class SearchFunDeclaration(
    val funName: String,
    val parameters: List<Parameter>,
    val returnInfo: SearchReturnDeclaration?,
    val body: ASTree?
) : ASTree