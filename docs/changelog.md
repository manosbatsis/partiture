---
id: changelog
title: Changelog
---

The following sections describe major changes per version and can be helpful with version upgrades.

## 0.6-SNAPSHOT

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

