package com.mikesajak.library.db.rdf

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainAll
import io.kotest.matchers.maps.shouldContainAll
import io.kotest.matchers.shouldBe

class SparqlJoinQueryTest : FunSpec({

    test("prefixMap") {
        // given
        val query1 = createTestQuery1()
        val query2 = createTestQuery2()
        val query = SparqlJoinQuery(query1, query2, "")

        // when, then
        query.prefixMap shouldContainAll query1.prefixMap
        query.prefixMap shouldContainAll query2.prefixMap
    }

    test("selectors") {
        // given
        val query1 = createTestQuery1()
        val query2 = createTestQuery2()
        val query = SparqlJoinQuery(query1, query2, "")

        // when, then
        query.selectors shouldContainAll query1.selectors
        query.selectors shouldContainAll query2.selectors
    }

    test("!build") { }

    test("!buildPrefixes") { }

    test("!buildSelectClause") { }



    test("!buildWhereClause") { }

    test("toString") {
        // given
        val query = SparqlJoinQuery(createTestQuery1(), createTestQuery2(), "some join")

        // when, then
        query.toString() shouldBe query.build()
    }
})

private fun createTestQuery1(): BasicSparqlQuery {
    val query = BasicSparqlQuery(mapOf("schema1" to "http://some.schema.org/schema1"))
    query.addSelector("subject1")
    query.addCondition(RdfCondition("?subject1", "schema1:predicate1", "object1"))
    return query
}

private fun createTestQuery2(): BasicSparqlQuery {
    val query = BasicSparqlQuery(mapOf("schema2" to "http://some.schema.org/schema2"))
    query.addSelector("subject2")
    query.addCondition(RdfCondition("?subject2", "schema2:predicate2", "object2"))
    return query
}