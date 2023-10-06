package com.ck.token.keyword

import com.ck.token.LiteralToken

/**
 * @author 陈坤
 * 2023/10/7
 */
object NullToken : LiteralToken<String> {
    override val value: String
        get() = "null"

}