package network;

public enum ResponseCode {
    OK(0),
    INVALID_THREADS_NUMBER(1),
    INVALID_ELEMENTS_NUMBER(2),
    INVALID_ELEMENT(3),
    ERROR_SERVER(4);

    private byte value;

    private ResponseCode(int value){
        this.value = (byte)value;
    }

    byte getValue() {
        return value;
    }
}
