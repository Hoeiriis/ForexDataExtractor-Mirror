package roots.Snapshots;

import java.util.UUID;

public class Snapshot <T> {

    T[] Window;
    public final UUID id;
    public String description;

    public Snapshot(UUID subWinID, String description){
        id = subWinID;
        this.description = description;
    }

    public void setWindow(T[] window) {
        Window = window.clone();
    }

    public T[] getWindow() {
        return Window;
    }
}
