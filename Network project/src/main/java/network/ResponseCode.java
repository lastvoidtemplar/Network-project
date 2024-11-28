package network;

public enum ResponseCode {
    OK(0),
    INVALID_THREADS_NUMBER(1),
    INVALID_ARRAY_LENGTH(2),
    SERVER_ERROR(3);

    private final byte value;

    ResponseCode(int value){
        this.value = (byte)value;
    }

    byte getValue() {
        return value;
    }
}
