package com.ck.ast

/**
 * 查询方法信息定义
 *
 * @author 陈坤
 * 2023/10/3
 */
data class SearchFunDeclaration(

    /**
     * 函数名称
     */
    val funName: String,

    /**
     * 形参列表
     */
    val formalParameters: List<FormalParameter>,

    /**
     * 返回信息
     */
    val returnInfo: SearchReturnDeclaration?,

    /**
     * 函数体
     */
    val body: SearchFunBlockStatement
) : ASTree
