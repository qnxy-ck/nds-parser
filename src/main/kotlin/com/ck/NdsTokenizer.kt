package com.ck

import com.ck.token.NamespaceIdentifierToken
import com.ck.token.Token
import com.ck.token.keyword.EntityToken
import com.ck.token.keyword.ImportToken
import com.ck.token.keyword.NamespaceToken

/**
 * @author 陈坤
 * 2023/10/2
 */
class NdsTokenizer {

    companion object {
        private val REGEXP_INFO_ARR = arrayOf<Pair<Regex, (String) -> Token<*>?>>(

            // --------------------------------------------------------------
            // 跳过空白符
            """^\s+""".toRegex() to { null },

            // 跳过单行注释
            "^//.*".toRegex() to { null },

            // 跳过多行注释
            "^/\\*[\\s\\S]*?\\*/".toRegex() to { null },


            // --------------------------------------------------------------
            // 关键字
            "^\\bnamespace\\b".toRegex() to { NamespaceToken },
            "^\\bimport\\b".toRegex() to { ImportToken },
            "^\\bentity\\b".toRegex() to { EntityToken },


            // --------------------------------------------------------------
            // Identifier
            "[a-zA-Z]+[0-9a-zA-Z_]*(\\.[a-zA-Z]+[0-9a-zA-Z_]*)*".toRegex() to { NamespaceIdentifierToken(it) }
        )
    }


    private lateinit var text: String

    private var textInit = false;
    private var cursor = 0

    fun init(text: String) {
        this.text = text
        this.textInit = true
    }

    private fun hasMoreTokens(): Boolean {
        if (!textInit) {
            throw RuntimeException("请先调用初始化方法(#.init(string))")
        }
        return this.cursor < this.text.length
    }

    fun getNextToken(): Token<*>? {
        if (!hasMoreTokens()) return null

        val input = this.text.substring(this.cursor)
        for ((regexp, tokenBuilder) in REGEXP_INFO_ARR) {
            val tokenValue = regexp.matchAt(input, 0)?.value ?: continue

            this.cursor += tokenValue.length
            return tokenBuilder(tokenValue) ?: this.getNextToken()
        }

        throw SyntaxException("Unexpected token: [${this.text[this.cursor]}]")
    }


}
