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
    id("com.android.application") version "8.9.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("com.getkeepsafe.dexcount") version "4.0.0"
}

//subprojects{
//    tasks.withType(Test).configureEach{
//        jvmArgs = jvmArgs + ['--add-opens=java.base/java.lang=ALL-UNNAMED']
//    }
//}
