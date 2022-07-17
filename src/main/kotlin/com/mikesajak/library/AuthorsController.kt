package com.mikesajak.library

import com.mikesajak.library.model.Author
import com.mikesajak.library.model.AuthorId
import com.mikesajak.library.model.AuthorService
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import jakarta.inject.Named

@Controller("/api/authors")
class AuthorsController(@Named("Rdf")  private val authorService: AuthorService) {

    @Get
    fun getAuthors(): List<Author> =
            authorService.getAllAuthors()

    @Get
    fun getAuthor(id: AuthorId): Author? =
            authorService.getAuthor(id)
}