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
package mypackage.cordapp.workflow

import com.github.manosbatsis.vaultaire.annotation.VaultaireDtoStrategyKeys
import com.github.manosbatsis.vaultaire.annotation.VaultaireStateDtoMixin
import com.github.manosbatsis.vaultaire.annotation.VaultaireStateUtilsMixin
import mypackage.cordapp.contract.AccountYoContract.AccountYoState
import mypackage.cordapp.contract.AccountYoContract.AccountYoState.AccountYoSchemaV1.PersistentAccountYoState

@VaultaireStateUtilsMixin(name = "fungibleTokenConditions",
        persistentStateType = PersistentAccountYoState::class,
        contractStateType = AccountYoState::class)
@VaultaireStateDtoMixin(
        persistentStateType = PersistentAccountYoState::class,
        contractStateType = AccountYoState::class,
        strategies = [VaultaireDtoStrategyKeys.CORDAPP_CLIENT_DTO, VaultaireDtoStrategyKeys.CORDAPP_LOCAL_DTO])
class AccountYoMixin
