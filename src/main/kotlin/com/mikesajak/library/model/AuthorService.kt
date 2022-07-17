package com.mikesajak.library.model

interface AuthorService {
    fun getAllAuthors(): List<Author>
    fun getAuthor(id: AuthorId): Author?
}