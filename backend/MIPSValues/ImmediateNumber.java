package backend.MIPSValues;

public class ImmediateNumber implements Immediate {
    private final String value;

    public ImmediateNumber(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        return value;
    }
}
