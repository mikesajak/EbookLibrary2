package com.mikesajak.library.db.rdf

import com.mikesajak.library.db.parser.ParseException
import com.mikesajak.library.db.parser.QuerySyntaxErrorException
import com.mikesajak.library.db.parser.UnknownOperatorException
import cz.jirutka.rsql.parser.RSQLParser
import cz.jirutka.rsql.parser.ast.*
import jakarta.inject.Singleton
import mu.KotlinLogging
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.apache.jena.vocabulary.SchemaDO

@Singleton
class RSQLToSparqlQueryParser {
    private val logger = KotlinLogging.logger {}

    private val parser: RSQLParser

    init {
        val operators = RSQLOperators.defaultOperators()
        operators.add(ComparisonOperator("=rx=", false))
        operators.add(ComparisonOperator("=notrx=", false))
        operators.add(ComparisonOperator("=like=", false))
        parser = RSQLParser(operators)
    }

    fun parse(query: String): SparqlQuery {
        logger.debug { "Parsing (caseInsensitive=true) query: $query"}
        try {
            val rootNode = parser.parse(query)
            return rootNode.accept(SparqlQueryLSQLVisitor()).sparqlQuery()
        } catch (e: ParseException) {
            logger.warn("Query parsing exception", e)
            throw e
        } catch (e: Exception) {
            logger.warn("Query Parsing exception", e)
            throw QuerySyntaxErrorException(e.message, e)
        }
    }
}

class SparqlQueryLSQLVisitor : NoArgRSQLVisitorAdapter<SparqlBookQuery>() {
    private val logger = KotlinLogging.logger {}

    companion object {
        val prefixes = mapOf("foaf" to FOAF.NS,
            "schema" to SchemaDO.NS,
            "bl" to BookLibrary.NS,
            "rdf" to RDF.getURI(),
            "rdfs" to RDFS.getURI())
    }

    override fun visit(node: AndNode): SparqlBookQuery {
        logger.debug { "And node: $node" }
        val childFilters = node.children.map { it.accept(this) }
        return childFilters.reduce { acc, query -> acc.and(query) }
    }

    override fun visit(node: OrNode): SparqlBookQuery {
        logger.debug { "Or node: $node" }
        val childFilters = node.children.map { it.accept(this) }
        return childFilters.reduce { acc, query -> acc.or(query) }
    }

    override fun visit(node: ComparisonNode): SparqlBookQuery {
        logger.debug { "visiting node=$node" }

        val query = SparqlBookQuery(prefixes)

        val operator = when (node.operator.symbol) {
            "==" -> QueryOperator.EQUALS
            "!=" -> QueryOperator.NOT_EQUALS
            "=in=" -> QueryOperator.IN
            "=notin=" -> QueryOperator.NOT_IN
            "=like=" -> QueryOperator.LIKE
            "=notlike=" -> QueryOperator.NOT_LIKE

            else -> throw UnknownOperatorException(node.toString())
        }

        val field = correctFieldName(node.selector)

        when (field) {
            "title" -> query.withTitle(node.arguments, operator)
            "authors" -> query.withAuthor(node.arguments, operator)
            "tags" -> query.withTag(node.arguments, operator)
            "languages" -> query.withLanguage(node.arguments, operator)
            "identifiers" -> query.withIdentifier(node.arguments, operator)
            "publisher" -> query.withPublisher(node.arguments, operator)
            "series" -> query.withSeries(node.arguments, operator)
            "seriesVolume" -> query.withSeriesVolume(node.arguments.map { it.toInt() }, operator)

            else -> TODO("Not yet implemented")
        }

        return query
    }

    private fun correctFieldName(name: String) = when (name) {
        "author" -> "authors"
        "tag" -> "tags"
        "language" -> "languages"
        "lang" -> "languages"
        else -> name
    }

}
