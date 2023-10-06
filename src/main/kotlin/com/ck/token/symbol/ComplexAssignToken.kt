package com.ck.token.symbol

import com.ck.token.Token

/**
 * 复杂赋值运算符
 * -=
 * +=
 * *=
 * /=
 *
 * @author 陈坤
 * 2023/10/7
 */
enum class ComplexAssignToken(
    private val operator: String
) : Token<String> {

    MUL_ASSIGN("*="),
    DIV_ASSIGN("/="),
    ADD_ASSIGN("+="),
    SUB_ASSIGN("-="),
    ;

    override val value: String
        get() = this.operator
}