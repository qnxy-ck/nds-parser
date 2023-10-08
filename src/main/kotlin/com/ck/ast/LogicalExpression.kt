package com.ck.ast

import com.ck.token.BinaryOperatorToken

/**
 * @author 陈坤
 * 2023/10/7
 */
data class LogicalExpression(
    val operator: BinaryOperatorToken,
    val left: ASTree,
    val right: ASTree
) : ASTree
