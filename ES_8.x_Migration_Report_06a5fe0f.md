
# Migration Report: jmorphy2 Elasticsearch Plugin 7.x -> 8.x

## 1. Feasibility Analysis

**Migration is FEASIBLE** but requires significant changes across multiple modules. The core morphological analysis engine (jmorphy2-core, dawg) is unaffected since it doesn't depend on ES/Lucene APIs. The main impact is on:

- **jmorphy2-elasticsearch** (major rewrite required)
- **jmorphy2-lucene** (Lucene 8 -> 9 migration)
- Build system and CI/CD

No architectural blockers exist; all used APIs have direct replacements in ES 8.x.

---

## 2. Key Breaking Changes & Required Code Modifications

### 2.1 Java Version: 11 -> 17

**Why:** ES 8.x requires Java 17+ to run. The bundled JDK is Java 17.

**Files to change:**
- `buildSrc/src/main/kotlin/Versions.kt` — change `JavaVersion.VERSION_11` to `JavaVersion.VERSION_17`
- `.github/workflows/java.yaml` — change `java-version: 13` to `java-version: 17`

### 2.2 Plugin Constructor Signature Change

**Why:** In ES 8.x, `Plugin` no longer accepts `(Settings settings, Path configPath)` in its constructor. The framework now uses the default no-arg constructor and provides services via `createComponents(PluginServices services)`.

**Current code** in `AnalysisJmorphy2Plugin.java`:
```java
public AnalysisJmorphy2Plugin(Settings settings, Path configPath) {
    super();
    Environment env = new Environment(settings, configPath);
    jmorphy2Service = new Jmorphy2Service(settings, env);
}
```

**Must become:**
```java
public AnalysisJmorphy2Plugin() {
    // No-arg constructor required by ES 8.x
}

@Override
public Collection<?> createComponents(PluginServices services) {
    Environment env = services.environment();
    Settings settings = env.settings();
    this.jmorphy2Service = new Jmorphy2Service(settings, env);
    return Collections.singletonList(jmorphy2Service);
}
```

**Impact:** The `Jmorphy2Service` instance needs to be lazily initialized or initialized in `createComponents()`. The `getTokenFilters()` method is called after `createComponents()`, so this ordering works.

### 2.3 AbstractTokenFilterFactory Constructor Change

**Why:** In ES 8.x, `AbstractTokenFilterFactory` removed the `IndexSettings` parameter. Constructor changed from `(IndexSettings, String, Settings)` to `(String, Settings)`.

**Files to change:**
- `Jmorphy2StemTokenFilterFactory.java` — remove `IndexSettings` from `super()` call
- `Jmorphy2SubjectTokenFilterFactory.java` — remove `IndexSettings` from `super()` call

**Current code:**
```java
public Jmorphy2StemTokenFilterFactory(IndexSettings indexSettings, Environment environment, String name, Settings settings, Jmorphy2Service jmorphy2Service) {
    super(indexSettings, name, settings);
```

**Must become:**
```java
public Jmorphy2StemTokenFilterFactory(IndexSettings indexSettings, Environment environment, String name, Settings settings, Jmorphy2Service jmorphy2Service) {
    super(name, settings);
```

Note: The `IndexSettings` parameter is still passed by the `AnalysisProvider.get()` callback, but it's no longer forwarded to the parent class. The plugin may still use it internally if needed.

### 2.4 AbstractIndexAnalyzerProvider Constructor Change

**Why:** Same reason as 2.3 — `IndexSettings` removed from constructor. Changed from `(IndexSettings, String, Settings)` to `(String, Settings)`.

**File:** `Jmorphy2AnalyzerProvider.java`

**Current code:**
```java
super(indexSettings, name, settings);
```

**Must become:**
```java
super(name, settings);
```

### 2.5 Lucene 8 -> 9 Migration (jmorphy2-lucene module)

**Why:** ES 8.x bundles Lucene 9.x, which has breaking package and artifact renames.

#### 2.5.1 Maven Artifact Rename

| Old (Lucene 8) | New (Lucene 9) |
|---|---|
| `org.apache.lucene:lucene-analyzers-common` | `org.apache.lucene:lucene-analysis-common` |
| `org.apache.lucene:lucene-test-framework` | `org.apache.lucene:lucene-test-framework` (same name, version 9.x) |

**File:** `jmorphy2-lucene/build.gradle.kts` — update artifact names

#### 2.5.2 Package Renames for Analysis Factories

| Old Package (Lucene 8) | New Package (Lucene 9) |
|---|---|
| `org.apache.lucene.analysis.util.TokenFilterFactory` | `org.apache.lucene.analysis.TokenFilterFactory` |
| `org.apache.lucene.analysis.util.ResourceLoader` | `org.apache.lucene.analysis.ResourceLoader` |
| `org.apache.lucene.analysis.util.ResourceLoaderAware` | `org.apache.lucene.analysis.ResourceLoaderAware` |

**Files to change:**
- `Jmorphy2StemFilterFactory.java` — update 3 import statements
- `Jmorphy2SubjectFilterFactory.java` — update 3 import statements
- `LuceneFileLoader.java` — update 1 import statement

#### 2.5.3 Lucene SPI Changes (TokenFilterFactory)

**Why:** Lucene 9 requires each `TokenFilterFactory` subclass to have:
1. A `public static final String NAME = "..."` field
2. A public no-arg constructor that throws `UnsupportedOperationException`

**Files to change:**
- `Jmorphy2StemFilterFactory.java` — add `NAME` field and no-arg constructor
- `Jmorphy2SubjectFilterFactory.java` — add `NAME` field and no-arg constructor

**Example:**
```java
public static final String NAME = "jmorphy2_stem";

public Jmorphy2StemFilterFactory() {
    throw defaultCtorException();
}
```

