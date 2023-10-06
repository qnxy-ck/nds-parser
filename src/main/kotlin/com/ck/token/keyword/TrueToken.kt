package com.ck.token.keyword

import com.ck.token.BooleanToken

/**
 * @author 陈坤
 * 2023/10/7
 */
object TrueToken : BooleanToken {
    override val value: Boolean
        get() = true
}