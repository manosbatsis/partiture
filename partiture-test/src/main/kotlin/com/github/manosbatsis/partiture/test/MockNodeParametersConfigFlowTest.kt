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
package com.github.manosbatsis.partiture.test

import net.corda.testing.core.ALICE_NAME
import net.corda.testing.core.BOB_NAME
import net.corda.testing.core.CHARLIE_NAME
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import org.slf4j.LoggerFactory

/** Configuration for [MockNodeParametersConfigFlowTest] tests */
interface MockNodeParametersConfig {

    /** Provide cordapp package names */
    fun getCordappPackages(): List<String>

    /**
     * Get hints for the [StartedMockNode]s to create. The default implementation  returns hints using
     * [ALICE_NAME], [BOB_NAME] and [CHARLIE_NAME].
     */
    fun getNodeParameters(): List<MockNodeParameters>
}


open class SimpleMockNodeParametersConfig: MockNodeParametersConfig {

    override fun getCordappPackages(): List<String>  = emptyList()

    override fun getNodeParameters(): List<MockNodeParameters> =
            listOf(ALICE_NAME, BOB_NAME, CHARLIE_NAME).map {
                MockNodeParameters(legalName = it)
            }
}

/**
 * Automatically intitializes a [MockNetwork] with [StartedMockNode]s based on
 * the given [MockNodeParametersConfig]
 */
open class MockNodeParametersConfigFlowTest(val config: MockNodeParametersConfig): MockNetworkFlowTest(config.getNodeParameters()) {

    companion object {
        private val logger = LoggerFactory.getLogger(MockNodeParametersConfigFlowTest::class.java)
    }

    /** Delegate to config for providing cordapp package names */
    override fun getCordappPackages(): List<String> = config.getCordappPackages()
}