### 2.6 ES-Lucene Version Mapping Update

**Why:** ES 8.x uses Lucene 9.x. The `Versions.esLuceneVersions` map needs ES 8.x entries.

**File:** `buildSrc/src/main/kotlin/Versions.kt`

**Add entries:**
```kotlin
EsVersion(8, 0) to "9.0.0",
EsVersion(8, 1) to "9.0.0",
EsVersion(8, 2) to "9.1.0",
EsVersion(8, 3) to "9.2.0",
EsVersion(8, 4) to "9.3.0",
EsVersion(8, 5) to "9.4.0",
EsVersion(8, 6) to "9.4.1",
EsVersion(8, 7) to "9.5.0",
EsVersion(8, 8) to "9.6.0",
EsVersion(8, 9) to "9.7.0",
EsVersion(8, 10) to "9.7.0",
EsVersion(8, 11) to "9.8.0",
EsVersion(8, 12) to "9.8.1",
EsVersion(8, 13) to "9.10.0",
EsVersion(8, 14) to "9.10.1",
EsVersion(8, 15) to "9.11.1",
```

### 2.7 Build System Changes

#### 2.7.1 Gradle Version Upgrade

**Why:** ES 8.x build-tools requires Gradle 8.x+. Current version is 7.3.3.

**File:** `gradle/wrapper/gradle-wrapper.properties` — update `distributionUrl` to Gradle 8.5+

#### 2.7.2 Kotlin Plugin Version

**Why:** Gradle 8.x requires newer Kotlin plugin. Current version is 1.5.20.

**File:** `buildSrc/build.gradle.kts` — update to Kotlin 1.9.x

#### 2.7.3 ES build-tools Version

**Why:** The `elasticsearch.gradle:build-tools` version must match the target ES version.

**File:** `jmorphy2-elasticsearch/build.gradle.kts` — the version is already dynamically resolved from `es.version` file, so just updating `es.version` handles this.

#### 2.7.4 Default ES Version

**File:** `es.version` — change from `7.17.6` to target ES 8.x version (e.g., `8.15.0`)

#### 2.7.5 Nebula OSPackage Plugin

**Why:** The `nebula.ospackage` plugin version may need updating for Gradle 8 compatibility. Also, the `deb` task references `elasticsearch-oss` which was removed in ES 8.x.

**File:** `jmorphy2-elasticsearch/build.gradle.kts` — update plugin version and remove `elasticsearch-oss` requirement from the deb task.

### 2.8 Security Manager / Entitlements

**Why:** ES 8.x (up to 8.18) still supports the Java SecurityManager and `plugin-security.policy`. Starting from ES 8.19/9.0, the SecurityManager is completely replaced by Entitlements.

**Current code in `CachingMorphAnalyzer.java`:**
```java
SpecialPermission.check();
cache = AccessController.doPrivileged(...)
```

**Short term (ES 8.15):** The existing `plugin-security.policy` and `SpecialPermission`/`AccessController` code still works. No change needed.

**Long term (ES 9.x):** Will need to migrate to `entitlement-policy.yaml`.

### 2.9 Test Code Updates

**Why:** The `Index` constructor signature is the same, but test helper methods may differ.

**Files:** Test classes in `jmorphy2-elasticsearch/src/test/`

Key concerns:
- `ESTestCase` still exists but import paths may have changed (e.g., `org.elasticsearch.test.ESTestCase`)
- `createTestAnalysis()` method still works
- `Index("test", "_na_")` constructor still valid

### 2.10 Docker and CI/CD

**File:** `Dockerfile.elasticsearch` — update default ES version to 8.x
**File:** `.github/workflows/java.yaml` — update Java version, update deprecated actions (actions/checkout@v2 -> v4, actions/setup-java@v1 -> v4, etc.)

---

## 3. Migration Step Order (Recommended)

1. Update `es.version` and `Versions.kt` (Java 17, Lucene version map)
2. Update Gradle wrapper to 8.5+
3. Update `buildSrc/build.gradle.kts` (Kotlin plugin version)
4. Update `jmorphy2-lucene/build.gradle.kts` (Lucene 9 artifact names)
5. Update `jmorphy2-lucene` Java sources (import renames, SPI changes)
6. Update `jmorphy2-elasticsearch/build.gradle.kts` (nebula plugin, deb task)
7. Update `AnalysisJmorphy2Plugin.java` (constructor -> createComponents)
8. Update `Jmorphy2StemTokenFilterFactory.java` (remove IndexSettings from super)
9. Update `Jmorphy2SubjectTokenFilterFactory.java` (remove IndexSettings from super)
10. Update `Jmorphy2AnalyzerProvider.java` (remove IndexSettings from super)
11. Update test classes
12. Update `Dockerfile.elasticsearch`
13. Update `.github/workflows/java.yaml`
14. Verify build and tests pass

---

## 4. Risk Assessment

| Risk | Severity | Mitigation |
|---|---|---|
| Lucene 9 binary format incompatibility | Medium | Index recreation required when upgrading ES; not a plugin concern |
| Caffeine 3.x + Java 17 compatibility | Low | Caffeine 3.0.2 supports Java 11+, confirmed Java 17 compatible |
| Internal API usage (`new Environment()`) | Medium | Use `PluginServices.environment()` instead of creating Environment manually |
| ES build-tools strict version matching | High | Must build against exact ES version; cannot mix 7.x and 8.x builds |
| Test framework changes | Medium | May need to adapt to ES 8.x test infrastructure changes |

---

## 5. Recommended Target Version

**ES 8.15.0** (Lucene 9.11.1) — this is a stable, widely-deployed version with good documentation and community support.
