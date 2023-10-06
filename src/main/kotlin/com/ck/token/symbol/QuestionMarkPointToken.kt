package com.ck.token.symbol

import com.ck.token.Token

/**
 * 条件控制函数调用符号
 *
 * @author 陈坤
 * 2023/10/5
 */
object QuestionMarkPointToken : Token<String> {
    override val value: String
        get() = "?."
}

