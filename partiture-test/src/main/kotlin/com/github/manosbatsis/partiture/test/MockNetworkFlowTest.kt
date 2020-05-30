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

import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.testing.common.internal.testNetworkParameters
import net.corda.testing.internal.chooseIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.MockNodeParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

/**
 * Automatically initializes a [MockNetwork] with [StartedMockNode]s based on constructor parameters
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class MockNetworkFlowTest(val input: List<MockNodeParameters>) {

    constructor(vararg names: CordaX500Name) : this(names.map {
        MockNodeParameters(legalName = it)
    })

    constructor(vararg names: String) : this(
            *names.map {
                try {CordaX500Name.parse(it) }
                catch (e: Throwable){CordaX500Name(it, "London", "GB")}
            }.toTypedArray()
    )

    constructor(numberOfNodes: Int) : this(*(1..numberOfNodes).map { "Party${it.toChar() + 64}" }.toTypedArray())


    protected lateinit var network: MockNetwork

    /**
     * Maps nodes by hint identity ([Party]), identity name ([CordaX500Name] or string representation),
     * organization name and decapitalized organization name without spaces
     */
    protected lateinit var nodeMap: Map<Any, StartedMockNode>

    /** Setup a network based on [getNodeHints] */
    @BeforeAll
    fun beforeAll() {
        println("beforeAll, input: $input")
        network = buildMockNetwork()
        nodeMap = input
                .map {
                    val node = network.createNode(it)
                    val identity = node.info.chooseIdentity()
                    mapOf(
                            identity to node,
                            identity.name to node,
                            identity.name.toString() to node,
                            identity.name.organisation to node,
                            identity.name.organisation.replace(" ", "").decapitalize() to node
                    )
                }
                .flatMap { it.entries }
                .map { it.key to it.value }.toMap()
        network.startNodes()
        postSetup()
    }

    protected open fun buildMockNetwork(): MockNetwork =
            MockNetwork(
                    parameters = MockNetworkParameters(
                            cordappsForAllNodes = getCordappPackages()
                                    .map { it.trim() }
                                    .toSet()
                                    .filter { it.isNotBlank() }
                                    .map {TestCordapp.findCordapp(it)},
                            threadPerNode = true,
                            networkParameters = testNetworkParameters(minimumPlatformVersion = 4)
                    )
            )


    /** Provide cordapp package names */
    abstract fun getCordappPackages(): List<String>

    /**
     * Override to add opst-setup initialization logic,
     * e.g. assign [nodeMap] entry values to variables for ease of use.
     */
    protected open fun postSetup() {
        // NO-OP
    }

    /** Shut down the network */
    @AfterAll
    fun afterAll() = stopNodes()

    open fun stopNodes(){
        network.stopNodes()
    }
}

