package com.mikesajak.library.db.rdf

import com.mikesajak.library.util.IdGenerator
import org.apache.jena.query.Dataset

class SparqlBookQueryOld(private val prefixMap: Map<String, String>, private val baseWhereClause: String? = null,
                         private val baseIndent: String = "", baseConditions: List<RdfCondition> = listOf(),
                         private val baseSparqlQuery: SparqlQuery? = null) {
    constructor(dataset: Dataset, baseWhereClause: String = "") : this(dataset.prefixMapping.nsPrefixMap, baseWhereClause)

    private val resourceIdGenerator = IdGenerator()
    private val propertyIdGenerator = IdGenerator()

    private val resourcePropertyConditions = mutableListOf<ResourcePropertyCondition>()
    private val propertyConditions = mutableListOf<PropertyCondition>()
    // date conditions

    private val sparqlQuery = BasicSparqlQuery(prefixMap, baseIndent)

    init {
        sparqlQuery.addSelector("book")
        sparqlQuery.addCondition("book", "rdf:type", "schema:Book")

        baseConditions
            .filter { it.subject == "book" && it.predicate == "rdf:type" && it.targetObject == "schema:Book" }
            .forEach { sparqlQuery.addCondition(it) }
    }

    override fun toString(): String = build()

    fun withAuthor(authorName: String, operator: QueryOperator = QueryOperator.EQUALS): SparqlBookQueryOld {
        resourceCondition("schema:author", "author", "foaf:name", authorName, operator)
        val authorResName = "author${resourceIdGenerator.get()}"
        sparqlQuery.addCondition("book", "schema:author", authorResName)
        sparqlQuery.addCondition(buildRdfCondition(authorResName, "foaf:name", listOf(authorName), operator))
        return this
    }

    private fun resourceCondition(resRelation: String, resourceName: String = "resource", propertyName: String, propertyValue: String,
                                  operator: QueryOperator = QueryOperator.EQUALS
    ) {
        resourcePropertyConditions.add(
            ResourcePropertyCondition(
                resRelation, "$resourceName${resourceIdGenerator.get()}",
                PropertyCondition(propertyName, propertyValue, operator)
            )
        )
    }

    fun withTitle(title: String, operator: QueryOperator = QueryOperator.EQUALS) = propertyCondition("schema:title", title, operator)
    fun withTitle(titles: List<String>, operator: QueryOperator = QueryOperator.IN) = propertyCondition("schema:title", titles, operator)

    fun withTag(tag: String, operator: QueryOperator = QueryOperator.EQUALS) = propertyCondition("bl:tag", tag, operator)
    fun withTag(tags: List<String>, operator: QueryOperator = QueryOperator.EQUALS) = propertyCondition("bl:tag", tags, operator)

    fun withLanguage(lang: String, operator: QueryOperator = QueryOperator.EQUALS) = propertyCondition("schema:language", lang, operator)
    fun withLanguage(langs: List<String>, operator: QueryOperator = QueryOperator.EQUALS) = propertyCondition("schema:language", langs, operator)

    fun withIdentifier(ident: String, operator: QueryOperator = QueryOperator.EQUALS) = propertyCondition("bl:identifier", ident, operator)
    fun withIdentifier(idents: List<String>, operator: QueryOperator = QueryOperator.EQUALS) = propertyCondition("bl:identifier", idents, operator)

    fun withPublisher(pubName: String, operator: QueryOperator = QueryOperator.EQUALS) = propertyCondition("schema:publishedBy", pubName, operator)
    fun withPublisher(pubNames: List<String>, operator: QueryOperator = QueryOperator.EQUALS) = propertyCondition("schema:publishedBy", pubNames, operator)

    fun withSeries(seriesName: String, operator: QueryOperator = QueryOperator.EQUALS) = propertyCondition("schema:partOfSeries", seriesName, operator)
    fun withSeries(seriesNames: List<String>, operator: QueryOperator = QueryOperator.EQUALS) = propertyCondition("schema:partOfSeries", seriesNames, operator)

    fun withSeries(seriesName: String, volumeNumber: Int): SparqlBookQueryOld {
        propertyCondition("schema:partOfSeries", seriesName)
        return propertyCondition("schema:volumeNumber", volumeNumber.toString())
    }

    private fun propertyCondition(property: String, value: String, operator: QueryOperator = QueryOperator.EQUALS): SparqlBookQueryOld =
            propertyCondition(property, listOf(value), operator)

    private fun propertyCondition(property: String, values: List<String>, operator: QueryOperator = QueryOperator.EQUALS): SparqlBookQueryOld {
        val propertySegments = splitPrefix(property)
        if (propertySegments.first.isNotEmpty() && propertySegments.second.isNotEmpty()
                && prefixMap.containsKey(propertySegments.first)) {
            propertyConditions.add(PropertyCondition(property, values, operator))

            sparqlQuery.addCondition(buildRdfCondition("book", property, values, operator))
            return this
        }
        else throw SparqlBookQueryException("Can't add condition property with unknown prefix: $propertySegments")
    }

    fun build(): String {
        val prefixes = prefixMap.map { entry -> "PREFIX ${entry.key}: <${entry.value}>" }
                .joinToString("\n$baseIndent")

        val whereClause = buildWhereClause()
                .wrap(baseIndent)

        return """
            |$prefixes
            |
            |${baseIndent}SELECT ?book
            |${baseIndent}WHERE
            |$whereClause""".trimMargin()
    }

    fun sparqlQuery(): SparqlQuery = sparqlQuery

    private fun buildWhereClause(indent: String = ""): String {
        val query = StringBuilder()

        baseWhereClause?.let { query.append(it).append("\n") }

        query.append("${indent}?book a schema:Book .")

        if (resourcePropertyConditions.isNotEmpty()) {
            val resourcePropertiesQuery = resourcePropertyConditions.joinToString("\n") { buildResourcePropertyCondition(it) }
                    .prependIndent(indent)
            query.append("\n").append(resourcePropertiesQuery)
        }

        if (propertyConditions.isNotEmpty()) {
            val propertiesQuery = propertyConditions.joinToString("\n") { buildPropertyCondition("book", it) }
                    .prependIndent(indent)
            query.append("\n").append(propertiesQuery)
        }
        return query.toString()
    }

    fun or(otherQuery: SparqlBookQueryOld): SparqlBookQueryOld =
            joinQuery(otherQuery, "UNION")

    fun and(otherQuery: SparqlBookQueryOld): SparqlBookQueryOld =
            joinQuery(otherQuery, "")

    private fun joinQuery(otherQuery: SparqlBookQueryOld, joinOperation: String): SparqlBookQueryOld {
        val joinedClause =
            """|${buildWhereClause().wrap()}
               |$joinOperation
               |${otherQuery.buildWhereClause().wrap()}""".trimMargin()
                    .wrap(baseIndent)

        return SparqlBookQueryOld(
            prefixMap + otherQuery.prefixMap, joinedClause, "", listOf(),
            SparqlJoinQuery(sparqlQuery, otherQuery.sparqlQuery, joinOperation)
        )
    }

    private fun buildResourcePropertyCondition(condition: ResourcePropertyCondition) =
            """|?book ${condition.resRelation} ?${condition.resName} .
               |${buildPropertyCondition(condition.resName, condition.propertyCondition)}
               |""".trimMargin()

    private fun buildPropertyCondition(subjectResource: String, condition: PropertyCondition) =
            when(condition.operator) {
                QueryOperator.EQUALS ->
                    """?$subjectResource ${condition.predicateName} "${condition.values[0]}" ."""
                QueryOperator.NOT_EQUALS -> {
                    val valuePlaceholder = "cond_value${propertyIdGenerator.get()}"
                    """?$subjectResource ${condition.predicateName} ?$valuePlaceholder .
                      |FILTER (?$valuePlaceholder != "${condition.values[0]}")""".trimMargin()
                }
                QueryOperator.LIKE -> {
                    val valuePlaceholder = "cond_value${propertyIdGenerator.get()}"
                    """?$subjectResource ${condition.predicateName} ?$valuePlaceholder .
                      |FILTER regex(?$valuePlaceholder, "${condition.values[0]}", "i")""".trimMargin()
                }
                QueryOperator.NOT_LIKE -> {
                    val valuePlaceholder="cond_value${propertyIdGenerator.get()}"
                    """?$subjectResource ${condition.predicateName} ?$valuePlaceholder .
                      |FILTER (!regex(?$valuePlaceholder, "${condition.values[0]}", "i")""".trimMargin()
                }
                QueryOperator.IN -> {
                    val valuePlaceholder = "cond_value${propertyIdGenerator.get()}"
                    """?$subjectResource ${condition.predicateName} ?$valuePlaceholder .
                      |FILTER (?$valuePlaceholder IN "${condition.values.joinToString(", ", "(", ")") { "\"$it\"" }}}"""
                            .trimMargin()
                }
                QueryOperator.NOT_IN -> {
                    val valuePlaceholder = "cond_value${propertyIdGenerator.get()}"
                    """?$subjectResource ${condition.predicateName} ?$valuePlaceholder .
                      |FILTER (?$valuePlaceholder NOT IN "${condition.values.joinToString(", ", "(", ")") { "\"$it\"" }}}"""
                            .trimMargin()
                }
            }

    private fun buildRdfCondition(subjectResource: String, predicateName: String, values: List<String>, operator: QueryOperator) =
        when(operator) {
            QueryOperator.EQUALS ->
                RdfCondition(subjectResource, predicateName, values[0])
            QueryOperator.NOT_EQUALS -> {
                val valuePlaceholder = "cond_value${propertyIdGenerator.get()}"
                RdfCondition(subjectResource, predicateName, valuePlaceholder, "?$valuePlaceholder != \"${values[0]}\"")
            }
            QueryOperator.LIKE -> {
                val valuePlaceholder = "cond_value${propertyIdGenerator.get()}"
                RdfCondition(
                    subjectResource,
                    predicateName,
                    valuePlaceholder,
                    "regex(?$valuePlaceholder, \"${values[0]}\", \"i\")"
                )
            }
            QueryOperator.NOT_LIKE -> {
                val valuePlaceholder="cond_value${propertyIdGenerator.get()}"
                RdfCondition(
                    subjectResource,
                    predicateName,
                    valuePlaceholder,
                    "!regex(?$valuePlaceholder, \"${values[0]}\", \"i\")"
                )
            }
            QueryOperator.IN -> {
                val valuePlaceholder = "cond_value${propertyIdGenerator.get()}"
                RdfCondition(subjectResource, predicateName, valuePlaceholder,
                    "$valuePlaceholder IN \"${values.joinToString(", ", "(", ")") { "\"$it\"" }}}"
                )
            }
            QueryOperator.NOT_IN -> {
                val valuePlaceholder = "cond_value${propertyIdGenerator.get()}"
                RdfCondition(subjectResource, predicateName, valuePlaceholder,
                    "?$valuePlaceholder NOT IN \"${values.joinToString(", ", "(", ")") { "\"$it\"" }}}"
                )
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