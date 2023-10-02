package com.ck

import com.ck.ast.ASTree
import com.ck.ast.ImportStatement
import com.ck.ast.NamespaceStatement
import com.ck.ast.Program
import com.ck.token.NamespaceIdentifierToken
import com.ck.token.Token
import com.ck.token.keyword.EntityToken
import com.ck.token.keyword.ImportToken
import com.ck.token.keyword.NamespaceToken
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


    fun parse(): ASTree {
        this.ndsTokenizer.init(this.text)
        this.lookahead = this.ndsTokenizer.getNextToken()

        return this.program()
    }

    private fun program() = Program(this.statementList())

    private fun statementList(): List<ASTree> {
        val statementList = mutableListOf<ASTree>(this.namespaceStatement())
        statementList.add(this.importEntityStatement())

        this.optImportStatementList().let {
            if (it.isNotEmpty()) statementList.addAll(it)
        }

        return statementList
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

        val token = this.consume(NamespaceIdentifierToken::class)
        return ImportStatement(token.value, entity)
    }


    /**
     * NamespaceStatement
     *  : 'namespace' NAMESPACE_IDENTIFIER -> namespace com.ck.test
     *  ;
     */
    private fun namespaceStatement(): NamespaceStatement {
        this.consume(NamespaceToken::class)
        val token = this.consume(NamespaceIdentifierToken::class)
        return NamespaceStatement(token.value)
    }


    private fun <T : Token<*>> test(tokenType: KClass<T>) = tokenType.isInstance(this.lookahead)

    private fun <T : Token<*>> consume(tokenType: KClass<T>): T {
        val token = this.lookahead
            ?: throw SyntaxException("Unexpected end of input, expected: ${tokenType.simpleName}")

        if (!tokenType.isInstance(token)) {
            throw SyntaxException("Unexpected token: ${token.value}, expected: ${tokenType.simpleName}")
        }

        this.lookahead = this.ndsTokenizer.getNextToken()

        @Suppress("UNCHECKED_CAST")
        return token as T
    }

}
