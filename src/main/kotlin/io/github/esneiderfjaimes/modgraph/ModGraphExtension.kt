package io.github.esneiderfjaimes.modgraph

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import javax.inject.Inject

abstract class ModGraphExtension @Inject constructor(objects: ObjectFactory) {

    val outputDirPath: Property<String> = objects.property(String::class.java)

    val outputFilePrefix: Property<String> = objects.property(String::class.java)

    val outputFileType: Property<String> = objects.property(String::class.java)

    val stylePath: Property<String> = objects.property(String::class.java)

}