package core;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

public class Manager {
    private final Date initDate;
    private final ArrayDeque<Ticket> collection = new ArrayDeque<>();
    private final ArrayDeque<String> history;
    private final ReentrantLock lock = new ReentrantLock();
    private final DatabaseManager dbManager;

    public Manager(DatabaseManager dbManager) {
        this.dbManager = dbManager;
        initDate = new Date();
        history = new ArrayDeque<>();
        try {
            dbManager.loadCollection(this);
        } catch (Exception e) {
            System.err.println("Ошибка загрузки коллекции из БД: " + e.getMessage());
        }
    }

    public DatabaseManager getDbManager() { return dbManager; }

    public void addTicketDirectly(Ticket ticket) {
        lock.lock();
        try { collection.add(ticket); } finally { lock.unlock(); }
    }

    public boolean removeTicketById(long id) {
        lock.lock();
        try { return collection.removeIf(t -> t.getId() == id); } finally { lock.unlock(); }
    }

    public void removeUserTickets(int userId) {
        lock.lock();
        try { collection.removeIf(t -> t.getUserId() == userId); } finally { lock.unlock(); }
    }

    public List<Ticket> getSnapshot() {
        lock.lock();
        try { return new ArrayList<>(collection); } finally { lock.unlock(); }
    }

    public ArrayDeque<String> getHistory() { return history; }
    public void updateHistory(String commandName) {
        history.add(commandName);
        if (history.size() > 10) history.removeFirst();
    }


    public String info() {
        lock.lock();
        try {
            return "type: " + collection.getClass().getName() + "\n" +
                    "init date: " + initDate + "\n" +
                    "amount of elements: " + collection.size();
        } finally { lock.unlock(); }
    }

    public String getStringToShow() {
        List<Ticket> snapshot = getSnapshot();
        if (snapshot.isEmpty()) return "Коллекция пуста";
        StringBuilder result = new StringBuilder();
        for (Ticket t : snapshot) result.append(t.toString()).append("\n");
        return result.toString();
    }

    public boolean replaceTicket(long id, Ticket newTicket) {
        lock.lock();
        try {
            Iterator<Ticket> it = collection.iterator();
            while (it.hasNext()) {
                Ticket t = it.next();
                if (t.getId() == id) {
                    it.remove();
                    collection.add(newTicket);
                    return true;
                }
            }
            return false;
        } finally {
            lock.unlock();
        }
    }

}