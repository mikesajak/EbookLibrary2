package com.mikesajak.library

import io.micronaut.runtime.Micronaut.build
import org.h2.tools.Server

fun main(args: Array<String>) {
//	Server.createTcpServer("-tcp", "-tcpAllowOthers", "-tcpPort", "9090").start()
	Server.createWebServer().start()
	build()
	    .args(*args)
		.packages("com.mikesajak.library")
		.start()
}

