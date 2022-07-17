//package com.mikesajak.library.db
//
//import com.mikesajak.library.db.parser.sql.RSQLtoSQLQueryParser
//import io.micronaut.data.jdbc.annotation.JdbcRepository
//import io.micronaut.data.jdbc.runtime.JdbcOperations
//import io.micronaut.data.model.query.builder.sql.Dialect
//import io.micronaut.data.repository.CrudRepository
//import io.micronaut.data.repository.PageableRepository
//
//@JdbcRepository(dialect = Dialect.H2)
//interface AuthorsJdbcRepository : PageableRepository<AuthorEntity, Long>
//
//@JdbcRepository(dialect = Dialect.H2)
//interface SeriesJdbcRepository: PageableRepository<SeriesEntity, Long>
//
//@JdbcRepository(dialect = Dialect.H2)
//abstract class BooksJdbcRepository(private val queryParser: RSQLtoSQLQueryParser,
//                                   private val jdbcOps: JdbcOperations)
//        : CrudRepository<BookEntity, Long> {
//    //}, JpaSpecificationExecutor<BookEntity> {
//    fun findByQuery(query: String): List<BookEntity> {
//        val parsedQuery = queryParser.parse(query)
//        return jdbcOps.prepareStatement(parsedQuery) { statement ->
//            val resultSet = statement.executeQuery()
//            jdbcOps.entityStream(resultSet, BookEntity::class.java)
//                    .toList()
//        }
//    }
//}
