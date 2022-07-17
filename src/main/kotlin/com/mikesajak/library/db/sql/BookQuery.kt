package com.mikesajak.library.db.sql

import com.mikesajak.library.db.parser.sql.Join
import com.mikesajak.library.db.parser.sql.LeftJoin

data class BookQuery(val query: String, val joinTables: Set<Join>) {
    fun withAuthors(): BookQuery = BookQuery(query, joinTables.plus(AuthorsJoin))
    fun withSeries(): BookQuery = BookQuery(query, joinTables.plus(SeriesJoin))
    fun withTags(): BookQuery = BookQuery(query, joinTables.plus(TagsJoin))
    fun withLanguages(): BookQuery = BookQuery(query, joinTables.plus(LanguagesJoin))
    fun withIdentifiers(): BookQuery = BookQuery(query, joinTables.plus(IdentifiersJoin))
    fun withQuery(queryElem: String) = BookQuery("$query $queryElem", joinTables)
    fun and(queryElem: String) = BookQuery("$query AND $queryElem", joinTables)
    fun or(queryElem: String) = BookQuery("$query OR $queryElem", joinTables)

    override fun toString(): String {
        return "select * from BOOKS " + joinTables.joinToString(" "){ it.joinClause() } +
                " where " + query +
                joinTables.joinToString(separator=" and ", prefix = " and "){ it.joinCondition() }
    }

    companion object {
        fun and(queries: List<BookQuery>) = joinWithCondition("AND", queries)
        fun or(queries: List<BookQuery>) = joinWithCondition("OR", queries)

        private fun joinWithCondition(joiner: String, queries: List<BookQuery>): BookQuery {
            val andQuery = queries.joinToString(separator = " ${joiner.trim()} ") { elem -> "(${elem.query})" }
            val usedTables = queries.fold(mutableSetOf<Join>()) { acc, elem -> acc.addAll(elem.joinTables); acc }
            return BookQuery(andQuery, usedTables.toSet())
        }

        val AuthorsJoin = LeftJoin("BOOKS", "AUTHORS", "AUTHORS_ID", "ID")
        val TagsJoin = LeftJoin("BOOKS", "TAGS", "TAGS_ID", "NAME")
        val IdentifiersJoin = LeftJoin("BOOKS", "IDENTIFIERS", "IDENTIFIERS_ID", "NAME")
        val LanguagesJoin = LeftJoin("BOOKS", "LANGUAGES", "LANGUAGES_ID", "NAME")
        val SeriesJoin = LeftJoin("BOOKS", "SERIES", "SERIES_ID", "ID")
    }
}