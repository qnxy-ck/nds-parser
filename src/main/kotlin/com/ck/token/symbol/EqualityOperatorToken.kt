package com.ck.token.symbol

import com.ck.token.BinaryOperatorToken

/**
 * 等式运算符
 * ==
 * !=
 *
 * @author 陈坤
 * 2023/10/7
 */
enum class EqualityOperatorToken(
    private val operator: String
) : BinaryOperatorToken {

    EQUALITY("=="),
    NOT_EQUALITY("!="),
    ;

    override val value: String
        get() = this.operator
}