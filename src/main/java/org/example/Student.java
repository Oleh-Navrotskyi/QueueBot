package org.example;

import java.io.Serializable;

public class Student implements Comparable<Student>, Serializable {
    private final String name;
    private final Long chatId;
    private final char type;
    private final int number;
    private final long joinTime;

    public Student(String name, Long chatId, char type) {
        this.chatId = chatId;
        this.type = type;
        this.joinTime = System.currentTimeMillis();
        String[] buffer = name.split(" ");
        this.name = buffer[0];
        number = Integer.parseInt(buffer[1]);
    }

    // Розрахунок пріоритету (чим більше число, тим важливіше)
    public int getPriority() {
        //A - ще не починав. В - доздача
        return switch (type) {
            case 'A' -> 1000/number;
            case 'B' -> 1000/number - 1;
            default -> 1;
        };
    }

    @Override
    public int compareTo(Student other) {
        // 1. Порівнюємо за пріоритетом (від більшого до меншого)
        int priorityDiff = other.getPriority() - this.getPriority();
        if (priorityDiff != 0) {
            return priorityDiff;
        }
        // 2. Якщо пріоритет однаковий, порівнюємо за часом (хто раніше встав)
        return Long.compare(this.joinTime, other.joinTime);
    }

    @Override
    public String toString() {
        return (type == 'B' ? "⚡" : "🎓") + " " + name + " (" + type + ") Пріорітет: "+getPriority();
    }

    public Long getChatId() { return chatId; }
    public String getName() { return name; }
}
