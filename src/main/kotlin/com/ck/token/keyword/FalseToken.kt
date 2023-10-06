package com.ck.token.keyword

import com.ck.token.BooleanToken

/**
 * @author 陈坤
 * 2023/10/7
 */
object FalseToken : BooleanToken {
    override val value: Boolean
        get() = false
}