package com.broxus.utils

/**
 * Inspired by Crayon project, but with OS dependency
 */

import org.apache.commons.lang3.SystemUtils

fun String.bold() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[1m${this}\u001b[0m"
fun String.italic() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[3m${this}\u001b[0m"
fun String.underline() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[4m${this}\u001b[0m"
fun String.reversed() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[7m${this}\u001b[0m"
fun String.black() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[30m${this}\u001b[0m"
fun String.blue() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[34m${this}\u001b[0m"
fun String.cyan() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[36m${this}\u001b[0m"
fun String.green() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[32m${this}\u001b[0m"
fun String.magenta() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[35m${this}\u001b[0m"
fun String.red() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[31m${this}\u001b[0m"
fun String.white() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[37m${this}\u001b[0m"
fun String.yellow() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[33m${this}\u001b[0m"
fun String.brightBlack() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[30;1m${this}\u001b[0m"
fun String.brightBlue() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[34;1m${this}\u001b[0m"
fun String.brightCyan() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[36;1m${this}\u001b[0m"
fun String.brightGreen() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[32;1m${this}\u001b[0m"
fun String.brightMagenta() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[35;1m${this}\u001b[0m"
fun String.brightRed() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[31;1m${this}\u001b[0m"
fun String.brightWhite() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[37;1m${this}\u001b[0m"
fun String.brightYellow() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[33;1m${this}\u001b[0m"
fun String.bgBlack() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[40m${this}\u001b[0m"
fun String.bgBlue() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[44m${this}\u001b[0m"
fun String.bgCyan() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[46m${this}\u001b[0m"
fun String.bgGreen() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[42m${this}\u001b[0m"
fun String.bgMagenta() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[45m${this}\u001b[0m"
fun String.bgRed() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[41m${this}\u001b[0m"
fun String.bgWhite() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[47m${this}\u001b[0m"
fun String.bgYellow() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[43m${this}\u001b[0m"
fun String.bgBrightBlack() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[40;1m${this}\u001b[0m"
fun String.bgBrightBlue() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[44;1m${this}\u001b[0m"
fun String.bgBrightCyan() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[46;1m${this}\u001b[0m"
fun String.bgBrightGreen() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[42;1m${this}\u001b[0m"
fun String.bgBrightMagenta() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[45;1m${this}\u001b[0m"
fun String.bgBrightRed() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[41;1m${this}\u001b[0m"
fun String.bgBrightWhite() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[47;1m${this}\u001b[0m"
fun String.bgBrightYellow() = if(SystemUtils.IS_OS_WINDOWS) this else "\u001b[43;1m${this}\u001b[0m"
