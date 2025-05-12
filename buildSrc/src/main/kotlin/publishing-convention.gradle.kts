import com.vanniktech.maven.publish.SonatypeHost
import java.net.URI

plugins {
    `maven-publish`
    signing
    alias(libs.plugins.maven.publish)
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates((group as String), name, version.toString())
    pom {
        url.set("https://github.com/GenKt/GenKt")
        licenses {
            license {
                name.set("Apache License Version 2.0")
                url.set("https://www.apache.org/licenses/LICENSE-2.0")
                distribution.set("https://www.apache.org/licenses/LICENSE-2.0")
            }
        }
        developers {
            developer {
                id.set("Stream29")
                name.set("Stream")
                url.set("https://github.com/Stream29/")
            }
            developer {
                id.set("ConstasJ")
                name.set("ConstasJ")
                url.set("https://github.com/ConstasJ/")
            }
        }
        scm {
            url.set("https://github.com/GenKt/GenKt")
            connection.set("scm:git:git://github.com/GenKt/GenKt.git")
            developerConnection.set("scm:git:ssh://git@github.com/GenKt/GenKt.git")
        }
    }
}

publishing {
    repositories {
        maven {
            name = "GitHubPackages"
            url = URI("https://maven.pkg.github.com/GenKt/GenKt")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}

signing {
    if (project.hasProperty("signing.gnupg.keyName")) {
        useGpgCmd()
        sign(publishing.publications)
    }
}