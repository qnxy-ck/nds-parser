package com.ck

import com.ck.ast.*
import com.ck.ast.literal.BooleanLiteral
import com.ck.ast.literal.NullLiteral
import com.ck.ast.literal.NumberLiteral
import com.ck.ast.literal.StringLiteral
import com.ck.token.BinaryOperatorToken
import com.ck.token.BooleanToken
import com.ck.token.IdentifierToken
import com.ck.token.Token
import com.ck.token.keyword.*
import com.ck.token.literal.NumberLiteralToken
import com.ck.token.literal.StringLiteralToken
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
            this.ignoreNewLines()

        }

        return list
    }

    /**
     * SearchStatement
     *  : SimpleIfStatement
     *  | ArrowIfStatement
     *  | CallMemberExpression
     *  ;
     */
    private fun searchStatement(): ASTree {
        // ?.
        if (this.match(QuestionMarkPointToken::class, NewLineToken::class)) {
            return this.simpleIfStatement()
        }

        // -> 
        if (this.match(ArrowToken::class, NewLineToken::class)) {
            return this.arrowIfStatement()
        }

        // call
        if (this.lookahead() is ColonToken) return this.callMemberExpression()

        // sql
        return this.zipSqlLiteral()
    }

    /**
     * SimpleIfStatement
     *  | ZipSqlLiteral CallMemberExpression
     *  ;
     */
    private fun simpleIfStatement(): ASTree {
        val consequent = this.zipSqlLiteral()
        val test = this.callMemberExpression()

        if (test is CallExpression && test.ifCall && consequent.value.isBlank()) {
            throw SyntaxException("悬空的的IfExpression, 不存在被控制语句. 如: [空的 :username?.test() / 空的 -> :username::test()]")
        }

        return SimpleIfStatement(
            test,
            consequent
        )
    }

    /**
     * ArrowIfStatement
     *  | ZipSqlLiteral Members '->' BooleanExpression
     *  ;
     */
    private fun arrowIfStatement(): ASTree {
        val consequent = this.zipSqlLiteral()
        if (consequent.value.isBlank()) throw SyntaxException("悬空的的IfExpression, 不存在被控制语句. 如: [空的 :username?.test() / 空的 -> :username::test()]")

        val member = this.members()
        this.consume(ArrowToken::class)
        val test = this.booleanExpression()

        return ArrowIfStatement(
            member,
            test,
            consequent
        ).also {
            this.consume(NewLineToken::class)
            this.ignoreNewLines()
        }
    }

    /**
     * BooleanExpression
     *  : LogicalOrExpression
     *  ;
     */
    private fun booleanExpression(): ASTree {
        return this.logicalOrExpression()
    }

    /**
     * Logical OR expression
     * x || y
     *
     * LogicalOrExpression
     *  : LogicalAndExpression
     *  | LogicalOrExpression LOGICAL_OR LogicalAndExpression
     *  ;
     */
    private fun logicalOrExpression() = this.logicalExpression(LogicalOrToken::class) { this.logicalAndExpression() }

    /**
     * Logical AND expression
     * x && y
     *
     * LogicalAndExpression
     *  : EqualityExpression
     *  | LogicalAndExpression LOGICAL_AND EqualityExpression
     *  ;
     */
    private fun logicalAndExpression() = this.logicalExpression(LogicalAndToken::class) { this.equalityExpression() }

    /**
     * EQUALITY_OPERATOR: ==, !=
     * x == y
     * x != y
     *
     * EqualityExpression
     *  : RelationalExpression
     *  | EqualityExpression EQUALITY_OPERATOR RelationalExpression
     *  ;
     */
    private fun equalityExpression() = this.logicalExpression(EqualityOperatorToken::class) { this.relationalExpression() }

    /**
     * RELATIONAL_OPERATOR: >, >=, <, <=
     * x > y
     * x >= y
     * x < y
     * x <= y
     *
     * RelationalExpression
     *  : AdditiveExpression
     *  | RelationalExpression RELATIONAL_OPERATOR AdditiveExpression
     *  ;
     */
    private fun relationalExpression() = this.binaryExpression(RelationalOperatorToken::class) { this.additiveExpression() }

    /**
     * 加减
     *
     * AdditiveExpression
     *  : MultiplicativeExpression
     *  | AdditiveExpression ADDITIVE_OPERATOR MultiplicativeExpression
     *  ;
     */
    private fun additiveExpression() = this.binaryExpression(AdditiveOperatorToken::class) { this.multiplicativeExpression() }

    /**
     * 乘除
     *
     * MultiplicativeExpression
     *  : UnaryExpression
     *  | MultiplicativeExpression MULTIPLICATIVE_OPERATOR UnaryExpression
     *  ;
     */
    private fun multiplicativeExpression() = this.binaryExpression(MultiplicativeOperatorToken::class) { this.unaryExpression() }

    /**
     * 构建算数运算表达式
     *
     * @param operatorTokenType 运算符类型
     * @param builderMethod 运算符左侧表达式
     */
    private inline fun binaryExpression(operatorTokenType: KClass<out BinaryOperatorToken>, builderMethod: () -> ASTree): ASTree {
        var left = builderMethod()

        while (this.test(operatorTokenType)) {
            val operator = this.consume(operatorTokenType)
            val right = builderMethod()

            left = BinaryExpression(operator, left, right)
        }

        return left
    }

    private inline fun logicalExpression(operatorTokenType: KClass<out BinaryOperatorToken>, builderMethod: () -> ASTree): ASTree {
        var left = builderMethod()

        while (this.test(operatorTokenType)) {
            val operator = this.consume(operatorTokenType)
            val right = builderMethod()

            left = LogicalExpression(operator, left, right)
        }

        return left
    }

    /**
     * UnaryExpression
     *  : LeftHandSideExpression
     *  | LOGICAL_NOT UnaryExpression
     *  ;
     */
    private fun unaryExpression(): ASTree {
        if (!this.test(LogicalNotToken::class)) return this.leftHandSideExpression()

        val operator = this.consume(LogicalNotToken::class)
        return UnaryExpression(operator, this.unaryExpression())
    }

    /**
     * LeftHandSideExpression
     *  : CallMemberExpression -> '::' 方式
     *  | NumberLiteral
     *  | StringLiteral
     *  | BooleanLiteral
     *  | NullLiteral
     *  | ParenthesizedExpression
     *  ;
     */
    private fun leftHandSideExpression(): ASTree {
        return when (this.lookahead()) {
            is ColonToken -> {
                val expression = this.callMemberExpression()
                if (expression is CallExpression && expression.ifCall) throw SyntaxException("仅可存在函数调用, 不可为条件控制语法.") else expression
            }

            is NumberLiteralToken -> this.numberLiteral()
            is StringLiteralToken -> this.stringLiteral()
            is BooleanToken -> this.booleanLiteral()
            is NullToken -> this.nullLiteral()
            is OpenParenthesisToken -> this.parenthesizedExpression()
            else -> throw SyntaxException("Illegal left side expression.")
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
        if (ifCall) {
            this.consume(NewLineToken::class)

            // 忽略潜在的更多换行
            this.ignoreNewLines()
        }

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
        return this.literal()
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
     * ParenthesizedExpression
     *  : '(' BooleanExpression ')'
     *  ;
     */
    private fun parenthesizedExpression(): ASTree {
        this.consume(OpenParenthesisToken::class)
        val expression = this.booleanExpression()
        this.consume(ClosedParenthesisToken::class)

        return expression
    }

    private fun zipSqlLiteral(): SqlLiteral {
        val list = mutableListOf<Token<*>>()

        while (!(this.test(ColonToken::class) || this.test(ArrowToken::class) || this.test(NewLineToken::class))) {
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
            } else if (start && this.test(MultiplicativeOperatorToken.MULTIPLICATION::class)) {
                namespaceList.append(this.consume(MultiplicativeOperatorToken.MULTIPLICATION::class).value)

                // 最后的星号如果匹配成功, 那么则需要结束匹配
                break
            }
        }

        return namespaceList.toString()
    }

    /**
     * Literal
     *  : Identifier
     *  | BooleanLiteral
     *  | StringLiteral
     *  | NumberLiteral
     *  | NullLiteral
     *  ;
     */
    private fun literal(): ASTree {
        return when (this.lookahead()) {
            is IdentifierToken -> this.identifier()
            is BooleanToken -> this.booleanLiteral()
            is StringLiteralToken -> this.stringLiteral()
            is NumberLiteralToken -> this.numberLiteral()
            is NullToken -> this.nullLiteral()
            else -> throw SyntaxException("Literal: unexpected literal production -> ${this.lookahead()}")
        }
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
     * BooleanLiteral
     *  : 'true'
     *  | 'false'
     *  ;
     */
    private fun booleanLiteral(): BooleanLiteral {
        val token = this.consume(BooleanToken::class)
        return BooleanLiteral(token.value)
    }


    /**
     * NullLiteral
     *  : 'null'
     *  ;
     */
    private fun nullLiteral(): NullLiteral {
        this.consume(NullToken::class)
        return NullLiteral
    }

    /**
     * StringLiteral
     *  : STRING_LITERAL
     *  ;
     */
    private fun stringLiteral(): StringLiteral {
        val token = this.consume(StringLiteralToken::class)
        return StringLiteral(token.value.substring(1, token.value.length - 1))
    }

    /**
     * NumberLiteral
     *  : NUMBER_LITERAL
     *  ;
     */
    private fun numberLiteral(): NumberLiteral {
        val token = this.consume(NumberLiteralToken::class)
        return NumberLiteral(token.value, token.numberType)
    }

    /**
     * ------------------------------------------------------------------------------------------------
     */

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

        // 优先判断已匹配过的token
        this.tokens.forEach {
            if (tokenType.isInstance(it)) return true

            if (stopToken.isInstance(it)) return false
        }

        while (true) {
            if (tokenType.isInstance(token)) return true

            token = this.ndsTokenizer.getNextToken() ?: return false
            this.tokens.offer(token)
            if (stopToken.isInstance(token)) return false
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

        val nextToken = this.ndsTokenizer.getNextToken()
        if (nextToken != null) {
            this.tokens.offer(nextToken)
        }

        @Suppress("UNCHECKED_CAST")
        return token as T
    }

}
