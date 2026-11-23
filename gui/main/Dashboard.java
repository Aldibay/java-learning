package kz.uib.kitaphana.gui;

import kz.uib.kitaphana.dao.AuthorDAO;
import kz.uib.kitaphana.dao.AuthorDAO.Author;
import kz.uib.kitaphana.dao.BookDAO;
import kz.uib.kitaphana.dao.BookDAO.Book;
import kz.uib.kitaphana.dao.LoanDAO;
import kz.uib.kitaphana.dao.LoanDAO.Loan;
import kz.uib.kitaphana.dao.ReaderDAO;
import kz.uib.kitaphana.dao.ReaderDAO.Reader;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableRowSorter;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

/** PostgreSQL CRUD-операцияларын бұрыннан бар DAO әдістерімен орындайтын Swing терезесі. */
public final class Dashboard extends JFrame {
    private enum Kind { BOOKS, AUTHORS, READERS, LOANS }

    private record Snapshot(List<Author> authors, List<Book> books,
                            List<Reader> readers, List<Loan> loans) { }

    @FunctionalInterface
    private interface SqlAction { boolean execute() throws SQLException; }

    private final AuthorDAO authorDAO = new AuthorDAO();
    private final BookDAO bookDAO = new BookDAO();
    private final ReaderDAO readerDAO = new ReaderDAO();
    private final LoanDAO loanDAO = new LoanDAO();
    private final Map<Kind, Section> sections = new EnumMap<>(Kind.class);
    private final JTabbedPane tabs = new JTabbedPane();
    private final JLabel totalBooks = numberLabel();
    private final JLabel availableCopies = numberLabel();
    private final JLabel totalReaders = numberLabel();
    private final JLabel activeLoans = numberLabel();
    private final JLabel status = new JLabel("Деректерді жүктеуге дайын");
    private Snapshot snapshot = new Snapshot(List.of(), List.of(), List.of(), List.of());
    private boolean busy;
    private boolean connected;

