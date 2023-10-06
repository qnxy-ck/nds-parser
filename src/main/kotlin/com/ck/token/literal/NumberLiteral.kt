package com.ck.token.literal

import com.ck.token.Token

/**
 * @author 陈坤
 * 2023/10/7
 */
data class NumberLiteral(
    override val value: String,
    val numberType: NumberType
) : Token<String>

/**
 * 数字类型
 */
enum class NumberType {
    INT,
    LONG,
    FLOAT,
    DOUBLE,

}
