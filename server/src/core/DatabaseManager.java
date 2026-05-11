package core;

import java.sql.*;
import java.util.Date;

public class DatabaseManager {
    private final String url;
    private final String login;
    private final String password;
    private Connection connection;

    public DatabaseManager(String host, int port, String dbName, String login, String password) {
        this.login = login;
        this.password = password;
        this.url = "jdbc:postgresql://"+host+":"+port+"/"+dbName;
    }

    public void connect() throws SQLException {
        connection = DriverManager.getConnection(url, login, password);
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void initDatabase() throws SQLException {
        try (Statement st = connection.createStatement()) {
            st.execute("CREATE SEQUENCE IF NOT EXISTS ticket_id_seq");
            st.execute("CREATE TABLE IF NOT EXISTS tickets (" +
                    "id BIGINT PRIMARY KEY DEFAULT nextval('ticket_id_seq')," +
                    "name TEXT NOT NULL," +
                    "x DOUBLE PRECISION," +
                    "y DOUBLE PRECISION NOT NULL," +
                    "creation_date TIMESTAMP NOT NULL DEFAULT NOW()," +
                    "price DOUBLE PRECISION CHECK (price > 0)," +
                    "comment TEXT NOT NULL," +
                    "refundable BOOLEAN," +
                    "type TEXT," +
                    "event_name TEXT," +
                    "event_tickets_count BIGINT," +
                    "event_event_type TEXT," +
                    "user_id INTEGER NOT NULL)");
            st.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id SERIAL PRIMARY KEY," +
                    "login TEXT UNIQUE NOT NULL," +
                    "password_hash TEXT NOT NULL)");
            st.execute("SELECT setval('ticket_id_seq', COALESCE((SELECT MAX(id) FROM tickets), 0) + 1, false)");
        }
    }

    public void loadCollection(Manager manager) throws SQLException {
        String sql = "SELECT * FROM tickets";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Ticket ticket = new Ticket();
                ticket.setId(rs.getLong("id"));
                ticket.setName(rs.getString("name"));

                Coordinates coordinates = new Coordinates();
                coordinates.setX(rs.getDouble("x"));
                coordinates.setY(rs.getDouble("y"));
                ticket.setCoordinates(coordinates);

                ticket.setCreationDate(new Date(rs.getTimestamp("creation_date").getTime()));

                ticket.setPrice(rs.getDouble("price"));
                ticket.setComment(rs.getString("comment"));
                ticket.setRefundable(rs.getBoolean("refundable"));

                String typeStr = rs.getString("type");
                if (typeStr != null) {
                    ticket.setType(TicketType.valueOf(typeStr));
                }

                String eventName = rs.getString("event_name");
                if (eventName != null) {
                    Event event = new Event();
                    event.setName(eventName);
                    event.setTicketsCount(rs.getLong("event_tickets_count"));
                    String eventTypeStr = rs.getString("event_event_type");
                    if (eventTypeStr != null) {
                        event.setEventType(EventType.valueOf(eventTypeStr));
                    }
                    ticket.setEvent(event);
                }

                ticket.setUserId(rs.getInt("user_id"));

                manager.addTicketDirectly(ticket);
            }
            System.out.println("Коллекция загружена из БД. Элементов: " + manager.getSnapshot().size());
        }
    }

    public long addTicket(Ticket ticket, int userId) throws SQLException {
        String sql = "INSERT INTO tickets (name, x, y, creation_date, price, comment, refundable, type, " +
                "event_name, event_tickets_count, event_event_type, user_id) " +
                "VALUES (?,?,?,?,?,?,?,?,?,?,?,?) RETURNING id";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, ticket.getName());
            Coordinates c = ticket.getCoordinates();
            if (c.getX() != null) {
                ps.setDouble(2, c.getX());
            } else {
                ps.setNull(2, Types.DOUBLE);
            }
            ps.setDouble(3, c.getY());
            ps.setTimestamp(4, new Timestamp(ticket.getCreationDate().getTime()));
            ps.setDouble(5, ticket.getPrice());
            ps.setString(6, ticket.getComment());
            ps.setBoolean(7, ticket.isRefundable());
            ps.setString(8, ticket.getType() != null ? ticket.getType().name() : null);
            Event e = ticket.getEvent();
            if (e != null) {
                ps.setString(9, e.getName());
                ps.setLong(10, e.getTicketsCount());
                ps.setString(11, e.getEventType().name());
            } else {
                ps.setNull(9, Types.VARCHAR);
                ps.setNull(10, Types.BIGINT);
                ps.setNull(11, Types.VARCHAR);
            }
            ps.setInt(12, userId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    long newId = rs.getLong(1);
                    ticket.setId(newId);
                    ticket.setUserId(userId);
                    return newId;
                }
            }
        }
        throw new SQLException("Не удалось получить id после вставки");
    }

    public void updateTicket(long id, Ticket newData, int userId) throws SQLException {
        String checkSql = "SELECT user_id FROM tickets WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException("Билет с id=" + id + " не найден");
                }
                if (rs.getInt("user_id") != userId) {
                    throw new SecurityException("Недостаточно прав для изменения билета");
                }
            }
        }

        String updateSql = "UPDATE tickets SET name=?, x=?, y=?, price=?, comment=?, refundable=?, type=?, " +
                "event_name=?, event_tickets_count=?, event_event_type=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(updateSql)) {
            ps.setString(1, newData.getName());
            Coordinates c = newData.getCoordinates();
            ps.setDouble(2, c.getX() != null ? c.getX() : 0);
            ps.setDouble(3, c.getY());
            ps.setDouble(4, newData.getPrice());
            ps.setString(5, newData.getComment());
            ps.setBoolean(6, newData.isRefundable());
            ps.setString(7, newData.getType() != null ? newData.getType().name() : null);
            Event e = newData.getEvent();
            if (e != null) {
                ps.setString(8, e.getName());
                ps.setLong(9, e.getTicketsCount());
                ps.setString(10, e.getEventType().name());
            } else {
                ps.setNull(8, Types.VARCHAR);
                ps.setNull(9, Types.BIGINT);
                ps.setNull(10, Types.VARCHAR);
            }
            ps.setLong(11, id);
            ps.executeUpdate();
        }
    }

    public boolean removeTicket(long id, int userId) throws SQLException {
        String checkSql = "SELECT user_id FROM tickets WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;
                if (rs.getInt("user_id") != userId) {
                    throw new SecurityException("Недостаточно прав для удаления");
                }
            }
        }
        String deleteSql = "DELETE FROM tickets WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(deleteSql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public void clearUserTickets(int userId) throws SQLException {
        String sql = "DELETE FROM tickets WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public int registerUser(String login, String password) throws SQLException {
        // Проверим, не занят ли логин
        String checkSql = "SELECT id FROM users WHERE login = ?";
        try (PreparedStatement ps = connection.prepareStatement(checkSql)) {
            ps.setString(1, login);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    throw new SQLException("Логин уже используется");
                }
            }
        }

        String insertSql = "INSERT INTO users (login, password_hash) VALUES (?, md5(?)) RETURNING id";
        try (PreparedStatement ps = connection.prepareStatement(insertSql)) {
            ps.setString(1, login);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        throw new SQLException("Не удалось зарегистрировать пользователя");
    }

    public int authenticateUser(String login, String password) throws SQLException {
        String sql = "SELECT id FROM users WHERE login = ? AND password_hash = md5(?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, login);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                } else {
                    throw new SQLException("Неверный логин или пароль");
                }
            }
        }
    }
}
