@file:Suppress("jol", "unused")

package com.mikesajak.library.db.jpa

import io.micronaut.context.annotation.Requires
import java.time.LocalDate
import javax.persistence.*

@Requires(property = "storage.type", value = "jpa")
@Entity(name="Books")
class JpaBookEntity(
        var identifier: String,
        var title: String,
        @ManyToMany
        var authors: List<JpaAuthorEntity>,
        @ElementCollection
        var tags: List<String>,
        @ElementCollection
        var identifiers: List<String>,
        var creationDate: LocalDate?,
        var publicationDate: LocalDate?,
        var publisher: String?,
        @ElementCollection
        var languages: List<String>,

        @ManyToOne//(cascade = [CascadeType.MERGE, CascadeType.PERSIST])
        var series: JpaSeriesEntity?,
        var description: String,

        @OneToMany(cascade = [CascadeType.ALL])
        @JoinColumn(name = "book_id")
        var formats: List<JpaBookFormatEntity>,

        @OneToOne(cascade = [CascadeType.ALL])
        var cover: JpaBookCoverEntity?,

        @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null
)

//@MappedEntity("Tags")
//class TagEntity(@Id var name: String)
//
//@MappedEntity("Identifiers")
//class IdentifierEntity(@Id var name: String)
//
//@MappedEntity("Languages")
//class LanguageEntity(@Id var name: String)

@Requires(property = "storage.type", value = "jpa")
@Entity(name = "Series")
class JpaSeriesEntity(var title: String,
                      var description: String?,
                      @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null)

@Requires(property = "storage.type", value = "jpa")
@Entity(name = "BookFormats")
class JpaBookFormatEntity(var type: String,
                          var fileLocation: String,
                          @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null)

@Requires(property = "storage.type", value = "jpa")
@Entity(name = "BookCovers")
class JpaBookCoverEntity(var mimeType: String,
                         var fileLocation: String,
                         @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null)

@Requires(property = "storage.type", value = "jpa")
@Entity(name = "Authors")
class JpaAuthorEntity(var name: String,
                      @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null)