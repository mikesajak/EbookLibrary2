package com.mikesajak.library.db.rdf

import com.mikesajak.library.db.rdf.util.ReadTx
import com.mikesajak.library.db.rdf.util.WriteTx
import com.mikesajak.library.model.Book
import com.mikesajak.library.model.SeriesEntry
import org.apache.jena.query.Dataset
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QuerySolution
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.Resource
import org.apache.jena.tdb2.TDB2Factory
import java.time.LocalDate

fun main(args: Array<String>) {
    val dir = "jena/DB1"
    val dataset = TDB2Factory.connectDataset(dir)

    val rdfRepo = RdfRepository(dataset, RSQLToSparqlQueryParser())

//    recreateDB(dataset, rdfRepo)

    ReadTx(dataset).use {
        dataset.defaultModel.write(System.out, "TURTLE")
    }

//    runQuery(SparqlBookQuery(dataset)
//                     .withAuthor("Neil Gaiman")
//                     .build(), dataset, "Query 1 - builder")
//
//    runQuery(SparqlBookQuery(dataset.prefixMapping.nsPrefixMap)
//                     .withSeries("Wiedźmin")
//                     .build(), dataset, "Query 3 - builder")
//
//
//
//    println("--------------------------")
//    println("findAllBooks:")
//    rdfRepo.findAllBooks().forEach { println(it) }


    val q1 = SparqlBookQueryOld(dataset.prefixMapping.nsPrefixMap)
        .withTag("fantasy")
        .and(SparqlBookQueryOld(dataset.prefixMapping.nsPrefixMap)
            .withAuthor("Sap", QueryOperator.LIKE))
        .or(
            SparqlBookQueryOld(dataset.prefixMapping.nsPrefixMap)
                .withTag("prog", QueryOperator.LIKE)
                .withAuthor("Pimpuś", QueryOperator.LIKE)
                .withTag("python")
        )

//    println(q1.build())

    val q = SparqlBookQuery(dataset.prefixMapping.nsPrefixMap)
        .withTag("fantasy")
        .and()
        .withAuthor("Sap", QueryOperator.LIKE)
        .or()
        .withTag("prog", QueryOperator.LIKE)
        .withAuthor("Pimpuś", QueryOperator.LIKE)
        .withTag("python")


    runQuery(q.build(), dataset, "union query")

    runQuery(q1.build(), dataset, "original union query")

    val books = rdfRepo.findAllBooks()
    books.forEach { println(it) }



//    runQuery(SparqlBookQuery(dataset)
//                     .withTag("fantasy")
//                     .withAuthor("Sapkowski")
//                     .build(), dataset)
}

private fun recreateDB(dataset: Dataset, rdfRepo: RdfRepository) {
    WriteTx(dataset).use {
        rdfRepo.clearAll()

        val authorGaiman = rdfRepo.addAuthor("Neil Gaiman")
        val authorSap = rdfRepo.addAuthor("Andrzej Sapkowski")
        val authorProg1 = rdfRepo.addAuthor("Programus Wielgus")
        val authorProg2 = rdfRepo.addAuthor("Programus Pimpuś")

        rdfRepo.addBook(
            Book(
                null, "Nigdziebądź",
                listOf(authorGaiman), listOf(), listOf("pl"), listOf("fantasy", "fantastyka"),
                LocalDate.parse("2022-05-01"), LocalDate.parse("2022-06-02"),
                "Some publisher", null, null
            )
        )

        rdfRepo.addBook(Book.of("Ocean na końcu drogi", authorGaiman))

        rdfRepo.addBook(
            Book(
                null, "Krew elfów", listOf(authorSap), listOf(),
                listOf("pl"), listOf("wiedźmin", "fantasy", "fantastyka", "magia"),
                null, null, null, SeriesEntry("Wiedźmin", 1), null
            )
        )

        rdfRepo.addBook(
            Book(
                null, "Czas pogardy", listOf(authorSap), listOf(),
                listOf("pl"), listOf("wiedźmin", "fantasy", "fantastyka", "magia"),
                null, null, null, SeriesEntry("Wiedźmin", 2), null
            )
        )

        rdfRepo.addBook(
            Book(
                null, "Chrzest ognia", listOf(authorSap), listOf(),
                listOf("pl"), listOf("wiedźmin", "fantasy", "fantastyka", "magia"),
                null, null, null, SeriesEntry("Wiedźmin", 3), null
            )
        )

        rdfRepo.addBook(
            Book(
                null, "Wieża jaskółki", listOf(authorSap), listOf(),
                listOf("pl"), listOf("wiedźmin", "fantasy", "fantastyka", "magia"),
                null, null, null, SeriesEntry("Wiedźmin", 4), null
            )
        )

        rdfRepo.addBook(
            Book(
                null, "Pani jeziora", listOf(authorSap), listOf(),
                listOf("pl"), listOf("wiedźmin", "fantasy", "fantastyka", "magia"),
                null, null, null, SeriesEntry("Wiedźmin", 5), null
            )
        )

        rdfRepo.addBook(
            Book(
                null, "Programowanie w Javie dla opornych",
                listOf(authorProg1), listOf(), listOf(), listOf("programowanie", "java"),
                null, null, null, null, null
            )
        )

        rdfRepo.addBook(
            Book(
                null, "Programowanie w C++ dla opornych",
                listOf(authorProg2), listOf(), listOf(), listOf("programowanie", "c++"),
                null, null, null, null, null
            )
        )

        rdfRepo.addBook(
            Book(
                null, "Programowanie w Pythonie dla koksów",
                listOf(authorProg2), listOf(), listOf(), listOf("programowanie", "python"),
                null, null, null, null, null
            )
        )
    }
}

private fun runQuery(query: String, dataset: Dataset, queryName: String? = null) {
    ReadTx(dataset).use {
        println("----------------------------------")
        println("Executing query ${if (queryName != null) "($queryName)" else ""}:\n$query")
        val result = executeQuery(query, dataset)
        println("Results:")
        result.forEach { println(it) }
        result.map { RdfBookLibraryMapper.createBookFrom(it.getResource("book").asResource())}
                .map { it.title }
                .forEach { println(it) }

    }
}

class DbConsistencyException(msg: String) : Exception(msg)



fun listProperties(resource: Resource, property: Property) =
        resource.listProperties(property)
                .toList()
                .mapNotNull { stmt -> stmt?.string }

fun executeQuery(query: String, dataset: Dataset): List<QuerySolution> {
    val queryExecution = QueryExecutionFactory.create(query, dataset)
    try {
        return queryExecution.execSelect().asSequence().toList()
    } catch (e: Exception) {
        println("Exception: $e")
        throw e
    } finally {
        queryExecution.close()
    }
}
