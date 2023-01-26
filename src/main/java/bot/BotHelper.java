package bot;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class BotHelper {

    public static void reportException(AbsSender bot, Exception exception) {
        try {
            bot.execute(SendMessage.builder()
                    .chatId(BotUtils.ID_BOT_OWNER)
                    .text(exception.getMessage())
                    .build());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
