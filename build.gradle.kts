// Top-level build file where you can add configuration options common to all sub-projects/modules.
//buildscript {
//    dependencies {
//        classpath 'com.android.tools.build:gradle:8.7.0'
//        classpath 'com.getkeepsafe.dexcount:dexcount-gradle-plugin:0.8.2'
//        classpath "org.jetbrains.kotlin:kotlin-gradle-plugin:$KOTLIN_VERSION"
//        classpath 'org.codehaus.groovy:groovy-all:2.4.15'
//    }
//}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.github.triplet.play) apply false
    alias(libs.plugins.getkeepsafe.dexcount)
}

//subprojects{
//    tasks.withType(Test).configureEach{
//        jvmArgs = jvmArgs + ['--add-opens=java.base/java.lang=ALL-UNNAMED']
//    }
//}
