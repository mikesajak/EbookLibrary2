package com.mikesajak.library.db.rdf

import com.mikesajak.library.model.Author
import com.mikesajak.library.model.AuthorId
import com.mikesajak.library.model.AuthorService
import jakarta.inject.Singleton

@Singleton
class RdfAuthorService(private val rdfRepository: RdfRepository) : AuthorService {
    override fun getAllAuthors(): List<Author> =
        rdfRepository.findAllAuthors()

    override fun getAuthor(id: AuthorId): Author? =
        rdfRepository.findAuthorById(id)

}