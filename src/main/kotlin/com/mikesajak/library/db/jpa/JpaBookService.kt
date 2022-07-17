package com.mikesajak.library.db.jpa

import com.mikesajak.library.db.parser.jpa.RSQLToJPAQueryParser
import com.mikesajak.library.model.*
import io.micronaut.data.repository.jpa.criteria.QuerySpecification
import jakarta.inject.Singleton
import mu.KotlinLogging

@Singleton
class JpaBookService(private val bookRepository: BooksJpaRepository,
                     private val rsqlToJPAQueryParser: RSQLToJPAQueryParser) : BookService {
    private val logger = KotlinLogging.logger {}

    override fun getBook(id: BookId): Book? {
        return try {
            bookRepository.findById(id.value.toLong())
                    .map { b -> createBook(b) }.orElse(null)
        } catch (e: NumberFormatException) {
            logger.info("Invalid book id: $id")
            null
        }
    }

    override fun getAllBooks(): List<Book> {
        return bookRepository.findAll()
                .map { b -> createBook(b) }
    }

    override fun findByTitle(title: String): List<Book> {
        return bookRepository.findAll(fromQuery("title==$title"))
                .map { createBook(it) }
    }

    override fun findBooks(query: String): List<Book> {
        return bookRepository.findAll(fromQuery(query))
                .map { b -> createBook(b) }
    }

    override fun findByAuthor(authorId: AuthorId): List<Book> {
        return bookRepository.findByAuthorId(authorId.value.toLong())
                .map { createBook(it) }
    }

    private fun createBook(b: JpaBookEntity) = Book(
            BookId(b.id.toString()),
            b.title,
            b.authors.map { a -> Author(AuthorId(a.id.toString()), a.name) },
            b.identifiers.map { i -> Identifier.from(i) },
            b.languages,
            b.tags,
            b.creationDate,
            b.publicationDate,
            b.publisher,
            b.series?.let { s -> SeriesEntry(s.title, 0) },
            b.description
    )

    private fun fromQuery(rsqlQuery: String) = QuerySpecification<JpaBookEntity> { root, query, criteriaBuilder ->
        criteriaBuilder.and(query.restriction,
                            rsqlToJPAQueryParser.parse(rsqlQuery, criteriaBuilder, root))
    }

    override fun addBook(book: Book): String {
        TODO("Not yet implemented")
    }

    override fun addBookCover(bookCover: BookCover): String {
        TODO("Not yet implemented")
    }

    override fun getBookCover(coverId: CoverId): ByteArray {
        TODO("Not yet implemented")
    }

    override fun getBookCover(bookId: BookId): ByteArray {
        TODO("Not yet implemented")
    }

    override fun addBookFormat(bookFormat: BookFormat): String {
        TODO("Not yet implemented")
    }
}
