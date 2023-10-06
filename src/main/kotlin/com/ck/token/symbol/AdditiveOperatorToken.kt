package com.ck.token.symbol

import com.ck.token.Token

/**
 * 加减法运算符
 *
 * @author 陈坤
 * 2023/10/7
 */
enum class AdditiveOperatorToken(
    private val operator: String
) : Token<String> {

    ADDITION("+"),
    SUBTRACTION("-"),
    ;

    override val value: String
        get() = this.operator
}