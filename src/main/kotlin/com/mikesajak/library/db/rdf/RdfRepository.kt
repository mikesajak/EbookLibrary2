package com.mikesajak.library.db.rdf

import com.mikesajak.library.db.BookNotFoundException
import com.mikesajak.library.db.rdf.util.ReadTx
import com.mikesajak.library.db.rdf.util.WriteTx
import com.mikesajak.library.model.*
import io.micronaut.context.annotation.Property
import jakarta.inject.Inject
import jakarta.inject.Singleton
import mu.KotlinLogging
import org.apache.jena.query.Dataset
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QuerySolution
import org.apache.jena.rdf.model.Literal
import org.apache.jena.rdf.model.Resource
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.tdb2.TDB2Factory
import org.apache.jena.vocabulary.RDF
import org.apache.jena.vocabulary.RDFS
import org.apache.jena.vocabulary.SchemaDO
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.*

@Singleton
class RdfRepository(private val dataset: Dataset,
                    private val rsqlParser: RSQLToSparqlQueryParser) {
    @Inject
    constructor(@Property(name = "booklibrary.rdf.repository_dir") dbDirectory: String,
                rsqlParser: RSQLToSparqlQueryParser) : this(TDB2Factory.connectDataset(dbDirectory),
                                                            rsqlParser)

    private val model = dataset.defaultModel

    private val logger = KotlinLogging.logger {}

    val uri = "https://booklibrary.org/"

    init {
        WriteTx(dataset).use {
            model.setNsPrefix("foaf", FOAF.NS)
            model.setNsPrefix("schema", SchemaDO.NS)
            model.setNsPrefix("bl", BookLibrary.NS)
            model.setNsPrefix("rdf", RDF.getURI())
            model.setNsPrefix("rdfs", RDFS.getURI())
        }
    }

    fun findBookById(id: BookId): Book? {
        val bookRes = model.getResource(id.value)
        return bookRes?.let { RdfBookLibraryMapper.createBookFrom(it) }
    }

    fun findAllBooks(): List<Book> {
        val query = SparqlBookQuery(dataset.prefixMapping.nsPrefixMap).sparqlQuery()
        return executeBookQuery(query, dataset)
    }

    fun findBooks(rsqlQuery: String): List<Book> {
        val sparqlQuery = rsqlParser.parse(rsqlQuery)
        return executeBookQuery(sparqlQuery, dataset)
    }

    fun findBooksByAuthorId(authorId: AuthorId): List<Book> {
        val sparqlQuery = SparqlBookQuery(dataset.prefixMapping.nsPrefixMap)
                .withAuthorId(authorId)
                .sparqlQuery()
        return executeBookQuery(sparqlQuery, dataset)
    }

    fun addBook(book: Book): Resource {
        return WriteTx(dataset).use {
            val bookUri = UUID.randomUUID().toString()
            val bookRes = model.createResource(bookUri, SchemaDO.Book)

            bookRes.addProperty(SchemaDO.title, book.title)
            book.authors.forEach {author ->
                val authorRes = getOrCreateAuthor(author)
                bookRes.addProperty(SchemaDO.author, authorRes)
            }
            book.identifiers.forEach { bookRes.addProperty(BookLibrary.identifier, it.id) }
            book.tags.forEach { bookRes.addProperty(BookLibrary.tag, it) }
            book.languages.forEach { bookRes.addProperty(SchemaDO.language, it) }
            book.creationDate?.let { bookRes.addProperty(SchemaDO.dateCreated, createDateLiteral(it.toString())) }
            book.publicationDate?.let { bookRes.addProperty(SchemaDO.publishedOn, createDateLiteral(it.toString())) }
            book.publishedBy?.let { bookRes.addProperty(SchemaDO.publishedBy, it) }
            book.series?.let {
                bookRes.addProperty(SchemaDO.partOfSeries, it.name)
                bookRes.addLiteral(SchemaDO.volumeNumber, it.number)
            }
            book.description?.let { bookRes.addProperty(SchemaDO.description, it) }

            logger.info { "Added new book to DB: $bookRes"}
            bookRes
        }
    }

    fun addBookFormat(format: BookFormat): Resource {
        return WriteTx(dataset).use {
            val bookRes = model.getResource(format.bookId.value)
            if (bookRes == null) {
                logger.info { "Unable to add book format $format - target book not found." }
                throw BookNotFoundException("Can't find book with id=${format.bookId}")
            }

            val formatUri = UUID.randomUUID().toString()
            val formatRes = model.createResource(formatUri, SchemaDO.BookFormatType)

            formatRes.addProperty(SchemaDO.fileFormat, format.mimeType)
            formatRes.addProperty(SchemaDO.location, format.location)

            bookRes.addProperty(SchemaDO.bookFormat, formatRes)
            formatRes
        }
    }

    fun addBookCover(bookCover: BookCover): Resource {
        return WriteTx(dataset).use {
            val bookRes = model.getResource(bookCover.bookId.value)
            if (bookRes == null) {
                logger.info { "Unable to add book cover $bookCover - target book not found." }
                throw BookNotFoundException("Can't find book with id=${bookCover.bookId}")
            }
            val coverUri = UUID.randomUUID().toString()
            val coverRes = model.createResource(coverUri, SchemaDO.CoverArt)

            coverRes.addProperty(SchemaDO.fileFormat, bookCover.mimeType)
            coverRes.addProperty(SchemaDO.location, bookCover.location)

            bookRes.addProperty(SchemaDO.image, coverRes)

            logger.info { "Added book cover: $bookCover" }

            coverRes
        }
    }

    private fun createDateLiteral(value: String): Literal {
        val localDate = LocalDate.parse(value)
        val calendar = Calendar.getInstance()
        calendar.time = Date.from(localDate.atStartOfDay().toInstant(ZoneOffset.UTC))
        return model.createTypedLiteral(calendar)
    }

    private fun getOrCreateAuthor(author: Author): Resource {
        val authorRes = model.getResource(author.id.value)
        if (authorRes == null)
            logger.info { "Author $author does not exists in DB. Creating new resource." }

        return authorRes ?: addAuthorRes(author.name)
    }

    fun findAuthorById(id: AuthorId): Author? {
        val authorRes = model.getResource(id.value)
        return authorRes?.let {
            Author(AuthorId(authorRes.uri), authorRes.getProperty(FOAF.name).string)
        }
    }

    fun findAllAuthors(): List<Author> {
        val query = BasicSparqlQuery(dataset.prefixMapping.nsPrefixMap)
                .addSelector("author")
                .addCondition("?author", "rdf:type", "foaf:Person")
        val solutions = executeQuery(query, dataset)
        return solutions.map { createAuthorFrom(it) }
    }

    fun addAuthor(authorName: String): Author {
        val authorRes = addAuthorRes(authorName)
        return Author(AuthorId(authorRes.uri), authorName)
    }

    private fun addAuthorRes(authorName: String): Resource {
        return WriteTx(dataset).use {
            val authorUri = UUID.randomUUID().toString()
            val authorRes = model.createResource(authorUri, FOAF.Person)
                    .addProperty(FOAF.name, authorName)
            logger.info { "Added new author to DB: $authorRes"}
            authorRes
        }
    }

    private fun executeBookQuery(sparqlQuery: SparqlQuery, dataset: Dataset): List<Book> {
        val querySolutions = executeQuery(sparqlQuery, dataset)
        return querySolutions.map { createBookFrom(it) }
    }

    private fun executeQuery(sparqlQuery: SparqlQuery, dataset: Dataset): List<QuerySolution> =
            executeQuery(sparqlQuery.build(), dataset)

    private fun executeQuery(query: String, dataset: Dataset): List<QuerySolution> {
        return ReadTx(dataset).use {
            val queryWithPrefixes = dataset.prefixMapping.nsPrefixMap
                    .map { e -> "PREFIX ${e.key}: <${e.value}>" }
                    .joinToString("\n", "", "\n$query")
            logger.debug { "Executing query:\n$queryWithPrefixes" }

            val queryExecution = QueryExecutionFactory.create(queryWithPrefixes, dataset)
            try {
                queryExecution.execSelect().asSequence().toList()
            } catch (e: Exception) {
                logger.warn("Exception occurred during query execution. Query: $queryWithPrefixes", e)
                throw e
            } finally {
                queryExecution.close()
            }
        }
    }

    private fun query(q: String): String {
        return model.nsPrefixMap
                .map { e -> "PREFIX ${e.key}: <${e.value}>" }
                .joinToString("\n", "", "\n$q")
    }

    private fun createBookFrom(solution: QuerySolution): Book {
        val bookRes = solution["book"]?.asResource()
                      ?: throw DbConsistencyException("Invalid book resource: $solution")
        return RdfBookLibraryMapper.createBookFrom(bookRes)
    }

    private fun createAuthorFrom(solution: QuerySolution): Author {
        val authRes = solution["author"]?.asResource()
                      ?: throw DbConsistencyException("Invalid author resource: $solution")
        return RdfBookLibraryMapper.createAuthorFrom(authRes)
    }

    fun clearAll() {
        WriteTx(dataset).use {
            model.removeAll()
        }
    }
}

