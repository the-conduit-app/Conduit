plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlinx.serialization)
}

kotlin {
    jvmToolchain(26)
}

dependencies {
    // Compose
    implementation(compose.desktop.currentOs)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.resources)

    // KotlinX
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.kotlinx.logger)

    implementation(libs.kotlinx.coroutines.swing)

    // Tools & Native
    implementation(libs.jna)
    implementation(libs.jna.platform)

    // Kuiver Treeview
    implementation("io.github.justdeko:kuiver:0.4.1")
}

compose.desktop {
    application {
        mainClass = "com.utilities.conduit.MainKt"

        jvmArgs("--enable-native-access=ALL-UNNAMED")
    }
}

tasks.register<JavaExec>("runMaintTest") {
    group = "verification"
    description = "Run standalone maintenance test"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.utilities.conduit.debug.MaintTestMainKt")

    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
