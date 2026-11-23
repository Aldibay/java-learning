package kz.uib.kitaphana.gui;

import kz.uib.kitaphana.dao.AuthorDAO.Author;
import kz.uib.kitaphana.dao.BookDAO.Book;
import kz.uib.kitaphana.dao.LoanDAO.Loan;
import kz.uib.kitaphana.dao.ReaderDAO.Reader;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

/** Енгізу формалары; деректер тексерілген соң ғана DAO шақырылады. */
final class FormDialogs {
    record AuthorValues(String name, String country) { }
    record ReaderValues(String name, String phone) { }
    record BookValues(String title, int authorId, int year, String isbn, int copies) { }
    record LoanValues(int bookId, int readerId, LocalDate due) { }

    private FormDialogs() { }

    static AuthorValues author(Component parent, Author old) {
        JTextField name = field(old == null ? "" : old.fullName());
        JTextField country = field(old == null ? "" : old.country());
        JPanel form = panel();
        row(form, 0, "Автордың аты-жөні", name);
        row(form, 1, "Елі", country);
        while (confirm(parent, form, old == null ? "Автор қосу" : "Авторды өзгерту")) {
            try {
                return new AuthorValues(required(name, "Автор", 120),
                        required(country, "Ел", 80));
            } catch (IllegalArgumentException e) {
                invalid(parent, e);
            }
        }
        return null;
    }

    static ReaderValues reader(Component parent, Reader old) {
        JTextField name = field(old == null ? "" : old.fullName());
        JTextField phone = field(old == null ? "" : old.phone());
        JPanel form = panel();
        row(form, 0, "Оқырманның аты-жөні", name);
        row(form, 1, "Телефон (бірегей)", phone);
        while (confirm(parent, form, old == null ? "Оқырман қосу" : "Оқырманды өзгерту")) {
            try {
                return new ReaderValues(required(name, "Аты-жөні", 120),
                        required(phone, "Телефон", 30));
            } catch (IllegalArgumentException e) {
                invalid(parent, e);
            }
        }
        return null;
    }

