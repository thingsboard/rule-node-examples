# ThingsBoard rule node examples

Example custom rule nodes for [ThingsBoard](https://thingsboard.io). Use this project as a starting point for your own rule nodes: it builds a JAR that you add to a ThingsBoard installation, and the nodes appear in the rule chain editor next to the built-in ones.

The [rule node development guide](https://thingsboard.io/docs/pe/user-guide/contribution/rule-node-development/) explains how rule nodes work and walks through this project step by step.

## Versions

Each release branch targets one ThingsBoard release line and is kept on its latest patch release. Pick the branch that matches your ThingsBoard version:

| Branch | ThingsBoard | JDK |
|---|---|---|
| `release-4.2` | 4.2 | 17 |
| `release-4.3` | 4.3 | 17 |
| `release-4.4` | 4.4 | 25 |

`master` follows the latest release line, currently 4.4.

This branch builds against ThingsBoard 4.3.1.8. The version is set by the `thingsboard.version` property in `pom.xml`. All dependency versions come from the ThingsBoard BOM, so the project uses the same libraries as ThingsBoard itself.

## Example nodes

| Node | Type | Class | What it does |
|---|---|---|---|
| check key | Filter | `TbKeyFilterNode` | Routes the message via `True` if the configured key exists in the message data or metadata, otherwise via `False`. Also shows [configuration versioning](#configuration-versioning). |
| get sum into metadata | Enrichment | `TbGetSumIntoMetadata` | Sums the message data fields whose names start with the input key and puts the result into the metadata under the output key. |
| calculate sum | Transformation | `TbCalculateSumNode` | Sums the message data fields whose names start with the input key and replaces the message data with `{"<output key>": <sum>}`. |

For example, with the input key `temperature`, the message `{"temperature1": 10, "temperature2": 15.5, "humidity": 40}` gives a sum of `25.5`.

## Project layout

```
src/main/java/org/thingsboard/rule/engine/node/
├── filter/        check key
├── enrichment/    get sum into metadata
└── transform/     calculate sum
src/main/resources/public/static/rulenode/
└── custom-nodes-config.js    configuration forms for the rule chain editor
src/test/java/                 unit tests
```

Each node is a class annotated with `@RuleNode` that implements `TbNode`, plus a configuration class that implements `NodeConfiguration`.

## Build

Requirements: JDK 17 and Maven.

```bash
mvn clean install
```

The build runs the unit tests, checks dependency versions and license headers, and produces `target/rule-engine-1.0.0-custom-nodes.jar` with the nodes and their configuration forms.

New source files need the license header. Add it with `mvn license:format`.

## Deploy

After deploying, clear the browser cache and reload the ThingsBoard UI so the rule chain editor loads the new configuration forms.

### Package installation

```bash
sudo cp target/rule-engine-1.0.0-custom-nodes.jar /usr/share/thingsboard/extensions/
sudo chown thingsboard:thingsboard /usr/share/thingsboard/extensions/*
sudo service thingsboard restart
```

### Docker

The `Dockerfile` adds the JAR to the official `thingsboard/tb-node` image of the matching version:

```bash
docker build . -t your_repo/tb-node:4.3.1.8-custom-1
```

Use this image instead of `thingsboard/tb-node` in your deployment.

### Custom package name

ThingsBoard looks for rule nodes in the `org.thingsboard.server.extensions` and `org.thingsboard.rule.engine` packages. If your nodes are in a different package, set the `PLUGINS_SCAN_PACKAGES` environment variable (`plugins.scan_packages` in `thingsboard.yml`) to the default packages plus yours, for example `org.thingsboard.server.extensions,org.thingsboard.rule.engine,com.example.rule.node`.

## Configuration forms

`@RuleNode` points to the configuration form with `uiResources` (the JavaScript bundle) and `configDirective` (the Angular component in it). Without them, the rule chain editor shows a JSON editor for the node configuration.

The bundle in this project is built from [rule-node-examples-ui-ngx](https://github.com/thingsboard/rule-node-examples-ui-ngx). To change a form, edit it there, build the bundle and replace `custom-nodes-config.js` in this project.

## Configuration versioning

The `check key` node shows how to change a node configuration without breaking existing rule chains. Version 1 of the node added the `filterSource` field, and the `upgrade` method sets it to `DATA` in configurations saved by version 0.

To change the configuration of your own node:

1. Increase `version` in the `@RuleNode` annotation.
2. Migrate older configurations in `upgrade(fromVersion, oldConfiguration)`. Use a `switch` on `fromVersion` with `break` only before `default`, so that a configuration several versions old goes through every step.
3. Return `true` with the new configuration if anything changed.

ThingsBoard calls `upgrade` for rule nodes saved with an older version during the ThingsBoard upgrade and when a rule chain is saved or imported. Rule nodes are not upgraded when they start, so release configuration changes together with a new ThingsBoard version, and `init` can expect the current configuration.

## License

[Apache License 2.0](LICENSE)
