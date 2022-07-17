package com.mikesajak.library.db.rdf

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class RdfConditionTest : FunSpec({

    listOf("object1", "\"object with spaces and quotes\"")
            .forEach { obj ->
                test("toString should produce SPARQL query representation of the simple condition") {
                    val rdfCond = RdfCondition("?subject1", "schema:predicate1", obj)

                    rdfCond.toString() shouldBe "?subject1 schema:predicate1 $obj ."
                }
            }

   listOf("""regex(?object1, "some pattern", "i")""",
          """IN ("value1", "value2")""")
           .forEach { filter ->
               test("toString should produce SPARQL query representation of the condition with filter") {
                   val rdfCond = RdfCondition("?subject1", "schema:predicate1", "?object1", filter)

                   rdfCond.toString() shouldBe "?subject1 schema:predicate1 ?object1 .\nFILTER ($filter)"
               }
           }

})
