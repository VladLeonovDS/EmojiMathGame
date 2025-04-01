package org.deathstrix;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Телеграм-бот для игры "Эмодзи-Математика".
 * Игрок должен составить комбинацию из эмодзи, которые преобразуют начальное число,
 * чтобы получить максимально возможный результат.
 */
public class EmojiMathGameBot extends TelegramLongPollingBot {
    private static final Logger log = LoggerFactory.getLogger(EmojiMathGameBot.class);
    private final Map<String, Long> records = new HashMap<>();
    private final Map<String, Integer> currentScores = new HashMap<>();
    private final Map<String, List<String>> usedEmojis = new HashMap<>();
    private final Map<String, EmojiAction> emojiActions = new HashMap<>();

    public EmojiMathGameBot() {
        super("bottoken");
        // Инициализация всех доступных эмодзи-действий
        emojiActions.put("➕", new AddAction(5));
        emojiActions.put("➖", new SubtractAction(3));
        emojiActions.put("✖️", new MultiplyAction(2));
        emojiActions.put("➗", new DivideAction(2));
        emojiActions.put("🔼", new AddAction(10));
        emojiActions.put("🔽", new SubtractAction(5));
        emojiActions.put("🔢", new AppendLastDigitAction());
        emojiActions.put("🔀", new ShuffleDigitsAction());
        emojiActions.put("🔄", new ReverseNumberAction());
        emojiActions.put("📈", new AddLastTwoDigitsAction());
        emojiActions.put("📉", new SubtractLastDigitAction());
        emojiActions.put("🎲", new RandomMultiplyAction());
        emojiActions.put("💯", new MultiplyAction(100));
        emojiActions.put("🔟", new MultiplyAction(10));
        emojiActions.put("1️⃣", new AppendDigitAction(1));
        emojiActions.put("2️⃣", new AppendDigitAction(2));
        emojiActions.put("3️⃣", new AppendDigitAction(3));
        emojiActions.put("4️⃣", new AppendDigitAction(4));
        emojiActions.put("5️⃣", new AppendDigitAction(5));
    }

    /**
     * Интерфейс для действий, выполняемых эмодзи
     */
    public interface EmojiAction {
        /**
         * Применяет действие к числу
         * @param num исходное число
         * @return результат применения действия
         */
        long apply(long num);

        /**
         * Возвращает описание действия
         * @return строковое описание
         */
        String getDescription();
    }

    /**
     * Класс для действия сложения
     */
    public static class AddAction implements EmojiAction {
        private final int value;

        /**
         * @param value число, которое будет прибавляться
         */
        public AddAction(int value) {
            this.value = value;
        }

        @Override
        public long apply(long num) {
            return num + value;
        }

        @Override
        public String getDescription() {
            return "Прибавляет " + value + " к числу";
        }
    }

    /**
     * Класс для действия вычитания
     */
    public static class SubtractAction implements EmojiAction {
        private final int value;

        /**
         * @param value число, которое будет вычитаться
         */
        public SubtractAction(int value) {
            this.value = value;
        }

        @Override
        public long apply(long num) {
            return num - value;
        }

        @Override
        public String getDescription() {
            return "Вычитает " + value + " из числа";
        }
    }

    /**
     * Класс для действия умножения
     */
    public static class MultiplyAction implements EmojiAction {
        private final int multiplier;

        /**
         * @param multiplier множитель
         */
        public MultiplyAction(int multiplier) {
            this.multiplier = multiplier;
        }

        @Override
        public long apply(long num) {
            return num * multiplier;
        }

        @Override
        public String getDescription() {
            return "Умножает число на " + multiplier;
        }
    }

    /**
     * Класс для действия деления
     */
    public static class DivideAction implements EmojiAction {
        private final int divisor;

        /**
         * @param divisor делитель
         */
        public DivideAction(int divisor) {
            this.divisor = divisor;
        }

        @Override
        public long apply(long num) {
            return num / divisor;
        }

        @Override
        public String getDescription() {
            return "Делит число на " + divisor;
        }
    }

    /**
     * Класс для действия добавления последней цифры
     */
    public static class AppendLastDigitAction implements EmojiAction {
        @Override
        public long apply(long num) {
            return num * 10 + (num % 10);
        }

        @Override
        public String getDescription() {
            return "Добавляет последнюю цифру числа в конец";
        }
    }

