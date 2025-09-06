package io.github.bionictigers.axiom.core.utils

import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.superclasses

/**
 * Non-inline recursive helper that searches through the supertypes
 * for a property named [propertyName] that is annotated with [annotationClass].
 */
internal fun <T : Annotation> searchSuperTypes(
    kClass: KClass<*>,
    propertyName: String,
    annotationClass: KClass<T>
): Boolean {
    for (supertype in kClass.superclasses) {
        val superProp = supertype.memberProperties.firstOrNull { it.name == propertyName }
        if (superProp != null && superProp.annotations.any { annotationClass.isInstance(it) }) {
            return true
        }
        if (searchSuperTypes(supertype, propertyName, annotationClass)) {
            return true
        }
    }
    return false
}

/**
 * Recursively searches through supertypes for a property named [propertyName]
 * and returns the first annotation of type [annotationClass], or null if not found.
 */
internal fun <T : Annotation> findAnnotationInSuperTypes(
    kClass: KClass<*>,
    propertyName: String,
    annotationClass: KClass<T>
): T? {
    for (supertype in kClass.superclasses) {
        // look for the property on this supertype
        val superProp = supertype.memberProperties.firstOrNull { it.name == propertyName }
        if (superProp != null) {
            // if found, check its annotations
            val ann = superProp.annotations.firstOrNull { annotationClass.isInstance(it) } as? T
            if (ann != null) return ann
        }
        // otherwise recurse up
        findAnnotationInSuperTypes(supertype, propertyName, annotationClass)?.let { return it }
    }
    return null
}

/** Finds a property by name on a class, searching declared members and supertypes. Includes private members. */
internal fun KClass<*>.findPropertyInHierarchy(name: String): KProperty1<Any, *>? {
    // First try the fast path (public/protected + inherited)
    @Suppress("UNCHECKED_CAST")
    memberProperties.firstOrNull { it.name == name }?.let { return it as KProperty1<Any, *> }

    // Then include private props by traversing declared members up the hierarchy
    var current: KClass<*>? = this
    while (current != null && current != Any::class) {
        @Suppress("UNCHECKED_CAST")
        current.declaredMemberProperties
            .firstOrNull { it.name == name }
            ?.let { return it as KProperty1<Any, *> }
        current = current.superclasses.firstOrNull()
    }
    return null
}


internal fun <T : Any> String.convertTo(targetClass: KClass<T>): T {
    return when (targetClass) {
        Int::class -> this.toInt() as T
        Double::class -> this.toDouble() as T
        Float::class -> this.toFloat() as T
        Long::class -> this.toLong() as T
        Boolean::class -> this.toBoolean() as T
        String::class -> this as T
        else -> throw IllegalArgumentException("Unsupported conversion to ${targetClass.simpleName}")
    }
}

