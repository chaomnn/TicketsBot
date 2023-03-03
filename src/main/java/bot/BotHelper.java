package bot;

import org.json.JSONObject;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class BotHelper {

    private static final String JSON_FILE = "bot.json";
    private static final String OWNER_KEY = "owner";
    private static final String ADMIN_KEY = "admin";
    private static final String BOTS_KEY = "bots";
    
    private static volatile JSONObject jsonObject;

    public static void reportException(AbsSender bot, Exception exception) {
        try {
            bot.execute(SendMessage.builder()
                    .chatId(BotHelper.getBotOwner())
                    .text(exception.getMessage())
                    .build());
            bot.execute(SendMessage.builder()
                    .chatId(BotHelper.getBotAdmin())
                    .text(exception.getMessage())
                    .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private static JSONObject getConfigJson() {
        if (jsonObject == null) {
            synchronized (BotHelper.class) {
                if (jsonObject == null) {
                    try {
                        String content = new String(Files.readAllBytes(Paths.get(JSON_FILE)));
                        jsonObject = new JSONObject(content);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return jsonObject;
    }

    public static long getBotOwner() {
        return getConfigJson().getLong(OWNER_KEY);
    }

    public static long getBotAdmin() {
        return getConfigJson().getLong(ADMIN_KEY);
    }

    public static List<String> getBotTokens() {
        var tokens = getConfigJson().getJSONArray(BOTS_KEY);
        return IntStream.range(0, tokens.length())
                .mapToObj(tokens::getString)
                .collect(Collectors.toList());
    }
}
