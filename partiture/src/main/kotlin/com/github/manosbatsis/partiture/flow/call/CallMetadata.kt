/*
 *     Partiture: a compact component framework for your Corda apps
 *     Copyright (C) 2018 Manos Batsis
 *
 *     This library is free software; you can redistribute it and/or
 *     modify it under the terms of the GNU Lesser General Public
 *     License as published by the Free Software Foundation; either
 *     version 3 of the License, or (at your option) any later version.
 *
 *     This library is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *     Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */
package com.github.manosbatsis.partiture.flow.call

/** Abstract metadata implementation. */
abstract class CallMetadata {
    /**  Additional (top-level) metadata */
    protected abstract var meta: MutableMap<String, Any>?

    /**
     * Add a custom metadatum
     * @return the previous value associated with the key, or `null` if the key was not present in the map.
     */
    fun addMeta(key: String, value: Any): Any? {
        return if (this.meta != null) this.meta!!.put(key, value)
        else {
            this.meta = mutableMapOf(key to value)
            null
        }
    }

    /**
     * Get the custom metadatum value matching the given key
     * @return the value associated with the key if one exists, `null` otherwise
     */
    fun getMeta(key: String): Any? {
        return this.meta?.get(key)
    }
}