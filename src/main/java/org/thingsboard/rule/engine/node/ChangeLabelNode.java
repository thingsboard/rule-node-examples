package org.thingsboard.rule.engine.node;

import org.thingsboard.rule.engine.api.RuleNode;
import org.thingsboard.rule.engine.api.TbContext;
import org.thingsboard.rule.engine.api.TbNode;
import org.thingsboard.rule.engine.api.TbNodeConfiguration;
import org.thingsboard.rule.engine.api.TbNodeException;
import org.thingsboard.rule.engine.api.util.TbNodeUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.thingsboard.rule.engine.api.ScriptEngine;
import org.thingsboard.server.common.data.Customer;
import org.thingsboard.server.common.data.Device;
import org.thingsboard.server.common.data.EntityType;
import org.thingsboard.server.common.data.asset.Asset;
import org.thingsboard.server.common.data.id.CustomerId;
import org.thingsboard.server.common.data.id.DeviceId;
import org.thingsboard.server.common.data.id.EntityId;
import org.thingsboard.server.common.data.id.AssetId;
import org.thingsboard.server.common.data.plugin.ComponentType;
import org.thingsboard.server.common.msg.TbMsg;
import org.thingsboard.server.dao.asset.AssetService;
import org.thingsboard.server.dao.customer.CustomerService;
import org.thingsboard.server.dao.device.DeviceService;

import java.io.IOException;

@Slf4j
@RuleNode(
    type = ComponentType.ACTION,
    name = "Change Entity Label",
    configClazz = ChangeLabelNodeConfiguration.class,
    relationTypes = {"Success", "Failure"},
    nodeDescription = "Changes the label/name of an incoming Device, Asset, or Customer entity.",
    nodeDetails = "The new label can be a static string, extracted from message metadata, or from the message payload (JSON). " +
                  "The node determines the entity type from the message originator. " +
                  "If the label is updated successfully, the message is routed via 'Success', otherwise 'Failure'.",
    uiResources = {"static/rulenode/change-label-node-config.js"},
    configDirective = "tb-action-node-change-label-config"
)
public class ChangeLabelNode implements TbNode {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private ChangeLabelNodeConfiguration config;

    @Override
    public void init(TbContext ctx, TbNodeConfiguration configuration) throws TbNodeException {
        this.config = TbNodeUtils.convert(configuration, ChangeLabelNodeConfiguration.class);
    }

    @Override
    public void onMsg(TbContext ctx, TbMsg msg) {
        EntityId originator = msg.getOriginator();
        EntityType entityType = originator.getEntityType();

        String newLabel = determineNewLabel(ctx, msg);

        if (newLabel == null || newLabel.trim().isEmpty()) {
            log.warn("[{}] New label is null or empty based on configuration. Routing to Failure.", originator);
            ctx.tellFailure(msg, new IllegalArgumentException("New label could not be determined or is empty."));
            return;
        }

        log.debug("[{}] Determined new label: '{}'", originator, newLabel);

        switch (entityType) {
            case DEVICE:
                processDevice(ctx, msg, (DeviceId) originator, newLabel);
                break;
            case ASSET:
                processAsset(ctx, msg, (AssetId) originator, newLabel);
                break;
            case CUSTOMER:
                processCustomer(ctx, msg, (CustomerId) originator, newLabel);
                break;
            default:
                log.warn("[{}] Unsupported entity type: {}. Routing to Failure.", originator, entityType);
                ctx.tellFailure(msg, new IllegalArgumentException("Unsupported entity type: " + entityType));
        }
    }

    private String determineNewLabel(TbContext ctx, TbMsg msg) {
        switch (config.getLabelSource()) {
            case STATIC:
                return config.getStaticLabelValue();
            case MESSAGE_METADATA:
                return msg.getMetaData().getValue(config.getLabelNameOrPattern());
            case MESSAGE_DATA:
                try {
                    JsonNode dataNode = MAPPER.readTree(msg.getData());
                    // Assuming labelNameOrPattern is a simple key for now.
                    // For JSON Pointer, we would need a library or more complex logic.
                    if (dataNode.has(config.getLabelNameOrPattern())) {
                        return dataNode.get(config.getLabelNameOrPattern()).asText();
                    } else {
                        log.warn("[{}] Label key '{}' not found in message data: {}", msg.getOriginator(), config.getLabelNameOrPattern(), msg.getData());
                        return null;
                    }
                } catch (IOException e) {
                    log.warn("[{}] Failed to parse message data to JSON: {}. Error: {}", msg.getOriginator(), msg.getData(), e.getMessage());
                    return null;
                }
            default:
                log.warn("[{}] Unknown label source: {}", msg.getOriginator(), config.getLabelSource());
                return null;
        }
    }

