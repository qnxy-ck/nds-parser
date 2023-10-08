package com.ck.token.symbol

import com.ck.token.BinaryOperatorToken

/**
 * @author 陈坤
 * 2023/10/7
 */
object LogicalAndToken : BinaryOperatorToken {
    override val value: String
        get() = "&&"
}