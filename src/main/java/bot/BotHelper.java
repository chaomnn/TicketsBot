package bot;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class BotHelper {

    private static final String PROPERTIES_FILE = "bot.properties";
    private static final String OWNER_KEY = "owner";
    private static final String ADMIN_KEY = "admin";

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

    public static Properties getProperties() {
        var botProperties = new Properties();
        try {
            botProperties.load(new FileInputStream(PROPERTIES_FILE));
            return botProperties;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static Long getBotOwner() {
        return Long.valueOf(getProperties().getProperty(OWNER_KEY));
    }

    public static Long getBotAdmin() {
        return Long.valueOf(getProperties().getProperty(ADMIN_KEY));
    }
}
