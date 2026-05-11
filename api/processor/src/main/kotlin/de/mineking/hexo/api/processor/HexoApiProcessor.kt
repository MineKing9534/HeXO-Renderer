package de.mineking.hexo.api.processor

import com.google.devtools.ksp.getClassDeclarationByName
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.reflect.KClass

private val SOCKET_EVENT_INTERFACE = ClassName("de.mineking.hexo.api.socket", "SocketEvent")
private val SOCKET_EVENT_NAME_ANNOTATION = ClassName("de.mineking.hexo.api.socket", "SocketEventName")
private val SERIALIZABLE_ANNOTATION = ClassName("kotlinx.serialization", "Serializable")

class HexoApiProcessor(private val codeGenerator: CodeGenerator) : SymbolProcessor {
    private var generated = false

    override fun process(resolver: Resolver): List<KSAnnotated> {
        if (generated) return emptyList()
        generateSocketEventRegistry(resolver)
        generated = true

        return emptyList()
    }

    private fun generateSocketEventRegistry(resolver: Resolver) {
        val socketEventInterface = resolver
            .getClassDeclarationByName(SOCKET_EVENT_INTERFACE.canonicalName)
            ?: error("Could not find marker interface '${SOCKET_EVENT_INTERFACE.canonicalName}'")

        val concreteEvents = socketEventInterface
            .getAllSealedSubclasses()
            .filterNot { it.classKind == ClassKind.INTERFACE }
            .associateBy { event ->
                val annotations = event.annotations.associateBy { it.annotationType.resolve().declaration.qualifiedName!!.asString() }
                require(SERIALIZABLE_ANNOTATION.canonicalName in annotations) {
                    "Expected event type ${event.qualifiedName?.asString()} to be @${SERIALIZABLE_ANNOTATION.canonicalName}"
                }

                val annotation = annotations[SOCKET_EVENT_NAME_ANNOTATION.canonicalName]
                require(annotation != null) {
                    "Expected event type ${event.qualifiedName?.asString()} to have the @${SOCKET_EVENT_NAME_ANNOTATION.canonicalName} annotation"
                }

                annotation.arguments[0].value as String
            }

        val dependencies = Dependencies(aggregating = true)
        FileSpec.builder("de.mineking.hexo.api.socket", "SocketEventRegistry")
            .addType(
                TypeSpec.objectBuilder("SocketEventRegistry")
                    .addModifiers(KModifier.INTERNAL)
                    .addProperty(createEventMappingsProperty(concreteEvents))
                    .addProperty(createEventNameMappingsProperty(concreteEvents))
                    .build(),
            )
            .build()
            .writeTo(codeGenerator, dependencies)
    }

    private fun createEventMappingsProperty(events: Map<String, KSClassDeclaration>): PropertySpec {
        val type = Map::class.asClassName().parameterizedBy(
            String::class.asClassName(),
            KClass::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(SOCKET_EVENT_INTERFACE)),
        )

        return PropertySpec.builder("events", type)
            .initializer(
                CodeBlock.builder()
                    .add("mapOf(\n")
                    .apply {
                        events.forEach { (name, type) ->
                            add("%S to %T::class,\n", name, type.toClassName())
                        }
                    }
                    .add(")")
                    .build(),
            )
            .build()
    }

    private fun createEventNameMappingsProperty(events: Map<String, KSClassDeclaration>): PropertySpec {
        val type = Map::class.asClassName().parameterizedBy(
            KClass::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(SOCKET_EVENT_INTERFACE)),
            String::class.asClassName(),
        )

        return PropertySpec.builder("eventNames", type)
            .initializer(
                CodeBlock.builder()
                    .add("mapOf(\n")
                    .apply {
                        events.forEach { (name, type) ->
                            add("%T::class to %S,\n", type.toClassName(), name)
                        }
                    }
                    .add(")")
                    .build(),
            )
            .build()
    }
}

private fun KSClassDeclaration.getAllSealedSubclasses(): Sequence<KSClassDeclaration> = getSealedSubclasses()
    .flatMap { sequenceOf(it) + it.getAllSealedSubclasses() }
