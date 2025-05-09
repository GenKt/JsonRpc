/*
    Copied from https://github.com/Kotlin/kotlinx.serialization
 */
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.*
import org.jetbrains.kotlin.gradle.dsl.*
import org.jetbrains.kotlin.gradle.plugin.mpp.*

fun getJvmTargetByVersion(version: Int) = when (version) {
    8 -> JvmTarget.JVM_1_8
    11 -> JvmTarget.JVM_11
    17 -> JvmTarget.JVM_17
    21 -> JvmTarget.JVM_21
    else -> JvmTarget.DEFAULT
}

fun KotlinMultiplatformExtension.configureJvm(jdkVersion: Int) {
    jvm {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget = getJvmTargetByVersion(jdkVersion)
            freeCompilerArgs.addAll("-Xjvm-default=all-compatibility")
        }
    }
    jvmToolchain(jdkVersion)
}

fun KotlinMultiplatformExtension.configureJs() {
    js {
        nodejs {
            testTask {
                useMocha {
                    timeout = "10s"
                }
            }
        }

        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            sourceMap = true
            moduleKind = JsModuleKind.MODULE_UMD
        }
    }
}

@OptIn(ExperimentalWasmDsl::class)
fun KotlinMultiplatformExtension.configureWasm() {
    wasmJs {
        nodejs()
    }
    wasmWasi {
        nodejs()
    }
}

fun KotlinMultiplatformExtension.configureNative() {
    linuxArm64()
    macosX64()
    macosArm64()
    iosSimulatorArm64()
    iosX64()

    // Tier 2
    linuxX64()
    linuxArm64()
    watchosSimulatorArm64()
    watchosX64()
    watchosArm32()
    watchosArm64()
    tvosSimulatorArm64()
    tvosX64()
    tvosArm64()
    iosArm64()

    // Tier 3
    mingwX64()
    watchosDeviceArm64()
    androidNativeArm32()
    androidNativeArm64()
    androidNativeX86()
    androidNativeX64()

    // setup tests running in RELEASE mode
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.test(listOf(NativeBuildType.RELEASE))
    }
    targets.withType<KotlinNativeTargetWithTests<*>>().configureEach {
        testRuns.create("releaseTest") {
            setExecutionSourceFrom(binaries.getTest(NativeBuildType.RELEASE))
        }
    }
}
