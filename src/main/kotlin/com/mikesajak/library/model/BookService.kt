package com.mikesajak.library.model

interface BookService {
    fun getBook(id: BookId): Book?
    fun getAllBooks(): List<Book>
    fun findByTitle(title: String): List<Book>
    fun findBooks(query: String): List<Book>
    fun findByAuthor(authorId: AuthorId): List<Book>

    fun addBook(book: Book): String

    fun addBookCover(bookCover: BookCover): String
    fun getBookCover(coverId: CoverId): ByteArray
    fun getBookCover(bookId: BookId): ByteArray

    fun addBookFormat(bookFormat: BookFormat): String
}