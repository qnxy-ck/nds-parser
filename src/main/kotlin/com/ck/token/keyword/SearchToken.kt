package com.ck.token.keyword

import com.ck.token.Token

/**
 * @author 陈坤
 * 2023/10/3
 */
object SearchToken : Token<String> {
    override val value: String
        get() = "search"
}