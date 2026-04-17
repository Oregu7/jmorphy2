# AI Agents for Jmorphy2 Project

This document describes the AI agents and skills available for working with the Jmorphy2 project.

---

## Overview

Jmorphy2 is a Java morphological analyzer for Russian and Ukrainian languages, providing an Elasticsearch plugin for text analysis. AI agents can assist with various tasks including development, testing, documentation, and deployment.

---

## Available Agent Types

### 1. Code Reviewer Agent

**Purpose:** Reviews code changes for quality, best practices, and potential issues.

**When to use:**
- After implementing new features
- Before submitting pull requests
- When refactoring existing code

**Capabilities:**
- Analyzes Java/Kotlin code quality
- Checks for potential bugs and edge cases
- Reviews Gradle build configurations
- Validates Elasticsearch plugin compatibility

---

### 2. Test Runner Agent

**Purpose:** Executes and validates test suites.

**When to use:**
- After code modifications
- Before releases
- To verify bug fixes

**Capabilities:**
- Runs unit tests for all modules
- Executes integration tests for Elasticsearch plugin
- Validates morphological analyzer accuracy
- Benchmarks performance with JMH

---

### 3. Documentation Agent

**Purpose:** Creates and maintains project documentation.

**When to use:**
- When adding new features
- For API documentation updates
- When creating user guides

**Capabilities:**
- Generates Russian and English documentation
- Creates API references from code
- Documents Elasticsearch plugin configuration
- Maintains README files

---

### 4. Release Agent

**Purpose:** Prepares and validates project releases.

**When to use:**
- Preparing new version releases
- Building distribution packages
- Creating GitHub releases

**Capabilities:**
- Validates version consistency
- Builds ZIP and DEB packages
- Tests Elasticsearch plugin installation
- Generates release notes

---

## Project-Specific Agent Tasks

### Elasticsearch Plugin Development

```
Task: Develop or modify Elasticsearch plugin components
Steps:
1. Analyze requirements for the plugin change
2. Review Elasticsearch API documentation for version 8.15.x
3. Implement changes in jmorphy2-elasticsearch module
4. Update token filter factories if needed
5. Test with Elasticsearch Docker container
6. Validate plugin security policy
```

### Morphological Analyzer Enhancement

```
Task: Improve morphological analysis capabilities
Steps:
1. Identify the analyzer unit to modify (DictionaryUnit, KnownSuffixUnit, etc.)
2. Review existing unit tests in jmorphy2-core
3. Implement changes with proper probability estimation
4. Test with Russian and Ukrainian dictionaries
5. Run benchmarks to verify performance
```

### Dictionary Updates

```
Task: Update or add morphological dictionaries
Steps:
1. Check dictionary version in jmorphy2-dicts-ru or jmorphy2-dicts-uk
2. Update build.gradle.kts with new version and MD5
3. Run Gradle task to download dictionaries
4. Verify dictionary loading in tests
5. Update documentation with new version info
```

---

## Common Agent Workflows

### Build and Test Workflow

```shell
# Full build with tests
./gradlew build

# Quick plugin build
./gradlew :jmorphy2-elasticsearch:assemble

# Test with Docker
docker compose up -d
curl -s http://localhost:9200/
```

### Release Preparation Workflow

```shell
# Update version
echo "0.2.5" > project.version

# Build release
./gradlew :jmorphy2-elasticsearch:assemble -Prelease=true

# Test in Docker
docker compose up -d --build

# Verify plugin
curl -X PUT 'localhost:9200/test' -d '{...}'
```

---

## Agent Interaction Guidelines

### For Code Changes

1. **Always run tests** after modifications
2. **Check Elasticsearch compatibility** for plugin changes
3. **Update documentation** for API changes
4. **Validate Docker build** for deployment changes

### For Documentation

1. Maintain both Russian (DOCS_RU.md) and English (README.md) versions
2. Include code examples with proper syntax highlighting
3. Document all configuration parameters
4. Keep version numbers up to date

### For Testing

1. Run unit tests first: `./gradlew :jmorphy2-core:test`
2. Test Elasticsearch integration: `./gradlew :jmorphy2-elasticsearch:test`
3. Validate with Docker before releases
4. Run benchmarks for performance-critical changes

---

## Key Files for Agents

| File | Purpose |
|------|---------|
| `build.gradle.kts` | Root build configuration |
| `buildSrc/src/main/kotlin/Versions.kt` | Dependency versions |
| `project.version` | Current project version |
| `es.version` | Target Elasticsearch version |
| `docker-compose.yaml` | Docker configuration |
| `DOCS_RU.md` | Russian documentation |
| `README.md` | English documentation |

---

## Module Structure Reference

```
dawg/                    → DAWG file reading library
jmorphy2-core/           → Core morphological analyzer
jmorphy2-dicts-ru/       → Russian dictionaries
jmorphy2-dicts-uk/       → Ukrainian dictionaries
jmorphy2-nlp/            → NLP components (tagger, parser)
jmorphy2-lucene/         → Lucene integration
jmorphy2-elasticsearch/  → Elasticsearch plugin
jmorphy2-solr/           → Solr integration
benchmarks/              → JMH performance benchmarks
buildSrc/                → Gradle build configuration
```

---

## Getting Help

When using AI agents with this project, provide context about:

1. **Target module** — which module you're working with
2. **Language support** — Russian (ru) or Ukrainian (uk)
3. **Elasticsearch version** — currently 8.15.x
4. **Task type** — development, testing, documentation, or release

This helps agents provide more accurate and relevant assistance.
