package com.mikesajak.library.db.rdf

import com.mikesajak.library.model.AuthorId
import com.mikesajak.library.util.IdGenerator

class SparqlBookQuery(private val prefixMap: Map<String, String>, baseIndent: String = "",
                      private val baseQuery: Pair<SparqlQuery, String>? = null) {

    private val resourceIdGenerator = IdGenerator()
    private val propertyIdGenerator = IdGenerator()

    private val sparqlQuery = BasicSparqlQuery(prefixMap, baseIndent)

    init {
        sparqlQuery.addSelector("book")
        sparqlQuery.addCondition("?book", "rdf:type", "schema:Book")
    }

    override fun toString(): String = sparqlQuery.build()

    fun withAuthor(authorName: String, operator: QueryOperator = QueryOperator.EQUALS): SparqlBookQuery =
        withAuthor(listOf(authorName), operator)

    fun withAuthor(authorNames: List<String>, operator: QueryOperator = QueryOperator.EQUALS): SparqlBookQuery {
        val authorResName = "?author${resourceIdGenerator.get()}"
        sparqlQuery.addCondition("?book", "schema:author", authorResName)
        sparqlQuery.addCondition(buildRdfCondition(authorResName, "foaf:name", authorNames, operator))
        return this
    }

    fun withAuthorId(authorId: AuthorId, operator: QueryOperator = QueryOperator.EQUALS): SparqlBookQuery =
            withProperty("schema:author", authorId.toString(), operator)

    fun withAuthorId(authorIds: List<AuthorId>, operator: QueryOperator = QueryOperator.EQUALS): SparqlBookQuery =
            withProperty("schema:author", authorIds.map { it.toString() }, operator)

    fun withTitle(title: String, operator: QueryOperator = QueryOperator.EQUALS) = withProperty("schema:title", title, operator)
    fun withTitle(titles: List<String>, operator: QueryOperator = QueryOperator.IN) = withProperty("schema:title", titles, operator)

    fun withTag(tag: String, operator: QueryOperator = QueryOperator.EQUALS) = withProperty("bl:tag", tag, operator)
    fun withTag(tags: List<String>, operator: QueryOperator = QueryOperator.EQUALS) = withProperty("bl:tag", tags, operator)

    fun withLanguage(lang: String, operator: QueryOperator = QueryOperator.EQUALS) = withProperty("schema:language", lang, operator)
    fun withLanguage(langs: List<String>, operator: QueryOperator = QueryOperator.EQUALS) = withProperty("schema:language", langs, operator)

    fun withIdentifier(ident: String, operator: QueryOperator = QueryOperator.EQUALS) = withProperty("bl:identifier", ident, operator)
    fun withIdentifier(idents: List<String>, operator: QueryOperator = QueryOperator.EQUALS) = withProperty("bl:identifier", idents, operator)

    fun withPublisher(pubName: String, operator: QueryOperator = QueryOperator.EQUALS) = withProperty("schema:publishedBy", pubName, operator)
    fun withPublisher(pubNames: List<String>, operator: QueryOperator = QueryOperator.EQUALS) = withProperty("schema:publishedBy", pubNames, operator)

    fun withSeries(seriesName: String, operator: QueryOperator = QueryOperator.EQUALS) = withProperty("schema:partOfSeries", seriesName, operator)
    fun withSeries(seriesNames: List<String>, operator: QueryOperator = QueryOperator.EQUALS) = withProperty("schema:partOfSeries", seriesNames, operator)

    fun withSeriesVolume(volumeNumber: Int, operator: QueryOperator) = withProperty("schema:volumeNumber", listOf(volumeNumber.toString()), operator)
    fun withSeriesVolume(volumeNumbers: List<Int>, operator: QueryOperator) = withProperty("schema:volumeNumber", volumeNumbers.map { it.toString() }, operator)

    fun withSeries(seriesName: String, volumeNumber: Int): SparqlBookQuery {
        withProperty("schema:partOfSeries", seriesName)
        withProperty("schema:volumeNumber", volumeNumber.toString())
        return this
    }

    fun withProperty(property: String, value: String, operator: QueryOperator = QueryOperator.EQUALS): SparqlBookQuery =
        withProperty(property, listOf(value), operator)

    fun withProperty(property: String, values: List<String>, operator: QueryOperator = QueryOperator.EQUALS): SparqlBookQuery {
        val propertySegments = splitPrefix(property)
        if (propertySegments.first.isNotEmpty() && propertySegments.second.isNotEmpty()
                && prefixMap.containsKey(propertySegments.first)) {
            sparqlQuery.addCondition(buildRdfCondition("?book", property, values, operator))
            return this
        }
        else throw SparqlBookQueryException("Can't add condition property with unknown prefix: $propertySegments")
    }

    fun and(otherQuery: SparqlBookQuery) = SparqlBookQuery(prefixMap + otherQuery.prefixMap, "",
        Pair(SparqlJoinQuery.and(sparqlQuery(), otherQuery.sparqlQuery()), ""))

    fun or(otherQuery: SparqlBookQuery) = SparqlBookQuery(prefixMap + otherQuery.prefixMap, "",
        Pair(SparqlJoinQuery.or(sparqlQuery(), otherQuery.sparqlQuery()), ""))

    fun and() = SparqlBookQuery(prefixMap, "", Pair(sparqlQuery(), ""))
    fun or() = SparqlBookQuery(prefixMap, "", Pair(sparqlQuery(), "UNION"))

    fun sparqlQuery(): SparqlQuery = when {
        baseQuery == null -> sparqlQuery
        baseQuery.second == "" -> SparqlJoinQuery.and(baseQuery.first, sparqlQuery)
        baseQuery.second == "UNION" -> SparqlJoinQuery.or(baseQuery.first, sparqlQuery)
        else -> throw java.lang.IllegalArgumentException("SparqlBookQuery only supports UNION or \"and\"(empty) join operators")
    }

    fun build(): String = sparqlQuery().build()

    private fun buildRdfCondition(subjectResource: String, predicateName: String, values: List<String>, operator: QueryOperator) =
        when(operator) {
            QueryOperator.EQUALS     ->
                RdfCondition(subjectResource, predicateName, "\"${values[0]}\"")
            QueryOperator.NOT_EQUALS -> {
                val valuePlaceholder = "?cond_value${propertyIdGenerator.get()}"
                RdfCondition(subjectResource, predicateName, valuePlaceholder, "$valuePlaceholder != \"${values[0]}\"")
            }
            QueryOperator.LIKE       -> {
                val valuePlaceholder = "?cond_value${propertyIdGenerator.get()}"
                RdfCondition(subjectResource, predicateName, valuePlaceholder, "regex($valuePlaceholder, \"${values[0]}\", \"i\")")
            }
            QueryOperator.NOT_LIKE   -> {
                val valuePlaceholder="?cond_value${propertyIdGenerator.get()}"
                RdfCondition(subjectResource, predicateName, valuePlaceholder, "!regex($valuePlaceholder, \"${values[0]}\", \"i\")")
            }
            QueryOperator.IN         -> {
                val valuePlaceholder = "?cond_value${propertyIdGenerator.get()}"
                RdfCondition(subjectResource, predicateName, valuePlaceholder,
                    "$valuePlaceholder IN \"${values.joinToString(", ", "(", ")") { "\"$it\"" }}}")
            }
            QueryOperator.NOT_IN     -> {
                val valuePlaceholder = "?cond_value${propertyIdGenerator.get()}"
                RdfCondition(subjectResource, predicateName, valuePlaceholder,
                    "?$valuePlaceholder NOT IN \"${values.joinToString(", ", "(", ")") { "\"$it\"" }}}")
            }
        }

    private fun splitPrefix(name: String): Pair<String, String> {
        val sepPos = name.indexOf(':')
        return if (sepPos >= 0) {
            if (name.length > sepPos+1) Pair(name.substring(0, sepPos), name.substring(sepPos + 1))
            else Pair(name.substring(0, sepPos), "")
        } else Pair("", name)
    }

    class SparqlBookQueryException(msg: String) : Exception(msg)
}

data class PropertyCondition(val predicateName: String, val values: List<String>, val operator: QueryOperator) {
    constructor(predicateName: String, value: String, operator: QueryOperator) : this(predicateName, listOf(value), operator)
}

data class ResourcePropertyCondition(val resRelation: String, val resName: String, val propertyCondition: PropertyCondition)

enum class QueryOperator {
    EQUALS, NOT_EQUALS, IN, NOT_IN, LIKE, NOT_LIKE;
}

fun String.wrap(indent: String = "", opening: String = "{", closing: String = "}") =
    """|$indent$opening
           |${this.prependIndent("$indent  ")}
           |$indent$closing""".trimMargin()