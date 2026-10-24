package org.example;

import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import java.io.*;
import java.util.*;

public class QueueBot extends TelegramLongPollingBot {

    // Зберігаємо чергу в пам'яті
    private final PriorityQueue<Student> queue = new PriorityQueue<>();
    private Map<Long, String> tempNames = new HashMap<>();
    @Override
    public String getBotUsername() {
        return "eQueue_For_OP_bot";
    }

    @Override
    public String getBotToken() {
        return "8426401184:AAFJlwbH3YsDet5W_sM9ee_SC5lObY39h1M";
    }

    @Override
    public void onUpdateReceived(Update update) {
        // 1. ОБРОБКА ТЕКСТОВИХ КОМАНД
        if (update.hasMessage() && update.getMessage().hasText()) {
            String text = update.getMessage().getText();
            Long chatId = update.getMessage().getChatId();

            if (text.equals("/start")) {
                sendMsg(chatId, "Привіт! Напиши, щоб записатися: /reg [Прізвище] [Номер лаби] (без дужок) \n" +
                        "Щоб встати в чергу. \n");
            }
            else if (text.startsWith("/reg")) {
                // Крок 1. Користувач ввів прізвище
                String[] parts = text.split(" ", 4); // Розбиваємо тільки на 2 частини
                if (parts.length < 3) {
                    sendMsg(chatId, "❌ Вкажи прізвище та номер лаби! Наприклад: /reg Шевченко 8");
                    return;
                }
                String name = parts[1]+" "+parts[2];
                // Запам'ятовуємо ім'я для цього чату
                tempNames.put(chatId, name);

                // Викликаємо меню з кнопками
                sendTypeButtons(chatId);
            }
            else if (text.equals("/list")) {
                if (queue.isEmpty()) {
                    sendMsg(chatId, "Черга порожня 😴");
                } else {
                    StringBuilder sb = new StringBuilder("📋 Поточна черга:\n");
                    // Копіюємо чергу, щоб не зіпсувати основну при переборі
                    PriorityQueue<Student> tempQueue = new PriorityQueue<>(queue);
                    int i = 1;
                    while (!tempQueue.isEmpty()) {
                        sb.append(i++).append(". ").append(tempQueue.poll().toString()).append("\n");
                    }
                    sendMsg(chatId, sb.toString());
                }
            }
            else if (text.equals("/next")) {
                Student nextStudent = queue.poll(); // Дістає і видаляє першого
                if (nextStudent == null) {
                    sendMsg(chatId, "Всі вільні! Черга порожня.");
                } else {
                    sendMsg(chatId, "Заходить: " + nextStudent.getName() + " ✅");
                    // Можна навіть написати студенту в особисті, що його черга
                    sendMsg(nextStudent.getChatId(), "Твоя черга! Заходь! 🏃‍♂️");
                }
            }
        }
        // 2. ОБРОБКА НАТИСКАННЯ КНОПОК (CALLBACK)
        else if (update.hasCallbackQuery()) {
            String callData = update.getCallbackQuery().getData();
            Long chatId = update.getCallbackQuery().getMessage().getChatId();

            // Перевіряємо, чи ми знаємо ім'я цього студента
            if (!tempNames.containsKey(chatId)) {
                sendMsg(chatId, "⚠️ Помилка! Спочатку введи /reg Прізвище");
                return;
            }

            String name = tempNames.get(chatId);

            // Створюємо студента і додаємо в чергу
            Student newStudent = new Student(name, chatId, callData.charAt(0));
            queue.add(newStudent);
            saveState(); // Зберігаємо у файл

            // Видаляємо з тимчасової пам'яті
            tempNames.remove(chatId);

            sendMsg(chatId, "✅ Готово! Ти в черзі з пріорітетом " + callData);
        }
    }

    // Допоміжний метод для відправки повідомлень
    private void sendMsg(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    // Main метод для запуску
    public static void main(String[] args) {
        try {
            DefaultBotOptions botOptions = new DefaultBotOptions();
            botOptions.setGetUpdatesTimeout(60);
            botOptions.setAllowedUpdates(List.of("message", "callback_query"));
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(new QueueBot(botOptions));
            System.out.println("Бот запущено! 🚀");
            Thread.currentThread().join();
        } catch (TelegramApiException e) {
            e.printStackTrace();
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            main(args);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    public QueueBot(DefaultBotOptions botSession) {
        super(botSession);
        loadState();
    }

    private void saveState() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("queue.dat"))) {
            oos.writeObject(queue);
            System.out.println("Стан збережено!");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Метод для завантаження (викликаємо при старті бота)
    private void loadState() {
        File file = new File("queue.dat");
        if (!file.exists()) return; // Якщо файлу немає - нічого не робимо

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            // Зчитуємо об'єкт і приводимо його до типу PriorityQueue
            PriorityQueue<Student> loadedQueue = (PriorityQueue<Student>) ois.readObject();

            queue.clear();
            queue.addAll(loadedQueue);

            System.out.println("Відновлено студентів з файлу: " + queue.size());
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    private void sendTypeButtons(Long chatId) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("👇 Що будемо робити?");

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        // Ряд 1
        List<InlineKeyboardButton> row1 = new ArrayList<>();

        // Кнопка 1: Завдання
        var btnTask = new InlineKeyboardButton();
        btnTask.setText("⚡ Хочу доздати");
        btnTask.setCallbackData("B"); // Це поле прилетить нам в код

        // Кнопка 2: Захист
        var btnDefense = new InlineKeyboardButton();
        btnDefense.setText("🎓 Починаю захист");
        btnDefense.setCallbackData("A");

        row1.add(btnTask);
        row1.add(btnDefense);

        rows.add(row1);
        markup.setKeyboard(rows);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
