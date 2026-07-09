package com.csj.archive.logistics.route;

import com.csj.archive.logistics.common.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;

@Component
public class SyntheticDistanceMatrix {
    private static final Map<String, BigDecimal> DISTANCES = Map.ofEntries(
            entry("FAC-A", "DC-SEOUL-01", 42),
            entry("FAC-A", "DC-DAEJEON-01", 151),
            entry("FAC-A", "DC-BUSAN-01", 325),
            entry("FAC-B", "DC-SEOUL-01", 88),
            entry("FAC-B", "DC-DAEJEON-01", 76),
            entry("FAC-B", "DC-BUSAN-01", 241),
            entry("FAC-C", "DC-SEOUL-01", 210),
            entry("FAC-C", "DC-DAEJEON-01", 132),
            entry("FAC-C", "DC-BUSAN-01", 185)
    );

    public BigDecimal distanceKm(String originCode, String destinationCode) {
        BigDecimal distance = DISTANCES.get(key(originCode, destinationCode));
        if (distance == null) {
            throw new BusinessException(
                    "UNKNOWN_ROUTE",
                    "Unknown synthetic route: " + originCode + " -> " + destinationCode,
                    HttpStatus.BAD_REQUEST
            );
        }
        return distance;
    }

    private static Map.Entry<String, BigDecimal> entry(String originCode, String destinationCode, int distanceKm) {
        return Map.entry(key(originCode, destinationCode), BigDecimal.valueOf(distanceKm));
    }

    private static String key(String originCode, String destinationCode) {
        return originCode + "->" + destinationCode;
    }
}
