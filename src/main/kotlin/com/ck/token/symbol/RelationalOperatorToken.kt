package com.ck.token.symbol

import com.ck.token.Token

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
) : Token<String> {

    LT("<"),
    LE("<="),
    GT(">"),
    GE(">="),
    ;

    override val value: String
        get() = this.operator
}