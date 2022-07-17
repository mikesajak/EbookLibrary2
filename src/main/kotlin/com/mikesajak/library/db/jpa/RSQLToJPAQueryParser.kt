package com.mikesajak.library.db.parser.jpa

import com.mikesajak.library.db.jpa.JpaAuthorEntity
import com.mikesajak.library.db.jpa.JpaBookEntity
import com.mikesajak.library.db.jpa.JpaBookFormatEntity
import com.mikesajak.library.db.jpa.JpaSeriesEntity
import com.mikesajak.library.db.parser.ParseException
import com.mikesajak.library.db.parser.QuerySyntaxErrorException
import com.mikesajak.library.db.parser.UnknownOperatorException
import cz.jirutka.rsql.parser.RSQLParser
import cz.jirutka.rsql.parser.ast.*
import jakarta.inject.Singleton
import jakarta.persistence.criteria.*
import mu.KotlinLogging

@Singleton
class RSQLToJPAQueryParser {
    private val logger = KotlinLogging.logger {}

    private val parser: RSQLParser

    init {
        val operators = RSQLOperators.defaultOperators()
        operators.add(ComparisonOperator("=rx=", false))
        operators.add(ComparisonOperator("=notrx=", false))
        operators.add(ComparisonOperator("=like=", false))
        parser = RSQLParser(operators)
    }

    fun parse(query: String, criteriaBuilder: CriteriaBuilder, root: Root<JpaBookEntity>): Predicate {
        logger.debug { "Parsing (caseInsensitive=true) query: $query"}
        try {
            val rootNode = parser.parse(query)
            return rootNode.accept(JpaQueryRSQLVisitor(criteriaBuilder, root, true))
        } catch (e: ParseException) {
            logger.warn("Query parsing exception", e)
            throw e
        } catch (e: Exception) {
            logger.warn("Query Parsing exception", e)
            throw QuerySyntaxErrorException(e.message, e)
        }
    }
}

class JpaQueryRSQLVisitor(
        private val critBuilder: CriteriaBuilder,
        private val root: Root<JpaBookEntity>,
        private val caseInsensitive: Boolean
) : NoArgRSQLVisitorAdapter<Predicate>() {
    private val logger = KotlinLogging.logger {}

    override fun visit(node: AndNode): Predicate {
        logger.debug { "And node: $node" }
        val childFilters = node.children.map { it.accept(this) }

        return critBuilder.and(*childFilters.toTypedArray())
    }

    override fun visit(node: OrNode): Predicate {
        logger.debug { "Or node: $node" }
        val childFilters = node.children.map { it.accept(this) }
        return critBuilder.or(*childFilters.toTypedArray())
    }

    override fun visit(node: ComparisonNode): Predicate {
        logger.debug { "Comparison node: $node" }
        val fieldName = correctArrayFieldName(node.selector)
//        val arrayField = isArrayField(fieldName)

        val field = resolveFieldPath(fieldName)
//            if (!arrayField) resolveFieldPath(fieldName)
//            else root.joinList<BookEntity, String>(fieldName, JoinType.LEFT)

        val filter = when (node.operator.symbol) {
            "==" -> eqPredicate(field, node)
            "!=" -> neqPredicate(field, node)
            "=in=" -> inPredicate(field, node)
            "=out=" -> not(inPredicate(field, node))
            "=~=" -> likePredicate(field, node)
            "=like=" -> likePredicate(field, node)
            "=notlike=" -> not(likePredicate(field, node))

//            "=rx=" -> regex(dbFieldName, node.arguments[0])
//            "=notrx=" -> not(regex(dbFieldName, node.arguments[0]))
            else -> throw UnknownOperatorException(node.toString())
        }
//        return if (arrayField) elemMatch("metadata.${fieldName}", filter)
//        else filter
        return filter
    }

    private fun eqPredicate(field: Path<String>, node: ComparisonNode): Predicate =
            critBuilder.equal(prepareField(field), prepareValue(node.arguments[0]))

    private fun neqPredicate(field: Path<String>, node: ComparisonNode): Predicate =
            critBuilder.notEqual(prepareField(field), prepareValue(node.arguments[0]))

    private fun inPredicate(field: Path<String>, node: ComparisonNode): Predicate {
        val inClause = critBuilder.`in`(prepareField(field))
        node.arguments.forEach { arg -> inClause.value(prepareValue(arg)) }
        return inClause
    }

    private fun likePredicate(field: Path<String>, node: ComparisonNode): Predicate =
            critBuilder.like(prepareField(field), "%${prepareValue(node.arguments[0])}%")

    private fun not(predicate: Predicate) = critBuilder.not(predicate)

//    private fun prepareLikeRegexMatcher(query: String) = "(?i).*?${Pattern.quote(query)}.*"

    private fun prepareField(field: Path<String>) =
            if (caseInsensitive) critBuilder.lower(field) else field

    private fun prepareValue(value: String) =
            if (caseInsensitive) value.lowercase() else value

    private fun correctArrayFieldName(name: String) = when (name) {
        "author" -> "authors"
        "tag" -> "tags"
        "language" -> "languages"
        "lang" -> "languages"
        else -> name
    }

    private fun resolveFieldPath(name: String) = when {
        name == "series" || name.startsWith("series.") -> {
            val subField = if (name == "series") "title" else name.substring("series.".length)
            root.get<JpaSeriesEntity>("series").get<String>(subField)
        }
        name == "format" -> root.get<JpaBookFormatEntity>("formats").get<String>("type")
        name == "authors" -> root.joinList<JpaBookEntity, JpaAuthorEntity>(name, JoinType.LEFT).get<String>("name")
        name =="tags" || name == "languages" -> root.joinList<JpaBookEntity, String>(name, JoinType.LEFT)
        else -> root.get<String>(name)
    }
}
