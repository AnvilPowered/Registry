/*
 *   Anvil - AnvilPowered
 *   Copyright (C) 2020-2021
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.anvilpowered.registry.key

import io.leangen.geantyref.TypeToken
import org.anvilpowered.registry.Named
import org.anvilpowered.registry.Registry
import java.util.function.Function
import kotlin.experimental.ExperimentalTypeInference

abstract class Key<T> internal constructor(
    val typeToken: TypeToken<T>,
    override val name: String,
    fallbackValue: T?,
    userImmutable: Boolean,
    sensitive: Boolean,
    description: String?,
    parser: Function<String, T>?,
    toStringer: Function<T, String>?
) : Named, Comparable<Key<T>> {
    val fallbackValue: T?
    private val isUserImmutable: Boolean
    private val isSensitive: Boolean
    private val description: String?
    private var parser: Function<String, T>?
    private var toStringer: Function<T, String>?

    init {
        this.fallbackValue = fallbackValue
        isUserImmutable = userImmutable
        isSensitive = sensitive
        this.description = description
        if (parser == null) {
            this.parser = extractParser(fallbackValue)
        } else {
            this.parser = parser
        }
        this.toStringer = toStringer
    }

    interface Builder<T> {
        /**
         * Sets the name of the generated [Key]
         *
         * @param name The name to set
         * @return `this`
         */
        fun name(name: String): Builder<T>

        /**
         * Sets the fallback value of the generated [Key]
         *
         * @param fallbackValue The fallback value to set
         * @return `this`
         */
        fun fallback(fallbackValue: T?): Builder<T>

        /**
         * Indicates that the generated [Key] cannot be changed by the user.
         *
         * @return `this`
         */
        fun userImmutable(): Builder<T>

        /**
         * Indicates that the generated [Key] is sensitive (e.g. connection details) that should not
         * be accessible through regedit by default. Values of sensitive keys can only be viewed or modified
         * through registries that have [Keys.REGEDIT_ALLOW_SENSITIVE] enabled.
         *
         * @return `this`
         */
        fun sensitive(): Builder<T>

        /**
         * Sets the description of the generated [Key].
         *
         * @param description The description to set or `null` to remove it
         * @return `this`
         */
        fun description(description: String?): Builder<T>

        /**
         * Sets the parser of the generated [Key].
         *
         * @param parser The parser to set or `null` to remove it
         * @return `this`
         */
        fun parser(parser: Function<String, T>?): Builder<T>

        /**
         * Sets the toStringer of the generated [Key].
         *
         * @param toStringer The toStringer to set or `null` to remove it
         * @return `this`
         */
        fun toStringer(toStringer: Function<T, String>?): Builder<T>

        /**
         * Generates a [Key] based on this builder.
         *
         * @return The generated [Key]
         */
        fun build(): Key<T>
    }

    private fun extractParser(value: T?): Function<String, T>? {
        when (value) {
            is String -> {
                return Function { s: String -> s as T }
            }
            is Boolean -> {
                return Function { s: String? -> java.lang.Boolean.valueOf(s) as T }
            }
            is Double -> {
                return Function { s: String? -> java.lang.Double.valueOf(s) as T }
            }
            is Float -> {
                return Function { s: String? -> java.lang.Float.valueOf(s) as T }
            }
            is Long -> {
                return Function { s: String? -> java.lang.Long.valueOf(s) as T }
            }
            is Int -> {
                return Function { s: String? -> Integer.valueOf(s) as T }
            }
            is Short -> {
                return Function { s: String -> s.toShort() as T }
            }
            is Byte -> {
                return Function { s: String? -> java.lang.Byte.valueOf(s) as T }
            }
            else -> return null
        }
    }

    fun parse(value: String): T? {
        return parser?.apply(value)
    }

    fun toString(value: T): String {
        return toStringer?.apply(value) ?: "No toStringer set for $name!"
    }

    override fun compareTo(o: Key<T>): Int {
        return name.compareTo(o.name, ignoreCase = true)
    }

    override fun equals(o: Any?): Boolean {
        return o is Key<*> && name.equals(o.name, ignoreCase = true)
    }

    override fun hashCode(): Int {
        return name.hashCode()
    }

    override fun toString(): String {
        return name
    }

    fun isSensitive(): Boolean {
        return isSensitive
    }

    fun isSensitive(registry: Registry, testKey: Key<Boolean>): Boolean {
        return isSensitive() && !registry.getOrDefault(testKey)
    }

    companion object {
        fun <T> builder(type: TypeToken<T>): Builder<T> {
            return KeyBuilderImpl(type)
        }

        @OptIn(ExperimentalTypeInference::class)
        inline fun <reified T> build(@BuilderInference block: Builder<T>.() -> Unit): Key<T> {
            return builder(object : TypeToken<T>() {}).apply(block).build()
        }
    }
}
