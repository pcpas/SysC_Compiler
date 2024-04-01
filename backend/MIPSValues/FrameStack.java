package backend.MIPSValues;

import java.util.ArrayList;
import java.util.List;

public class FrameStack {
    int regSize;
    int tempSize;
    int paramSize;

    public FrameStack(int initSize) {
        regSize = initSize;
        tempSize = 0;
        paramSize = 0;
    }

    int getFsSize() {
        return regSize + tempSize + paramSize;
    }

    int getRegOffset(int index) {
        if (index * 4 >= regSize) {
            System.out.println("getRegOffset: out of boundary");
            return 0;
        }
        return paramSize + tempSize + 4 * index;
    }

    int getTempOffset(int index) {
        if (index * 4 >= tempSize) {
            System.out.println("getTempOffset: out of boundary");
            return 0;
        }
        return paramSize + 4 * index;
    }

    int getParamOffset(int index) {
        if (index * 4 >= paramSize) {
            System.out.println("getParamOffset: out of boundary");
            return 0;
        }
        return index * 4;
    }

    int getParamInOffset(int index) {
        return paramSize + regSize + tempSize + 4 * index;
    }


    public MemAddr allocTemp(int words, boolean containPointer) {
        Placeholder placeholder = new Placeholder(this, Placeholder.PlaceholderType.TEMP, tempSize / 4);
        tempSize += 4 * words;
        return new MemAddr(Register.R30, placeholder, containPointer);
    }

    public MemAddr allocReg(int words) {
        Placeholder placeholder = new Placeholder(this, Placeholder.PlaceholderType.REG, regSize / 4);
        regSize += 4 * words;
        return new MemAddr(Register.R30, placeholder);
    }

    public MemAddr getParamInAddr(int index, boolean containPointer) {
        return new MemAddr(Register.R30, getParamIn(index), containPointer);
    }

    public Placeholder getParamIn(int index){
        return new Placeholder(this, Placeholder.PlaceholderType.PAPAMIN, index);
    }

    public List<MemAddr> allocParam(int paramCnt) {
        List<MemAddr> list = new ArrayList<>();
        for (int i = 0; i < paramCnt; i++) {
            Placeholder placeholder = new Placeholder(this, Placeholder.PlaceholderType.PARAM, i);
            list.add(new MemAddr(Register.R30, placeholder));
        }
        if (paramCnt * 4 > paramSize)
            paramSize = paramCnt * 4;
        return list;
    }

    public Placeholder getReg(int index) {
        return new Placeholder(this, Placeholder.PlaceholderType.REG, index);
    }

    public MemAddr getRegAddr(int index) {
        return new MemAddr(Register.R30, getReg(index));
    }

    public Placeholder getSize(int neg) {
        return new Placeholder(this, Placeholder.PlaceholderType.SIZE, 0, neg);
    }


    public class Placeholder implements Immediate {

        private final FrameStack parent;
        private final PlaceholderType type;
        private final int index;
        private final int neg;

        public Placeholder(FrameStack parent, PlaceholderType type, int index) {
            this.parent = parent;
            this.type = type;
            this.index = index;
            this.neg = 0;
        }

        public Placeholder(FrameStack parent, PlaceholderType type, int index, int neg) {
            this.parent = parent;
            this.type = type;
            this.index = index;
            this.neg = neg;
        }

        public Placeholder getOffsetPh(int words) {
            return new Placeholder(parent, type, index + words, neg);
        }

        @Override
        public String toString() {
            int ret = 0;
            switch (type) {
                case REG -> ret = parent.getRegOffset(index);
                case TEMP -> ret = parent.getTempOffset(index);
                case PARAM -> ret = parent.getParamOffset(index);
                case PAPAMIN -> ret = parent.getParamInOffset(index);
                case SIZE -> ret = parent.getFsSize();
            }
            if (neg == 1) ret = -ret;
            return String.valueOf(ret);
        }

        public enum PlaceholderType {
            REG, TEMP, PARAM, SIZE, PAPAMIN;
        }
    }
}
