package com.ck.token.symbol

import com.ck.token.BinaryOperatorToken

/**
 * @author 陈坤
 * 2023/10/3
 */
object SimpleAssignToken : BinaryOperatorToken {
    override val value: String
        get() = "="
}