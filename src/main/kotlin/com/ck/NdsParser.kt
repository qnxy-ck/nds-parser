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

    /**
     * statementList
     *  : NamespaceStatement
     *  | ImportEntityStatement
     *  | OptImportStatementList
     *  | FunStatementList
     *  ;
     *
     */
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
            .also { this.effortConsumption() }
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
            namespaceIdentifier(),
            entity
        )
            .also { this.effortConsumption() }
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
            if (this.test(NewLineToken::class)) {
                this.effortConsumption()
            }
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
            else -> throw SyntaxException("未知Token(funStatement): [${this.lookahead}]")
        }
    }

    /**
     * SearchFunDeclaration
     *  : 'search' Identifier FormalParameters OptSearchReturnDeclaration SearchFunBlockStatement
     *  ;
     */
    private fun searchFunDeclaration(): ASTree {
        this.consume(SearchToken::class)
        // Identifier
        val funName = this.identifier()
        // FormalParameters
        val parameterList = this.formalParameters()

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
     * FormalParameters
     *  : '(' OptFormalParameterList ')'
     *  ;
     */
    private fun formalParameters(): List<FormalParameter> {
        this.consume(OpenParenthesisToken::class)

        // OptFormalParameterList
        val formalParameterList = if (this.test(ClosedParenthesisToken::class)) emptyList() else this.formalParameterList()

        this.consume(ClosedParenthesisToken::class)
        return formalParameterList
    }

    /**
     * FormalParameterList
     *  : FormalParameter
     *  | FormalParameterList ',' FormalParameter
     *  ;
     */
    private fun formalParameterList(): List<FormalParameter> {
        val formalParameterList = mutableListOf(this.formalParameter())

        while (this.matchAndConsume(ColonToken::class) != null) {
            formalParameterList.add(this.formalParameter())
        }
        return formalParameterList
    }


    /**
     * FormalParameter
     *  : Identifier ':' NamespaceIdentifier
     *  | Identifier ':' 'mul' NamespaceIdentifier
     *  ;
     */
    private fun formalParameter(): FormalParameter {
        val parameterName = this.identifier()
        this.consume(ColonToken::class)

        val isMul = this.matchAndConsume(MulToken::class) != null

        val type = this.namespaceIdentifier(false)
        return FormalParameter(
            parameterName.value,
            isMul,
            type,
        )
    }

    /**
     * OptSearchReturnDeclaration
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
        return if (this.matchAndConsume(MulToken::class) != null) {
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
        this.effortConsumption()
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
                continue
            }


            list.add(searchStatement)
        }

        return list
    }

    /**
     * SearchStatement
     *  : SqlLiteral
     *  | CallMemberExpression
     *  ;
     */
    private fun searchStatement(): ASTree {
        return when (this.lookahead) {
            is ColonToken -> this.callMemberExpression()
            is NewLineToken -> this.newLineLiteral()
            else -> this.sqlLiteral()
        }
    }


    /**
     * CallMemberExpression
     *  : Members
     *  | Members CallExpression
     *  ;
     */
    private fun callMemberExpression(): ASTree {
        val members = this.members()
        if (this.test(DoubleColonToken::class) || this.test(QuestionMarkPointToken::class)) {
            return this.callExpression(members)
        }

        return members
    }

    /**
     * 属性调用函数
     *
     * CallExpression
     *  : '::' Identifier OptArguments
     *  ;
     */
    private fun callExpression(callee: MemberExpression): CallExpression {
        this.consume(DoubleColonToken::class)

        val funName = this.identifier().value

        return CallExpression(
            callee,
            funName,
            this.optArguments()
        )
    }

    /**
     * 可选参数以及括号
     *
     * OptArguments
     *  : '(' OptArgumentList ')'
     *  ;
     */
    private fun optArguments(): List<ASTree> {
        if (this.test(OpenParenthesisToken::class)) {

            this.consume(OpenParenthesisToken::class)
            // OptArgumentList
            val optArgumentList = if (this.test(ClosedParenthesisToken::class)) emptyList() else this.argumentList()
            this.consume(ClosedParenthesisToken::class)

            return optArgumentList
        }

        return emptyList()
    }

    /**
     * 函数参数列表
     *
     * ArgumentList
     *  : Argument
     *  | ArgumentList ',' Argument
     *  ;
     */
    private fun argumentList(): List<ASTree> {
        val argumentList = mutableListOf<ASTree>()
        argumentList.add(this.argument())

        while (this.matchAndConsume(CommaToken::class) != null) {
            argumentList.add(this.argument())
        }

        return argumentList
    }

    /**
     * 函数参数
     *
     * Argument
     *  : Identifier
     *  ;
     */
    private fun argument(): ASTree {
        return when (this.lookahead) {
            is IdentifierToken -> this.identifier()
            else -> throw SyntaxException("未知的参数类型(Argument) -> ${this.lookahead?.javaClass?.simpleName}")
        }
    }

    /**
     * Members
     *  : ':' MemberExpression
     *  ;
     */
    private fun members(): MemberExpression {
        this.consume(ColonToken::class)
        return this.memberExpression()
    }

    /**
     * MemberExpression
     *  : Identifier
     *  | MemberExpression '.' Identifier
     *  ;
     */
    private fun memberExpression(): MemberExpression {
        var member: ASTree = this.identifier()

        var existProperty = false
        while (this.matchAndConsume(DotToken::class) != null) {
            val property = this.identifier()
            member = MemberExpression(member, property)
            existProperty = true
        }

        if (existProperty) {
            return member as MemberExpression
        }

        return MemberExpression(member, null)
    }

    /**
     * SqlLiteral
     *  : IDENTIFIER
     *  | '*'
     *  | '='
     *  | '('
     *  | ')'
     *  | ','
     *  ;
     *
     */
    private fun sqlLiteral(): SqlLiteral {
        val token = when (this.lookahead) {
            is IdentifierToken -> this.consume(IdentifierToken::class)
            is StartToken -> this.consume(StartToken::class)
            is SimpleAssignToken -> this.consume(SimpleAssignToken::class)
            is OpenParenthesisToken -> this.consume(OpenParenthesisToken::class)
            is ClosedParenthesisToken -> this.consume(ClosedParenthesisToken::class)
            is CommaToken -> this.consume(CommaToken::class)
            else -> throw SyntaxException("未知sql类型: ${this.lookahead?.javaClass?.simpleName}")
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
        val namespaceList = StringBuilder(this.identifier().value)

        while (this.test(DotToken::class)) {
            namespaceList.append(this.consume(DotToken::class).value)

            if (this.test(IdentifierToken::class)) {
                namespaceList.append(this.identifier().value)
            } else if (start && this.test(StartToken::class)) {
                namespaceList.append(this.consume(StartToken::class).value)

                // 最后的星号如果匹配成功, 那么则需要结束匹配
                break
            }
        }

        return namespaceList.toString()
    }

    private fun newLineLiteral(): NewLineLiteral {
        this.consume(NewLineToken::class)
        return NewLineLiteral
    }

    /**
     * Identifier
     *  : IDENTIFIER
     *  ;
     */
    private fun identifier(): Identifier {
        val token = this.consume(IdentifierToken::class)
        return Identifier(token.value)
    }

    /**
     * 消费掉换行符
     */
    private fun effortConsumption() {
//        while (this.lookahead is NewLineToken) {
//            this.lookahead = this.ndsTokenizer.getNextToken()
//        }
    }

    private fun <T : Token<*>> matchAndConsume(tokenType: KClass<T>): T? {
        val token = this.lookahead

        return if (tokenType.isInstance(token)) {
            this.lookahead = this.ndsTokenizer.getNextToken()

            @Suppress("UNCHECKED_CAST")
            token as T?
        } else {
            null
        }
    }

    private fun <T : Token<*>> test(tokenType: KClass<T>) = tokenType.isInstance(this.lookahead)

    private fun <T : Token<*>> consume(tokenType: KClass<T>): T {
        val token = this.lookahead
            ?: throw SyntaxException("Unexpected end of input, expected: ${tokenType.simpleName}")

        if (!tokenType.isInstance(token)) {
            throw SyntaxException("Unexpected token: [${token}] -> ${token.javaClass.simpleName}, expected: ${tokenType.simpleName}")
        }

        this.lookahead = this.ndsTokenizer.getNextToken()

        @Suppress("UNCHECKED_CAST")
        return token as T
    }

}
