package com.ck

import com.ck.ast.*
import com.ck.token.IdentifierToken
import com.ck.token.Token
import com.ck.token.keyword.*
import com.ck.token.symbol.*
import java.util.*
import kotlin.reflect.KClass

/**
 * @author 陈坤
 * 2023/10/2
 */
class NdsParser(
    private val text: String
) {

    private val ndsTokenizer = NdsTokenizer()
    private val tokens = LinkedList<Token<*>>()

    fun parse(): Program {
        this.ndsTokenizer.init(this.text)
        this.tokens.offer(this.ndsTokenizer.getNextToken())

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
            .also { this.ignoreNewLines() }
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
            .also { this.ignoreNewLines() }
    }

    /**
     * FunStatementList
     *  : FunStatement
     *  | FunStatement FunStatementList
     *  ;
     */
    private fun funStatementList(): List<ASTree> {
        val funStatementList = mutableListOf<ASTree>()
        while (this.lookahead() != null) {
            funStatementList.add(this.funStatement())
            if (this.test(NewLineToken::class)) {
                this.ignoreNewLines()
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
        return when (this.lookahead()) {
            is SearchToken -> this.searchFunDeclaration()
            else -> throw SyntaxException("未知Token(funStatement): [${this.lookahead()}]")
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
        this.ignoreNewLines()
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

        while (this.lookahead() != null && !this.test(ClosedCurlyBracketToken::class)) {
            val searchStatement = this.searchStatement()
            list.add(searchStatement)
        }

        return list
    }

    /**
     * SearchStatement
     *  : CallMemberExpression
     *  | IfStatement
     *  ;
     */
    private fun searchStatement(): ASTree {
        return when (this.lookahead()) {
            is ColonToken -> this.callMemberExpression()
            else -> this.ifStatement()
        }
    }

    /**
     * IfStatement
     *  : ZipSqlLiteral
     *  | ZipSqlLiteral CallMemberExpression
     *  ;
     */
    private fun ifStatement(): ASTree {
        val zipSqlLiteral = this.zipSqlLiteral()

        if (this.test(NewLineToken::class)) {
            this.ignoreNewLines()
            return zipSqlLiteral
        }

        // 直至匹配到 '?.'
        if (!this.match(QuestionMarkPointToken::class, NewLineToken::class)) {
            return zipSqlLiteral
        }


        val callMemberExpression = this.callMemberExpression()

        return IfStatement(callMemberExpression, zipSqlLiteral)

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
     *  | '?.' Identifier OptArguments NEW_LINE
     *  ;
     */
    private fun callExpression(callee: MemberExpression): CallExpression {

        val ifCall = if (this.test(DoubleColonToken::class)) {
            this.consume(DoubleColonToken::class)
            false
        } else {
            this.consume(QuestionMarkPointToken::class)
            true
        }


        val funName = this.identifier().value
        val argumentList = this.optArguments()

        // newLine
        if (ifCall) this.consume(NewLineToken::class)

        // 忽略潜在的更多换行
        this.ignoreNewLines()

        return CallExpression(
            callee,
            funName,
            argumentList,
            ifCall
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
        return when (this.lookahead()) {
            is IdentifierToken -> this.identifier()
            else -> throw SyntaxException("未知的参数类型(Argument) -> ${this.lookahead()?.javaClass?.simpleName}")
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

    private fun zipSqlLiteral(): SqlLiteral {
        val list = mutableListOf<Token<*>>()

        while (!(this.test(ColonToken::class) || this.test(NewLineToken::class))) {
            list.add(this.consume(Token::class))
        }

        val sql = list.joinToString(" ", postfix = " ") { it.value.toString() }
        return SqlLiteral(sql)
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
     * 尽可能消费掉换行符
     */
    private fun ignoreNewLines() {
        while (this.lookahead() is NewLineToken) {
            this.consume(NewLineToken::class)
        }
    }

    /**
     * 匹配一个token, 如果匹配成功则直接消费
     * 匹配失败返回null
     */
    private fun <T : Token<*>> matchAndConsume(tokenType: KClass<T>): T? {
        return if (tokenType.isInstance(this.lookahead())) this.consume(tokenType) else null
    }

    /**
     * lookahead
     */
    private fun lookahead(): Token<*>? {
        return this.tokens.peek()
    }

    /**
     * 匹配当前 lookahead token是否与之匹配
     *
     * @param tokenType 待匹配的类型
     * @return boolean true(匹配成功)
     */
    private fun <T : Token<*>> test(tokenType: KClass<T>) = tokenType.isInstance(this.lookahead())

    /**
     * 匹配token, 直至匹配到 tokenType 类型 或者匹配到 stopToken 类型为止
     *
     * @param tokenType 待匹配的类型
     * @param stopToken 匹配到该token后没有找到 tokenType 类型则返回匹配失败
     * @return boolean true(匹配成功)
     */
    private fun <T : Token<*>> match(tokenType: KClass<T>, stopToken: KClass<*>): Boolean {
        var token = this.lookahead()

        while (true) {
            if (tokenType.isInstance(token)) {
                return true
            }

            token = this.ndsTokenizer.getNextToken() ?: throw SyntaxException("Unexpected end of input, expected: ${tokenType.simpleName}")
            this.tokens.offer(token)
            if (stopToken.isInstance(token)) {
                return false
            }
        }
    }

    /**
     * 消费一个Token
     */
    private fun <T : Token<*>> consume(tokenType: KClass<T>): T {
        val token = this.tokens.poll() ?: throw SyntaxException("Unexpected end of input, expected: ${tokenType.simpleName}")

        if (!tokenType.isInstance(token)) {
            throw SyntaxException("Unexpected token: [${token.value}] -> ${token.javaClass.simpleName}, expected: ${tokenType.simpleName}")
        }

        this.tokens.offer(this.ndsTokenizer.getNextToken())

        @Suppress("UNCHECKED_CAST")
        return token as T
    }

}
