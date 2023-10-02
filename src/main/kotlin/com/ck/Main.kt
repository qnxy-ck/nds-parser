package com.ck

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.BufferedReader
import java.io.InputStreamReader

class Main

fun main() {
    val resourceAsStream = Main::class.java
        .getResourceAsStream("/UserInfoMapper.txt") ?: throw RuntimeException("文件不存在!")


    val text = BufferedReader(InputStreamReader(resourceAsStream))
        .use { it.readText() }

     
    val ast = NdsParser(text).parse()
    val om = ObjectMapper()
    
    om.writerWithDefaultPrettyPrinter()
        .writeValueAsString(ast)
        .also { println(it) }

}