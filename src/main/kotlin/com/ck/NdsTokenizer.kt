package com.ck

import com.ck.token.IdentifierToken
import com.ck.token.Token
import com.ck.token.keyword.*
import com.ck.token.literal.NumberLiteral
import com.ck.token.literal.NumberType
import com.ck.token.literal.StringLiteralToken
import com.ck.token.symbol.*
import kotlin.enums.EnumEntries

/**
 * @author 陈坤
 * 2023/10/2
 */
class NdsTokenizer {

    companion object {
        private val REGEXP_INFO_ARR = arrayOf<Pair<Regex, (String) -> Token<*>?>>(

            // --------------------------------------------------------------
            // 匹配换行
            "\\n".toRegex() to { NewLineToken },
            "\\r\\n".toRegex() to { NewLineToken },


            // --------------------------------------------------------------
            // 跳过空白符
            """[ \t]+""".toRegex() to { null },

            // --------------------------------------------------------------
            // 跳过单行注释
            "^//.*".toRegex() to { null },


            // --------------------------------------------------------------
            // 跳过多行注释
            "^/\\*[\\s\\S]*?\\*/".toRegex() to { null },

            // --------------------------------------------------------------
            // 符号
            "^\\(".toRegex() to { OpenParenthesisToken },
            "^\\)".toRegex() to { ClosedParenthesisToken },
            "^,".toRegex() to { CommaToken },
            "^::".toRegex() to { DoubleColonToken },
            "^\\?\\.".toRegex() to { QuestionMarkPointToken },
            "^:".toRegex() to { ColonToken },
            "^\\{".toRegex() to { OpenCurlyBracketToken },
            "^}".toRegex() to { ClosedCurlyBracketToken },
            "^\\.".toRegex() to { DotToken },
            "^\\*".toRegex() to { StartToken },
            "^#".toRegex() to { PoundSignToken },


            // --------------------------------------------------------------
            // 关键字
            "^\\bnamespace \\b".toRegex() to { NamespaceToken },
            "^\\bimport \\b".toRegex() to { ImportToken },
            "^\\bentity \\b".toRegex() to { EntityToken },
            "^\\bsearch \\b".toRegex() to { SearchToken },
            "^\\bmul \\b".toRegex() to { MulToken },

            "^\\btrue\\b".toRegex() to { TrueToken },
            "^\\bfalse\\b".toRegex() to { FalseToken },
            "^\\bnull\\b".toRegex() to { NullToken },

            // --------------------------------------------------------------
            // 数字
            "[-+]\\d+\\.\\d+F".toRegex() to { NumberLiteral(it, NumberType.FLOAT) },
            "[-+]?\\d+\\.\\d+".toRegex() to { NumberLiteral(it, NumberType.DOUBLE) },
            "^[-+]?\\d+L".toRegex() to { NumberLiteral(it, NumberType.LONG) },
            "^[-+]?\\d+".toRegex() to { NumberLiteral(it, NumberType.INT) },

            // --------------------------------------------------------------
            // 标识符
            "^\\w+".toRegex() to { IdentifierToken(it) },

            // --------------------------------------------------------------
            // 等式运算符: ==. !=
            "^[=!]=".toRegex() to { this.assign(EqualityOperatorToken.entries, it) },


            // --------------------------------------------------------------
            // 赋值运算符: =, *=, /=, +=. -=
            "^=".toRegex() to { SimpleAssignToken },
            "^[*/+-]=".toRegex() to { this.assign(ComplexAssignToken.entries, it) },


            // --------------------------------------------------------------
            // 数学运算符: +, -, *, /
            "^[+-]".toRegex() to { this.assign(AdditiveOperatorToken.entries, it) },
            "^[*/]".toRegex() to { this.assign(MultiplicativeOperatorToken.entries, it) },


            // --------------------------------------------------------------
            // 关系运算符 >, >=, <, <=
            "^[><]=?".toRegex() to { this.assign(RelationalOperatorToken.entries, it) },


            // --------------------------------------------------------------
            // 逻辑运算符 &&, ||
            "^&&".toRegex() to { LogicalAndToken },
            "^\\|\\|".toRegex() to { LogicalOrToken },
            "^!".toRegex() to { LogicalNotToken },


            // --------------------------------------------------------------
            // 字符串
            "^\"[^\"]*\"".toRegex() to { StringLiteralToken(it) }

        )

        private fun <E> assign(v: EnumEntries<E>, value: String): E where E : Enum<E>, E : Token<*> {
            return v.find { it.value == value } ?: throw SyntaxException("无法匹配: $value -> ${v::class.java.simpleName}")
        }
    }


    private lateinit var text: String

    private var textInit = false
    private var cursor = 0

    private var lineNum = 1
    private var columnNum = 1

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

            this.columnNum += tokenValue.length
            val token = tokenBuilder(tokenValue) ?: return this.getNextToken()

            if (token is NewLineToken) {
                this.addLineNum()
                // return this.getNextToken()
            }
            return token
        }

        throw SyntaxException("Unexpected token: \n${input.substring(0, input.indexOf("\r\n"))} -> line/column: ${this.lineNum}/${this.columnNum}")
    }

    private fun addLineNum() {
        this.lineNum += 1
        this.columnNum = 1
    }


}
