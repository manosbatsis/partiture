---
id: changelog
title: Changelog
---

The following sections describe major changes per version and 
can be helpful with version upgrades.

## 0.16

- Improve error handling in TX strategies

## 0.15

- Upgraded Corda dependencies to 4.6

## 0.14

- Added tests for Corda Accounts.
- Fixed flow sessions issue; there were duplicates in some cases.

## 0.12-13

`PartitureAccountsAwareFlow` will now use `ShareStateAndSyncAccounts` 
to sync generated account keys for states of a transaction after it 
has been finalized.

## 0.11

- Upgraded Corda dependencies to 4.5
- Added support for Corda Accounts with `PartitureAccountsAwareFlow` 

## 0.10

- Upgraded Corda dependencies to 4.4
- Added  `open fun MockNetworkFlowTest.stopNodes()` 

## 0.9

- Fixed `SimpleTxStrategy` error in case of no counterparties

## 0.8

- Upgraded Corda dependencies to 4.3

## 0.7

- Deprecated `LinearStateVaultHelper` in favour of [Vaultaire](https://manosbatsis.github.io/vaultaire/)'s state services.


## 0.6

- Added `PartitureResponderFlow.preSignTransaction` method to allow custom logic before 
calling a `SignTransactionFlow`
- Added `TxStrategyExecutionException` and `PartitureFlow.handleFailedTxStrategy`

## 0.5

- Exposed counter-party sessions via `CallContext` 
- Added String to Party helpers
- Added the project logo, kindly contributed by [Mirza Zulfan](https://github.com/mirzazulfan)

## 0.3 - 0.4 

- Minor updates, cleanup

## 0.2

- `ParticipantsAwareTransactionBuilder` is now deprecated in favor of `TxContet` and will be removed in upcoming versions 
- Added `OutputStatesConverter` and `TypedOutputStatesConverter`
- Changed `CallContextEntry.participants` from `List<AbstractParty>` to `Set<AbstractParty>`

## 0.1

- Initial release