    /**
     * Класс для действия перемешивания цифр
     */
    public static class ShuffleDigitsAction implements EmojiAction {
        @Override
        public long apply(long num) {
            String s = String.valueOf(num);
            List<Character> chars = s.chars().mapToObj(c -> (char)c).collect(Collectors.toList());
            Collections.shuffle(chars);
            String shuffled = chars.stream().map(String::valueOf).collect(Collectors.joining());
            return Long.parseLong(shuffled);
        }

        @Override
        public String getDescription() {
            return "Перемешивает цифры числа";
        }
    }

    /**
     * Класс для действия разворота числа
     */
    public static class ReverseNumberAction implements EmojiAction {
        @Override
        public long apply(long num) {
            return Long.parseLong(new StringBuilder(String.valueOf(num)).reverse().toString());
        }

        @Override
        public String getDescription() {
            return "Переворачивает число задом наперед";
        }
    }

    /**
     * Класс для действия добавления последних двух цифр
     */
    public static class AddLastTwoDigitsAction implements EmojiAction {
        @Override
        public long apply(long num) {
            return num + (num % 100);
        }

        @Override
        public String getDescription() {
            return "Прибавляет к числу его последние две цифры";
        }
    }

    /**
     * Класс для действия вычитания последней цифры
     */
    public static class SubtractLastDigitAction implements EmojiAction {
        @Override
        public long apply(long num) {
            return num - (num % 10);
        }

        @Override
        public String getDescription() {
            return "Вычитает из числа его последнюю цифру";
        }
    }

    /**
     * Класс для действия случайного умножения
     */
    public static class RandomMultiplyAction implements EmojiAction {
        @Override
        public long apply(long num) {
            return num * (2 + (long)(Math.random() * 5));
        }

        @Override
        public String getDescription() {
            return "Умножает число на случайное значение от 2 до 6";
        }
    }

    /**
     * Класс для действия добавления конкретной цифры
     */
    public static class AppendDigitAction implements EmojiAction {
        private final int digit;

        /**
         * @param digit цифра для добавления
         */
        public AppendDigitAction(int digit) {
            this.digit = digit;
        }

        @Override
        public long apply(long num) {
            return num * 10 + digit;
        }

        @Override
        public String getDescription() {
            return "Добавляет цифру " + digit + " в конец числа";
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            handleMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            handleCallbackQuery(update.getCallbackQuery().getMessage(),
                    update.getCallbackQuery().getData());
        }
    }

    private void handleMessage(Message message) {
        String text = message.getText();
        String chatId = message.getChatId().toString();
        String userId = message.getFrom().getId().toString();

        if (text.equals("/start")) {
            sendWelcomeMessage(chatId);
        } else if (text.equals("/play")) {
            startNewGame(chatId, userId);
        } else if (text.equals("/records")) {
            showRecords(chatId);
        } else if (text.equals("/help")) {
            showHelp(chatId);
        } else if (text.startsWith("Комбинация: ")) {
            processEmojiCombination(chatId, userId, text.substring("Комбинация: ".length()));
        } else {
            sendMessage(chatId, "Неизвестная команда. Используйте /play чтобы начать игру.");
        }
    }

    private void handleCallbackQuery(Message message, String data) {
        String chatId = message.getChatId().toString();
        String userId = data.split(":")[1];

        if (data.startsWith("restart:")) {
            startNewGame(chatId, userId);
        }
    }

    private void sendWelcomeMessage(String chatId) {
        String welcomeText = """
                🎮 Добро пожаловать в игру Эмодзи-Математика! 🎮
                
                Правила игры:
                1. Вам дается начальное число и набор эмодзи.
                2. Каждое эмодзи выполняет математическое действие с числом.
                3. Вам нужно составить комбинацию из 8 эмодзи, чтобы получить максимально возможное число.
                4. Каждое эмодзи можно использовать только один раз за игру.
                
                Используйте /play чтобы начать новую игру.
                Используйте /records чтобы посмотреть рекорды.""";

        sendMessage(chatId, welcomeText);
    }

    private void startNewGame(String chatId, String userId) {
        currentScores.put(userId, 1); // Начинаем с 1
        usedEmojis.put(userId, new ArrayList<>());

        // Выбираем случайные 15 эмодзи из доступных
        List<String> availableEmojis = new ArrayList<>(emojiActions.keySet());
        Collections.shuffle(availableEmojis);
        List<String> gameEmojis = availableEmojis.subList(0, 15);

        String emojiList = String.join(" ", gameEmojis);

        String messageText = "🔢 Начальное число: 1\n\n" +
                "Доступные эмодзи:\n" + emojiList + "\n\n" +
                "Отправьте комбинацию из 8 эмодзи в формате:\n" +
                "Комбинация: ➕ ✖️ 🔼 🔢 🔀 🔟 1️⃣ 💯";

        sendMessage(chatId, messageText);
        showHelp(chatId);
    }

