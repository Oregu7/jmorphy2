# Data Flow and Processing Patterns

<cite>
**Referenced Files in This Document**
- [MorphAnalyzer.java](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java)
- [ParsedWord.java](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/ParsedWord.java)
- [Tag.java](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/Tag.java)
- [Grammeme.java](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/Grammeme.java)
- [ProbabilityEstimator.java](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/ProbabilityEstimator.java)
- [AnalyzerUnit.java](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/AnalyzerUnit.java)
- [DictionaryUnit.java](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/DictionaryUnit.java)
- [UnknownUnit.java](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/UnknownUnit.java)
- [NumberUnit.java](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/NumberUnit.java)
- [LatinUnit.java](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/LatinUnit.java)
- [RegexUnit.java](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/RegexUnit.java)
- [PrefixedUnit.java](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/PrefixedUnit.java)
- [KnownPrefixUnit.java](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/KnownPrefixUnit.java)
- [UnknownPrefixUnit.java](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/UnknownPrefixUnit.java)
- [PunctuationUnit.java](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/PunctuationUnit.java)
</cite>

## Table of Contents
1. [Introduction](#introduction)
2. [Project Structure](#project-structure)
3. [Core Components](#core-components)
4. [Architecture Overview](#architecture-overview)
5. [Detailed Component Analysis](#detailed-component-analysis)
6. [Dependency Analysis](#dependency-analysis)
7. [Performance Considerations](#performance-considerations)
8. [Troubleshooting Guide](#troubleshooting-guide)
9. [Conclusion](#conclusion)

## Introduction
This document explains the data flow and processing pipeline of the morphological analyzer. It traces how raw input words are transformed through a staged pipeline of AnalyzerUnits to produce ranked ParsedWord results enriched with Tag and Grammeme metadata. The pipeline includes input preprocessing, unit pipeline execution, duplicate filtering, optional probability estimation, and final ranking. The data models involved are String inputs, intermediate AnalyzerUnit-produced ParsedWord instances, Tag objects representing grammatical categories, and Grammeme objects encoding linguistic features.

## Project Structure
The morphological analysis system is centered around a core analyzer and a set of pluggable AnalyzerUnits. The analyzer composes a chain of units, each responsible for recognizing specific word types or patterns. Supporting classes model grammatical tags and grammemes, and optional probability estimation refines scores.

```mermaid
graph TB
subgraph "Core"
MA["MorphAnalyzer"]
PU["ParsedWord"]
TG["Tag"]
GM["Grammeme"]
PE["ProbabilityEstimator"]
end
subgraph "Units"
AU["AnalyzerUnit"]
DU["DictionaryUnit"]
NU["NumberUnit"]
LU["LatinUnit"]
KPU["KnownPrefixUnit"]
UPU["UnknownPrefixUnit"]
RU["RegexUnit"]
PUZ["PrefixedUnit"]
UNK["UnknownUnit"]
PUNC["PunctuationUnit"]
end
MA --> AU
AU --> DU
AU --> NU
AU --> LU
AU --> KPU
AU --> UPU
AU --> PUNC
AU --> UNK
DU --> PU
NU --> PU
LU --> PU
KPU --> PUZ
UPU --> PUZ
PUZ --> PU
PUNC --> PU
UNK --> PU
MA --> TG
TG --> GM
MA --> PE
```

**Diagram sources**
- [MorphAnalyzer.java:15-247](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L15-L247)
- [AnalyzerUnit.java:11-72](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/AnalyzerUnit.java#L11-L72)
- [DictionaryUnit.java:14-104](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/DictionaryUnit.java#L14-L104)
- [NumberUnit.java:10-54](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/NumberUnit.java#L10-L54)
- [LatinUnit.java:8-28](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/LatinUnit.java#L8-L28)
- [KnownPrefixUnit.java:12-75](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/KnownPrefixUnit.java#L12-L75)
- [UnknownPrefixUnit.java:11-75](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/UnknownPrefixUnit.java#L11-L75)
- [RegexUnit.java:11-31](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/RegexUnit.java#L11-L31)
- [PrefixedUnit.java:10-55](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/PrefixedUnit.java#L10-L55)
- [UnknownUnit.java:9-34](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/UnknownUnit.java#L9-L34)
- [PunctuationUnit.java:8-28](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/PunctuationUnit.java#L8-L28)
- [ParsedWord.java:9-89](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/ParsedWord.java#L9-L89)
- [Tag.java:7-230](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/Tag.java#L7-L230)
- [Grammeme.java:7-86](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/Grammeme.java#L7-L86)
- [ProbabilityEstimator.java:9-27](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/ProbabilityEstimator.java#L9-L27)

**Section sources**
- [MorphAnalyzer.java:15-247](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L15-L247)
- [AnalyzerUnit.java:11-72](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/AnalyzerUnit.java#L11-L72)

## Core Components
- MorphAnalyzer orchestrates parsing, applies duplicate filtering, optional probability estimation, and sorts results by score.
- AnalyzerUnit defines the contract for parsing a word into zero or more ParsedWord candidates, with termination semantics.
- ParsedWord carries the analyzed form, Tag, normal form, the matched dictionary/regex word, and a score.
- Tag encapsulates grammatical categories and provides containment checks; it aggregates Grammeme instances.
- Grammeme represents a single linguistic feature with hierarchical parent/root relationships.
- ProbabilityEstimator optionally reweights candidate scores using precomputed word-tag probabilities.

**Section sources**
- [MorphAnalyzer.java:178-247](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L178-L247)
- [AnalyzerUnit.java:46-72](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/AnalyzerUnit.java#L46-L72)
- [ParsedWord.java:9-89](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/ParsedWord.java#L9-L89)
- [Tag.java:27-159](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/Tag.java#L27-L159)
- [Grammeme.java:7-86](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/Grammeme.java#L7-L86)
- [ProbabilityEstimator.java:9-27](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/ProbabilityEstimator.java#L9-L27)

## Architecture Overview
The analyzer builds a pipeline of AnalyzerUnits configured for the detected language. Each unit contributes candidate analyses. The pipeline short-circuits when a unit declares itself “terminated” and has produced results. After collecting candidates, duplicates are removed, optional probability estimation is applied, and results are sorted descending by score.

```mermaid
sequenceDiagram
participant Client as "Caller"
participant MA as "MorphAnalyzer"
participant U1 as "AnalyzerUnit #1"
participant U2 as "AnalyzerUnit #2"
participant UN as "AnalyzerUnit #n"
Client->>MA : "parse(word)"
MA->>MA : "lowercase + special-case normalization"
loop "for each unit"
MA->>U1 : "parse(word, wordLower)"
alt "unit terminated and has results"
U1-->>MA : "results"
MA->>MA : "break"
else "continue pipeline"
U1-->>MA : "results or null"
MA->>U2 : "parse(...)"
end
end
MA->>MA : "filterDups()"
alt "probability estimation enabled"
MA->>MA : "estimate()"
end
MA->>MA : "sort(reverse by score)"
MA-->>Client : "List<ParsedWord>"
```

**Diagram sources**
- [MorphAnalyzer.java:178-200](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L178-L200)
- [MorphAnalyzer.java:234-245](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L234-L245)
- [MorphAnalyzer.java:202-232](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L202-L232)
- [AnalyzerUnit.java:46](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/AnalyzerUnit.java#L46)

## Detailed Component Analysis

### Data Models and Relationships
The core data model hierarchy connects String inputs to grammatical analysis results.

```mermaid
classDiagram
class ParsedWord {
+String word
+Tag tag
+String normalForm
+String foundWord
+float score
+rescore(newScore) ParsedWord
+getLexeme() ParsedWord[]
+compareTo(other) int
}
class Tag {
+Set~Grammeme~ grammemes
+contains(grammeme) bool
+containsAll(c) bool
+isProductive() bool
}
class Grammeme {
+String key
+String value
+getParent() Grammeme
+getRoot() Grammeme
}
class MorphAnalyzer {
+parse(word) ParsedWord[]
+normalForms(word) String[]
+tag(word) Tag[]
}
ParsedWord --> Tag : "has"
Tag --> Grammeme : "aggregates"
MorphAnalyzer --> ParsedWord : "produces"
MorphAnalyzer --> Tag : "queries"
```

**Diagram sources**
- [ParsedWord.java:9-89](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/ParsedWord.java#L9-L89)
- [Tag.java:27-159](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/Tag.java#L27-L159)
- [Grammeme.java:7-86](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/Grammeme.java#L7-L86)
- [MorphAnalyzer.java:178-200](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L178-L200)

**Section sources**
- [ParsedWord.java:9-89](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/ParsedWord.java#L9-L89)
- [Tag.java:27-159](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/Tag.java#L27-L159)
- [Grammeme.java:7-86](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/Grammeme.java#L7-L86)

### Processing Pipeline Stages
- Input preprocessing: Lowercasing and special-case normalization are applied before pipeline execution.
- Unit pipeline execution: Units are invoked in order; results are concatenated. If a unit is marked terminated and has results, the pipeline stops early.
- Duplicate filtering: Removes equivalent analyses by Tag and normalForm.
- Probability estimation: Optionally reweights scores using word-tag probabilities; otherwise preserves original scores.
- Ranking: Results are sorted in descending order by score.

```mermaid
flowchart TD
Start(["parse(word)"]) --> Pre["Lowercase + normalize"]
Pre --> LoopUnits{"Iterate units"}
LoopUnits --> ExecUnit["unit.parse(word, wordLower)"]
ExecUnit --> HasRes{"Has results?"}
HasRes --> |Yes & terminated| StopEarly["Stop pipeline"]
HasRes --> |No| NextUnit["Next unit"]
HasRes --> |Yes & not terminated| Collect["Collect results"] --> NextUnit
NextUnit --> LoopUnits
StopEarly --> Filter["filterDups()"]
Collect --> Filter
Filter --> Prob{"Probability estimator present?"}
Prob --> |Yes| Estimate["estimate()"]
Prob --> |No| Sort
Estimate --> Sort["Sort desc by score"]
Sort --> End(["Return List<ParsedWord>"])
```

**Diagram sources**
- [MorphAnalyzer.java:178-200](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L178-L200)
- [MorphAnalyzer.java:234-245](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L234-L245)
- [MorphAnalyzer.java:202-232](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L202-L232)

**Section sources**
- [MorphAnalyzer.java:178-247](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L178-L247)

### AnalyzerUnit Types and Their Roles
- DictionaryUnit: Looks up similar words in the dictionary, constructs normal forms and Tags, and produces candidates with initial scores.
- NumberUnit: Recognizes numeric tokens and assigns NUMERIC tags.
- LatinUnit: Recognizes Latin-character sequences via RegexUnit and tags them accordingly.
- PunctuationUnit: Recognizes punctuation sequences via RegexUnit.
- KnownPrefixUnit and UnknownPrefixUnit: Apply known or trial prefixes to base analyses produced by another unit; filter out non-productive tags.
- UnknownUnit: Assigns an UNKNOWN tag to unanalyzable tokens.

```mermaid
classDiagram
class AnalyzerUnit {
+parse(word, wordLower) ParsedWord[]
+isTerminated() bool
}
class DictionaryUnit
class NumberUnit
class LatinUnit
class PunctuationUnit
class RegexUnit
class PrefixedUnit
class KnownPrefixUnit
class UnknownPrefixUnit
class UnknownUnit
AnalyzerUnit <|-- DictionaryUnit
AnalyzerUnit <|-- NumberUnit
AnalyzerUnit <|-- LatinUnit
AnalyzerUnit <|-- PunctuationUnit
RegexUnit <|-- LatinUnit
RegexUnit <|-- PunctuationUnit
AnalyzerUnit <|-- PrefixedUnit
PrefixedUnit <|-- KnownPrefixUnit
PrefixedUnit <|-- UnknownPrefixUnit
AnalyzerUnit <|-- UnknownUnit
```

**Diagram sources**
- [AnalyzerUnit.java:11-72](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/AnalyzerUnit.java#L11-L72)
- [DictionaryUnit.java:14-104](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/DictionaryUnit.java#L14-L104)
- [NumberUnit.java:10-54](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/NumberUnit.java#L10-L54)
- [LatinUnit.java:8-28](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/LatinUnit.java#L8-L28)
- [PunctuationUnit.java:8-28](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/PunctuationUnit.java#L8-L28)
- [RegexUnit.java:11-31](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/RegexUnit.java#L11-L31)
- [PrefixedUnit.java:10-55](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/PrefixedUnit.java#L10-L55)
- [KnownPrefixUnit.java:12-75](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/KnownPrefixUnit.java#L12-L75)
- [UnknownPrefixUnit.java:11-75](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/UnknownPrefixUnit.java#L11-L75)
- [UnknownUnit.java:9-34](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/UnknownUnit.java#L9-L34)

**Section sources**
- [DictionaryUnit.java:56-104](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/DictionaryUnit.java#L56-L104)
- [NumberUnit.java:31-54](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/NumberUnit.java#L31-L54)
- [LatinUnit.java:8-28](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/LatinUnit.java#L8-L28)
- [PunctuationUnit.java:8-28](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/PunctuationUnit.java#L8-L28)
- [RegexUnit.java:21-31](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/RegexUnit.java#L21-L31)
- [PrefixedUnit.java:18-55](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/PrefixedUnit.java#L18-L55)
- [KnownPrefixUnit.java:62-75](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/KnownPrefixUnit.java#L62-L75)
- [UnknownPrefixUnit.java:64-75](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/UnknownPrefixUnit.java#L64-L75)
- [UnknownUnit.java:27-34](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/UnknownUnit.java#L27-L34)

### Example Processing Flows

#### Numeric Token
```mermaid
sequenceDiagram
participant C as "Client"
participant MA as "MorphAnalyzer"
participant NU as "NumberUnit"
participant AU as "AnalyzerUnit"
participant PW as "ParsedWord"
C->>MA : "parse(\"123\")"
MA->>NU : "parse(word, wordLower)"
NU->>AU : "new AnalyzerParsedWord(..., score)"
AU-->>NU : "List<ParsedWord>"
NU-->>MA : "List<ParsedWord>"
MA->>MA : "filterDups(), sort"
MA-->>C : "List<ParsedWord> with NUMERIC tag"
```

**Diagram sources**
- [NumberUnit.java:31-54](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/NumberUnit.java#L31-L54)
- [AnalyzerUnit.java:48-71](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/AnalyzerUnit.java#L48-L71)
- [MorphAnalyzer.java:178-200](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L178-L200)

#### Latin Word
```mermaid
sequenceDiagram
participant C as "Client"
participant MA as "MorphAnalyzer"
participant LU as "LatinUnit"
participant RU as "RegexUnit"
participant AU as "AnalyzerUnit"
participant PW as "ParsedWord"
C->>MA : "parse(\"alpha\")"
MA->>LU : "parse(word, wordLower)"
LU->>RU : "match against Latin regex"
RU->>AU : "new AnalyzerParsedWord(..., LATN tag)"
AU-->>RU : "List<ParsedWord>"
RU-->>LU : "List<ParsedWord>"
LU-->>MA : "List<ParsedWord>"
MA->>MA : "filterDups(), sort"
MA-->>C : "List<ParsedWord> with LATN tag"
```

**Diagram sources**
- [LatinUnit.java:8-28](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/LatinUnit.java#L8-L28)
- [RegexUnit.java:21-31](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/RegexUnit.java#L21-L31)
- [AnalyzerUnit.java:48-71](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/AnalyzerUnit.java#L48-L71)
- [MorphAnalyzer.java:178-200](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L178-L200)

#### Unknown Word
```mermaid
sequenceDiagram
participant C as "Client"
participant MA as "MorphAnalyzer"
participant UNK as "UnknownUnit"
participant AU as "AnalyzerUnit"
participant PW as "ParsedWord"
C->>MA : "parse(\"xyz123\")"
MA->>UNK : "parse(word, wordLower)"
UNK->>AU : "new AnalyzerParsedWord(..., UNKN tag)"
AU-->>UNK : "List<ParsedWord>"
UNK-->>MA : "List<ParsedWord>"
MA->>MA : "filterDups(), sort"
MA-->>C : "List<ParsedWord> with UNKN tag"
```

**Diagram sources**
- [UnknownUnit.java:27-34](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/UnknownUnit.java#L27-L34)
- [AnalyzerUnit.java:48-71](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/AnalyzerUnit.java#L48-L71)
- [MorphAnalyzer.java:178-200](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L178-L200)

#### Dictionary Lookup with Prefixes
```mermaid
sequenceDiagram
participant C as "Client"
participant MA as "MorphAnalyzer"
participant KPU as "KnownPrefixUnit"
participant DU as "DictionaryUnit"
participant PUZ as "PrefixedUnit"
participant AU as "AnalyzerUnit"
participant PW as "ParsedWord"
C->>MA : "parse(\"unhappiness\")"
MA->>KPU : "parse(word, wordLower)"
KPU->>DU : "parse(remainder)"
DU->>AU : "DictionaryParsedWord(...)"
AU-->>DU : "List<ParsedWord>"
DU-->>KPU : "List<ParsedWord>"
KPU->>PUZ : "wrap with prefix"
PUZ-->>MA : "List<ParsedWord>"
MA->>MA : "filterDups(), estimate(), sort"
MA-->>C : "Ranked ParsedWord list"
```

**Diagram sources**
- [KnownPrefixUnit.java:62-75](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/KnownPrefixUnit.java#L62-L75)
- [DictionaryUnit.java:56-104](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/DictionaryUnit.java#L56-L104)
- [PrefixedUnit.java:18-55](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/PrefixedUnit.java#L18-L55)
- [AnalyzerUnit.java:48-71](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/AnalyzerUnit.java#L48-L71)
- [MorphAnalyzer.java:178-200](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L178-L200)

## Dependency Analysis
- MorphAnalyzer depends on Tag.Storage for grammatical metadata and ProbabilityEstimator when enabled.
- AnalyzerUnit implementations depend on Tag.Storage to construct Tag and Grammeme instances.
- DictionaryUnit depends on Dictionary and WordsDAWG to retrieve paradigm and wordform data.
- PrefixedUnit composes another AnalyzerUnit and augments its results with prefix information.
- ProbabilityEstimator depends on a DAWG-backed integer dictionary for probability lookups.

```mermaid
graph TB
MA["MorphAnalyzer"] --> TS["Tag.Storage"]
MA --> PE["ProbabilityEstimator"]
AU["AnalyzerUnit"] --> TS
DU["DictionaryUnit"] --> DU2["Dictionary"]
DU --> WDWG["WordsDAWG"]
KPU["KnownPrefixUnit"] --> PUZ["PrefixedUnit"]
UPU["UnknownPrefixUnit"] --> PUZ
PUZ --> AU
PE --> DAWG["IntegerDAWG"]
```

**Diagram sources**
- [MorphAnalyzer.java:16-247](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L16-L247)
- [DictionaryUnit.java:14-104](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/DictionaryUnit.java#L14-L104)
- [PrefixedUnit.java:10-55](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/PrefixedUnit.java#L10-L55)
- [ProbabilityEstimator.java:9-27](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/ProbabilityEstimator.java#L9-L27)

**Section sources**
- [MorphAnalyzer.java:16-247](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L16-L247)
- [DictionaryUnit.java:14-104](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/DictionaryUnit.java#L14-L104)
- [PrefixedUnit.java:10-55](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/PrefixedUnit.java#L10-L55)
- [ProbabilityEstimator.java:9-27](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/ProbabilityEstimator.java#L9-L27)

## Performance Considerations
- Pipeline short-circuiting: Units marked terminated reduce total work when applicable.
- Duplicate filtering: Prevents redundant analyses and reduces downstream processing.
- Score normalization: When probability estimation is disabled, scores remain unchanged; when enabled, normalization ensures consistent weighting across diverse units.
- Regex-based units avoid heavy computation for simple pattern matching.
- Prefix scanning bounds: KnownPrefixUnit and UnknownPrefixUnit limit prefix length and minimum remainder to control combinatorial growth.

[No sources needed since this section provides general guidance]

## Troubleshooting Guide
- Unexpected empty results: Verify that the dictionary is loaded and language-specific known prefixes are configured. Confirm that at least one unit can match the input.
- Incorrect tags or missing grammemes: Inspect Tag.Storage initialization and ensure resource loading succeeded.
- Poor ranking for unknown words: UnknownUnit assigns a generic tag; consider enabling probability estimation if available.
- Non-productive tags filtered out: PrefixedUnit excludes non-productive analyses; adjust prefix scanning parameters if legitimate forms are being dropped.
- Performance regressions: Review unit ordering and termination flags; ensure probability estimation is only enabled when needed.

**Section sources**
- [MorphAnalyzer.java:178-247](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/MorphAnalyzer.java#L178-L247)
- [PrefixedUnit.java:22-28](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/units/PrefixedUnit.java#L22-L28)
- [ProbabilityEstimator.java:9-27](file://jmorphy2-core/src/main/java/company/evo/jmorphy2/ProbabilityEstimator.java#L9-L27)

## Conclusion
The morphological analyzer implements a modular, extensible pipeline that transforms raw text into rich morphological analyses. By composing AnalyzerUnits, applying deduplication and optional probability estimation, and ranking results, it delivers accurate and efficient analysis outcomes. The Tag and Grammeme models provide a structured representation of grammatical information, while ParsedWord consolidates all analysis artifacts for downstream consumers.