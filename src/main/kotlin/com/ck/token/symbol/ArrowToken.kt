package com.ck.token.symbol

import com.ck.token.Token

/**
 * @author 陈坤
 * 2023/10/7
 */
object ArrowToken : Token<String> {
    override val value: String
        get() = "->"
}