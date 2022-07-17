package com.mikesajak.library.db.jpa

import io.micronaut.data.annotation.Query
import io.micronaut.data.annotation.Repository
import io.micronaut.data.repository.CrudRepository
import io.micronaut.data.repository.PageableRepository
import io.micronaut.data.repository.jpa.JpaSpecificationExecutor

@Repository
interface AuthorsJpaRepository : PageableRepository<JpaAuthorEntity, Long>

@Repository
interface SeriesJpaRepository: PageableRepository<JpaSeriesEntity, Long>

@Repository
abstract class BooksJpaRepository
    : CrudRepository<JpaBookEntity, Long>, JpaSpecificationExecutor<JpaBookEntity> {
    @Query("select * from book b where b.authors.id = :id")
    abstract fun findByAuthorId(id: Long): List<JpaBookEntity>
}
