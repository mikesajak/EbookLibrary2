package com.mikesajak.library.db.rdf.util

import mu.KotlinLogging
import org.apache.jena.query.Dataset
import org.apache.jena.query.ReadWrite

class ReadTx(val dataset: Dataset) : AutoCloseable {
    private val logger = KotlinLogging.logger {}
    private var managedTx = false

    init {
        if (dataset.transactionMode() != ReadWrite.READ) {
            logger.debug { "Starting managed read transaction" }
            managedTx = true
            dataset.begin(ReadWrite.READ)
        } else {
            logger.debug { "Dataset already in read transaction, reusing." }
        }
    }

    override fun close() {
        if (managedTx) {
            logger.debug { "Closing managed read transaction" }
            dataset.close()
        }
    }
}