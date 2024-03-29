package com.github.mynameisscr.kinja

import com.github.mynameisscr.kinja.exception.CandidateIsAlreadyExistsException
import com.github.mynameisscr.kinja.exception.CandidateWithoutNameException
import com.github.mynameisscr.kinja.inject.InjectCandidate
import com.github.mynameisscr.kinja.inject.InjectScope
import com.github.mynameisscr.kinja.uuid.UUIDGenerator
import java.util.LinkedList
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KType

class KinjaCandidatesStorage(
    private val uuidGenerator: UUIDGenerator,
) {

    private val candidates = LinkedList<InjectCandidate>()

    fun single(func: KFunction<Any>, block: CandidateProperties.() -> Unit = {}) {
        addCandidate(InjectScope.SINGLE, block, func)
    }

    fun factory(func: KFunction<Any>, block: CandidateProperties.() -> Unit = {}) {
        addCandidate(InjectScope.FACTORY, block, func)
    }

    fun getCandidates(): List<InjectCandidate> =
        candidates.map { it.copy() }

    private fun addCandidate(scope: InjectScope, block: CandidateProperties.() -> Unit, func: KFunction<Any>) {
        val props = CandidateProperties(scope)
        props.block()

        val name = calculateName(props.name, func.returnType)
        validate(name, func.returnType)

        candidates.add(
            InjectCandidate(
                id = uuidGenerator.generate(),
                name = name,
                func = func,
                scope = props.scope,
            )
        )
    }

    private fun calculateName(propName: String?, funcReturnType: KType): String =
        propName
            ?: (funcReturnType.classifier as KClass<*>).simpleName
                ?.replaceFirstChar { it.lowercase() }
            ?: throw CandidateWithoutNameException()

    private fun validate(name: String, returnType: KType) {
        if (name.isBlank()) {
            throw CandidateWithoutNameException()
        }
        if (candidates.any { it.returnType == returnType && it.name == name }) {
            throw CandidateIsAlreadyExistsException(name, returnType)
        }
    }

    @Suppress("DataClassShouldBeImmutable")
    data class CandidateProperties(
        val scope: InjectScope,
        var name: String? = null,
    )
}
