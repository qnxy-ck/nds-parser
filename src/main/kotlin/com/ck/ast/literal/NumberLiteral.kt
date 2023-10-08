package com.ck.ast.literal

import com.ck.ast.ASTree
import com.ck.token.NumberType

/**
 * @author 陈坤
 * 2023/10/7
 */
data class NumberLiteral(
    val value: String,
    val numberType: NumberType
) : ASTree
