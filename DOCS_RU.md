# Jmorphy2 — Полная документация

Java-порт библиотеки [pymorphy2](https://github.com/kmike/pymorphy2) — морфологического анализатора для русского и украинского языков.
Предоставляет плагин для Elasticsearch со стеммером и фильтром извлечения подлежащих.

---

## Содержание

1. [Общее описание](#общее-описание)
2. [Требования](#требования)
3. [Архитектура и модули](#архитектура-и-модули)
   - [dawg](#dawg)
   - [jmorphy2-core](#jmorphy2-core)
   - [jmorphy2-dicts-ru](#jmorphy2-dicts-ru)
   - [jmorphy2-dicts-uk](#jmorphy2-dicts-uk)
   - [jmorphy2-nlp](#jmorphy2-nlp)
   - [jmorphy2-lucene](#jmorphy2-lucene)
   - [jmorphy2-elasticsearch](#jmorphy2-elasticsearch)
   - [jmorphy2-solr](#jmorphy2-solr)
   - [benchmarks](#benchmarks)
   - [buildSrc](#buildsrc)
4. [Используемые библиотеки и зависимости](#используемые-библиотеки-и-зависимости)
5. [Команды для сборки](#команды-для-сборки)
6. [Плагин для Elasticsearch](#плагин-для-elasticsearch)
7. [Запуск через Docker Compose](#запуск-через-docker-compose)
8. [CI/CD](#cicd)

---

## Общее описание

**Jmorphy2** — это морфологический анализатор, портированный с Python-библиотеки pymorphy2 на Java. Он позволяет:

- **Разбирать слова** — получать все возможные морфологические разборы (часть речи, падеж, число, род и т.д.)
- **Получать нормальные формы** (леммы) слов
- **Вычислять вероятность** разбора на основе корпусных данных
- **Извлекать подлежащие** из текста с помощью NLP-парсера
- **Интегрироваться** с Elasticsearch и Lucene через готовые плагины и токен-фильтры

Поддерживаемые языки: **русский** и **украинский**.

Версия проекта: **0.2.4-SNAPSHOT**

---

## Требования

- **Java 17** или новее (JDK, не JRE — необходим для компиляции)
- **Git**
- **Docker** (опционально, для запуска Elasticsearch через Docker Compose)

Проверка установки Java:

```shell
java -version
```

Установка Java:

```shell
# macOS (Homebrew)
brew install openjdk@17

# Ubuntu / Debian
sudo apt install openjdk-17-jdk

# Fedora
sudo dnf install java-17-openjdk-devel
```

---

## Архитектура и модули

Проект состоит из 9 модулей, организованных в виде иерархии зависимостей:

```
dawg
 └── jmorphy2-core
      ├── jmorphy2-dicts-ru
      ├── jmorphy2-dicts-uk
      ├── jmorphy2-nlp
      │    └── jmorphy2-lucene
      │         ├── jmorphy2-elasticsearch
      │         └── jmorphy2-solr
      └── benchmarks
```

### dawg

**Описание:** Библиотека для чтения файлов в формате DAWG (Directed Acyclic Word Graph).

DAWG — это структура данных на основе направленного ациклического графа, используемая для компактного хранения словарей. Все словарные файлы pymorphy2 (слова, суффиксы, вероятности) хранятся в этом формате.

**Ключевые классы:**

| Класс | Назначение |
|-------|-----------|
| `DAWG` | Базовый абстрактный класс для чтения DAWG-файлов |
| `Dict` | Низкоуровневая реализация DAWG: навигация по графу (followByte, followBytes), проверка наличия ключей и получение значений |
| `IntegerDAWG` | DAWG с целочисленными значениями; используется для хранения вероятностей P(t\|w) |
| `PayloadsDAWG` | DAWG с произвольными payload-данными; поддерживает поиск похожих слов (similarItems) с заменой символов; используется для хранения слов и суффиксов предсказания |

**Зависимости:**
- `commons-io` — для чтения данных (SwappedDataInputStream)
- `commons-codec` — для декодирования Base64 в payload

---

### jmorphy2-core

**Описание:** Основной модуль — Java-порт pymorphy2. Реализует морфологический анализатор с поддержкой словарного разбора, предсказания по суффиксам/префиксам и оценки вероятности разборов.

**Ключевые классы:**

| Класс | Назначение |
|-------|-----------|
| `MorphAnalyzer` | Главный класс морфологического анализатора. Методы: `parse()` — разбор слова, `normalForms()` — получение лемм, `tag()` — получение тегов. Использует паттерн Builder для конфигурации |
| `Dictionary` | Загрузка и представление морфологического словаря: парадигмы, суффиксы, грамматическая таблица, слова, суффиксы предсказания |
| `Dictionary.Meta` | Метаданные словаря (версия формата, язык, параметры компиляции) |
| `Dictionary.Paradigm` | Парадигма словоизменения — массив суффиксов, префиксов и тегов |
| `Tag` | Морфологический тег (набор граммем): часть речи, одушевлённость, род, число, падеж, вид, переходность, лицо, время, наклонение, залог |
| `Tag.Storage` | Хранилище тегов и граммем (кэширование, нормализация) |
| `Grammeme` | Граммема — минимальная морфологическая единица. Имеет иерархию (parent/root), русское название и описание |
| `ParsedWord` | Результат разбора слова: исходное слово, тег, нормальная форма, найденное слово, оценка (score). Поддерживает склонение (`inflect()`) |
| `ProbabilityEstimator` | Оценка вероятности P(t\|w) — вероятности тега для данного слова на основе корпусных данных. Хранит данные в IntegerDAWG |
| `Resources` | Загрузка языковых ресурсов: замены символов (`char_substitutes.txt`), известные приставки (`known_prefixes.txt`) |
| `JSONUtils` | Утилита для парсинга JSON-файлов словаря |
| `FileLoader` | Интерфейс загрузки файлов словаря |
| `FSFileLoader` | Загрузка файлов из файловой системы |
| `ResourceFileLoader` | Загрузка файлов из classpath (JAR-ресурсов) |

**Единицы анализа (units):**

Анализатор использует цепочку единиц анализа (AnalyzerUnit), которые обрабатывают слово по очереди. Если единица находит разбор и является терминальной (`isTerminated()`), цепочка прерывается:

| Класс | Назначение | Вес |
|-------|-----------|-----|
| `DictionaryUnit` | Словарный разбор — поиск слова в DAWG-словаре | 1.0 |
| `NumberUnit` | Распознавание чисел | 0.9 |
| `PunctuationUnit` | Распознавание пунктуации | 0.9 |
| `RomanUnit` | Распознавание римских чисел | 0.9 |
| `LatinUnit` | Распознавание латинских слов | 0.9 |
| `KnownPrefixUnit` | Разбор слов с известными приставками (если есть для языка) | 0.75 |
| `UnknownPrefixUnit` | Разбор слов с неизвестными приставками (перебор) | 0.5 |
| `KnownSuffixUnit` | Предсказание по известному суффиксу | 0.5 |
| `UnknownUnit` | Базовый разбор для неизвестных слов | 1.0 |

---

### jmorphy2-dicts-ru

**Описание:** Русские морфологические словари pymorphy2.

- Версия словаря: **2.4.404381.4453942**
- MD5: `bdd5d23660f2ad5e8ec2721743a8b419`
- Словари автоматически загружаются с PyPI при сборке (задача `fetchPymorphy2Dicts`)

**Файлы словаря:**

| Файл | Назначение |
|------|-----------|
| `meta.json` | Метаданные словаря |
| `grammemes.json` | Иерархия граммем |
| `gramtab-opencorpora-int.json` | Грамматическая таблица (внутренний формат OpenCorpora) |
| `words.dawg` | DAWG со всеми словоформами |
| `paradigms.array` | Бинарный массив парадигм словоизменения |
| `suffixes.json` | Массив суффиксов |
| `prediction-suffixes-0.dawg` | Суффиксы предсказания (уровень 0) |
| `prediction-suffixes-1.dawg` | Суффиксы предсказания (уровень 1) |
| `prediction-suffixes-2.dawg` | Суффиксы предсказания (уровень 2) |
| `p_t_given_w.intdawg` | Вероятности P(t\|w) |

---

### jmorphy2-dicts-uk

**Описание:** Украинские морфологические словари pymorphy2.

- Версия словаря: **2.4.1.1.1460299261**
- MD5: `f193a4ac7a8e6124e6fd8846f06ccca0`
- Механизм загрузки аналогичен русскому модулю

**Файлы словаря:**

| Файл | Назначение |
|------|-----------|
| `meta.json` | Метаданные словаря |
| `grammemes.json` | Иерархия граммем |
| `gramtab-opencorpora-int.json` | Грамматическая таблица |
| `words.dawg` | DAWG со словоформами |
| `paradigms.array` | Массив парадигм |
| `suffixes.json` | Массив суффиксов |
| `prediction-suffixes-0.dawg` | Суффиксы предсказания (уровень 0) |
| `prediction-suffixes-1.dawg` | Суффиксы предсказания (уровень 1) |
| `prediction-suffixes-2.dawg` | Суффиксы предсказания (уровень 2) |
| `prediction-suffixes-3.dawg` | Суффиксы предсказания (уровень 3) |

---

### jmorphy2-nlp

**Описание:** Модуль NLP (обработка естественного языка) на основе словарей pymorphy2. Реализует теггер, парсер и извлекатель подлежащих.

**Ключевые классы:**

| Класс | Назначение |
|-------|-----------|
| `Tagger` | Абстрактный класс теггера |
| `SimpleTagger` | Теггер — назначает морфологические теги токенам. Использует правила (Ruleset) и морфологический анализатор. Порог по умолчанию: 1000 вариантов |
| `Parser` | Абстрактный класс парсера |
| `SimpleParser` | Парсер — строит синтаксическое дерево из тегированных токенов. Правила по умолчанию описывают именные (NP), глагольные (VP) и предложные (PP) группы. Порог по умолчанию: 100 |
| `SubjectExtractor` | Извлекатель подлежащих — извлекает подлежащие из синтаксического дерева по конфигурационной строке. Правило по умолчанию: `+NP,nomn +NP,accs -PP -Geox NOUN,nomn NOUN,accs LATN NUMB` |
| `Node` | Узел синтаксического дерева (граммемные значения, вложенные дети, оценка) |
| `Rule` | Правило грамматики (левая часть → правая часть, вес) |
| `Ruleset` | Набор правил грамматики |

**Правила парсера по умолчанию (SimpleParser):**

```
NP -> $NP @CONJ $NP   (вес: 10)  — именная группа с союзом
NP -> NP PP            (вес: 9)   — именная группа с предложной
VP -> VP PP            (вес: 9)   — глагольная группа с предложной
PP -> PREP NP          (вес: 8)   — предложная группа
PP -> PP LATN          (вес: 8)   — предложная группа с латинским
PP -> PP NUMB          (вес: 8)   — предложная группа с числом
PP -> PREP LATN        (вес: 8)
PP -> PREP NUMB        (вес: 8)
NP -> $NP,nomn @NP,gent (вес: 8) — именная группа в именительном + родительный
NP -> NP @LATN         (вес: 5)
NP -> NP @NUMB         (вес: 5)
NP -> @LATN NP         (вес: 4)
NP -> @NUMB NP         (вес: 4)
NP -> ADJF NP          (вес: 9)   — прилагательное + именная группа
NP -> NP ADJF          (вес: 8)
VP -> INFN VERB         (вес: 2)
NP -> NOUN,nomn         (вес: 2)  — существительное в именительном
NP -> NOUN,accs         (вес: 1)  — существительное в винительном
NP -> NOUN              (вес: 1)
VP -> INFN              (вес: 1)  — инфинитив
VP -> VERB              (вес: 1)  — глагол
```

---

### jmorphy2-lucene

**Описание:** Модуль интеграции с Apache Lucene — стеммер и извлекатель подлежащих на основе jmorphy2.

**Ключевые классы:**

| Класс | Назначение |
|-------|-----------|
| `Jmorphy2StemFilter` | Lucene TokenFilter — заменяет токены их нормальными формами (леммами). Поддерживает фильтрацию по граммемам (include/exclude), position increments и ключевые слова |
| `Jmorphy2StemFilterFactory` | Фабрика стеммер-фильтра для Lucene/Solr. Имя: `jmorphy2_stemmer`. Параметры: `dict`, `replaces`, `includeTags`, `excludeTags`, `enablePositionIncrements` |
| `Jmorphy2SubjectFilter` | Lucene TokenFilter — извлекает подлежащие из потока токенов. Накапливает токены до maxSentenceLength, затем анализирует и фильтрует |
| `Jmorphy2SubjectFilterFactory` | Фабрика фильтра подлежащих для Lucene/Solr. Имя: `jmorphy2_subject`. Параметры: `dict`, `replaces`, `taggerRules`, `taggerThreshold`, `parserRules`, `parserThreshold`, `extract`, `maxSentenceLength` |
| `Jmorphy2Analyzer` | Анализатор на базе jmorphy2 (заготовка) |
| `LuceneFileLoader` | Загрузчик файлов словаря через Lucene ResourceLoader |

**Параметры фильтра `jmorphy2_stemmer`:**

| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `dict` | `pymorphy2_dicts` | Путь к словарям |
| `replaces` | — | Путь к файлу замен символов (JSON) |
| `includeTags` | — | Включить только слова с указанными граммемами (через пробел, внутри через запятую) |
| `excludeTags` | — | Исключить слова с указанными граммемами |
| `enablePositionIncrements` | `true` | Учитывать позиционные инкременты |

**Параметры фильтра `jmorphy2_subject`:**

| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `dict` | `pymorphy2_dicts` | Путь к словарям |
| `replaces` | — | Путь к файлу замен символов |
| `taggerRules` | — | Путь к файлу правил теггера |
| `taggerThreshold` | `1000` | Порог теггера (макс. вариантов) |
| `parserRules` | — | Путь к файлу правил парсера |
| `parserThreshold` | `100` | Порог парсера (макс. вариантов) |
| `extract` | — | Конфигурация извлечения подлежащих |
| `maxSentenceLength` | `10` | Максимальная длина предложения для анализа |

---

### jmorphy2-elasticsearch

**Описание:** Плагин для Elasticsearch, предоставляющий два токен-фильтра: `jmorphy2_stemmer` (стеммер/лемматизатор) и `jmorphy2_subject` (извлечение подлежащих).

- Поддерживаемая версия Elasticsearch: **8.15.x**
- Версия Lucene: **9.11.1**
- Класс плагина: `company.evo.jmorphy2.elasticsearch.plugin.AnalysisJmorphy2Plugin`

**Ключевые классы:**

| Класс | Назначение |
|-------|-----------|
| `AnalysisJmorphy2Plugin` | Главный класс плагина. Регистрирует два токен-фильтра: `jmorphy2_stemmer` и `jmorphy2_subject` |
| `Jmorphy2Service` | Сервис управления экземплярами MorphAnalyzer и SubjectExtractor. Загружает словари из файловой системы или из ресурсов JAR. Поддерживает кэширование по ключу (язык, путь к заменам, размер кэша) |
| `CachingMorphAnalyzer` | Обёртка над MorphAnalyzer с кэшированием результатов через Caffeine. При `cacheSize > 0` результаты `parse()` кэшируются |
| `Jmorphy2StemTokenFilterFactory` | Фабрика стеммер-фильтра для Elasticsearch. Параметры: `name` (язык: ru/uk), `cacheSize`, `replaces` |
| `Jmorphy2SubjectTokenFilterFactory` | Фабрика фильтра подлежащих для Elasticsearch. Параметры: `name` (язык), `cacheSize`, `replaces`, `taggerRules`, `taggerThreshold`, `parserRules`, `parserThreshold`, `extract`, `maxSentenceLength` |

**Артефакты сборки:**

| Файл | Описание |
|------|----------|
| `analysis-jmorphy2-<version>-es8.15.0.zip` | ZIP-архив плагина для `elasticsearch-plugin install` |
| `elasticsearch-analysis-jmorphy2-plugin_<version>~es8.15.0_all.deb` | Debian-пакет для apt-установки |

**Политика безопасности (`plugin-security.policy`):**

Плагин требует разрешение `java.lang.reflect.ReflectPermission "suppressAccessChecks"` для работы Caffeine Cache.

**Настройка словарей:**

Elasticsearch настраивает расположение словарей через параметр:
```
indices.analysis.jmorphy2.dictionary.location
```
По умолчанию: `jmorphy2` (относительно конфигурационной директории ES).

Словари загружаются в следующем порядке:
1. Из файловой системы: `<configDir>/<location>/<lang>/pymorphy2_dicts/`
2. Из ресурсов JAR (встроенные словари): `/company/evo/jmorphy2/<lang>/pymorphy2_dicts/`

---

### jmorphy2-solr

**Описание:** Модуль интеграции с Apache Solr — стеммер и теггер на основе jmorphy2.

**Зависимости:**
- `solr-core` (версия Lucene)
- `jmorphy2-lucene`

Модуль находится в минимальной стадии реализации (исходный код не содержит Java-классов, только зависимости).

---

### benchmarks

**Описание:** Модуль бенчмарков производительности на основе JMH (Java Microbenchmark Harness). Написан на Kotlin.

**Класс бенчмарка:** `MorphAnalyzerBenchmarks`

**Параметры JMH:**

| Параметр | Значение |
|----------|----------|
| fork | 2 |
| warmup | 1s, 2 итерации |
| iteration | 2s, 4 итерации |

---

### buildSrc

**Описание:** Модуль конфигурации сборки Gradle.

**Файлы:**

| Файл | Назначение |
|------|-----------|
| `Versions.kt` | Версии всех зависимостей, утилиты для определения версии ES и Lucene |
| `downloadAndUnpackDicts.kt` | Gradle-задача `Pymorphy2Dicts` — скачивание и распаковка словарей с PyPI |

**Задача `Pymorphy2Dicts`:**
- Скачивает `pymorphy2-dicts-<lang>-<version>.tar.gz` с PyPI
- Проверяет MD5-хэш
- Распаковывает в `src/main/resources/company/evo/jmorphy2/<lang>/pymorphy2_dicts/`

---

## Используемые библиотеки и зависимости

### Внешние зависимости

| Библиотека | Версия | Модуль | Назначение |
|-----------|--------|--------|-----------|
| `commons-io` | 2.4 | dawg, jmorphy2-core | Утилиты ввода-вывода (SwappedDataInputStream для чтения бинарных данных) |
| `commons-codec` | [1.10, 1.11] | dawg | Кодирование/декодирование (Base64 для payload в DAWG) |
| `noggit` | 0.8 | jmorphy2-core | Парсер JSON для файлов словаря |
| `caffeine` | 3.0.2 | jmorphy2-elasticsearch | Высокопроизводительный кэш (Caffeine LoadingCache) |
| `lucene-core` | 9.11.1 | jmorphy2-lucene | Ядро Apache Lucene |
| `lucene-analysis-common` | 9.11.1 | jmorphy2-lucene | Стандартные анализаторы Lucene |
| `lucene-test-framework` | 9.11.1 | jmorphy2-lucene | Тестовый фреймворк Lucene |
| `solr-core` | 9.11.1 | jmorphy2-solr | Ядро Apache Solr |
| `elasticsearch build-tools` | 8.15.0 | jmorphy2-elasticsearch | Инструменты сборки плагинов ES |
| `gradle-ospackage-plugin` | 12.2.0 | jmorphy2-elasticsearch | Сборка Debian-пакетов |
| `JUnit` | 4.11 | jmorphy2-core, jmorphy2-nlp | Модульное тестирование |
| `JMH Gradle Plugin` | 0.6.5 | benchmarks | Запуск микробенчмарков |
| `Kotlin JVM` | — | benchmarks | Язык для написания бенчмарков |

### Внутренние зависимости (межмодульные)

```
dawg ← jmorphy2-core
jmorphy2-core ← jmorphy2-nlp
jmorphy2-core ← jmorphy2-lucene
jmorphy2-dicts-ru ← jmorphy2-core (тесты)
jmorphy2-dicts-uk ← jmorphy2-core (тесты)
jmorphy2-core ← jmorphy2-nlp
jmorphy2-nlp ← jmorphy2-lucene
jmorphy2-dicts-ru ← jmorphy2-lucene
jmorphy2-dicts-uk ← jmorphy2-lucene
jmorphy2-lucene ← jmorphy2-elasticsearch
jmorphy2-dicts-ru ← jmorphy2-elasticsearch
jmorphy2-dicts-uk ← jmorphy2-elasticsearch
jmorphy2-lucene ← jmorphy2-solr
jmorphy2-core ← benchmarks
jmorphy2-dicts-ru ← benchmarks
```

---

## Команды для сборки

Проект использует Gradle Wrapper — установка Gradle отдельно не требуется. На первый запуск wrapper скачивается автоматически.

### Клонирование репозитория

```shell
git clone https://github.com/anti-social/jmorphy2
cd jmorphy2
```

> На Windows используйте `gradlew.bat` вместо `./gradlew`.

### Сборка плагина Elasticsearch

```shell
./gradlew :jmorphy2-elasticsearch:assemble
```

Эта команда компилирует проект, скачивает морфологические словари и упаковывает плагин в ZIP-архив.

**Результат сборки:**

```
jmorphy2-elasticsearch/build/distributions/
├── analysis-jmorphy2-0.2.4-SNAPSHOT-es8.15.0.zip
└── elasticsearch-analysis-jmorphy2-plugin_0.2.4~es8.15.0_all.deb
```

### Сборка всего проекта (все модули + тесты)

```shell
./gradlew build
```

### Запуск тестов

```shell
# Тесты плагина Elasticsearch
./gradlew :jmorphy2-elasticsearch:test

# Тесты всех модулей
./gradlew check

# Тесты конкретного модуля
./gradlew :jmorphy2-core:test
./gradlew :jmorphy2-nlp:test
./gradlew :jmorphy2-lucene:test
```

### Сборка с конкретной версией Elasticsearch

```shell
./gradlew :jmorphy2-elasticsearch:assemble -PesVersion=8.15.0
```

### Release-сборка

```shell
./gradlew assemble -Prelease=true
```

### Очистка

```shell
./gradlew clean
```

### Запуск бенчмарков

```shell
./gradlew :benchmarks:jmh
```

### Полезные команды

```shell
# Список всех задач
./gradlew tasks

# Список проектов (модулей)
./gradlew projects

# Зависимости модуля
./gradlew :jmorphy2-core:dependencies

# Проверка версии Java
java -version
```

---

## Плагин для Elasticsearch

### Установка из локальной сборки

```shell
export es_home=/usr/share/elasticsearch
sudo ${es_home}/bin/elasticsearch-plugin install \
  "file://$(pwd)/jmorphy2-elasticsearch/build/distributions/analysis-jmorphy2-0.2.4-SNAPSHOT-es8.15.0.zip"
```

### Настройка анализаторов

Создание индекса с русским и украинским анализаторами:

```shell
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
```

### Тестирование анализаторов

```shell
# Тест русского анализатора: "теплые перчатки" → "тёплый", "перчатка"
curl -s -X GET -H 'Content-Type: application/json' \
  'localhost:9200/test_index/_analyze' \
  -d '{"analyzer": "text_ru", "text": "теплые перчатки"}'

# Тест украинского анализатора: "Пригоди Котигорошка"
curl -s -X GET -H 'Content-Type: application/json' \
  'localhost:9200/test_index/_analyze' \
  -d '{"analyzer": "text_uk", "text": "Пригоди Котигорошка"}'
```

### Параметры токен-фильтра `jmorphy2_stemmer`

| Параметр | Обязательный | По умолчанию | Описание |
|----------|-------------|-------------|----------|
| `name` | Да | — | Код языка: `ru` или `uk` |
| `cacheSize` | Нет | 0 | Размер кэша Caffeine (0 — без кэша) |
| `replaces` | Нет | — | Путь к файлу замен символов (JSON, относительно configDir) |

### Параметры токен-фильтра `jmorphy2_subject`

| Параметр | Обязательный | По умолчанию | Описание |
|----------|-------------|-------------|----------|
| `name` | Да | — | Код языка: `ru` или `uk` |
| `cacheSize` | Нет | 0 | Размер кэша морфоанализатора |
| `replaces` | Нет | — | Путь к файлу замен символов |
| `taggerRules` | Нет | встроенные | Путь к файлу правил теггера |
| `taggerThreshold` | Нет | 1000 | Макс. количество вариантов теггера |
| `parserRules` | Нет | встроенные | Путь к файлу правил парсера |
| `parserThreshold` | Нет | 100 | Макс. количество вариантов парсера |
| `extract` | Нет | встроенные | Конфигурация извлечения подлежащих |
| `maxSentenceLength` | Нет | 10 | Макс. длина предложения |

### Формат файла замен символов

JSON-файл, задающий символы и их замены для коррекции опечаток (например, ё→е):

```json
{
  "ё": "е",
  "і": "и"
}
```

---

## Запуск через Docker Compose

Проект включает `docker-compose.yaml` и `Dockerfile.elasticsearch` для сборки Docker-образа с предустановленным плагином.

**1. Собрать плагин:**

```shell
./gradlew :jmorphy2-elasticsearch:assemble
```

**2. Собрать образ и запустить контейнер:**

```shell
docker compose up -d
```

Это:
- Собирает Docker-образ на основе `elasticsearch:8.15.0`
- Копирует ZIP-архив плагина в образ
- Устанавливает плагин через `elasticsearch-plugin install`
- Запускает Elasticsearch на порту 9200 с отключённой безопасностью (для локальной разработки)

**3. Проверить работу Elasticsearch:**

```shell
curl -s http://localhost:9200/
```

Должен вернуть JSON с `"number" : "8.15.0"`.

**4. Пересборка после изменений:**

```shell
./gradlew :jmorphy2-elasticsearch:assemble
docker compose up -d --build
```

**5. Остановка и очистка:**

```shell
# Остановить контейнер
docker compose down

# Удалить также данные Elasticsearch
docker compose down -v
```

---

## CI/CD

Проект использует GitHub Actions для непрерывной интеграции (`.github/workflows/java.yaml`).

**Workflow: Java CI**

- Запускается при `push` и `pull_request`
- Платформа: `ubuntu-latest`
- Шаги:
  1. Checkout кода
  2. Установка JDK
  3. Кэширование Gradle wrapper и пакетов
  4. Сборка и тестирование:
     - `./gradlew assemble` — сборка
     - `./gradlew check` — тесты
     - При теге `v*` добавляется флаг `-Prelease=true`
     - При теге `v*-es*` извлекается версия ES: `-PesVersion=<version>`
  5. Загрузка артефактов (при push тега `v*-es*`)

**Release:**

При push тега в формате `v*-es*`:
- Собирается ZIP-архив и Debian-пакет
- Создаётся GitHub Release
- Артефакты загружаются как assets релиза
