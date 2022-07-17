package com.mikesajak.library.db.parser.sql

import com.mikesajak.library.db.parser.ParseException
import com.mikesajak.library.db.parser.QuerySyntaxErrorException
import com.mikesajak.library.db.parser.UnknownOperatorException
import com.mikesajak.library.db.sql.BookQuery
import com.mikesajak.library.db.sql.BookQuery.Companion.AuthorsJoin
import com.mikesajak.library.db.sql.BookQuery.Companion.IdentifiersJoin
import com.mikesajak.library.db.sql.BookQuery.Companion.LanguagesJoin
import com.mikesajak.library.db.sql.BookQuery.Companion.SeriesJoin
import com.mikesajak.library.db.sql.BookQuery.Companion.TagsJoin
import cz.jirutka.rsql.parser.RSQLParser
import cz.jirutka.rsql.parser.ast.*
import jakarta.inject.Singleton
import mu.KotlinLogging

@Singleton
class RSQLtoSQLQueryParser {
    private val logger = KotlinLogging.logger {}

    private val parser: RSQLParser

    init {
        val operators = RSQLOperators.defaultOperators()
        operators.add(ComparisonOperator("=rx=", false))
        operators.add(ComparisonOperator("=notrx=", false))
        operators.add(ComparisonOperator("=like=", false))
        parser = RSQLParser(operators)
    }

    fun parse(query: String): String {
        logger.debug { "Parsing (caseInsensitive=true) query: $query"}
        try {
            val rootNode = parser.parse(query)
            val parsedQuery = rootNode.accept(SQLQueryRSQLVisitor(true))
            return parsedQuery.toString()
        } catch (e: ParseException) {
            logger.warn("Query parsing exception", e)
            throw e
        } catch (e: Exception) {
            logger.warn("Query Parsing exception", e)
            throw QuerySyntaxErrorException(e.message, e)
        }
    }
}

open class Join(val leftTable: String, val rightTable: String,
                val joinType: JoinType,
                val leftField: String, val rightField: String) {
    fun joinClause() = "LEFT JOIN $rightTable"
    fun joinCondition() = "$leftTable.$leftField=$rightTable.$rightField"
}

class LeftJoin(leftTable: String, rightTable: String, leftField: String, rightField: String)
    : Join(leftTable, rightTable, JoinType.LEFT, leftField, rightField)

enum class JoinType {
    LEFT,
    RIGHT,
    INNER
}

//interface WhereExpression
//class Condition(val value: String) : WhereExpression {
//    override fun toString() = value
//}
//open class CombineExpression(val joiner: String, val left: WhereExpression, val right: WhereExpression): WhereExpression{
//    override fun toString() = "($left) $joiner ($right)"
//}
//class AndExpression(left: WhereExpression, right: WhereExpression) : CombineExpression("AND", left, right)
//class OrExpression(left: WhereExpression, right: WhereExpression) : CombineExpression("OR", left, right)

//class SqlQueryBuilder {
//    private var selectBuilder = StringBuilder()
//    private var fromBuilder = StringBuilder()
//    private val leftJoins = mutableListOf<String>()
//    private var condition: WhereExpression? = null
//
//    fun select(clause: String): SqlQueryBuilder {
//        selectBuilder.append(clause)
//        return this
//    }
//
//    fun from(fromClause: String): SqlQueryBuilder {
//        fromBuilder.append(fromClause)
//        return this
//    }
//
//    fun leftJoin(joinClause: String): SqlQueryBuilder {
//        leftJoins.add(joinClause)
//        return this
//    }
//
//    fun where(whereClause: String) {
//        conditions.clear()
//        conditions.add()
//    }
//
//}

class SQLQueryRSQLVisitor(
    private val caseInsensitive: Boolean
) : NoArgRSQLVisitorAdapter<BookQuery>() {
    private val logger = KotlinLogging.logger {}

    override fun visit(node: AndNode): BookQuery {
        logger.debug { "And node: $node" }
        val childFilters = node.children.map { it.accept(this) }
        return BookQuery.and(childFilters)
    }

    override fun visit(node: OrNode): BookQuery {
        logger.debug { "Or node: $node" }
        val childFilters = node.children.map { it.accept(this) }
        return BookQuery.or(childFilters)
    }

    override fun visit(node: ComparisonNode): BookQuery {
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
        val joinCondition = joinTableFor(fieldName)
        return BookQuery(filter, joinCondition?.let { setOf(it) } ?: setOf())
    }

    private fun eqPredicate(field: String, node: ComparisonNode) =
            "$field='${prepareSingleValue(node)}'"

    private fun neqPredicate(field: String, node: ComparisonNode) =
            "$field!='${prepareSingleValue(node)}'"

    private fun inPredicate(field: String, node: ComparisonNode): String =
            node.arguments.joinToString(prefix ="$field IN (", postfix = ")", separator = ", ") { prepareValue(it) }

    private fun likePredicate(field: String, node: ComparisonNode) =
            "$field='%${prepareSingleValue(node)}%'"

    private fun not(predicate: String) = "NOT ($predicate)"

//    private fun prepareField(field: String) =
//            if (caseInsensitive) "UPPER($field)" else field

    private fun prepareValue(value: String) = if (caseInsensitive) value.uppercase() else value

    private fun prepareSingleValue(node: ComparisonNode) = prepareValue(node.arguments[0])

//    private fun isArrayField(name: String) = when (name) {
//        "authors" -> true
//        "tags" -> true
//        "languages" -> true
//        else -> false
//    }

    private fun correctArrayFieldName(name: String) = when (name) {
        "author" -> "authors"
        "tag" -> "tags"
        "language" -> "languages"
        "lang" -> "languages"
        else -> name
    }

    private fun joinTableFor(fieldName: String): Join? = when (fieldName) {
        "authors"     -> AuthorsJoin
        "tags"        -> TagsJoin
        "identifiers" -> IdentifiersJoin
        "languages"   -> LanguagesJoin
        "series"      -> SeriesJoin
        else          -> null
    }


    private fun resolveFieldPath(field: String): String {
        val fieldName = field.uppercase()
        return when {
            fieldName == "series" || fieldName.startsWith("series.") -> {
                val subField = if (fieldName == "series") "TITLE" else fieldName.substring("series.".length)
//            root.get<SeriesEntity>("series").get<String>(subField)
                "SERIES.$subField"
            }
            fieldName == "FORMAT"                                    -> "FORMATS.type"//root.get<BookFormatEntity>("formats").get<String>("type")
            fieldName == "AUTHORS"                                   -> "AUTHORS.name"//root.joinList<BookEntity, AuthorEntity>(name, JoinType.LEFT).get<String>("name")
            fieldName == "TAGS" || fieldName == "LANGUAGES"          -> "$fieldName.name"//root.joinList<BookEntity, String>(name, JoinType.LEFT)
            else                                                     -> fieldName //root.get<String>(fieldName)
        }
    }
}
