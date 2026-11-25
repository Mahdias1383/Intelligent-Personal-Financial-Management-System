// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

val kspAwtExceptionHandler = Thread.UncaughtExceptionHandler { thread, throwable ->
  val isKspAwtNpe =
    thread.name.startsWith("AWT-EventQueue") &&
      throwable is NullPointerException &&
      (throwable.message?.contains("ksp.com.intellij.openapi.application.Application") == true ||
        throwable.stackTrace.any { it.className.startsWith("ksp.com.intellij.") })
  if (!isKspAwtNpe) {
    System.err.print("Exception in thread \"${thread.name}\" ")
    throwable.printStackTrace(System.err)
  }
}

fun installKspAwtExceptionFilter() {
  System.setProperty("java.awt.headless", "true")
  Thread.setDefaultUncaughtExceptionHandler(kspAwtExceptionHandler)
  Thread.getAllStackTraces().keys
    .filter { it.name.startsWith("AWT-EventQueue") }
    .forEach { it.uncaughtExceptionHandler = kspAwtExceptionHandler }
}

installKspAwtExceptionFilter()

