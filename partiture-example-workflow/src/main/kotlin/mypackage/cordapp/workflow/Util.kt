package mypackage.cordapp.workflow

import com.github.manosbatsis.vaultaire.annotation.VaultaireDtoStrategyKeys
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateDtoForDependency
import com.github.manosbatsis.vaultaire.annotation.VaultaireGenerateForDependency
import mypackage.cordapp.contract.AccountYoContract.AccountYoState
import mypackage.cordapp.contract.AccountYoContract.AccountYoState.AccountYoSchemaV1.PersistentAccountYoState

@VaultaireGenerateForDependency(name = "fungibleTokenConditions",
        persistentStateType = PersistentAccountYoState::class,
        contractStateType = AccountYoState::class)
@VaultaireGenerateDtoForDependency(
        persistentStateType = PersistentAccountYoState::class,
        contractStateType = AccountYoState::class,
        strategies = [VaultaireDtoStrategyKeys.DEFAULT, VaultaireDtoStrategyKeys.LITE])
class AccountYoMixin
