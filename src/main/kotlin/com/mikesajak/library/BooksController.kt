package com.mikesajak.library

import com.mikesajak.library.model.AuthorId
import com.mikesajak.library.model.Book
import com.mikesajak.library.model.BookId
import com.mikesajak.library.model.BookService
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jakarta.inject.Named

@Controller("/api/books")
class BooksController(@Named("Rdf") private val bookService: BookService) {

    @Get
    fun getBook(bookId: BookId): Book? =
            bookService.getBook(bookId)

    @Get
    fun getBooks(query: String?): List<Book> =
            if (query == null) bookService.getAllBooks()
            else bookService.findBooks(query)

    @Get
    fun getAllBooksByAuthor(authorId: AuthorId): List<Book> =
            bookService.findByAuthor(authorId)
}