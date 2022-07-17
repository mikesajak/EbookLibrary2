package com.mikesajak.library.db.rdf

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.containExactlyInAnyOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldMatch

class BasicSparqlQueryTest : FunSpec({

    test("prefixMap should return the provided map of prefixes") {
        // given
        val query = createTestQuery()

        // when, then
        query.prefixMap.size shouldBe 3
        query.prefixMap.keys should containExactlyInAnyOrder("prefix1", "prefix2", "prefix3")
        query.prefixMap["prefix1"] shouldBe "http://schema.org/schema1"
        query.prefixMap["prefix2"] shouldBe "http://schema.org/schema2"
        query.prefixMap["prefix3"] shouldBe "http://test.abc.com/some/schema"
    }

    test("buildPrefixes should prepare string with SPARQL prefixes") {
        // given
        val query = createTestQuery()

        // when
        val prefixes = query.buildPrefixes()

        // then
        testPrefixMap.entries.forEach { e ->
            prefixes shouldContain("PREFIX ${e.key}: <${e.value}>")
        }
    }

    test("addSelector should add the selector toh the selectors list") {
        // given
        val query = createTestQuery()
        query.addSelector("book")
        query.addSelector("author")
        query.addSelector("title")

        // when, then
        query.selectors should containExactlyInAnyOrder("book", "author", "title")
    }

    test("buildSelectClause should prepare SPARQL query select clause") {
        // given
        val query = createTestQuery()
        query.addSelector("book")
        query.addSelector("author")
        query.addSelector("title")

        // when, then
        query.buildSelectClause() shouldMatch """\s*SELECT\s+\?book\s*,\s*\?author\s*,\s*\?title\s*"""
    }

    test("addCondition should allow adding different RDF conditions to query") {
        // given
        val query = createTestQuery()

        query.addCondition(RdfCondition("subject1", "schema1:predicate1", "object1"))
        query.addCondition(RdfCondition("subject1", "schema1:predicate2", "object2"))

        query.addCondition("subject3", "schema2:predicate3", "object3")
        query.addCondition(RdfCondition("subject4", "schema3:predicate4", "object4", "test filter abc"))

        // when, then
        query.conditions should containExactlyInAnyOrder(RdfCondition("subject1", "schema1:predicate1", "object1"),
                                                         RdfCondition("subject1", "schema1:predicate2", "object2"),
                                                         RdfCondition("subject3", "schema2:predicate3", "object3"),
                                                         RdfCondition("subject4", "schema3:predicate4", "object4", "test filter abc"))
    }

    test("buildWhereClause") {
        // given
        val query = createTestQuery()

        val conditions = listOf(RdfCondition("subject1", "schema1:predicate1", "object1"),
                                RdfCondition("subject1", "schema1:predicate2", "object2"),
                                RdfCondition("subject3", "schema2:predicate3", "object3"),
                                RdfCondition("subject4", "schema3:predicate4", "object4", "test filter abc"))

        conditions.forEach { query.addCondition(it) }

        // when, then
        val whereClause = query.buildWhereClause()
        conditions.map { it.toString() }
                .forEach { whereClause shouldContain it }

    }

    test("build") {
        // given
        val query = BasicSparqlQuery(mapOf("schema1" to "http://some.schema.org/schema1"))
        val conditions = listOf(RdfCondition("?subject1", "schema1:predicate1", "object1"))

        query.addSelector("subject1")
        conditions.forEach { query.addCondition(it) }

        // when
        val queryStr = query.build()

        // then
        queryStr.trim() shouldContain Regex("""PREFIX schema1: <http://some.schema.org/schema1>
                                              |
                                              |SELECT \?subject1
                                              |WHERE
                                              |\{
                                              |\s+\?subject1 schema1:predicate1 object1 \.
                                              |}""".trimMargin())
    }
    test("toString") {
        // given
        val query = createTestQuery()

        // when, then
        query.toString() shouldBe query.build()
    }

})

private val testPrefixMap = mapOf("prefix1" to "http://schema.org/schema1",
                                  "prefix2" to "http://schema.org/schema2",
                                  "prefix3" to "http://test.abc.com/some/schema")

private fun createTestQuery(): BasicSparqlQuery {
    return BasicSparqlQuery(testPrefixMap)
}

