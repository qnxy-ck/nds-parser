package com.ck.ast

import com.fasterxml.jackson.annotation.JsonGetter
import com.fasterxml.jackson.annotation.JsonPropertyOrder

/**
 * @author 陈坤
 * 2023/10/2
 */
@JsonPropertyOrder("type")
interface ASTree {

    @JsonGetter
    fun type(): String = this::class.java.simpleName
    
}