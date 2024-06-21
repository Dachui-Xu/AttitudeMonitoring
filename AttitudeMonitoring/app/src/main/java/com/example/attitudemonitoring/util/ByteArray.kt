package com.example.attitudemonitoring.util


@OptIn(ExperimentalUnsignedTypes::class)
fun ByteArray.toHex(): String = asUByteArray().joinToString("") { it.toString(radix = 16).padStart(2, '0') }
