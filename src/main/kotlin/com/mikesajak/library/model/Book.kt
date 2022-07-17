package com.mikesajak.library.model

import java.time.LocalDate

@JvmInline
value class BookId(val value: String)

data class Identifier(val schema: String, val id: String) {
    companion object {
        fun from(value: String): Identifier {
            val segments = value.split(':')
            val schema = if (!segments.isEmpty()) segments[0] else "id"
            val id = if (segments.size > 1) value else value.substring(schema.length+1)
            return Identifier(schema, id)
        }
    }
}

//@JvmInline
//value class Tag(val value: String)
//
//@JvmInline
//value class Language(val value: String)

data class SeriesEntry(val name: String, val number: Int)

@JvmInline
value class AuthorId(val value: String)

data class Author(val id: AuthorId, val name: String)

data class Book(val id: BookId?,
                val title: String,
                val authors: List<Author>,
                val identifiers: List<Identifier>,
                val languages: List<String>,
                val tags: List<String>,
                val creationDate: LocalDate?,
                val publicationDate: LocalDate?,
                val publishedBy: String?,
                val series: SeriesEntry?,
                val description: String?) {
    companion object {
        fun of(title: String) = of(title, listOf())

        fun of(title: String, author: Author) = of(title, listOf(author))

        fun of(title: String, authors: List<Author>) =
                Book(null, title, authors, listOf(), listOf(), listOf(),
                     null, null, null, null, null)
    }
}

@JvmInline
value class FormatId(val value: String)

data class BookFormat(val formatId: FormatId, val bookId: BookId, val mimeType: String, val location: String)

@JvmInline
value class CoverId(val value: String)

data class BookCover(val coverId: CoverId, val bookId: BookId, val mimeType: String, val location: String)
