package commands;

import core.DatabaseManager;
import core.Manager;
import core.CommandResponse;
import interfases.Command;

import java.sql.SQLException;
import java.util.Map;

/**
 * Команда очистки коллекции.
 */
public class Clear implements Command {
    /**
     * Вызывает команду.
     *
     * @return Ответ клиенту
     */
    @Override
    public CommandResponse execute(Manager manager, Map<String, Object> args, int userId) {
        if (args != null) return new CommandResponse("Команда не принимает аргументов");
        try {
            DatabaseManager db = manager.getDbManager();
            db.clearUserTickets(userId);
            manager.removeUserTickets(userId);
            return new CommandResponse("Ваши билеты удалены");
        } catch (SQLException e) {
            return new CommandResponse("Ошибка БД: " + e.getMessage());
        }
    }
}
