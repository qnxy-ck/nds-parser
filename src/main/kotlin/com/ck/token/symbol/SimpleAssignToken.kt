package com.ck.token.symbol

import com.ck.token.Token

/**
 * @author 陈坤
 * 2023/10/3
 */
object SimpleAssignToken : Token<String> {
    override val value: String
        get() = "="
}