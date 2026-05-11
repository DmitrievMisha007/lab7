package core;

import java.io.Serializable;

public class CommandResponse implements Serializable {
    private final String string;

    public CommandResponse(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }
}
