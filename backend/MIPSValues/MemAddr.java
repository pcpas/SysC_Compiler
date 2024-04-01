package backend.MIPSValues;

public class MemAddr implements MValue {
    private MValue base;
    private Immediate offset;

    private boolean containPointer;

    public MemAddr(MValue base, Immediate offset) {
        this.base = base;
        this.offset = offset;
        this.containPointer = false;
    }

    public MemAddr(MValue base, Immediate offset, boolean containBoolean) {
        this.base = base;
        this.offset = offset;
        this.containPointer = containBoolean;
    }

    public Immediate getOffset() {
        return offset;
    }

    public MValue getBase() {
        return base;
    }

    public MemAddr getOffsetAddr(int words) {
        Immediate newOffset = null;
        if (offset instanceof ImmediateNumber number) {
            newOffset = new ImmediateNumber(String.valueOf(Integer.parseInt(number.toString()) + words * 4));
        } else if (offset instanceof FrameStack.Placeholder ph) {
            newOffset = ph.getOffsetPh(words);
        }
        return new MemAddr(base, newOffset);
    }

    public boolean isContainPointer() {
        return containPointer;
    }
}
