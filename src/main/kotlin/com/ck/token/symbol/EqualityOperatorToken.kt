package com.ck.token.symbol

import com.ck.token.Token

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
) : Token<String> {

    EQUALITY("=="),
    NOT_EQUALITY("!="),
    ;

    companion object {
//        fun assign(operator: String): EqualityOperatorToken {
//            return if (operator == EQUALITY.operator) EQUALITY else NOT_EQUALITY
//        }
    }

    override val value: String
        get() = this.operator
}