    private void processEmojiCombination(String chatId, String userId, String emojiCombination) {
        if (!currentScores.containsKey(userId)) {
            sendMessage(chatId, "Сначала начните игру с помощью /play");
            return;
        }

        List<String> combination = Arrays.stream(emojiCombination.split(" "))
                .filter(s -> !s.isEmpty())
                .toList();

        if (combination.size() != 8) {
            sendMessage(chatId, "Нужно выбрать ровно 8 эмодзи!");
            return;
        }

        List<String> used = usedEmojis.get(userId);
        long currentNumber = currentScores.get(userId);

        // Проверяем, что все эмодзи уникальны и допустимы
        Set<String> uniqueEmojis = new HashSet<>();
        for (String emoji : combination) {
            if (!emojiActions.containsKey(emoji)) {
                sendMessage(chatId, "Эмодзи '" + emoji + "' не допустимо в этой игре!");
                return;
            }

            if (!uniqueEmojis.add(emoji)) {
                sendMessage(chatId, "Эмодзи '" + emoji + "' используется более одного раза!");
                return;
            }

            if (used.contains(emoji)) {
                sendMessage(chatId, "Эмодзи '" + emoji + "' уже использовалось в этой игре!");
                return;
            }
        }

        // Применяем эмодзи по очереди
        StringBuilder steps = new StringBuilder("🔢 Начальное число: " + currentNumber + "\n\n");

        for (String emoji : combination) {
            long newNumber = emojiActions.get(emoji).apply(currentNumber);
            steps.append(emoji).append(": ").append(currentNumber).append(" → ").append(newNumber).append("\n");
            currentNumber = newNumber;
            used.add(emoji);
        }

        // Обновляем текущий счет
        currentScores.put(userId, (int) currentNumber);

        // Проверяем рекорд
        long record = records.getOrDefault(userId, 0L);
        boolean isNewRecord = currentNumber > record;

        if (isNewRecord) {
            records.put(userId, currentNumber);
        }

        // Формируем результат
        String resultText = steps + "\n" +
                "🎉 Итоговое число: " + currentNumber + "\n" +
                (isNewRecord ? "🏆 Новый рекорд!" : "Текущий рекорд: " + record) + "\n\n" +
                "Хотите сыграть еще раз?";

        // Создаем клавиатуру с кнопкой "Играть еще"
        SendMessage message = getMessage(chatId, userId, resultText);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка: ", e);
        }
    }

    private static SendMessage getMessage(String chatId, String userId, String resultText) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        InlineKeyboardButton playAgainButton = new InlineKeyboardButton();
        playAgainButton.setText("Играть еще");
        playAgainButton.setCallbackData("restart:" + userId);

        List<InlineKeyboardButton> row = new ArrayList<>();
        row.add(playAgainButton);

        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();
        keyboard.add(row);
        keyboardMarkup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(resultText);
        message.setReplyMarkup(keyboardMarkup);
        return message;
    }

    private void showRecords(String chatId) {
        if (records.isEmpty()) {
            sendMessage(chatId, "Рекордов пока нет. Сыграйте первую игру с помощью /play!");
            return;
        }

        // Сортируем записи по убыванию
        List<Map.Entry<String, Long>> sortedRecords = records.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .toList();

        StringBuilder recordsText = new StringBuilder("🏆 ТОП рекордов:\n\n");

        for (int i = 0; i < Math.min(sortedRecords.size(), 10); i++) {
            Map.Entry<String, Long> entry = sortedRecords.get(i);
            recordsText.append(i + 1).append(". ID: ").append(entry.getKey())
                    .append(" - ").append(entry.getValue()).append("\n");
        }

        sendMessage(chatId, recordsText.toString());
    }

    private void sendMessage(String chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Ошибка: ", e);
        }
    }

    private void showHelp(String chatId) {
        StringBuilder helpText = new StringBuilder("📚 Список возможных эмодзи и их действий:\n\n");

        emojiActions.forEach((emoji, action) -> helpText.append(emoji).append(" - ").append(action.getDescription()).append("\n"));

        helpText.append("\nИспользуйте /play чтобы начать новую игру.");

        sendMessage(chatId, helpText.toString());
    }

    @Override
    public String getBotUsername() {
        return "EmojiSpeller";
    }
}