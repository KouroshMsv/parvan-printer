package com.parvanpajooh.sunmi.utils

enum class PrinterConnectionStatus(val code: Int) {

    NO_PRINTER(0x00000000),
    CHECK_PRINTER(0x00000001),
    FOUND_PRINTER(0x00000002),
    LOST_PRINTER(0x00000003)

}