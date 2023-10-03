package com.ck.token

/**
 * @author 陈坤
 * 2023/10/3
 */
data class IdentifierToken(
    override val value: String
) : Token<String>
