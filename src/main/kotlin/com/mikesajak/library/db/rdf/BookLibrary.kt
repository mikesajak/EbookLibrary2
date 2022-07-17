package com.mikesajak.library.db.rdf

import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Property

class BookLibrary {
    companion object {
        private val model = ModelFactory.createDefaultModel()
        const val NS = "http://booklibrary.org/"

        fun getURI(): String = NS

        val identifier: Property = property("identifier")
        val tag: Property = property("tag")

        private fun property(name: String) = model.createProperty(NS, name)
    }
}