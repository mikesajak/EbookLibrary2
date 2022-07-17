package com.mikesajak.library.db.rdf

import com.mikesajak.library.model.*
import org.apache.jena.rdf.model.Resource
import org.apache.jena.sparql.vocabulary.FOAF
import org.apache.jena.vocabulary.SchemaDO
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RdfBookLibraryMapper {
    companion object {
        fun createBookFrom(bookRes: Resource): Book {
            val title = bookRes.getProperty(SchemaDO.title)?.string
                ?: throw DbConsistencyException("Invalid book resource - title is mandatory. $bookRes")

            val authorRefs = bookRes.listProperties(SchemaDO.author)
                    .toList()
                    .map { stmt -> stmt.resource }
                    .filterNotNull()
                    .map {
                        Author(
                            AuthorId(it.uri ?: "unknown"),
                            it.getProperty(FOAF.name).string
                        )
                    }

            val identifiers = listProperties(bookRes, BookLibrary.identifier)
                    .map { Identifier.from(it) }

            val tags = listProperties(bookRes, BookLibrary.tag)

            val languages = listProperties(bookRes, SchemaDO.language)

            val creationDate = bookRes.getProperty(SchemaDO.dateCreated)?.string?.let {
                LocalDate.parse(
                    it,
                    DateTimeFormatter.ISO_DATE_TIME
                )
            }
            val publicationDate = bookRes.getProperty(SchemaDO.publishedOn)?.string?.let {
                LocalDate.parse(
                    it,
                    DateTimeFormatter.ISO_DATE_TIME
                )
            }
            val publisher = bookRes.getProperty(SchemaDO.publishedBy)?.string

//    val series = bookRes.getProperty(SchemaDO.partOfSeries).resource?.let { seriesRes->
//        val seriesId = seriesRes.id.labelString
//            ?: throw DbConsistencyException("Invalid series resource: $seriesRes referenced by book: $bookRes")
//        val seriesTitle = seriesRes.getProperty(SchemaDO.title)?.string
//            ?: throw DbConsistencyException("Invalid series resource: $seriesRes referenced by book: $bookRes")
//        val seriesVolumeNumber = bookRes.getProperty(SchemaDO.volumeNumber)?.int
//            ?: throw DbConsistencyException("Invalid series number in book: $bookRes")
//        SeriesEntry(seriesTitle, seriesVolumeNumber)
//    }
            val series = bookRes.getProperty(SchemaDO.partOfSeries)?.string?.let { seriesTitle ->
                val seriesVolumeNum = bookRes.getProperty(SchemaDO.volumeNumber)?.int
                    ?: throw DbConsistencyException("Invalid series number in book: $bookRes")
                SeriesEntry(seriesTitle, seriesVolumeNum)
            }

            val description = bookRes.getProperty(SchemaDO.description)?.string

            return Book(
                BookId(bookRes.uri), title, authorRefs, identifiers, languages,
                tags, creationDate, publicationDate, publisher, series, description
            )
        }

        fun createAuthorFrom(authorRes: Resource): Author {
            val name = authorRes.getProperty(FOAF.name)?.string
                        ?: throw DbConsistencyException("Invalid author resource - name is mandatory. $authorRes")

            return Author(AuthorId(authorRes.uri), name)
        }
    }
}