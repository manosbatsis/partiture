/*
 *     Cordapi: Common components for Cordapps
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
package com.github.manosbatsis.cordapi.commons.flow

import com.github.manosbatsis.cordapi.commons.flow.tx.TxContext
import com.github.manosbatsis.cordapi.commons.flow.call.CallDelegate
import com.github.manosbatsis.cordapi.commons.flow.input.FlowInput
import com.github.manosbatsis.cordapi.commons.flow.input.FlowInputConverter
import net.corda.core.flows.FlowLogic

/**
 * Base [FlowLogic] implementation utilizing [TxContext]
 */
abstract class CordapiInputFlow<IN : FlowInput, out OUT>(
        strategy: CallDelegate,
        val input: IN,
        val inputConverter: FlowInputConverter<IN>
) : CordapiFlow<OUT>(strategy) {

    override fun createTxContext(): TxContext {
        return inputConverter.convert(input)
    }
}