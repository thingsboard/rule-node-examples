# Custom ThingsBoard Rule Node: Change Entity Label

This project provides a custom ThingsBoard rule node that allows you to change the label (name) of an entity (Device, Asset, or Customer) based on incoming messages.

## Functionality

The "Change Entity Label" rule node can determine the new label from:
*   A static string value.
*   A value from the incoming message's metadata.
*   A value from the incoming message's JSON payload.

The node identifies the entity type (Device, Asset, Customer) from the message originator. If the label update is successful, the message is routed via the "Success" chain; otherwise, it's routed via the "Failure" chain.

## Prerequisites

Before you begin, ensure you have the following installed:
*   [OpenJDK 17](https://adoptium.net/)
*   [Apache Maven](https://maven.apache.org/download.cgi) (3.6.0+ recommended)
*   [Docker](https://www.docker.com/get-started)
*   `kubectl` (Kubernetes command-line tool, configured to connect to your GKE cluster)
*   Access to a Docker image registry (e.g., Google Container Registry (GCR) or Artifact Registry)

## Compilation

1.  **Clone the Repository:**
    ```bash
    git clone https://your-repository-url/rule-node-change-label.git # Replace with actual URL
    cd rule-node-change-label
    ```

2.  **Configure for ThingsBoard PE:**
    Open the `pom.xml` file and ensure the `thingsboard.version` property is set to use the Professional Edition. For version 4.0.1PE, it should look like this:
    ```xml
    <properties>
        ...
        <thingsboard.version>4.0.1PE</thingsboard.version>
        ...
    </properties>
    ```
    *(This step should already be done if you followed the plan, but it's good to have it in the README).*

3.  **Build the JAR:**
    Compile the project and package it into a JAR file:
    ```bash
    mvn clean install
    ```
    This command will generate the JAR file in the `target/` directory (e.g., `target/custom-nodes-1.0.0.jar`).

## Building the Docker Image

This rule node is designed to be deployed as part of the ThingsBoard PE Rule Engine microservice in a Kubernetes environment like GKE.

1.  **Dockerfile:**
    The provided `Dockerfile` is set up to package your custom rule node JAR into a Docker image. It should look similar to this:

    ```dockerfile
    # Copyright © 2018-2025 The Thingsboard Authors
    # ... (license headers) ...

    # Use the latest available ThingsBoard PE node image if 4.0.1PE is not available/suitable.
    FROM thingsboard/tb-pe-node:4.0.1PE
    COPY target/custom-nodes-1.0.0.jar /usr/share/thingsboard/extensions/
    ```
    *   Ensure the `FROM` line points to the correct ThingsBoard PE rule engine base image for your ThingsBoard version.
    *   Ensure the `COPY` command correctly references the JAR file produced by the `mvn clean install` command (e.g., `custom-nodes-1.0.0.jar`).

2.  **Build the Image:**
    Use the `docker build` command to create your image. Replace `YOUR_REGISTRY`, `YOUR_IMAGE_NAME`, and `YOUR_TAG` with your details.
    ```bash
    docker build . -t YOUR_REGISTRY/YOUR_IMAGE_NAME:YOUR_TAG
    ```
    Example for Google Container Registry (GCR):
    ```bash
    docker build . -t gcr.io/your-gcp-project-id/tb-pe-node-change-label:latest
    ```

## Pushing the Docker Image

Push the newly built image to your Docker registry:
```bash
docker push YOUR_REGISTRY/YOUR_IMAGE_NAME:YOUR_TAG
```
Example for GCR:
```bash
docker push gcr.io/your-gcp-project-id/tb-pe-node-change-label:latest
```

## Deployment to ThingsBoard PE on GKE

1.  **Update Kubernetes Manifest:**
    You'll need to update the Kubernetes deployment or statefulset manifest for your ThingsBoard PE `tb-rule-engine` microservice (the exact name might vary based on your deployment). Modify the manifest to use the new Docker image you just pushed.

    Find the container spec for the rule engine and update the `image` field:
    ```yaml
    spec:
      template:
        spec:
          containers:
            - name: tb-rule-engine # Or your rule engine container name
              image: YOUR_REGISTRY/YOUR_IMAGE_NAME:YOUR_TAG # <-- Update this line
              env: # Ensure PLUGINS_SCAN_PACKAGES is correctly set
                - name: PLUGINS_SCAN_PACKAGES
                  value: "org.thingsboard.server.extensions,org.thingsboard.rule.engine,org.thingsboard.custom.rule.node" # Add other custom packages if any
    ```

2.  **Package Scanning (`PLUGINS_SCAN_PACKAGES`):**
    ThingsBoard needs to scan the package containing your custom rule node. The current node resides in `org.thingsboard.rule.engine.node`.
    The `pom.xml` uses `org.thingsboard.custom` as the `groupId`. The Java files are under `org.thingsboard.rule.engine.node`.
    The default `PLUGINS_SCAN_PACKAGES` in ThingsBoard PE usually includes `org.thingsboard.rule.engine`.
    If you've used a different root package for your custom nodes (e.g., `com.mycompany.thingsboard.nodes`), ensure that package is added to the `PLUGINS_SCAN_PACKAGES` environment variable in your `tb-rule-engine` Kubernetes manifest, as shown in the example above. The current example `org.thingsboard.custom.rule.node` might be needed if the default doesn't pick up sub-packages of `org.thingsboard.rule.engine` when they are in a different artifact. It's safer to include it.

3.  **Apply Changes:**
    Apply the updated manifest to your GKE cluster:
    ```bash
    kubectl apply -f your-rule-engine-deployment.yaml -n YOUR_THINGSBOARD_NAMESPACE
    ```
    Replace `your-rule-engine-deployment.yaml` with the actual filename of your manifest and `YOUR_THINGSBOARD_NAMESPACE` with the namespace where ThingsBoard is deployed. This will trigger a rolling update of your rule engine pods.

## Adding and Configuring the Node in ThingsBoard UI

1.  **Refresh ThingsBoard UI:**
    After the `tb-rule-engine` pods have restarted with the new image, clear your browser cache and refresh the ThingsBoard web UI.

2.  **Add to Rule Chain:**
    *   Navigate to the Rule Chain where you want to add the node.
    *   Click the "+" icon to add a new node.
    *   The "Change Entity Label" node should appear in the **Action** section of the rule node list (based on `ComponentType.ACTION` in its definition).
    *   Drag and drop it onto your rule chain.

3.  **Configure the Node:**
    *   Click on the newly added "Change Entity Label" node to open its configuration dialog.
    *   You will see fields to define:
        *   **Label Source:** Choose from `STATIC`, `MESSAGE_METADATA`, or `MESSAGE_DATA`.
        *   **Static Label Value:** (If Label Source is `STATIC`) Enter the fixed label string.
        *   **Label Name or Pattern:** (If Label Source is `MESSAGE_METADATA` or `MESSAGE_DATA`) Enter the metadata key or JSON key/path to extract the label from.
        *   **Target Entity Type:** (This field was in the Angular component but might not be actively used by the Java node if it relies purely on originator type. The Java node determines type via `msg.getOriginator().getEntityType()`).
    *   Configure the settings according to your requirements.
    *   Save the configuration and the rule chain.

## Troubleshooting

*   Check the logs of your `tb-rule-engine` pods in GKE for any errors during startup or message processing:
    ```bash
    kubectl logs YOUR_TB_RULE_ENGINE_POD_NAME -n YOUR_THINGSBOARD_NAMESPACE -f
    ```
*   Enable debug mode for the "Change Entity Label" node in your rule chain to see how it processes messages and what labels are being generated.