    public Dashboard() {
        super("Кітапхана | Java Swing + PostgreSQL");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1200, 760);
        setMinimumSize(new Dimension(990, 630));
        setLocationRelativeTo(null);
        setContentPane(buildUi());
        refreshAll();
    }

    private static JLabel numberLabel() {
        JLabel label = new JLabel("—");
        label.setFont(new Font("Dialog", Font.BOLD, 25));
        label.setForeground(UiTheme.NAVY);
        return label;
    }

    private JPanel buildUi() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(UiTheme.BG);

        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(UiTheme.NAVY);
        top.setBorder(UiTheme.inset(21, 27, 19, 27));
        JLabel title = new JLabel("КІТАПХАНА — БАСҚАРУ ЖҮЙЕСІ");
        title.setFont(new Font("Dialog", Font.BOLD, 23));
        title.setForeground(Color.WHITE);
        JLabel subtitle = new JLabel("Java Swing  •  PostgreSQL  •  Деректерді басқару");
        subtitle.setForeground(new Color(205, 224, 233));
        subtitle.setFont(UiTheme.NORMAL);
        top.add(title, BorderLayout.NORTH);
        top.add(subtitle, BorderLayout.SOUTH);
        root.add(top, BorderLayout.NORTH);

        JPanel content = new JPanel(new BorderLayout(0, 17));
        content.setBackground(UiTheme.BG);
        content.setBorder(UiTheme.inset(17, 22, 12, 22));
        JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
        stats.setOpaque(false);
        stats.add(statCard("Кітап атаулары", totalBooks));
        stats.add(statCard("Қолдағы даналар", availableCopies));
        stats.add(statCard("Оқырмандар", totalReaders));
        stats.add(statCard("Белсенді берулер", activeLoans));
        content.add(stats, BorderLayout.NORTH);

        sections.put(Kind.BOOKS, new Section(Kind.BOOKS,
                new String[]{"id", "title (Кітап атауы)", "author (Автор)",
                        "publication_year", "isbn", "total_copies", "available_copies"},
                new int[]{55, 230, 190, 145, 150, 130, 145}));
        sections.put(Kind.AUTHORS, new Section(Kind.AUTHORS,
                new String[]{"id", "full_name (Аты-жөні)", "country (Елі)"},
                new int[]{80, 390, 220}));
        sections.put(Kind.READERS, new Section(Kind.READERS,
                new String[]{"id", "full_name (Аты-жөні)", "phone (Телефон)"},
                new int[]{80, 390, 240}));
        sections.put(Kind.LOANS, new Section(Kind.LOANS,
                new String[]{"id", "book (Кітап)", "reader (Оқырман)",
                        "loan_date", "due_date", "return_date", "Күйі"},
                new int[]{55, 220, 180, 130, 130, 130, 140}));
        tabs.setFont(UiTheme.STRONG);
        tabs.setBackground(Color.WHITE);
        tabs.addTab("Кітаптар", sections.get(Kind.BOOKS));
        tabs.addTab("Авторлар", sections.get(Kind.AUTHORS));
        tabs.addTab("Оқырмандар", sections.get(Kind.READERS));
        tabs.addTab("Кітап беру", sections.get(Kind.LOANS));
        content.add(tabs, BorderLayout.CENTER);
        root.add(content, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UiTheme.BORDER),
                UiTheme.inset(9, 22, 9, 22)));
        status.setFont(new Font("Dialog", Font.PLAIN, 12));
        status.setForeground(UiTheme.MUTED);
        footer.add(status, BorderLayout.WEST);
        JLabel helper = new JLabel("Жазбаны таңдаңыз → Edit / Delete");
        helper.setFont(new Font("Dialog", Font.PLAIN, 12));
        helper.setForeground(UiTheme.MUTED);
        footer.add(helper, BorderLayout.EAST);
        root.add(footer, BorderLayout.SOUTH);
        return root;
    }

    private JPanel statCard(String heading, JLabel value) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UiTheme.BORDER), UiTheme.inset(11, 16, 13, 16)));
        JLabel label = new JLabel(heading);
        label.setFont(new Font("Dialog", Font.PLAIN, 13));
        label.setForeground(UiTheme.MUTED);
        card.add(label, BorderLayout.NORTH);
        card.add(value, BorderLayout.CENTER);
        return card;
    }

    private void setBusy(boolean value, String message) {
        busy = value;
        status.setText(message);
        for (Section section : sections.values()) section.updateButtons();
    }

    private void refreshAll() {
        if (busy) return;
        setBusy(true, "PostgreSQL деректерін жүктеу...");
        new SwingWorker<Snapshot, Void>() {
            @Override protected Snapshot doInBackground() throws SQLException {
                return new Snapshot(authorDAO.list(), bookDAO.list(),
                        readerDAO.list(), loanDAO.list());
            }
            @Override protected void done() {
                try {
                    snapshot = get();
                    applySnapshot(snapshot);
                    connected = true;
                    status.setText("PostgreSQL қосылды. Барлық бөлімдер жаңартылды.");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    status.setText("Жүктеу тоқтатылды.");
                } catch (ExecutionException e) {
                    connected = false;
                    showDatabaseError(e.getCause());
                } finally {
                    busy = false;
                    for (Section section : sections.values()) section.updateButtons();
                }
            }
        }.execute();
    }

    private void applySnapshot(Snapshot data) {
        sections.get(Kind.AUTHORS).updateData(data.authors());
        sections.get(Kind.BOOKS).updateData(data.books());
        sections.get(Kind.READERS).updateData(data.readers());
        sections.get(Kind.LOANS).updateData(data.loans());
        totalBooks.setText(Integer.toString(data.books().size()));
        int available = data.books().stream().mapToInt(Book::availableCopies).sum();
        availableCopies.setText(Integer.toString(available));
        totalReaders.setText(Integer.toString(data.readers().size()));
        activeLoans.setText(Long.toString(data.loans().stream().filter(Loan::active).count()));
    }

    private void runAction(String name, SqlAction action) {
        if (busy) return;
        setBusy(true, name + " — сақталуда...");
        new SwingWorker<Boolean, Void>() {
            @Override protected Boolean doInBackground() throws SQLException {
                return action.execute();
            }
            @Override protected void done() {
                boolean changed = false;
                try {
                    changed = get();
                    if (changed) {
                        status.setText(name + " — орындалды.");
                    } else {
                        JOptionPane.showMessageDialog(Dashboard.this,
                                "Операция орындалмады. Жазба табылмаған немесе шарт орындалмаған болуы мүмкін.\n"
                                + "Деректер жаңартылады.", "Орындалмады", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    status.setText("Операция тоқтатылды.");
                } catch (ExecutionException e) {
                    showDatabaseError(e.getCause());
                } finally {
                    busy = false;
                    for (Section section : sections.values()) section.updateButtons();
                    refreshAll();
                }
            }
        }.execute();
    }

    private void showDatabaseError(Throwable error) {
        SQLException sql = error instanceof SQLException e ? e : null;
        String advice = "Деректер қорына қосылуды және енгізілген мәндерді тексеріңіз.";
        if (sql != null) {
            String code = sql.getSQLState();
            if ("23505".equals(code)) advice = "Бұл мән бұрыннан бар (ISBN немесе телефон бірегей болуы тиіс).";
            else if ("23503".equals(code)) advice = "Жазба басқа кестемен байланысқан немесе көрсетілген ID жоқ.";
            else if ("23514".equals(code)) advice = "Өріс мәні деректер қорының шектеуіне сәйкес емес.";
            else if ("28P01".equals(code)) advice = "DB_USER және DB_PASSWORD мәндерін тексеріңіз.";
            else if ("3D000".equals(code)) advice = "library_db деректер қорын жасаңыз.";
            else if (code != null && code.startsWith("08")) advice = "PostgreSQL сервері мен DB_URL мекенжайын тексеріңіз.";
        }
        String detail = error == null ? "Белгісіз қате" : error.getMessage();
        status.setText("Деректер қорының қатесі");
        JOptionPane.showMessageDialog(this, advice + "\n\nТехникалық мәлімет:\n" + detail,
                "PostgreSQL қатесі", JOptionPane.ERROR_MESSAGE);
    }

    private void add(Kind kind) {
        if (busy) return;
        switch (kind) {
            case AUTHORS -> {
                FormDialogs.AuthorValues v = FormDialogs.author(this, null);
                if (v != null) runAction("Автор қосу", () -> {
                    authorDAO.create(v.name(), v.country()); return true;
                });
            }
            case READERS -> {
                FormDialogs.ReaderValues v = FormDialogs.reader(this, null);
                if (v != null) runAction("Оқырман қосу", () -> {
                    readerDAO.create(v.name(), v.phone()); return true;
                });
            }
            case BOOKS -> {
                FormDialogs.BookValues v = FormDialogs.book(this, null, snapshot.authors());
                if (v != null) runAction("Кітап қосу", () -> {
                    bookDAO.create(v.title(), v.authorId(), v.year(), v.isbn(), v.copies());
                    return true;
                });
            }
            case LOANS -> {
                FormDialogs.LoanValues v = FormDialogs.loan(this, snapshot.books(), snapshot.readers());
                if (v != null) runAction("Кітап беру", () -> loanDAO.create(v.bookId(), v.readerId(), v.due()));
            }
        }
    }

    private void edit(Kind kind, Object value) {
        if (busy || value == null) return;
        switch (kind) {
            case AUTHORS -> {
                Author a = (Author) value;
                FormDialogs.AuthorValues v = FormDialogs.author(this, a);
                if (v != null) runAction("Авторды өзгерту", () -> authorDAO.update(a.id(), v.name(), v.country()));
            }
            case READERS -> {
                Reader r = (Reader) value;
                FormDialogs.ReaderValues v = FormDialogs.reader(this, r);
                if (v != null) runAction("Оқырманды өзгерту", () -> readerDAO.update(r.id(), v.name(), v.phone()));
            }
            case BOOKS -> {
                Book b = (Book) value;
                FormDialogs.BookValues v = FormDialogs.book(this, b, snapshot.authors());
                if (v != null) runAction("Кітапты өзгерту", () -> bookDAO.update(
                        b.id(), v.title(), v.authorId(), v.year(), v.isbn(), v.copies()));
            }
            case LOANS -> {
                Loan l = (Loan) value;
                if (!l.active()) {
                    JOptionPane.showMessageDialog(this, "Қайтарылған кітаптың мерзімін өзгертуге болмайды.",
                            "Ескерту", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                LocalDate date = FormDialogs.dueDate(this, l);
                if (date != null) runAction("Қайтару мерзімін өзгерту", () -> loanDAO.updateDueDate(l.id(), date));
            }
        }
    }

    private void delete(Kind kind, Object value) {
        if (busy || value == null) return;
        int id = switch (kind) {
            case AUTHORS -> ((Author) value).id();
            case READERS -> ((Reader) value).id();
            case BOOKS -> ((Book) value).id();
            case LOANS -> ((Loan) value).id();
        };
        String warning = kind == Kind.LOANS
                ? "Беру жазбасын өшіресіз бе? Белсенді болса, кітап қоры қалпына келеді."
                : "ID=" + id + " жазбасын біржола жоясыз ба?";
        int confirmed = JOptionPane.showConfirmDialog(this, warning, "Жоюды растау",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirmed != JOptionPane.YES_OPTION) return;
        runAction("Жазбаны жою", () -> switch (kind) {
            case AUTHORS -> authorDAO.delete(id);
            case READERS -> readerDAO.delete(id);
            case BOOKS -> bookDAO.delete(id);
            case LOANS -> loanDAO.delete(id);
        });
    }

    private void returnBook(Object value) {
        if (busy || !(value instanceof Loan loan) || !loan.active()) return;
        int confirmed = JOptionPane.showConfirmDialog(this,
                "«" + loan.bookTitle() + "» кітабын қайтаруды растайсыз ба?",
                "Кітапты қайтару", JOptionPane.YES_NO_OPTION);
        if (confirmed == JOptionPane.YES_OPTION) {
            runAction("Кітапты қайтару", () -> loanDAO.returnBook(loan.id()));
        }
    }

    private final class Section extends JPanel {
        private final Kind kind;
        private final DefaultTableModel model;
        private final JTable table;
        private final TableRowSorter<DefaultTableModel> sorter;
        private final JTextField search = new JTextField(21);
        private final JButton addButton = UiTheme.button("Add • Қосу", UiTheme.TEAL, Color.WHITE);
        private final JButton editButton = UiTheme.button("Edit • Өзгерту", Color.WHITE, UiTheme.TEXT);
        private final JButton deleteButton = UiTheme.button("Delete • Жою", Color.WHITE, UiTheme.DANGER);
        private final JButton refreshButton = UiTheme.button("Refresh • Жаңарту", Color.WHITE, UiTheme.TEXT);
        private final JButton returnButton = UiTheme.button("Қайтару", UiTheme.NAVY, Color.WHITE);
        private final JLabel count = new JLabel();
        private List<?> data = List.of();

        private Section(Kind kind, String[] columns, int[] widths) {
            super(new BorderLayout(0, 14));
            this.kind = kind;
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(UiTheme.BORDER), UiTheme.inset(15, 17, 13, 17)));
            model = new DefaultTableModel(columns, 0) {
                @Override public boolean isCellEditable(int row, int col) { return false; }
            };
            table = new JTable(model);
            table.setRowHeight(32);
            table.setFont(UiTheme.NORMAL);
            table.setForeground(UiTheme.TEXT);
            table.setShowVerticalLines(false);
            table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            table.setSelectionBackground(new Color(220, 242, 239));
            table.setSelectionForeground(UiTheme.TEXT);
            table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
            table.setFillsViewportHeight(true);
            table.setGridColor(new Color(238, 241, 245));
            JTableHeader header = table.getTableHeader();
            header.setFont(UiTheme.STRONG);
            header.setPreferredSize(new Dimension(0, 36));
            header.setBackground(new Color(232, 240, 244));
            header.setForeground(UiTheme.NAVY);
            for (int i = 0; i < widths.length; i++) {
                table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
            }
            table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
                @Override public java.awt.Component getTableCellRendererComponent(
                        JTable target, Object value, boolean selected, boolean focus, int row, int column) {
                    java.awt.Component comp = super.getTableCellRendererComponent(
                            target, value, selected, focus, row, column);
                    if (!selected) comp.setBackground(row % 2 == 0 ? Color.WHITE : UiTheme.BG);
                    setBorder(UiTheme.inset(0, 8, 0, 8));
                    return comp;
                }
            });
            sorter = new TableRowSorter<>(model);
            table.setRowSorter(sorter);
            table.getSelectionModel().addListSelectionListener(e -> {
                if (!e.getValueIsAdjusting()) updateButtons();
            });
            JScrollPane scroll = new JScrollPane(table);
            scroll.setBorder(BorderFactory.createLineBorder(UiTheme.BORDER));
            scroll.getViewport().setBackground(Color.WHITE);

            JPanel toolbar = new JPanel(new BorderLayout(12, 7));
            toolbar.setBackground(Color.WHITE);
            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 3));
            left.setBackground(Color.WHITE);
            JLabel searchLabel = new JLabel("Іздеу:");
            searchLabel.setForeground(UiTheme.MUTED);
            search.setToolTipText("Кестеден атауы, аты-жөні немесе басқа өріс бойынша іздеу");
            UiTheme.focusBorder(search);
            left.add(searchLabel);
            left.add(search);
            toolbar.add(left, BorderLayout.WEST);
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 3));
            actions.setBackground(Color.WHITE);
            actions.add(addButton);
            actions.add(editButton);
            actions.add(deleteButton);
            if (kind == Kind.LOANS) actions.add(returnButton);
            actions.add(refreshButton);
            toolbar.add(actions, BorderLayout.CENTER);
            add(toolbar, BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
            JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
            row.setBackground(Color.WHITE);
            count.setFont(new Font("Dialog", Font.PLAIN, 12));
            count.setForeground(UiTheme.MUTED);
            row.add(count);
            add(row, BorderLayout.SOUTH);

            search.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { filter(); }
                @Override public void removeUpdate(DocumentEvent e) { filter(); }
                @Override public void changedUpdate(DocumentEvent e) { filter(); }
            });
            addButton.addActionListener(e -> Dashboard.this.add(kind));
            editButton.addActionListener(e -> Dashboard.this.edit(kind, selected()));
            deleteButton.addActionListener(e -> Dashboard.this.delete(kind, selected()));
            refreshButton.addActionListener(e -> refreshAll());
            returnButton.addActionListener(e -> Dashboard.this.returnBook(selected()));
            updateButtons();
        }

        private void updateData(List<?> rows) {
            this.data = new ArrayList<>(rows);
            model.setRowCount(0);
            for (Object row : rows) {
                switch (kind) {
                    case AUTHORS -> {
                        Author a = (Author) row;
                        model.addRow(new Object[]{a.id(), a.fullName(), a.country()});
                    }
                    case READERS -> {
                        Reader r = (Reader) row;
                        model.addRow(new Object[]{r.id(), r.fullName(), r.phone()});
                    }
                    case BOOKS -> {
                        Book b = (Book) row;
                        model.addRow(new Object[]{b.id(), b.title(), b.authorName(),
                                b.year(), b.isbn(), b.totalCopies(), b.availableCopies()});
                    }
                    case LOANS -> {
                        Loan l = (Loan) row;
                        model.addRow(new Object[]{l.id(), l.bookTitle(), l.readerName(),
                                l.loanDate(), l.dueDate(), l.returnDate() == null ? "—" : l.returnDate(),
                                l.active() ? "Берілген" : "Қайтарылған"});
                    }
                }
            }
            table.clearSelection();
            count.setText("Көрсетілген жазбалар: " + table.getRowCount() + " / " + data.size());
            updateButtons();
        }

        private void filter() {
            String query = search.getText().trim();
            sorter.setRowFilter(query.isEmpty() ? null
                    : RowFilter.regexFilter("(?iu)" + Pattern.quote(query)));
            count.setText("Көрсетілген жазбалар: " + table.getRowCount() + " / " + data.size());
            updateButtons();
        }

        private Object selected() {
            int selected = table.getSelectedRow();
            if (selected < 0) return null;
            int idx = table.convertRowIndexToModel(selected);
            return idx < data.size() ? data.get(idx) : null;
        }

        private void updateButtons() {
            boolean row = selected() != null;
            addButton.setEnabled(!busy && connected);
            editButton.setEnabled(!busy && connected && row);
            deleteButton.setEnabled(!busy && connected && row);
            refreshButton.setEnabled(!busy);
            if (kind == Kind.LOANS) {
                Object value = selected();
                returnButton.setEnabled(!busy && connected
                        && value instanceof Loan loan && loan.active());
            }
        }
    }
}
