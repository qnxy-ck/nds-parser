package com.ck.token.literal

import com.ck.token.LiteralToken

/**
 * @author 陈坤
 * 2023/10/7
 */
data class StringLiteralToken(override val value: String) : LiteralToken<String>
