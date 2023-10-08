package com.ck.token.symbol

import com.ck.token.BinaryOperatorToken

/**
 * 乘除法运算符
 *
 * @author 陈坤
 * 2023/10/7
 */
enum class MultiplicativeOperatorToken(
    private val operator: String
) : BinaryOperatorToken {

    MULTIPLICATION("*"),
    DIVISION("/"),
    ;

    override val value: String
        get() = this.operator
}