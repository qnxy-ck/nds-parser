package com.ck.ast

/**
 * @author 陈坤
 * 2023/10/3
 */
data class SearchReturnDeclaration(
    val returnType: String
) : ASTree {

    companion object {
        val DEFAULT = SearchReturnDeclaration("")
    }
}
