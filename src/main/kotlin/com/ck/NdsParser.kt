package com.ck

import com.ck.ast.*
import com.ck.token.IdentifierToken
import com.ck.token.Token
import com.ck.token.keyword.*
import com.ck.token.symbol.*
import kotlin.reflect.KClass

/**
 * @author 陈坤
 * 2023/10/2
 */
class NdsParser(
    private val text: String
) {

    private val ndsTokenizer = NdsTokenizer()
    private var lookahead: Token<*>? = null


    fun parse(): Program {
        this.ndsTokenizer.init(this.text)
        this.lookahead = this.ndsTokenizer.getNextToken()

        return this.program()
    }

    private fun program() = Program(this.statementList())

    private fun statementList(): List<ASTree> {
        val statementList = mutableListOf<ASTree>()
        statementList.add(this.namespaceStatement())
        statementList.add(this.importEntityStatement())

        this.optImportStatementList().let { if (it.isNotEmpty()) statementList.addAll(it) }

        statementList.addAll(this.funStatementList())

        return statementList
    }

    /**
     * NamespaceStatement
     *  : 'namespace' NamespaceIdentifier -> namespace com.ck.test
     *  ;
     */
    private fun namespaceStatement(): NamespaceStatement {
        this.consume(NamespaceToken::class)
        return NamespaceStatement(this.namespaceIdentifier(false))
    }

    /**
     * ImportEntityStatement
     *  : 'import' 'entity' NAMESPACE_IDENTIFIER -> import com.ck.test
     *  ;
     */
    private fun importEntityStatement(): ImportStatement {
        return this.importStatement(true)
    }

    /**
     * OptImportStatementList
     *  : ImportStatementList
     *  | ε
     *  ;
     */
    private fun optImportStatementList(): List<ImportStatement> {
        return if (this.test(ImportToken::class)) this.importStatementList() else listOf()
    }


    /**
     * ImportStatementList
     *  : ImportOptEntityStatement
     *  | ImportStatementList ImportOptEntityStatement
     *  ;
     */
    private fun importStatementList(): List<ImportStatement> {
        val importStatementList = mutableListOf(this.importStatement())

        while (this.test(ImportToken::class)) {
            importStatementList.add(this.importStatement())
        }
        return importStatementList
    }

    private fun importStatement(entity: Boolean = false): ImportStatement {
        this.consume(ImportToken::class)
        if (entity) this.consume(EntityToken::class)

        return ImportStatement(
            this.namespaceIdentifier(),
            entity
        )
    }

    /**
     * FunStatementList
     *  : FunStatement
     *  | FunStatement FunStatementList
     *  ;
     */
    private fun funStatementList(): List<ASTree> {
        val funStatementList = mutableListOf<ASTree>()
        while (this.lookahead != null) {
            funStatementList.add(this.funStatement())
        }

        return funStatementList
    }

    /**
     * FunStatement
     *  : SearchFunDeclaration
     *  ;
     */
    private fun funStatement(): ASTree {
        return when (this.lookahead) {
            is SearchToken -> this.searchFunDeclaration()
            else -> EmptyStatement
        }
    }

    /**
     * SearchFunDeclaration
     *  : 'search' Identifier '(' OptParameterList ')' OptSearchReturnDeclaration SearchFunBlockStatement
     *  ;
     */
    private fun searchFunDeclaration(): ASTree {
        this.consume(SearchToken::class)
        // Identifier
        val funName = this.consume(IdentifierToken::class)

        this.consume(OpenParenthesisToken::class)
        // OptParameterList
        val parameterList = if (this.test(ClosedParenthesisToken::class)) emptyList() else this.parameterList()
        this.consume(ClosedParenthesisToken::class)

        // OptSearchReturnDeclaration
        val returnInfo = this.optSearchReturnDeclaration()

        val body = this.searchFunBlockStatement()
        return SearchFunDeclaration(
            funName.value,
            parameterList,
            returnInfo,
            body
        )
    }

    /**
     * ParameterList
     *  : Parameter
     *  | ParameterList ',' Parameter
     *  ;
     */
    private fun parameterList(): List<Parameter> {
        val parameterList = mutableListOf<Parameter>()
        parameterList.add(this.parameter())

        while (this.test(CommaToken::class)) {
            this.consume(CommaToken::class)
            parameterList.add(this.parameter())
        }

        return parameterList
    }

    /**
     * Parameter
     *  : Identifier ':' ParameterType
     *  ;
     */
    private fun parameter(): Parameter {
        val parameterName = this.consume(IdentifierToken::class)
        this.consume(ColonToken::class)
        val parameterType = this.parameterType()

        return Parameter(
            parameterName.value,
            parameterType,
        )
    }

    /**
     * ParameterType
     *  : NamespaceIdentifier
     *  | 'mul' NamespaceIdentifier
     *  ;
     */
    private fun parameterType(): ParameterType {
        var isMul = false
        if (this.test(MulToken::class)) {
            this.consume(MulToken::class)
            isMul = true
        }

        val token = this.consume(IdentifierToken::class)
        return ParameterType(isMul, token.value)
    }

    /**
     * ReturnDeclaration
     *  : ':' SearchReturnDeclaration
     *  ;
     */
    private fun optSearchReturnDeclaration(): SearchReturnDeclaration {
        if (this.test(OpenCurlyBracketToken::class)) {
            // 如果是花括号开始, 直接返回
            return SearchReturnDeclaration.DEFAULT
        }

        this.consume(ColonToken::class)
        return this.searchReturnDeclaration()
    }

    /**
     * SearchReturnDeclaration
     *  : mul
     *  | mul NamespaceIdentifier
     *  ;
     */
    private fun searchReturnDeclaration(): SearchReturnDeclaration {
        return if (this.test(MulToken::class)) {
            this.consume(MulToken::class)
            if (this.test(OpenCurlyBracketToken::class)) {
                SearchReturnDeclaration.MUL_DEFAULT
            } else {
                val value = this.namespaceIdentifier()
                SearchReturnDeclaration(true, value)
            }
        } else {
            val returnType = this.namespaceIdentifier()
            SearchReturnDeclaration(false, returnType)
        }
    }


    /**
     * SearchFunBlockStatement
     *  : '{' SearchFunStatementList '}'
     *  ;
     */
    private fun searchFunBlockStatement(): SearchFunBlockStatement {
        this.consume(OpenCurlyBracketToken::class)
        val body = this.searchFunStatementList()
        this.consume(ClosedCurlyBracketToken::class)

        return SearchFunBlockStatement(body)
    }

    /**
     * SearchFunStatementList
     *  : SearchStatement
     *  | SearchFunStatementList SearchStatement
     *  ;
     */
    private fun searchFunStatementList(): List<ASTree> {
        val list = mutableListOf<ASTree>()

        val sqlLiteralList = mutableListOf<SqlLiteral>()

        while (!this.test(ClosedCurlyBracketToken::class)) {
            val searchStatement = this.searchStatement()

            if (searchStatement is SqlLiteral) {
                sqlLiteralList.add(searchStatement)
            } else {
                if (sqlLiteralList.isNotEmpty()) {
                    val compressedSql = sqlLiteralList.joinToString(" ") { it.value }
                    sqlLiteralList.clear()
                    list.add(SqlLiteral(compressedSql))
                }
                list.add(searchStatement)
            }
        }

        return list
    }

    /**
     * SearchStatement
     *  : SqlLiteral
     *  | MemberExpression
     *  ;
     */
    private fun searchStatement(): ASTree {
        return when (this.lookahead) {
            is ColonToken -> this.memberExpression()
            else -> this.sqlLiteral()
        }
    }

    /**
     * MemberExpression
     *  : ':' Identifier
     *  ;
     */
    private fun memberExpression(): ASTree {
        this.consume(ColonToken::class)
        val root = this.consume(IdentifierToken::class)

        // TODO: 变量属性待完善 
        return MemberExpression(root.value, null)
    }

    private fun sqlLiteral(): ASTree {
        val token = when (this.lookahead) {
            is IdentifierToken -> this.consume(IdentifierToken::class)
            is StartToken -> this.consume(StartToken::class)
            is SimpleAssignToken -> this.consume(SimpleAssignToken::class)
            is OpenParenthesisToken -> this.consume(OpenParenthesisToken::class)
            is ClosedParenthesisToken -> this.consume(ClosedParenthesisToken::class)
            is CommaToken -> this.consume(CommaToken::class)
            else -> throw SyntaxException("未知类型: ${this.lookahead}")
        }

        return SqlLiteral(token.value)
    }


    /**
     * 命名空间标识符
     *
     * @param start 是否可以匹配最后的星号 默认true
     * @return 包名称
     */
    private fun namespaceIdentifier(start: Boolean = true): String {
        // 至少存在一个
        val namespaceList = StringBuilder(this.consume(IdentifierToken::class).value)

        while (this.test(DotToken::class)) {
            namespaceList.append(this.consume(DotToken::class).value)

            if (this.test(IdentifierToken::class)) {
                namespaceList.append(this.consume(IdentifierToken::class).value)
            } else if (start && this.test(StartToken::class)) {
                namespaceList.append(this.consume(StartToken::class).value)

                // 最后的星号如果匹配成功, 那么则需要结束匹配
                break
            }
        }

        return namespaceList.toString()
    }

    private fun <T : Token<*>> test(tokenType: KClass<T>) = tokenType.isInstance(this.lookahead)

    private fun <T : Token<*>> consume(tokenType: KClass<T>): T {
        val token = this.lookahead
            ?: throw SyntaxException("Unexpected end of input, expected: ${tokenType.simpleName}")

        if (!tokenType.isInstance(token)) {
            throw SyntaxException("Unexpected token: [${token.value}] -> ${token.javaClass.simpleName}, expected: ${tokenType.simpleName}")
        }

        this.lookahead = this.ndsTokenizer.getNextToken()

        @Suppress("UNCHECKED_CAST")
        return token as T
    }

}
