import com.google.protobuf.gradle.ProtobufExtension

plugins {
    alias(libs.plugins.openpiano.android.library)
    alias(libs.plugins.protobuf)
}

android {
    namespace = "dev.thomas_kiljanczyk.openpiano.core.datastore.proto"
}

extensions.configure<ProtobufExtension> {
    protoc {
        artifact = libs.protobuf.protoc.get().toString()
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                register("java") { option("lite") }
                register("kotlin") { option("lite") }
            }
        }
    }
}

dependencies {
    api(libs.protobuf.kotlin.lite)
    api(libs.androidx.datastore)
}
