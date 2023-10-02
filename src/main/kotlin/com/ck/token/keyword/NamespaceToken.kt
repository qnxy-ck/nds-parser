package com.ck.token.keyword

import com.ck.token.Token

/**
 * keyword namespace
 *
 * @author 陈坤
 * 2023/10/2
 */
data object NamespaceToken : Token<String> {
    override val value: String
        get() = "namespace"
}
