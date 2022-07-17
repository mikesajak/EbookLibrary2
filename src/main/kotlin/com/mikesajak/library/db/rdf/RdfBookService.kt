package com.mikesajak.library.db.rdf

import com.mikesajak.library.model.*
import jakarta.inject.Singleton

@Singleton
class RdfBookService(private val rdfRepository: RdfRepository) : BookService {
    override fun getBook(id: BookId): Book? =
            rdfRepository.findBookById(id)

    override fun getAllBooks(): List<Book> =
            rdfRepository.findAllBooks()

    override fun findByTitle(title: String): List<Book> {
        return rdfRepository.findBooks("""title=="$title"""")
    }

    override fun findBooks(query: String): List<Book> =
            rdfRepository.findBooks(query)

    override fun findByAuthor(authorId: AuthorId): List<Book> =
            rdfRepository.findBooksByAuthorId(authorId)

    override fun addBook(book: Book): String {
        val bookRes = rdfRepository.addBook(book)
        return bookRes.uri
    }

    override fun addBookCover(bookCover: BookCover): String {
        val coverRes = rdfRepository.addBookCover(bookCover)
        return coverRes.uri
    }

    override fun getBookCover(coverId: CoverId): ByteArray {
        TODO("Not yet implemented")
    }

    override fun getBookCover(bookId: BookId): ByteArray {
        TODO("Not yet implemented")
    }

    override fun addBookFormat(bookFormat: BookFormat): String {
        val formatRes = rdfRepository.addBookFormat(bookFormat)
        return formatRes.uri
    }

}