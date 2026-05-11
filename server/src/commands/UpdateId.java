package commands;

import core.DatabaseManager;
import core.Manager;
import core.CommandResponse;
import core.Ticket;
import interfases.Command;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Команда, которая обновляет элемент по id.
 */
public class UpdateId implements Command {
    public CommandResponse execute(Manager manager, Map<String, Object> args, int userId) {
        if (args == null) return new CommandResponse("Некорректные аргументы");
        try {
            long id = Long.parseLong((String) args.get("arg1"));
            List<Ticket> snapshot = manager.getSnapshot();
            Optional<Ticket> optTicket = snapshot.stream().filter(t -> t.getId() == id).findFirst();
            if (!optTicket.isPresent()) return new CommandResponse("Элемент с таким id не найден");

            Ticket newTicket = new Ticket();
            newTicket.fromRequest(args);
            newTicket.setId(id);
            newTicket.setUserId(optTicket.get().getUserId());
            newTicket.setCreationDate(optTicket.get().getCreationDate());
            newTicket.setCoordinates(optTicket.get().getCoordinates());
            DatabaseManager db = manager.getDbManager();
            db.updateTicket(id, newTicket, userId);
            manager.replaceTicket(id, newTicket);
            return new CommandResponse("Элемент с id=" + id + " успешно обновлён");
        } catch (NumberFormatException e) {
            return new CommandResponse("Некорректный аргумент");
        } catch (SecurityException e) {
            return new CommandResponse("Недостаточно прав для изменения");
        } catch (SQLException e) {
            return new CommandResponse("Ошибка БД: " + e.getMessage());
        }
    }
}
