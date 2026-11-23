package kz.uib.kitaphana.dao;

import kz.uib.kitaphana.config.DBConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class LoanDAO {
    public record Loan(int id, int bookId, String bookTitle, int readerId,
                       String readerName, LocalDate loanDate, LocalDate dueDate,
                       LocalDate returnDate) {
        public boolean active() { return returnDate == null; }
    }

    private static final String LIST_SQL =
            "SELECT l.id, l.book_id, l.reader_id, b.title, r.full_name AS reader, l.loan_date, "
          + "l.due_date, l.return_date FROM loans l "
          + "JOIN books b ON b.id=l.book_id "
          + "JOIN readers r ON r.id=l.reader_id ";

    /** Кесте үшін бұрынғы LIST_SQL нәтижесін объектілер ретінде қайтарады. */
    public List<Loan> list() throws SQLException {
        List<Loan> result = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(LIST_SQL + "ORDER BY l.id");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Date returned = rs.getDate("return_date");
                result.add(new Loan(rs.getInt("id"), rs.getInt("book_id"),
                        rs.getString("title"), rs.getInt("reader_id"),
                        rs.getString("reader"), rs.getDate("loan_date").toLocalDate(),
                        rs.getDate("due_date").toLocalDate(),
                        returned == null ? null : returned.toLocalDate()));
            }
        }
        return result;
    }

    // Бір транзакцияда беру жазбасы жасалып, бос даналар саны кемиді.
    public boolean create(int bookId, int readerId, LocalDate dueDate) throws SQLException {
        LocalDate today = LocalDate.now();
        if (dueDate.isBefore(today)) {
            System.out.println("Қайтару мерзімі бүгіннен ерте болмауы керек.");
            return false;
        }
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int available;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT available_copies FROM books WHERE id=? FOR UPDATE")) {
                    ps.setInt(1, bookId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            System.out.println("Кітап ID табылмады.");
                            c.rollback();
                            return false;
                        }
                        available = rs.getInt(1);
                    }
                }
                if (available == 0) {
                    System.out.println("Қолда бос кітап жоқ.");
                    c.rollback();
                    return false;
                }
                String sql = "INSERT INTO loans(book_id, reader_id, loan_date, due_date) "
                           + "VALUES (?, ?, ?, ?)";
                try (PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, bookId);
                    ps.setInt(2, readerId);
                    ps.setDate(3, Date.valueOf(today));
                    ps.setDate(4, Date.valueOf(dueDate));
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE books SET available_copies=available_copies-1 WHERE id=?")) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }
                c.commit();
                System.out.println("Кітап оқырманға берілді.");
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    public void readAll() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(LIST_SQL + "ORDER BY l.id");
             ResultSet rs = ps.executeQuery()) {
            print(rs);
        }
    }

    public void searchByReader(String name) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(
                     LIST_SQL + "WHERE r.full_name ILIKE ? ORDER BY l.id")) {
            ps.setString(1, "%" + name + "%");
            try (ResultSet rs = ps.executeQuery()) { print(rs); }
        }
    }

    public boolean updateDueDate(int loanId, LocalDate newDueDate) throws SQLException {
        String sql = "UPDATE loans SET due_date=? WHERE id=? "
                   + "AND return_date IS NULL AND loan_date <= ?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(newDueDate));
            ps.setInt(2, loanId);
            ps.setDate(3, Date.valueOf(newDueDate));
            int count = ps.executeUpdate();
            System.out.println(count > 0
                    ? "Кітапты қайтару мерзімі өзгертілді."
                    : "Белсенді жазба табылмады немесе жаңа мерзім беру күнінен ерте.");
            return count > 0;
        }
    }

    public boolean returnBook(int loanId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int bookId;
                boolean alreadyReturned;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT book_id, return_date FROM loans WHERE id=? FOR UPDATE")) {
                    ps.setInt(1, loanId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            System.out.println("Берілген кітап жазбасы табылмады.");
                            c.rollback();
                            return false;
                        }
                        bookId = rs.getInt("book_id");
                        alreadyReturned = rs.getDate("return_date") != null;
                    }
                }
                if (alreadyReturned) {
                    System.out.println("Бұл кітап бұрын қайтарылған.");
                    c.rollback();
                    return false;
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE loans SET return_date=? WHERE id=?")) {
                    ps.setDate(1, Date.valueOf(LocalDate.now()));
                    ps.setInt(2, loanId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "UPDATE books SET available_copies=available_copies+1 WHERE id=?")) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }
                c.commit();
                System.out.println("Кітап қайтарылды. Қолдағы дана саны артты.");
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    // Белсенді беру жазбасын өшіргенде кітап қайтадан қолда бар деп саналады.
    public boolean delete(int loanId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int bookId;
                boolean active;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT book_id, return_date FROM loans WHERE id=? FOR UPDATE")) {
                    ps.setInt(1, loanId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            System.out.println("Жазба табылмады.");
                            c.rollback();
                            return false;
                        }
                        bookId = rs.getInt("book_id");
                        active = rs.getDate("return_date") == null;
                    }
                }
                try (PreparedStatement ps = c.prepareStatement(
                        "DELETE FROM loans WHERE id=?")) {
                    ps.setInt(1, loanId);
                    ps.executeUpdate();
                }
                if (active) {
                    try (PreparedStatement ps = c.prepareStatement(
                            "UPDATE books SET available_copies=available_copies+1 WHERE id=?")) {
                        ps.setInt(1, bookId);
                        ps.executeUpdate();
                    }
                }
                c.commit();
                System.out.println("Кітап беру жазбасы жойылды.");
                return true;
            } catch (SQLException e) {
                c.rollback();
                throw e;
            }
        }
    }

    private void print(ResultSet rs) throws SQLException {
        boolean found = false;
        while (rs.next()) {
            found = true;
            Date returned = rs.getDate("return_date");
            System.out.printf("ID: %d | Кітап: %s | Оқырман: %s | Берілді: %s | Мерзімі: %s | Күйі: %s%n",
                    rs.getInt("id"), rs.getString("title"), rs.getString("reader"),
                    rs.getDate("loan_date"), rs.getDate("due_date"),
                    returned == null ? "Қайтарылмаған" : "Қайтарылды (" + returned + ")");
        }
        if (!found) System.out.println("Кітап беру жазбалары табылмады.");
    }
}
