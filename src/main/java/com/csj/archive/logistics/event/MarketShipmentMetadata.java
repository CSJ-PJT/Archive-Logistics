package com.csj.archive.logistics.event;

import static org.springframework.util.StringUtils.hasText;

public record MarketShipmentMetadata(
        String orderId,
        String customerId,
        String customerType,
        String productType,
        Long orderAmount,
        Long totalAmount,
        Integer riskLevel,
        Boolean expressOrder,
        String marketPriority,
        Boolean vipCustomer,
        String riskTag,
        String simulationRunId,
        String settlementCycleId,
        String correlationId,
        String causationId,
        Integer hopCount,
        Integer maxHop
) {
    public static MarketShipmentMetadata fromPayload(NexusLogisticsEventRequest.Payload payload) {
        if (payload == null) {
            return new MarketShipmentMetadata(
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            );
        }

        return new MarketShipmentMetadata(
                trim(payload.orderId()),
                trim(payload.customerId()),
                trim(payload.customerType()),
                trim(payload.productType()),
                payload.orderAmount(),
                payload.totalAmount(),
                payload.riskLevel(),
                payload.expressOrder(),
                trim(payload.marketPriority()),
                payload.vipCustomer(),
                trim(payload.riskTag()),
                payload.simulationRunId(),
                payload.settlementCycleId(),
                payload.correlationId(),
                payload.causationId(),
                payload.hopCount(),
                payload.maxHop()
        );
    }

    public boolean isExpressOrder() {
        return Boolean.TRUE.equals(expressOrder);
    }

    public boolean isVipCustomer() {
        return "VIP_CUSTOMER".equalsIgnoreCase(customerType)
                || Boolean.TRUE.equals(vipCustomer);
    }

    public boolean isHighRiskCustomer() {
        return "HIGH_RISK_CUSTOMER".equalsIgnoreCase(riskTag)
                || "HIGH_RISK_CUSTOMER".equalsIgnoreCase(customerType)
                || (riskLevel != null && riskLevel >= 4);
    }

    public int effectiveRiskLevel() {
        return riskLevel == null ? 0 : riskLevel;
    }

    public int safeHopCount() {
        return hopCount == null ? 0 : Math.max(0, hopCount);
    }

    public int safeMaxHop(int fallbackMaxHop) {
        return maxHop == null || maxHop <= 0 ? fallbackMaxHop : Math.max(1, maxHop);
    }

    private static String trim(String value) {
        return hasText(value) ? value : null;
    }
}
