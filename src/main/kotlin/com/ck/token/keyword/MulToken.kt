package com.ck.token.keyword

import com.ck.token.Token

/**
 * @author 陈坤
 * 2023/10/5
 */
object MulToken : Token<String> {
    override val value: String
        get() = "mul"
}