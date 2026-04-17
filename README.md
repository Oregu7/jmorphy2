![Java CI](https://github.com/anti-social/jmorphy2/workflows/Java%20CI/badge.svg)
[![Appveyor status](https://ci.appveyor.com/api/projects/status/x9df34q1er8r5kc0/branch/master?svg=true)](https://ci.appveyor.com/project/anti-social/jmorphy2/branch/master)

# Jmorphy2

Java port of the [pymorphy2](https://github.com/kmike/pymorphy2) — morphological analyzer for Russian and Ukrainian languages.
Provides an analysis plugin for Elasticsearch with stemming and subject extraction token filters.

## Requirements

- **Java 17** or later (JDK, not JRE — needed for compilation)
- **Git**

Verify that Java is installed and has the correct version:

```shell
java -version
```

The output should contain `version "17.x.x"` or higher. If Java is not installed, download
[OpenJDK 17](https://adoptium.net/temurin/releases/?version=17) or install via a package manager:

```shell
# macOS (Homebrew)
brew install openjdk@17

# Ubuntu / Debian
sudo apt install openjdk-17-jdk

# Fedora
sudo dnf install java-17-openjdk-devel
```

## Building from source

### 1. Clone the repository

```shell
git clone https://github.com/anti-social/jmorphy2
cd jmorphy2
```

### 2. Build the Elasticsearch plugin

The project uses a Gradle wrapper — you do **not** need to install Gradle separately.
On the first run it will be downloaded automatically.

```shell
./gradlew :jmorphy2-elasticsearch:assemble
```

This command compiles the project, downloads the required morphological dictionaries,
and packages the plugin into a zip archive ready for installation into Elasticsearch.

> On Windows use `gradlew.bat` instead of `./gradlew`.

### 3. Where to find the built plugin

After a successful build the artifacts are located in:

```
jmorphy2-elasticsearch/build/distributions/
```

The directory contains:

| File | Description |
|------|-------------|
| `analysis-jmorphy2-<version>-es8.15.0.zip` | Plugin zip archive for `elasticsearch-plugin install` |
| `elasticsearch-analysis-jmorphy2-plugin_<version>~es8.15.0_all.deb` | Debian package for apt-based installation |

For example, for version 0.2.4:

```
analysis-jmorphy2-0.2.4-es8.15.0.zip
elasticsearch-analysis-jmorphy2-plugin_0.2.4~es8.15.0_all.deb
```

### 4. Run the tests (optional)

```shell
./gradlew :jmorphy2-elasticsearch:test
```

### 5. Build the whole project (all modules + tests)

```shell
./gradlew build
```

## Elasticsearch plugin

Default Elasticsearch version: **8.15.0** (Lucene 9.11.1).
Supported Elasticsearch versions: **8.15.x**. Builds and runtime require **Java 17**.

### Installing the plugin

#### From a local build

After building the project (see above), install the plugin from the local zip file:

```shell
# Specify correct path of your Elasticsearch installation
export es_home=/usr/share/elasticsearch
sudo ${es_home}/bin/elasticsearch-plugin install "file://$(pwd)/jmorphy2-elasticsearch/build/distributions/analysis-jmorphy2-0.2.4-SNAPSHOT-es8.15.0.zip"
```

### Running with Docker Compose

The project includes `docker-compose.yaml` and `Dockerfile.elasticsearch` that build a Docker image
with the locally compiled plugin already installed into Elasticsearch.

**1. Build the plugin** (if you haven't already):

```shell
./gradlew :jmorphy2-elasticsearch:assemble
```

**2. Build the Docker image and start the container:**

```shell
docker compose up -d
```

This will:
- build a Docker image based on the official `elasticsearch:8.15.0` image,
- copy the plugin zip from `jmorphy2-elasticsearch/build/distributions/` into the image,
- install the plugin via `elasticsearch-plugin install`,
- start Elasticsearch on port `9200` with security disabled (for local development).

**3. Check that Elasticsearch is running:**

```shell
curl -s http://localhost:9200/
```

Wait a few seconds after startup. You should see a JSON response with `"number" : "8.15.0"`.

**4. Rebuild after code changes:**

If you modify the plugin code, rebuild and restart the container:

```shell
./gradlew :jmorphy2-elasticsearch:assemble
docker compose up -d --build
```

**5. Stop and clean up:**

```shell
docker compose down
```

To also remove the Elasticsearch data volume:

```shell
docker compose down -v
```

### Testing the plugin

Once Elasticsearch is running, create an index with the jmorphy2 analyzer and test it:

```shell
# Create an index with Russian and Ukrainian analyzers
curl -X PUT -H 'Content-Type: application/json' 'localhost:9200/test_index' -d '{
  "settings": {
    "index": {
      "analysis": {
        "filter": {
          "jmorphy2_russian": {
            "type": "jmorphy2_stemmer",
            "name": "ru"
          },
          "jmorphy2_ukrainian": {
            "type": "jmorphy2_stemmer",
            "name": "uk"
          }
        },
        "analyzer": {
          "text_ru": {
            "tokenizer": "standard",
            "filter": ["lowercase", "jmorphy2_russian"]
          },
          "text_uk": {
            "tokenizer": "standard",
            "filter": ["lowercase", "jmorphy2_ukrainian"]
          }
        }
      }
    }
  }
}'

# Test Russian analyzer: "теплые перчатки" → "тёплый", "перчатка"
curl -s -X GET -H 'Content-Type: application/json' \
  'localhost:9200/test_index/_analyze' \
  -d '{"analyzer": "text_ru", "text": "теплые перчатки"}'

# Test Ukrainian analyzer: "Пригоди Котигорошка"
curl -s -X GET -H 'Content-Type: application/json' \
  'localhost:9200/test_index/_analyze' \
  -d '{"analyzer": "text_uk", "text": "Пригоди Котигорошка"}'
```
