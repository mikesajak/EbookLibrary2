package com.mikesajak.library.db.rdf

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.beInstanceOf

class RSQLToSparqlQueryParserTest : FunSpec({

    val parser = RSQLToSparqlQueryParser()

    listOf("SomeTitle",
           "Some Title with spaces",
           "a",
           "1",
           "abc123",
           "abc 123",
           "123 abc")
        .forEach { title ->
            test("should prepare title query") {
                // given, when
                val query = parser.parse("""title=="$title"""")

                // then
                query.selectors shouldBe listOf("book")
                query should beInstanceOf<BasicSparqlQuery>()
                val basicSparqlQuery = query as BasicSparqlQuery
                basicSparqlQuery.conditions shouldContainExactlyInAnyOrder
                        listOf(RdfCondition("?book", "rdf:type", "schema:Book"),
                               RdfCondition("?book", "schema:title", "\"$title\""))
            }
        }

    listOf("SomeAuthor",
           "Some Author with spaces",
           "a",
           "1",
           "abc123",
           "abc 123",
           "123 abc")
            .forEach { author ->
                test("should parse query with author condition") {
                    // given, when
                    val query = parser.parse("""author=="$author"""")

                    // then
                    query.selectors shouldBe listOf("book")
                    query should beInstanceOf<BasicSparqlQuery>()
                    val basicSparqlQuery = query as BasicSparqlQuery
                    basicSparqlQuery.conditions shouldContainExactlyInAnyOrder
                            listOf(RdfCondition("?book", "rdf:type", "schema:Book"),
                                   RdfCondition("?book", "schema:author", "?author0"),
                                   RdfCondition("?author0", "foaf:name", "\"$author\"")
                            )

                }
            }
})
