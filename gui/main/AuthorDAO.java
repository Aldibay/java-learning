package kz.uib.kitaphana.dao;

import kz.uib.kitaphana.config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AuthorDAO {
    public record Author(int id, String fullName, String country) {
        @Override public String toString() { return fullName + " (#" + id + ")"; }
    }

    /** JTable/ComboBox үшін сол дерекқор жазбаларын қайтарады. */
    public List<Author> list() throws SQLException {
        String sql = "SELECT id, full_name, country FROM authors ORDER BY id";
        List<Author> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Author(rs.getInt("id"), rs.getString("full_name"),
                        rs.getString("country")));
            }
        }
        return result;
    }

    public void create(String fullName, String country) throws SQLException {
        String sql = "INSERT INTO authors(full_name, country) VALUES (?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, country);
            ps.executeUpdate();
            System.out.println("Автор қосылды.");
        }
    }

    public void readAll() throws SQLException {
        String sql = "SELECT id, full_name, country FROM authors ORDER BY id";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            print(rs);
        }
    }

    public void search(String word) throws SQLException {
        String sql = "SELECT id, full_name, country FROM authors WHERE full_name ILIKE ? ORDER BY id";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + word + "%");
            try (ResultSet rs = ps.executeQuery()) { print(rs); }
        }
    }

    public boolean update(int id, String fullName, String country) throws SQLException {
        String sql = "UPDATE authors SET full_name=?, country=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, country);
            ps.setInt(3, id);
            int changed = ps.executeUpdate();
            printChanged(changed);
            return changed > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM authors WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            int changed = ps.executeUpdate();
            printChanged(changed);
            return changed > 0;
        }
    }

    private void print(ResultSet rs) throws SQLException {
        boolean found = false;
        while (rs.next()) {
            found = true;
            System.out.printf("ID: %d | Автор: %s | Ел: %s%n",
                    rs.getInt("id"), rs.getString("full_name"), rs.getString("country"));
        }
        if (!found) System.out.println("Жазба табылмады.");
    }

    private void printChanged(int count) {
        System.out.println(count > 0 ? "Операция орындалды." : "Көрсетілген ID табылмады.");
    }
}
