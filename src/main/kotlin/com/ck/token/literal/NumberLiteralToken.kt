package com.ck.token.literal

import com.ck.token.NumberType
import com.ck.token.Token

/**
 * @author 陈坤
 * 2023/10/7
 */
data class NumberLiteralToken(
    override val value: String,
    val numberType: NumberType
) : Token<String>


