package kz.uib.kitaphana.util;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

public final class Input {
    private final Scanner scanner = new Scanner(System.in);

    public String text(String message) {
        while (true) {
            System.out.print(message);
            String value = scanner.nextLine().trim();
            if (!value.isEmpty()) return value;
            System.out.println("Бос мәтін енгізуге болмайды.");
        }
    }

    public int number(String message, int min, int max) {
        while (true) {
            System.out.print(message);
            String raw = scanner.nextLine().trim();
            try {
                int value = Integer.parseInt(raw);
                if (value >= min && value <= max) return value;
            } catch (NumberFormatException ignored) { }
            System.out.println("Қате! " + min + " мен " + max + " аралығындағы бүтін санды енгізіңіз.");
        }
    }

    public int id(String message) {
        return number(message, 1, Integer.MAX_VALUE);
    }

    public LocalDate date(String message) {
        while (true) {
            String raw = text(message);
            try {
                return LocalDate.parse(raw);
            } catch (DateTimeParseException e) {
                System.out.println("Күнді ЖЖЖЖ-АА-КК түрінде енгізіңіз. Мысалы: 2026-12-01");
            }
        }
    }
}
