package midend.Value;

import java.util.ArrayList;
import java.util.List;

public class User extends IRValue {
    protected List<IRValue> valueList;

    public User() {
        valueList = new ArrayList<>();
    }

    protected void createUse(IRValue value) {
        this.valueList.add(value);
        value.addUse(new Use(value, this, this.valueList.size() - 1));
    }

    public List<IRValue> getValueList() {
        return valueList;
    }

    public static void DeleteUser(User user){
        //System.out.println("Delete User: "+ user);
        //System.out.println(user.valueList);
        for(IRValue v : user.valueList){
            v.removeUser(user);
        }
    }

    public IRValue getValue(int index) {
        return this.valueList.get(index);
    }
}
