package io.github.bionictigers.axiom.core.web

@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
annotation class Hidden(val exclude: Boolean = true)
