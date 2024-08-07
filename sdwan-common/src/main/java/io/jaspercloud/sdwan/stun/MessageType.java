package io.jaspercloud.sdwan.stun;

public enum MessageType {

    BindRequest(0x0001),
    BindResponse(0x0101),
    Transfer(0x0301),
    BindRelayRequest(0x0303),
    BindRelayResponse(0x0304),
    RefreshRelayRequest(0x0305),
    RefreshRelayResponse(0x0306),
    PingRequest(0x0307),
    PingResponse(0x0308);

    private int code;

    public int getCode() {
        return code;
    }

    MessageType(int code) {
        this.code = code;
    }

    public static MessageType valueOf(int code) {
        for (MessageType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        return null;
    }
}
