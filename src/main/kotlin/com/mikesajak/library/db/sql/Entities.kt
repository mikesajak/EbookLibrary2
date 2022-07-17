//@file:Suppress("jol", "unused")
//
//package com.mikesajak.library.db
//
//import java.time.LocalDate
//import javax.persistence.*
//
//@Entity(name="Books")
//class BookEntity(
//    var identifier: String,
//    var title: String,
//    @ManyToMany
//    var authors: List<AuthorEntity>,
//    @ElementCollection
//    var tags: List<String>,
//    @ElementCollection
//    var identifiers: List<String>,
//    var creationDate: LocalDate?,
//    var publicationDate: LocalDate?,
//    var publisher: String?,
//    @ElementCollection
//    var languages: List<String>,
//
//    @ManyToOne//(cascade = [CascadeType.MERGE, CascadeType.PERSIST])
//    var series: SeriesEntity?,
//    var description: String,
//
//    @OneToMany(cascade = [CascadeType.ALL])
//    @JoinColumn(name = "book_id")
//    var formats: List<BookFormatEntity>,
//
//    @OneToOne(cascade = [CascadeType.ALL])
//    var cover: BookCoverEntity?,
//
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null)
//
////@MappedEntity("Tags")
////class TagEntity(@Id var name: String)
////
////@MappedEntity("Identifiers")
////class IdentifierEntity(@Id var name: String)
////
////@MappedEntity("Languages")
////class LanguageEntity(@Id var name: String)
//
//@Entity(name = "Series")
//class SeriesEntity(
//    var title: String,
//    var description: String?,
//    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null)
//
//@Entity(name = "BookFormats")
//class BookFormatEntity(var type: String,
//                       var fileLocation: String,
//                       @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null)
//
//@Entity(name = "BookCovers")
//class BookCoverEntity(var mimeType: String,
//                      var fileLocation: String,
//                      @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null)
//
//@Entity(name = "Authors")
//class AuthorEntity(var name: String,
//                   @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null)