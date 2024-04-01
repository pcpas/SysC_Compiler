package midend.Value;

public class Use{
    private final User user;
    //index of Use
    private final int index;
    private IRValue value;

    public Use(IRValue value, User user, int index) {
        this.value = value;
        this.user = user;
        this.index = index;
    }
    public IRValue getValue(){
        return value;
    }
    public User getUser(){
        return user;
    }

    public int getIndex() {
        return index;
    }

    public String toString(){
        return String.format("[User: %-10s", user) + String.format(" Use %s]", value);
    }
}
