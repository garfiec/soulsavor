package com.garfiec.util.s3

fun String.endWithSlash(): String = if (this.endsWith("/")) this else "$this/"
