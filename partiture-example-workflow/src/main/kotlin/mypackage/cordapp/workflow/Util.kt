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
