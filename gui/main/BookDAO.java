package kz.uib.kitaphana.dao;

import kz.uib.kitaphana.config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {
    public record Book(int id, String title, int authorId, String authorName,
                       int year, String isbn, int totalCopies, int availableCopies) {
        public int borrowedCopies() { return totalCopies - availableCopies; }
        @Override public String toString() {
            return title + " — қолда: " + availableCopies + " (#" + id + ")";
        }
    }

    private static final String LIST_SQL =
            "SELECT b.id, b.title, b.author_id, a.full_name AS author, b.publication_year, "
          + "b.isbn, b.total_copies, b.available_copies "
          + "FROM books b JOIN authors a ON a.id=b.author_id ";

    /** Бірдей SELECT арқылы JTable мен редакторға қажет author_id-ді де аламыз. */
    public List<Book> list() throws SQLException {
        List<Book> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(LIST_SQL + "ORDER BY b.id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Book(rs.getInt("id"), rs.getString("title"),
                        rs.getInt("author_id"), rs.getString("author"),
                        rs.getInt("publication_year"), rs.getString("isbn"),
                        rs.getInt("total_copies"), rs.getInt("available_copies")));
            }
        }
        return result;
    }

    public void create(String title, int authorId, int year,
                       String isbn, int copies) throws SQLException {
        String sql = "INSERT INTO books(title, author_id, publication_year, isbn, "
                   + "total_copies, available_copies) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, title);
            ps.setInt(2, authorId);
            ps.setInt(3, year);
            ps.setString(4, isbn);
            ps.setInt(5, copies);
            ps.setInt(6, copies);
            ps.executeUpdate();
            System.out.println("Кітап қосылды.");
        }
    }

    public void readAll() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(LIST_SQL + "ORDER BY b.id");
             ResultSet rs = ps.executeQuery()) {
            print(rs);
        }
    }

    public void search(String word) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     LIST_SQL + "WHERE b.title ILIKE ? ORDER BY b.id")) {
            ps.setString(1, "%" + word + "%");
            try (ResultSet rs = ps.executeQuery()) { print(rs); }
        }
    }

    // Қордағы кітапты азайту кезінде берілген дана санын ескеру міндетті.
    public boolean update(int id, String title, int authorId, int year,
                       String isbn, int newTotal) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int borrowed;
                String checkSql = "SELECT total_copies, available_copies FROM books WHERE id=? FOR UPDATE";
                try (PreparedStatement ps = c.prepareStatement(checkSql)) {
                    ps.setInt(1, id);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            System.out.println("Көрсетілген кітап табылмады.");
                            c.rollback();
                            return false;
                        }
                        borrowed = rs.getInt("total_copies") - rs.getInt("available_copies");
                    }
                }
                if (newTotal < borrowed || newTotal < 1) {
                    System.out.println("Қате: жалпы дана саны берілген кітап санынан аз болмауы керек (берілгені: "
                            + borrowed + ").");
                    c.rollback();
                    return false;
                }
                String sql = "UPDATE books SET title=?, author_id=?, publication_year=?, "
                           + "isbn=?, total_copies=?, available_copies=? WHERE id=?";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setString(1, title);
                    ps.setInt(2, authorId);
                    ps.setInt(3, year);
                    ps.setString(4, isbn);
                    ps.setInt(5, newTotal);
                    ps.setInt(6, newTotal - borrowed);
                    ps.setInt(7, id);
                    ps.executeUpdate();
                }
                c.commit();
                System.out.println("Кітап ақпараты өзгертілді.");
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM books WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            int count = ps.executeUpdate();
            System.out.println(count > 0 ? "Кітап жойылды." : "Көрсетілген кітап табылмады.");
            return count > 0;
        }
    }

    private void print(ResultSet rs) throws SQLException {
        boolean found = false;
        while (rs.next()) {
            found = true;
            System.out.printf("ID: %d | %s | Автор: %s | Жылы: %d | ISBN: %s | Барлығы: %d | Қолда: %d%n",
                    rs.getInt("id"), rs.getString("title"), rs.getString("author"),
                    rs.getInt("publication_year"), rs.getString("isbn"),
                    rs.getInt("total_copies"), rs.getInt("available_copies"));
        }
        if (!found) System.out.println("Кітап табылмады.");
    }
}
