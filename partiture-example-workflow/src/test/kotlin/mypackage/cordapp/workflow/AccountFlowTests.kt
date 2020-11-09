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
 *     AccountYou should have received a copy of the GNU Lesser General Public
 *     License along with this library; if not, write to the Free Software
 *     Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301
 *     USA
 */
package mypackage.cordapp.workflow

import com.github.manosbatsis.partiture.flow.PartitureFlow
import com.github.manosbatsis.vaultaire.plugin.accounts.dto.AccountInfoLiteDto
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.flows.CreateAccount
import com.r3.corda.lib.accounts.workflows.services.AccountService
import mypackage.cordapp.contract.AccountYoContract.AccountYoState
import mypackage.cordapp.contract.YO_CONTRACT_PACKAGE
import net.corda.core.flows.FlowLogic
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.DUMMY_NOTARY_NAME
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkNotarySpec
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp.Companion.findCordapp
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.assertEquals


@Suppress(names = ["DEPRECATION"])
@TestInstance(TestInstance.Lifecycle.PER_CLASS) // allow non-static @BeforeAll etc.
class AccountYoFlowTests {

    // Works as long as the main and test package names are  in sync
    val cordappPackages = listOf(
            AccountInfo::class.java.`package`.name,
            AccountService::class.java.`package`.name,
            YO_CONTRACT_PACKAGE,
            this.javaClass.`package`.name,
            PartitureFlow::class.java.`package`.name)
    lateinit var network: MockNetwork
    lateinit var a: StartedMockNode
    lateinit var b: StartedMockNode
    lateinit var aAccount: AccountInfoLiteDto
    lateinit var bAccount: AccountInfoLiteDto

    @BeforeAll
    fun setup() {

        network = MockNetwork(MockNetworkParameters(
                notarySpecs = listOf(MockNetworkNotarySpec(DUMMY_NOTARY_NAME, true)),
                cordappsForAllNodes = cordappPackages.map {
                    findCordapp(it)
                }))

        a = network.createPartyNode()
        b = network.createPartyNode()
        network.runNetwork()

        aAccount = AccountInfoLiteDto.mapToDto(flowWorksCorrectly(a, CreateAccount("a")).state.data)
        bAccount = AccountInfoLiteDto.mapToDto(flowWorksCorrectly(b, CreateAccount("b")).state.data)
    }

    @AfterAll
    fun tearDown() {
        network.stopNodes()
    }


    @Test
    fun `Test flow with SingleFinalizedTxOutputConverter`() {
        val msg = "AccountYo1"
        val stx = flowWorksCorrectly(
                AccountYoFlow1(AccountYoStateLiteDto(sender = aAccount, recepient = bAccount, yo = msg)))
        // Check yo transaction is stored in the storage service.
        val bTx = b.services.validatedTransactions.getTransaction(stx.id)
        assertEquals(bTx, stx)
    }

    @Test
    fun `Test flow with testTypedOutputStatesConverter`() {
        val msg = "AccountYo2"
        val result = flowWorksCorrectly(
                AccountYoFlow1(AccountYoStateLiteDto(
                        sender = aAccount,
                        recepient = bAccount, yo = msg)))

        assertEquals(msg, result.tx.outputsOfType<AccountYoState>().single().yo)
    }

    @Test
    fun `Test flow with multiple participant instances of the same Corda Account`() {
        val msg = "AccountYo3"
        val yos = listOf(
                AccountYoStateLiteDto(aAccount, bAccount,"${msg}a"),
                AccountYoStateLiteDto(aAccount, bAccount,"${msg}b"),
                AccountYoStateLiteDto(bAccount, aAccount,"${msg}c"),
                AccountYoStateLiteDto(bAccount, aAccount,"${msg}d")
        )

        val result = flowWorksCorrectly(AccountYoFlow3(yos))

        assertEquals(4, result.filter{it.yo.startsWith(msg)}.size)
    }

    inline fun <reified OUT> flowWorksCorrectly(flow: PartitureFlow<*, OUT>): OUT {
        val future = a.startFlow(flow)
        // Ask nodes to process any queued up inbound messages
        network.runNetwork()
        return  future.getOrThrow()

    }

    inline fun <reified OUT> flowWorksCorrectly(node: StartedMockNode, flow: FlowLogic<OUT>): OUT {
        val result = node.startFlow(flow).getOrThrow()
        // Ask nodes to process any queued up inbound messages
        network.waitQuiescent()
        return result
    }
}
