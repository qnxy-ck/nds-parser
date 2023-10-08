package com.ck.token.symbol

import com.ck.token.BinaryOperatorToken

/**
 * 关系运算符
 * >
 * >=
 * <
 * <=
 *
 * @author 陈坤
 * 2023/10/7
 */
enum class RelationalOperatorToken(
    private val operator: String
) : BinaryOperatorToken {

    LT("<"),
    LE("<="),
    GT(">"),
    GE(">="),
    ;

    override val value: String
        get() = this.operator
}