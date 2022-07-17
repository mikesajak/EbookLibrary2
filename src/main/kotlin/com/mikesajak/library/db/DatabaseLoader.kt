package com.mikesajak.library.db

import com.mikesajak.library.db.jpa.*
import io.micronaut.context.event.ApplicationEventListener
import io.micronaut.runtime.server.event.ServerStartupEvent
import jakarta.inject.Singleton
import java.time.LocalDate

@Singleton
class DatabaseLoader(private val authorsRepository: AuthorsJpaRepository,
                     private val booksRepository: BooksJpaRepository,
                     private val seriesRepository: SeriesJpaRepository
)
        : ApplicationEventListener<ServerStartupEvent> {

    private val langs = setOf("pl", "en", "de", "fr", "it", "jp", )
    private val formats = listOf("epub", "pdf", "chm", "txt", "doc")
    private val mimeTypes = listOf("jpeg", "png")

    override fun onApplicationEvent(event: ServerStartupEvent?) {
        val availableAuthors = (1..10)
                .map { idx -> authorsRepository.save(JpaAuthorEntity("Author-$idx")) }
                .toList()

        val availableSeries =
                listOf("Pieśń Lodu i ognia" to "Gra o tron",
                       "Wiedźmin" to null,
                       "Gra Endera" to "Gra Endera",
                       "Diuna" to "Saga o Diunie")
                        .map { (title, descr) -> seriesRepository.save(JpaSeriesEntity(title, descr)) }

        (1..10)
                .map { idx ->  booksRepository.save(createBook(idx, availableAuthors, availableSeries)) }
    }

    fun createBook(num: Int, availableAuthors: List<JpaAuthorEntity>, availableSeries: List<JpaSeriesEntity>) =
            JpaBookEntity(
                "book-id-$num",
                "Book-$num",
                (1..randInt(availableAuthors.size) + 1)
                        .map { pickFrom(availableAuthors) }
                        .filterNotNull()
                        .toSet()
                        .toList(),
                randStrings("tag", 10),
                listOf("ISBN:123-456-7890", "x-id:1234567890"),
                LocalDate.of(2000, 1, 12),
                null,
                "Publisher1",
                randSubset(langs, 3),
                pickFrom(availableSeries),
                genBlabla("bla", 20),
                (1..randInt(5))
                        .map { genBookFormat() }
                        .toList(),
                if (Math.random() > 0.7) genCover() else null
            )

    fun genBookFormat(): JpaBookFormatEntity {
        val formatType = formats[randInt(formats.size)]
        return JpaBookFormatEntity(formatType.uppercase(), randString(5, 10) + ".$formatType")
    }

    fun genCover(): JpaBookCoverEntity {
        val mimeType = mimeTypes[randInt(mimeTypes.size)]
        return JpaBookCoverEntity("application/$mimeType", randString(5, 10) + ".$mimeType")
    }

    private fun genBlabla(segment: String, upTo: Int): String {
        val str = (1..(1 + randInt(upTo)))
                .map { if (Math.random() > 0.8) " " + capitalize(segment) else segment }
                .joinToString()
                .trim()

        return capitalize(str)
    }

    private fun randInt(range: Int) = (Math.random() * range).toInt()

    private fun <T> pickFrom(values: List<T>, missCoeff: Double = 0.5): T? {
        if (Math.random() > missCoeff) return null
        return values[randInt(values.size)]
    }

    private fun randStrings(prefix: String, maxNum: Int = Int.MAX_VALUE): List<String> =
            (1..(1 + randInt(maxNum)))
                    .map { "$prefix$it" }
                    .toList()

    private fun randSubset(set: Set<String>, size: Int) =
            set.shuffled()
                    .take(Math.max(0, Math.min(size, set.size)))
                    .toList()

    private fun capitalize(str: String) =
            str.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

    private fun randString(minSize: Int, maxSize: Int): String {
        val allowedChars = ('a'..'z') + ('A'..'Z') + ('0'..'9')
        val genSize = minSize + randInt(maxSize - minSize)
        return (1..genSize)
                .map { allowedChars.random() }
                .joinToString("")
    }

}