    private void processDevice(TbContext ctx, TbMsg msg, DeviceId deviceId, String newLabel) {
        DeviceService deviceService = ctx.getDeviceService();
        ListenableFuture<Device> deviceFuture = deviceService.findDeviceByIdAsync(ctx.getTenantId(), deviceId);

        Futures.addCallback(deviceFuture, new FutureCallback<Device>() {
            @Override
            public void onSuccess(@Nullable Device device) {
                if (device != null) {
                    device.setName(newLabel);
                    try {
                        // Assuming the boolean argument is for 'trigger events' or similar. Using 'false' as a safer default.
                        // API 4.0.1+ seems to have saveDevice as synchronous
                        deviceService.saveDevice(device, false);
                        log.debug("[{}] Successfully updated device label to '{}'", deviceId, newLabel);
                        ctx.tellNext(msg, "Success");
                    } catch (Exception e) {
                        log.error("[{}] Failed to save device with updated label: {}", deviceId, e.getMessage(), e);
                        ctx.tellFailure(msg, e);
                    }
                } else {
                    log.warn("[{}] Device not found.", deviceId);
                    ctx.tellFailure(msg, new RuntimeException("Device not found: " + deviceId));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to fetch device: {}", deviceId, t.getMessage(), t);
                ctx.tellFailure(msg, t);
            }
        }, ctx.getDbCallbackExecutor());
    }

    private void processAsset(TbContext ctx, TbMsg msg, AssetId assetId, String newLabel) {
        AssetService assetService = ctx.getAssetService();
        ListenableFuture<Asset> assetFuture = assetService.findAssetByIdAsync(ctx.getTenantId(), assetId);

        Futures.addCallback(assetFuture, new FutureCallback<Asset>() {
            @Override
            public void onSuccess(@Nullable Asset asset) {
                if (asset != null) {
                    asset.setName(newLabel);
                    try {
                        // Assuming the boolean argument is for 'trigger events' or similar. Using 'false' as a safer default.
                        // API 4.0.1+ seems to have saveAsset as synchronous
                        assetService.saveAsset(asset, false);
                        log.debug("[{}] Successfully updated asset label to '{}'", assetId, newLabel);
                        ctx.tellNext(msg, "Success");
                    } catch (Exception e) {
                        log.error("[{}] Failed to save asset with updated label: {}", assetId, e.getMessage(), e);
                        ctx.tellFailure(msg, e);
                    }
                } else {
                    log.warn("[{}] Asset not found.", assetId);
                    ctx.tellFailure(msg, new RuntimeException("Asset not found: " + assetId));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to fetch asset: {}", assetId, t.getMessage(), t);
                ctx.tellFailure(msg, t);
            }
        }, ctx.getDbCallbackExecutor());
    }

    private void processCustomer(TbContext ctx, TbMsg msg, CustomerId customerId, String newLabel) {
        CustomerService customerService = ctx.getCustomerService();
        ListenableFuture<Customer> customerFuture = customerService.findCustomerByIdAsync(ctx.getTenantId(), customerId);

        Futures.addCallback(customerFuture, new FutureCallback<Customer>() {
            @Override
            public void onSuccess(@Nullable Customer customer) {
                if (customer != null) {
                    customer.setTitle(newLabel); // Customer uses setTitle
                    try {
                        // API 4.0.1+ seems to have saveCustomer as synchronous and taking only one argument
                        customerService.saveCustomer(customer);
                        log.debug("[{}] Successfully updated customer label to '{}'", customerId, newLabel);
                        ctx.tellNext(msg, "Success");
                    } catch (Exception e) {
                        log.error("[{}] Failed to save customer with updated label: {}", customerId, e.getMessage(), e);
                        ctx.tellFailure(msg, e);
                    }
                } else {
                    log.warn("[{}] Customer not found.", customerId);
                    ctx.tellFailure(msg, new RuntimeException("Customer not found: " + customerId));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                log.error("[{}] Failed to fetch customer: {}", customerId, t.getMessage(), t);
                ctx.tellFailure(msg, t);
            }
        }, ctx.getDbCallbackExecutor());
    }

    @Override
    public void destroy() {
        // No specific resources to release for this node.
    }
}
