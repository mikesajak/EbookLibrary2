package com.mikesajak.library.db.parser

open class ParseException(msg: String?) : Exception(msg)

class UnknownOperatorException(msg: String) : ParseException(msg)

class QuerySyntaxErrorException(msg: String?) : ParseException(msg) {
    constructor(msg: String?, cause: Throwable) : this(msg) {
        initCause(cause)
    }
}