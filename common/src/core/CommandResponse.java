package core;

import java.io.Serializable;

/**
 * Сериализуемый объект ответа, возвращаемый сервером клиенту.
 * Инкапсулирует текстовое сообщение о результате выполнения команды.
 */
public class CommandResponse implements Serializable {
    private final String string;

    public CommandResponse(String string) {
        this.string = string;
    }

    public String getString() {
        return string;
    }
}
