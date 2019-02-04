---
title: Output Converters
---


As with input processing, Partiture attempts to decouple output and TX 
processing in flows. For that purpose, `PartitureFlow.processOutput()` 
attempts to use the `outputConverter` passed via it's constructor to 
create an instance of `OUT` (per `PartitureFlow`'s generic argument) 
as the flow's (`call`) output. If `outputConverter` is `null`, 
an error will be thrown.

> Overriding `processOutput()` is strongly discouraged, as output converters 
are typically more widely applicable than input converters. 

## Build-in Output Converters

Build-in converters reside in the `com.github.manosbatsis.partiture.flow.io.output` package.

| Class			                   	   	| OUT Type, Description                                                                                                                              	|
|-------------------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------	|
| `FinalizedTxOutputConverter` 	    	| `List<SignedTransaction>`. Converts to the finalized transactions of (each entry in) the given `CallContext`. Throws an error if none is available for any an entry 	|
| `SingleFinalizedTxOutputConverter`	| `SignedTransaction`. Converts to the finalized transaction of the __single__ `CallContext` entry if available, throws an error otherwise                      	|


## Custom Output Converters
 
To create a custom converter, begin by matching the `PartitureFlow`'s `OUT` 
generic argument of your flow subclass. For example, to return a list of `FooState`:

```kotlin
/** Get all FooState instances from the CallContext */
class FooStateOutputConverter : PartitureFlowDelegateBase(), OutputConverter<List<FooState>> {
		
	/** Get those foo states! */
    override fun convert(input: CallContext): List<FooState> {
        return input.entries.flatMap {
        	// get the entry's FooState(s)
        }
    }
}
```

