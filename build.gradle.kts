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
    implementation("org.jetbrains.compose.components:components-resources:1.10.3")

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

    // Fletch's liquid glass
    implementation("io.github.fletchmckee.liquid:liquid:1.1.1")
}

// Done after distributable is created
tasks.register<Sync>("copyNativeLibsToApp") {
    dependsOn("createReleaseDistributable")

    from("cpp/libConduit/libconduit.dylib")
    from("cpp/macwindow/libmacwindow.dylib")

    into(layout.buildDirectory.dir(
        "compose/binaries/main-release/app/Conduit.app/Contents/Frameworks"
    ))
}

compose.desktop {
    application {
        mainClass = "com.utilities.conduit.MainKt"

        jvmArgs("--enable-native-access=ALL-UNNAMED")

        buildTypes {
            release {
                proguard {
                    version.set("7.10.0")
                    configurationFiles.from(project.file("proguard-rules.pro"))
                }
            }
        }

        nativeDistributions {
            packageName = "Conduit"
            packageVersion = "1.0.0"

            macOS {
                iconFile.set(project.file("packaging/Conduit.icns"))
            }
        }
    }
}
