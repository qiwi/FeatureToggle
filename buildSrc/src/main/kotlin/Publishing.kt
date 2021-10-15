/**
 * Copyright (c) 2021 QIWI
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.dokka.gradle.DokkaTask

fun Project.getProperty(propertyName: String): String = findProperty(propertyName)?.toString() ?: ""

fun Project.isSnapshot(): Boolean = getProperty("VERSION_NAME").endsWith("-SNAPSHOT")

fun Project.setupJavaLibraryPublishing() = afterEvaluate {
    val sourcesJar by tasks.creating(Jar::class) {
        archiveClassifier.set("sources")
        val javaPlugin = project.convention.getPlugin(JavaPluginConvention::class)
        from(javaPlugin.sourceSets.getByName("main").allSource)
    }

    setupLibraryPublishing(components["java"], sourcesJar)
}

fun Project.setupAndroidLibraryPublishing() = afterEvaluate {
    val sourcesJar by tasks.creating(Jar::class) {
        archiveClassifier.set("sources")
        val androidExtension = project.extensions.getByType(LibraryExtension::class)
        from(androidExtension.sourceSets.getByName("main").java.srcDirs)
    }

    setupLibraryPublishing(components["release"], sourcesJar)
}

fun Project.setupLibraryPublishing(softwareComponent: SoftwareComponent, sourcesJar: Any) {
    apply<MavenPublishPlugin>()
    apply<SigningPlugin>()

    val dokkaJavadoc by tasks.named<DokkaTask>("dokkaHtml") {
        outputDirectory.set(project.buildDir.resolve("dokka"))
        dokkaSourceSets.configureEach {
            noAndroidSdkLink.set(false)
        }
    }
    val javaDockJar by tasks.creating(Jar::class.java) {
        archiveClassifier.set("javadoc")
        dependsOn(dokkaJavadoc)
        from(project.buildDir.resolve("dokka"))
    }

    configure<PublishingExtension> {
        publications {
            create<MavenPublication>("release") {
                from(softwareComponent)
                artifact(sourcesJar)
                artifact(javaDockJar)
                groupId = project.getProperty("GROUP")
                artifactId = "featuretoggle-${project.name}"
                version = project.getProperty("VERSION_NAME")
                pom {
                    name.set("featuretoggle-${project.name}")
                    url.set(project.getProperty("POM_URL"))
                    description.set(project.getProperty("POM_DESCRIPTION"))
                    inceptionYear.set(project.getProperty("POM_INCEPTION_YEAR"))
                    scm {
                        url.set(project.getProperty("POM_SCM_URL"))
                        connection.set(project.getProperty("POM_SCM_CONNECTION"))
                        developerConnection.set(project.getProperty("POM_SCM_DEV_CONNECTION"))
                    }
                    licenses {
                        license {
                            name.set(project.getProperty("POM_LICENSE_NAME"))
                            url.set(project.getProperty("POM_LICENSE_URL"))
                            distribution.set(project.getProperty("POM_LICENSE_DIST"))
                        }
                    }
                    developers {
                        developer {
                            id.set(project.getProperty("POM_DEVELOPER_ID"))
                            name.set(project.getProperty("POM_DEVELOPER_NAME"))
                            email.set(project.getProperty("POM_DEVELOPER_EMAIL"))
                            url.set(project.getProperty("POM_DEVELOPER_URL"))
                        }
                    }
                }
                repositories {
                    maven {
                        credentials {
                            username = project.getProperty("MAVEN_USERNAME")
                            password = project.getProperty("MAVEN_PASSWORD")
                        }
                        if(project.isSnapshot()) {
                            setUrl(project.getProperty("MAVEN_SNAPSHOTS_URL"))
                        } else {
                            setUrl(project.getProperty("MAVEN_RELEASES_URL"))
                        }
                    }
                }
            }
        }
    }

    if(!project.isSnapshot()) {
        configure<SigningExtension> {
            val key = project.getProperty("SIGNING_KEY")
            val password = project.getProperty("SIGNING_PASSWORD")
            val publishing: PublishingExtension by project

            useInMemoryPgpKeys(key, password)
            sign(publishing.publications)
        }
    }
}