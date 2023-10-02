package com.ck.token

import com.fasterxml.jackson.annotation.JsonValue

/**
 * @author 陈坤
 * 2023/10/2
 */
interface Token<T> {

    @get:JsonValue
    val value: T

}