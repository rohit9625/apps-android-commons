package fr.free.nrw.commons.wikidata.model

import android.util.SparseArray

class EnumCodeMap<T>(
    enumeration: Class<T>,
) where T : Enum<T>, T : EnumCode {
    private val map: SparseArray<T>

    init {
        map = codeToEnumMap(enumeration)
    }

    operator fun get(code: Int): T = map.get(code) ?: throw IllegalArgumentException("code=$code")

    private fun codeToEnumMap(enumeration: Class<T>): SparseArray<T> {
        val ret = SparseArray<T>()
        for (value in enumeration.enumConstants) {
            ret.put(value.code(), value)
        }
        return ret
    }

    fun size(): Int = map.size()
}
