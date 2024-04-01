package backend;

import backend.MIPSValues.Register;

import java.util.*;

public class RegisterManager {
    TreeSet<Register> freeSavedRegs = new TreeSet<>();
    TreeSet<Register> occupiedSavedRegs = new TreeSet<>();

    Set<Register> allGlobalReg = new HashSet<>();

    TreeSet<Register> freeGlobalRegs = new TreeSet<>();
    TreeSet<Register> occupiedGlobalRegs = new TreeSet<>();
    HashMap<Register, Boolean> globalRegsStatus = new HashMap<>();
    Stack<HashMap<Register, Boolean>> savedStatusStore = new Stack<>();
    Stack<HashMap<Register, Boolean>> globalStatusStore = new Stack<>();
    HashMap<Register, Boolean> SavedRegsStatus = new HashMap<>();

    Queue<Register> freeTempRegs = new LinkedList<>();
    Queue<Register> occupiedTempRegs = new LinkedList<>();
    HashMap<Register, Boolean> tempRegsStatus = new HashMap<>();

    private int globalCnt;

    public RegisterManager() {
        initFreeSavedRegs();
        initSavedStatus();
        initFreeTemp();
        initTempStatus();
        initAllGReg();
    }

    private void initAllGReg() {
        allGlobalReg = new TreeSet<>();
        int cnt = 0;
        for(Register reg : Register.values()){
            if(reg.isMyGlobal()){
                allGlobalReg.add(reg);
                cnt++;
            }
        }
        globalCnt = cnt;
    }

    public void initGlobal(Set<Register> occupied){
        initGlobalRegs(occupied);
        initGlobalStatus(occupied);
    }

    public void initGlobalAsSavedReg(Set<Register> occupied){
        for(Register reg : Register.values()){
            if(reg.isMyGlobal() && !occupied.contains(reg)){
                freeSavedRegs.add(reg);
                SavedRegsStatus.put(reg, true);
            }
        }
    }


    //Saved Regs
    private void initFreeSavedRegs() {
        for(Register reg : Register.values()){
            if(reg.isMySaved()){
                freeSavedRegs.add(reg);
            }
        }
    }

    private void initGlobalRegs(Set<Register> occupied) {
        freeGlobalRegs = new TreeSet<>();
        for(Register reg : Register.values()){
            if(reg.isMyGlobal() && !occupied.contains(reg)){
                freeGlobalRegs.add(reg);
            }
        }
    }

    public int getGlobalCnt(){
        return globalCnt;
    }

    public void initSavedStatus() {
        for(Register reg : Register.values()){
            if(reg.isMySaved()){
                SavedRegsStatus.put(reg, true);
            }
        }
    }

    public void initGlobalStatus(Set<Register> occupied) {
        globalRegsStatus = new HashMap<>();
        for(Register reg : Register.values()){
            if(reg.isMyGlobal()&& !occupied.contains(reg)){
                globalRegsStatus.put(reg, true);
            }
        }
    }

    public Register getFreeSavedReg() {
        Register reg;
        if (!freeSavedRegs.isEmpty()) {
            reg = freeSavedRegs.first();
            freeSavedRegs.remove(reg);
            occupiedSavedRegs.add(reg);
            SavedRegsStatus.put(reg, false);
        } else {
            reg = null;
        }
        return reg;
    }

    public Register getFreeGlobalReg() {
        Register reg;
        if (!freeGlobalRegs.isEmpty()) {
            reg = freeGlobalRegs.first();
            freeGlobalRegs.remove(reg);
            occupiedGlobalRegs.add(reg);
            globalRegsStatus.put(reg, false);
        } else {
            reg = null;
        }
        return reg;
    }

    public List<Register> getOccupiedSavedRegs(){
        return occupiedSavedRegs.stream().toList();
    }

    public List<Register> getOccupiedGlobalRegs(){
        return occupiedGlobalRegs.stream().toList();
    }

    public void setSavedRegFree(Register reg) {
        if (reg.isMySaved()) {
            freeSavedRegs.add(reg);
            occupiedSavedRegs.remove(reg);
            SavedRegsStatus.put(reg, true);
        }
    }

    public void setGlobalRegFree(Register reg) {
        if (reg.isMyGlobal()) {
            freeGlobalRegs.add(reg);
            occupiedGlobalRegs.remove(reg);
            globalRegsStatus.put(reg, true);
        }
    }

    public void setAllSavedRegFree() {
        while (!occupiedSavedRegs.isEmpty()) {
            Register reg = occupiedSavedRegs.first();
            occupiedSavedRegs.remove(reg);
            freeSavedRegs.add(reg);
            SavedRegsStatus.put(reg, true);
        }
    }

    public void setAllGlobalRegFree() {
        while (!occupiedGlobalRegs.isEmpty()) {
            Register reg = occupiedGlobalRegs.first();
            occupiedGlobalRegs.remove(reg);
            freeGlobalRegs.add(reg);
            globalRegsStatus.put(reg, true);
        }
    }

