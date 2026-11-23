package kz.uib.kitaphana.dao;

import kz.uib.kitaphana.config.DBConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ReaderDAO {
    public record Reader(int id, String fullName, String phone) {
        @Override public String toString() { return fullName + " (#" + id + ")"; }
    }

    /** JTable/ComboBox үшін оқу әдісі, бұрынғы readAll() сақталады. */
    public List<Reader> list() throws SQLException {
        String sql = "SELECT id, full_name, phone FROM readers ORDER BY id";
        List<Reader> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                result.add(new Reader(rs.getInt("id"), rs.getString("full_name"),
                        rs.getString("phone")));
            }
        }
        return result;
    }

    public void create(String fullName, String phone) throws SQLException {
        String sql = "INSERT INTO readers(full_name, phone) VALUES (?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, phone);
            ps.executeUpdate();
            System.out.println("Оқырман қосылды.");
        }
    }

    public void readAll() throws SQLException {
        String sql = "SELECT id, full_name, phone FROM readers ORDER BY id";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            print(rs);
        }
    }

    public void search(String word) throws SQLException {
        String sql = "SELECT id, full_name, phone FROM readers WHERE full_name ILIKE ? ORDER BY id";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + word + "%");
            try (ResultSet rs = ps.executeQuery()) { print(rs); }
        }
    }

    public boolean update(int id, String fullName, String phone) throws SQLException {
        String sql = "UPDATE readers SET full_name=?, phone=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, fullName);
            ps.setString(2, phone);
            ps.setInt(3, id);
            int changed = ps.executeUpdate();
            printChanged(changed);
            return changed > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM readers WHERE id=?";
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
            System.out.printf("ID: %d | Оқырман: %s | Телефон: %s%n",
                    rs.getInt("id"), rs.getString("full_name"), rs.getString("phone"));
        }
        if (!found) System.out.println("Жазба табылмады.");
    }

    private void printChanged(int count) {
        System.out.println(count > 0 ? "Операция орындалды." : "Көрсетілген ID табылмады.");
    }
}
