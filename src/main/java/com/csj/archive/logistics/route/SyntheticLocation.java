package com.csj.archive.logistics.route;

public enum SyntheticLocation {
    FAC_A("FAC-A"),
    FAC_B("FAC-B"),
    FAC_C("FAC-C"),
    DC_SEOUL_01("DC-SEOUL-01"),
    DC_DAEJEON_01("DC-DAEJEON-01"),
    DC_BUSAN_01("DC-BUSAN-01"),
    VENDOR_LOGISTICS_01("VENDOR-LOGISTICS-01"),
    VENDOR_LOGISTICS_02("VENDOR-LOGISTICS-02");

    private final String code;

    SyntheticLocation(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}