    public void setSavedRegOccupied(Register reg) {
        if (reg.isMySaved()) {
            System.out.println("set occupied 2 "+reg);
            occupiedSavedRegs.add(reg);
            freeSavedRegs.remove(reg);
            SavedRegsStatus.put(reg, false);
        }
    }

    public void setGlobalRegOccupied(Register reg) {
        if (reg.isMyGlobal()) {
            occupiedGlobalRegs.add(reg);
            freeGlobalRegs.remove(reg);
            globalRegsStatus.put(reg, false);
        }
    }

    public void printAllSavedRegs() {
        System.out.println("=========Temp Regs=========");
        System.out.print("Free: ");
        for (Register reg : freeSavedRegs) {
            System.out.print(reg + " ");
        }
        System.out.print("\nOccupied: ");
        for (Register reg : occupiedSavedRegs) {
            System.out.print(reg + " ");
        }
        System.out.print("\n");
    }

    public List<Register> getAllSavedRegisters() {
        return new ArrayList<>(SavedRegsStatus.keySet());
    }

    public void saveGlobalStatus() {
        globalStatusStore.push(new HashMap<>(globalRegsStatus));
        setAllGlobalRegFree();
    }

    public void saveSavedStatus() {
        savedStatusStore.push(new HashMap<>(SavedRegsStatus));
        setAllSavedRegFree();
    }

    public void recoverGlobalStatus() {
        globalRegsStatus = globalStatusStore.pop();
        for (Map.Entry<Register, Boolean> entry : globalRegsStatus.entrySet()) {
            if (entry.getValue()) {
                setGlobalRegFree(entry.getKey());
            } else
                setGlobalRegOccupied(entry.getKey());
        }
    }

    public void recoverSavedStatus() {
        SavedRegsStatus = savedStatusStore.pop();
        for (Map.Entry<Register, Boolean> entry : SavedRegsStatus.entrySet()) {
            if (entry.getValue()) {
                setSavedRegFree(entry.getKey());
            } else {
                //System.out.println("set occupied "+entry.getKey());
                setSavedRegOccupied(entry.getKey());
            }
        }
    }

    public void recoverStatus() {
        SavedRegsStatus = savedStatusStore.pop();
        for (Map.Entry<Register, Boolean> entry : SavedRegsStatus.entrySet()) {
            if (entry.getValue()) {
                setSavedRegFree(entry.getKey());
            } else {
                //System.out.println("set occupied "+entry.getKey());
                setSavedRegOccupied(entry.getKey());
            }
        }
        globalRegsStatus = globalStatusStore.pop();
        for (Map.Entry<Register, Boolean> entry : globalRegsStatus.entrySet()) {
            if (entry.getValue()) {
                setGlobalRegFree(entry.getKey());
            } else
                setGlobalRegOccupied(entry.getKey());
        }
    }

    //Temp Regs
    private void initFreeTemp() {
        for(Register reg : Register.values()){
            if(reg.isMyTemp()){
                freeTempRegs.offer(reg);
            }
        }
        //printAllTemp();
    }

    public void initTempStatus() {
        for(Register reg : Register.values()){
            if(reg.isMyTemp()){
                tempRegsStatus.put(reg, true);
            }
        }
    }

    public Register getFreeTReg() {
        Register reg;
        if (freeTempRegs.peek() != null) {
            reg = freeTempRegs.poll();
            occupiedTempRegs.offer(reg);
            tempRegsStatus.put(reg, false);
        } else {
            reg = occupiedTempRegs.poll();
            occupiedTempRegs.offer(reg);
            System.out.println("use busy reg " + reg + " as free");
        }
        //printAllTemp();
        return reg;
    }

    public void setTempRegFree(Register reg) {
        if (reg.isMyTemp() && !tempRegsStatus.get(reg)) {
            freeTempRegs.offer(reg);
            occupiedTempRegs.remove(reg);
            tempRegsStatus.put(reg, true);
        }
        //printAllTemp();
    }

    public void setAllTempRegFree() {
        while (!occupiedTempRegs.isEmpty()) {
            Register reg = occupiedTempRegs.poll();
            freeTempRegs.offer(reg);
            tempRegsStatus.put(reg, true);
        }
        //printAllTemp();
    }

    public void setTSRegFree(Register reg) {
        if (reg.isMyTemp())
            setTempRegFree(reg);
        else if (reg.isMySaved())
            setSavedRegFree(reg);
    }


    public Register getAGlobal(Set<Register> regs){
        TreeSet<Register> all = new TreeSet<>(allGlobalReg);
        all.removeAll(regs);
        if(all.isEmpty())
            return null;
        else
            return all.iterator().next();
    }

    public Set<Register> getAllGlobalReg(){
        Set<Register> regs = new HashSet<>();
        for(Register reg : Register.values()){
            if(reg.isMyGlobal()){
                regs.add(reg);
            }
        }
        return regs;
    }
}
