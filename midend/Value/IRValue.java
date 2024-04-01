package midend.Value;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class IRValue {
    protected HashMap<User, Use> useList = new HashMap<>();
    protected List<DefUse> defUseList = new ArrayList<>();
    protected void addUse(Use use) {
        useList.put(use.getUser(), use);
    }

    public void addDefUse(DefUse def){
        defUseList.add(def);
    }

    protected void removeUser(User user){
        useList.remove(user);
    }

    public Use findUse(User inst){
        return useList.get(inst);
    }

    public boolean isNeverUsed(){
//        System.out.println("debug: "+this.toString());
//        System.out.println(useList.values());
        return useList.isEmpty();
    }

    public HashMap<User, Use> getUseList(){
        return useList;
    }

    public List<DefUse> getDefUseList() {
        return defUseList;
    }
}
