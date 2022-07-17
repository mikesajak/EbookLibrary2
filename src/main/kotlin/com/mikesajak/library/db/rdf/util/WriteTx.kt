package com.mikesajak.library.db.rdf.util

import mu.KotlinLogging
import org.apache.jena.query.Dataset
import org.apache.jena.query.ReadWrite

class WriteTx(val dataset: Dataset): AutoCloseable {
    private val logger = KotlinLogging.logger {}
    private var managedTx = false
    init {
        if (dataset.transactionMode() != ReadWrite.WRITE) {
            logger.debug { "Starting managed write transaction" }
            managedTx = true
            dataset.begin(ReadWrite.WRITE)
        } else {
            logger.debug { "Dataset already in read transaction, reusing." }
        }
    }

    override fun close() {
        if (managedTx) {
            logger.debug { "Committing and closing managed write transaction" }
            dataset.commit()
            dataset.close()
        }
    }
}