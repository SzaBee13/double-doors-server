---
title: Development and Releases
id: Development-and-Releases
slug: /Development-and-Releases
---

## Local Build

Requirements:

- Java 25
- Maven

Build:

```bash
mvn package
```

Artifact:

- target/doubledoors-&lt;version&gt;.jar

## Source Layout

- src/main/java/szabee13/doubledoors
- src/main/java/szabee13/doubledoors/config
- src/main/java/szabee13/doubledoors/listeners
- src/main/java/szabee13/doubledoors/util
- src/main/resources/config.yml
- src/main/resources/plugin.yml

## Coding Rules

- Java 25 features are acceptable.
- Keep public methods documented.
- Avoid wildcard imports.
- Avoid direct block-state reads at MONITOR before scheduled delay.

## Manual Test Focus

1. Mirrored double door sync.
2. Recursive gate/trapdoor components.
3. Redstone-triggered state transitions.
4. Villager open and close events.
5. GriefPrevention claim boundary behavior.
6. Per-player preference toggles and persistence.

## Release Checklist

1. Update version in pom.xml.
2. Update version in src/main/resources/plugin.yml.
3. Build with mvn package.
4. Smoke-test on Paper 1.21.
5. Create release notes under releases/v&lt;version&gt;/RELEASE_NOTE.md.
6. Publish GitHub release with built jar.

## Documentation Checklist

- Keep wiki configuration keys in sync with src/main/resources/config.yml.
- Keep command/permission docs in sync with src/main/resources/plugin.yml and command handler behavior.
- Update troubleshooting page if behavior around recursive opening or compatibility changes.
