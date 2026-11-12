package com.blackout.files

object Version {
    val full: String get() = "File Manager ${BuildConfig.VERSION_NAME} \u00B7 build ${BuildConfig.BUILD_NUMBER} \u00B7 ${BuildConfig.GIT_SHA}"
    val tag: String get() = "build ${BuildConfig.BUILD_NUMBER}"
}
