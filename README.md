# rule-node-examples
Examples of custom Rule Nodes for ThingsBoard contribution guide

Requires JDK 25+

Unit tests examples for custom rule nodes added as well

The `check key` node shows how to version a node configuration. When the configuration changes, increase `version` in the `@RuleNode` annotation and migrate older configurations in the `upgrade` method. ThingsBoard calls `upgrade` for rule nodes saved with an older version during the ThingsBoard upgrade and when a rule chain is saved or imported. Rule nodes are not upgraded when they start, so release configuration changes together with a new ThingsBoard version, and `init` can expect the current configuration.

To build the project run `mvn clean install`

To build a custom Docker image run `DOCKER_BUILDKIT=0 docker build . -t your_repo/tb-node:4.4.0.1-custom-1`