    static BookValues book(Component parent, Book old, List<Author> authors) {
        if (authors.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Алдымен «Авторлар» бөлімінде автор қосыңыз.",
                    "Автор жоқ", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        JTextField title = field(old == null ? "" : old.title());
        JComboBox<Author> author = combo(authors);
        if (old != null) selectAuthor(author, old.authorId());
        JTextField year = field(old == null ? String.valueOf(LocalDate.now().getYear()) : String.valueOf(old.year()));
        JTextField isbn = field(old == null ? "" : old.isbn());
        JTextField copies = field(old == null ? "1" : String.valueOf(old.totalCopies()));
        JPanel form = panel();
        row(form, 0, "Кітап атауы", title);
        row(form, 1, "Автор", author);
        row(form, 2, "Басылған жылы", year);
        row(form, 3, "ISBN (бірегей)", isbn);
        row(form, 4, "Жалпы дана саны", copies);
        if (old != null) {
            row(form, 5, "Берілген дана саны", new JLabel(String.valueOf(old.borrowedCopies())));
        }
        while (confirm(parent, form, old == null ? "Кітап қосу" : "Кітапты өзгерту")) {
            try {
                int total = number(copies, "Дана саны", 1, Integer.MAX_VALUE);
                if (old != null && total < old.borrowedCopies()) {
                    throw new IllegalArgumentException("Дана саны берілген кітап санынан ("
                            + old.borrowedCopies() + ") аз болмауы керек.");
                }
                Author selected = (Author) author.getSelectedItem();
                if (selected == null) throw new IllegalArgumentException("Авторды таңдаңыз.");
                return new BookValues(required(title, "Кітап атауы", 180), selected.id(),
                        number(year, "Басылған жылы", 1450, 2100),
                        required(isbn, "ISBN", 20), total);
            } catch (IllegalArgumentException e) {
                invalid(parent, e);
            }
        }
        return null;
    }

    static LoanValues loan(Component parent, List<Book> books, List<Reader> readers) {
        List<Book> available = books.stream().filter(b -> b.availableCopies() > 0).toList();
        if (available.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Қолда бос кітап жоқ. Алдымен кітап қосыңыз немесе қайтарыңыз.",
                    "Кітап жоқ", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        if (readers.isEmpty()) {
            JOptionPane.showMessageDialog(parent, "Алдымен «Оқырмандар» бөлімінде оқырман қосыңыз.",
                    "Оқырман жоқ", JOptionPane.WARNING_MESSAGE);
            return null;
        }
        JComboBox<Book> book = combo(available);
        JComboBox<Reader> reader = combo(readers);
        JTextField due = field(LocalDate.now().plusDays(14).toString());
        JPanel form = panel();
        row(form, 0, "Кітап", book);
        row(form, 1, "Оқырман", reader);
        row(form, 2, "Қайтару мерзімі (YYYY-MM-DD)", due);
        while (confirm(parent, form, "Кітап беру")) {
            try {
                LocalDate date = date(due, "Қайтару мерзімі");
                if (date.isBefore(LocalDate.now())) {
                    throw new IllegalArgumentException("Қайтару мерзімі бүгіннен ерте болмауы керек.");
                }
                Book b = (Book) book.getSelectedItem();
                Reader r = (Reader) reader.getSelectedItem();
                if (b == null || r == null) throw new IllegalArgumentException("Кітап пен оқырманды таңдаңыз.");
                return new LoanValues(b.id(), r.id(), date);
            } catch (IllegalArgumentException e) {
                invalid(parent, e);
            }
        }
        return null;
    }

    static LocalDate dueDate(Component parent, Loan old) {
        JTextField due = field(old.dueDate().toString());
        JPanel form = panel();
        row(form, 0, "Кітап", new JLabel(old.bookTitle()));
        row(form, 1, "Оқырман", new JLabel(old.readerName()));
        row(form, 2, "Берілген күні", new JLabel(old.loanDate().toString()));
        row(form, 3, "Жаңа мерзім (YYYY-MM-DD)", due);
        while (confirm(parent, form, "Қайтару мерзімін өзгерту")) {
            try {
                LocalDate date = date(due, "Қайтару мерзімі");
                if (date.isBefore(old.loanDate())) {
                    throw new IllegalArgumentException("Мерзім кітап берілген күннен ерте болмауы керек.");
                }
                return date;
            } catch (IllegalArgumentException e) {
                invalid(parent, e);
            }
        }
        return null;
    }

    private static boolean confirm(Component parent, JPanel form, String title) {
        return JOptionPane.showConfirmDialog(parent, form, title,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private static JPanel panel() {
        JPanel form = new JPanel(new GridBagLayout());
        form.setBorder(UiTheme.inset(12, 10, 12, 10));
        return form;
    }

    private static void row(JPanel panel, int y, String name, JComponent component) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridy = y;
        c.insets = new Insets(7, 4, 7, 12);
        c.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(name), c);
        c.gridx = 1; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(7, 0, 7, 4);
        panel.add(component, c);
    }

    private static JTextField field(String value) {
        JTextField f = new JTextField(value, 24);
        f.setFont(UiTheme.NORMAL);
        UiTheme.focusBorder(f);
        return f;
    }

    private static <T> JComboBox<T> combo(List<T> values) {
        JComboBox<T> combo = new JComboBox<>();
        for (T value : values) combo.addItem(value);
        combo.setFont(UiTheme.NORMAL);
        return combo;
    }

    private static void selectAuthor(JComboBox<Author> combo, int authorId) {
        for (int i = 0; i < combo.getItemCount(); i++) {
            if (combo.getItemAt(i).id() == authorId) {
                combo.setSelectedIndex(i);
                return;
            }
        }
    }

    private static String required(JTextField field, String label, int length) {
        String text = field.getText().trim();
        if (text.isEmpty()) throw new IllegalArgumentException(label + " бос болмауы керек.");
        if (text.length() > length) {
            throw new IllegalArgumentException(label + " ұзындығы " + length + " таңбадан аспауға тиіс.");
        }
        return text;
    }

    private static int number(JTextField field, String label, int min, int max) {
        String text = field.getText().trim();
        try {
            int number = Integer.parseInt(text);
            if (number < min || number > max) throw new NumberFormatException();
            return number;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(label + " " + min + " және " + max + " аралығында болуы керек.");
        }
    }

    private static LocalDate date(JTextField field, String label) {
        try {
            return LocalDate.parse(field.getText().trim());
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(label + ": күнді YYYY-MM-DD пішімінде енгізіңіз.");
        }
    }

    private static void invalid(Component parent, IllegalArgumentException e) {
        JOptionPane.showMessageDialog(parent, e.getMessage(), "Деректерді тексеру",
                JOptionPane.WARNING_MESSAGE);
    }
}
