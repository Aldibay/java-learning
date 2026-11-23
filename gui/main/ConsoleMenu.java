package kz.uib.kitaphana.ui;

import kz.uib.kitaphana.dao.AuthorDAO;
import kz.uib.kitaphana.dao.BookDAO;
import kz.uib.kitaphana.dao.LoanDAO;
import kz.uib.kitaphana.dao.ReaderDAO;
import kz.uib.kitaphana.util.Input;

import java.sql.SQLException;

public class ConsoleMenu {
    private final Input input = new Input();
    private final AuthorDAO authors = new AuthorDAO();
    private final BookDAO books = new BookDAO();
    private final ReaderDAO readers = new ReaderDAO();
    private final LoanDAO loans = new LoanDAO();

    @FunctionalInterface
    private interface SqlAction { void execute() throws SQLException; }

    private void run(SqlAction action) {
        try {
            action.execute();
        } catch (SQLException e) {
            System.out.println("Деректер қорының қатесі: " + e.getMessage());
            String code = e.getSQLState();
            if ("23505".equals(code)) {
                System.out.println("Қайталанатын бірегей мән енгізілді (UNIQUE).");
            } else if ("23503".equals(code)) {
                System.out.println("Байланысқан жазба табылмады немесе бұл жазба басқа кестеде қолданылып тұр (FOREIGN KEY).");
            } else if ("23514".equals(code)) {
                System.out.println("Енгізілген мән кесте шектеуіне сай емес (CHECK).");
            } else {
                System.out.println("Қосылым параметрлерін, ID және кесте шектеулерін тексеріңіз.");
            }
        }
    }

    public void start() {
        while (true) {
            System.out.println("\n========== КІТАПХАНА ==========");
            System.out.println("1. Авторлар");
            System.out.println("2. Кітаптар");
            System.out.println("3. Оқырмандар");
            System.out.println("4. Кітап беру / қайтару");
            System.out.println("0. Шығу");
            int choice = input.number("Таңдаңыз: ", 0, 4);
            switch (choice) {
                case 1 -> authorMenu();
                case 2 -> bookMenu();
                case 3 -> readerMenu();
                case 4 -> loanMenu();
                case 0 -> { System.out.println("Бағдарлама аяқталды."); return; }
                default -> { }
            }
        }
    }

    private void authorMenu() {
        while (true) {
            System.out.println("\n--- АВТОРЛАР ---");
            System.out.println("1. Қосу  2. Көрсету  3. Іздеу  4. Өзгерту  5. Жою  0. Артқа");
            int c = input.number("Таңдаңыз: ", 0, 5);
            switch (c) {
                case 1 -> {
                    String name = input.text("Аты-жөні: ");
                    String country = input.text("Елі: ");
                    run(() -> authors.create(name, country));
                }
                case 2 -> run(authors::readAll);
                case 3 -> {
                    String word = input.text("Автор аты бойынша іздеу: ");
                    run(() -> authors.search(word));
                }
                case 4 -> {
                    int id = input.id("Автор ID: ");
                    String name = input.text("Жаңа аты-жөні: ");
                    String country = input.text("Жаңа елі: ");
                    run(() -> authors.update(id, name, country));
                }
                case 5 -> {
                    int id = input.id("Жойылатын автор ID: ");
                    run(() -> authors.delete(id));
                }
                case 0 -> { return; }
                default -> { }
            }
        }
    }

