package backend.MIPSValues;

public class Label implements MValue {
    private final String value;

    public Label(String value) {
        this.value = value;
    }

    public boolean isSame(MBasicBlock b){
        return b.getName().equals(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
