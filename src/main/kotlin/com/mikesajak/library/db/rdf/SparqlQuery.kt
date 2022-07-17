package com.mikesajak.library.db.rdf

data class RdfCondition(val subject: String, val predicate: String, val targetObject: String,
                        val filter: String? = null, val indent: String = "") {
    override fun toString(): String {
        val predicateValue = if (predicate == "rdf:type") "a" else predicate
        val sentence = "$subject $predicateValue $targetObject ."

        val result =
            if (filter?.isNotEmpty() == true)
                """$sentence
                  |FILTER ($filter)""".trimMargin()
            else sentence

        return result.prependIndent(indent)
    }
}

interface SparqlQuery {
    val selectors: List<String>
    val prefixMap: Map<String, String>

    fun build(): String =
        """${buildPrefixes()}
          |
          |${buildSelectClause()}
          |WHERE
          |${buildWhereClause().wrap()}""".trimMargin()

    fun buildPrefixes() = prefixMap.map { entry -> "PREFIX ${entry.key}: <${entry.value}>" }.joinToString("\n")
    fun buildSelectClause() = selectors.joinToString(", ", "SELECT ") { "?$it" }
    fun buildWhereClause(): String
}

class SparqlJoinQuery(private val query1: SparqlQuery, private val query2: SparqlQuery, private val joinOperation: String,
                      private val baseIndent: String = ""): SparqlQuery {
    companion object {
        fun and(query1: SparqlQuery, query2: SparqlQuery) = join(query1, query2, "")
        fun or(query1: SparqlQuery, query2: SparqlQuery) = join(query1, query2, "UNION")

        private fun join(query1: SparqlQuery, query2: SparqlQuery, joinOperation: String): SparqlJoinQuery {
            return SparqlJoinQuery(query1, query2, joinOperation)
        }
    }

    override val prefixMap: Map<String, String>
        get() = query1.prefixMap + query2.prefixMap

    override val selectors: List<String>
        get() {
            val sel = LinkedHashSet<String>()
            sel.addAll(query1.selectors)
            sel.addAll(query2.selectors)
            return sel.toList()
        }

    override fun buildWhereClause(): String =
        """${query1.buildWhereClause().wrap()}
          |$joinOperation
          |${query2.buildWhereClause().wrap()}""".trimMargin().prependIndent(baseIndent)

    override fun toString(): String = build()
}

class BasicSparqlQuery(override val prefixMap: Map<String, String>, private val baseIndent: String = ""): SparqlQuery {
    private val selectorList = linkedSetOf<String>()
    override val selectors: List<String> get() = selectorList.toList()

    private val conditionsList = mutableListOf<RdfCondition>()
    val conditions: List<RdfCondition> get() = conditionsList.toList()

    fun addSelector(selector: String) = apply { selectorList.add(selector) }

    fun addCondition(subject: String, predicate: String, targetObject: String, indent: String = "") =
        apply { addCondition(RdfCondition(subject, predicate, targetObject, null, indent)) }
    fun addCondition(triple: RdfCondition) = apply { conditionsList.add(triple) }

    override fun buildWhereClause(): String {
        val whereClause = conditionsList.joinToString("\n")

        return whereClause.prependIndent(baseIndent)
    }

    override fun toString(): String = build()
}