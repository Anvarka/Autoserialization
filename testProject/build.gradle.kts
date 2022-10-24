apply {
    plugin("application")
}

configure<JavaApplication> {
    mainClass.set("ru.ifmo.java.serialization.Main")
}

dependencies {
    implementation(project(":annotations"))
    annotationProcessor(project(":processor"))
}