    private void bookMenu() {
        while (true) {
            System.out.println("\n--- КІТАПТАР ---");
            System.out.println("1. Қосу  2. Көрсету  3. Іздеу  4. Өзгерту  5. Жою  0. Артқа");
            int c = input.number("Таңдаңыз: ", 0, 5);
            switch (c) {
                case 1 -> {
                    String title = input.text("Кітап атауы: ");
                    int authorId = input.id("Автор ID (алдымен авторларды көрсетіңіз): ");
                    int year = input.number("Басылған жылы: ", 1450, 2100);
                    String isbn = input.text("ISBN (бірегей): ");
                    int copies = input.id("Жалпы дана саны: ");
                    run(() -> books.create(title, authorId, year, isbn, copies));
                }
                case 2 -> run(books::readAll);
                case 3 -> {
                    String word = input.text("Кітап атауы бойынша іздеу: ");
                    run(() -> books.search(word));
                }
                case 4 -> {
                    int id = input.id("Өзгертілетін кітап ID: ");
                    String title = input.text("Жаңа атауы: ");
                    int authorId = input.id("Автор ID: ");
                    int year = input.number("Басылған жылы: ", 1450, 2100);
                    String isbn = input.text("ISBN: ");
                    int copies = input.id("Жаңа жалпы дана саны: ");
                    run(() -> books.update(id, title, authorId, year, isbn, copies));
                }
                case 5 -> {
                    int id = input.id("Жойылатын кітап ID: ");
                    run(() -> books.delete(id));
                }
                case 0 -> { return; }
                default -> { }
            }
        }
    }

    private void readerMenu() {
        while (true) {
            System.out.println("\n--- ОҚЫРМАНДАР ---");
            System.out.println("1. Қосу  2. Көрсету  3. Іздеу  4. Өзгерту  5. Жою  0. Артқа");
            int c = input.number("Таңдаңыз: ", 0, 5);
            switch (c) {
                case 1 -> {
                    String name = input.text("Оқырманның аты-жөні: ");
                    String phone = input.text("Телефон (бірегей): ");
                    run(() -> readers.create(name, phone));
                }
                case 2 -> run(readers::readAll);
                case 3 -> {
                    String word = input.text("Оқырман аты бойынша іздеу: ");
                    run(() -> readers.search(word));
                }
                case 4 -> {
                    int id = input.id("Оқырман ID: ");
                    String name = input.text("Жаңа аты-жөні: ");
                    String phone = input.text("Жаңа телефон: ");
                    run(() -> readers.update(id, name, phone));
                }
                case 5 -> {
                    int id = input.id("Жойылатын оқырман ID: ");
                    run(() -> readers.delete(id));
                }
                case 0 -> { return; }
                default -> { }
            }
        }
    }

    private void loanMenu() {
        while (true) {
            System.out.println("\n--- КІТАП БЕРУ / ҚАЙТАРУ ---");
            System.out.println("1. Кітап беру (Create)");
            System.out.println("2. Барлық берулер (Read)");
            System.out.println("3. Оқырман бойынша іздеу (Read)");
            System.out.println("4. Қайтару мерзімін өзгерту (Update)");
            System.out.println("5. Беру жазбасын жою (Delete)");
            System.out.println("6. Кітапты қайтару");
            System.out.println("0. Артқа");
            int c = input.number("Таңдаңыз: ", 0, 6);
            switch (c) {
                case 1 -> {
                    int bookId = input.id("Кітап ID: ");
                    int readerId = input.id("Оқырман ID: ");
                    var due = input.date("Қайтарылатын күн (ЖЖЖЖ-АА-КК): ");
                    run(() -> loans.create(bookId, readerId, due));
                }
                case 2 -> run(loans::readAll);
                case 3 -> {
                    String name = input.text("Оқырманның аты: ");
                    run(() -> loans.searchByReader(name));
                }
                case 4 -> {
                    int id = input.id("Беру жазбасының ID: ");
                    var due = input.date("Жаңа қайтару күні (ЖЖЖЖ-АА-КК): ");
                    run(() -> loans.updateDueDate(id, due));
                }
                case 5 -> {
                    int id = input.id("Жойылатын беру жазбасының ID: ");
                    run(() -> loans.delete(id));
                }
                case 6 -> {
                    int id = input.id("Қайтарылатын беру жазбасының ID: ");
                    run(() -> loans.returnBook(id));
                }
                case 0 -> { return; }
                default -> { }
            }
        }
    }
}
