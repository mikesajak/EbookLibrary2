//package com.mikesajak.library.db.sql
//
//import com.mikesajak.library.db.BookEntity
//import com.mikesajak.library.db.BooksJdbcRepository
//import com.mikesajak.library.model.*
//import mu.KotlinLogging
//
//class SqlBookService(private val booksRepository: BooksJdbcRepository) : BookService {
//    private val logger = KotlinLogging.logger {}
//
//    override fun getBook(id: BookId): Book? {
//        try {
//            return booksRepository.findById(id.value.toLong())
//                    .map { createBook(it) }
//                    .orElse(null)
//        } catch (e: NumberFormatException) {
//            logger.info("Invalid book id: $id")
//            return null
//        }
//    }
//
//    override fun getAllBooks(): List<Book> {
//        return booksRepository.findAll()
//                .map { entity -> createBook(entity) }
//    }
//
//    override fun findBooks(query: String): List<Book> {
//        return booksRepository.findByQuery(query)
//                .map { createBook(it) }
//    }
//
//    override fun findByTitle(title: String): Book? {
//        TODO("Not yet implemented")
//    }
//
//    private fun createBook(entity: BookEntity) = Book(
//        BookId(entity.id.toString()),
//        entity.title,
//        entity.authors.map { AuthorRef(AuthorId(it.id.toString()), it.name) },
//        entity.identifiers.map { Identifier("", it) },
//        entity.languages,
//        entity.tags,
//        creationDate = entity.creationDate,
//        publicationDate = entity.publicationDate,
//        publishedBy = entity.publisher,
//        series = entity.series?.let { SeriesEntry(it.title, 1) }, // TODO: hardcoded volume==1
//        entity.description
//    )
//}