package com.ck.ast

/**
 * @author 陈坤
 * 2023/10/3
 */
data class SearchReturnDeclaration(
    // 是否为多个返回值 
    val multiple: Boolean,
    val returnType: String?
) : ASTree {

    companion object {
        val DEFAULT = SearchReturnDeclaration(false, null)
        val MUL_DEFAULT = SearchReturnDeclaration(true, null)
        
    }
}
