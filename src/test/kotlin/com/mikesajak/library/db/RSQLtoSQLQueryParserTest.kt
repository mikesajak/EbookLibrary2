package com.mikesajak.library.db

import com.mikesajak.library.db.parser.sql.RSQLtoSQLQueryParser
import io.kotest.core.spec.style.StringSpec

class RSQLtoSQLQueryParserTest : StringSpec({
    "parse should correctly parse query" {
        val parser = RSQLtoSQLQueryParser()
        val sqlQuery = parser.parse("authors=like=Author-1 and series=like=Gra")
        println(sqlQuery)
    }
})
