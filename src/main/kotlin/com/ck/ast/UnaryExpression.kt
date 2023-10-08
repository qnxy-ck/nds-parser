package com.ck.ast

import com.ck.token.BinaryOperatorToken

/**
 * @author 陈坤
 * 2023/10/7
 */
data class UnaryExpression(
    val operator: BinaryOperatorToken,
    val argument: ASTree
) : ASTree
