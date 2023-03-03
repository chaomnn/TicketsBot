import bot.Bot;
import bot.BotHelper;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Application {

    public static void main(String[] args) {
        TelegramBotsApi botsApi;
        try {
            botsApi = new TelegramBotsApi(DefaultBotSession.class);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
        var tokens = BotHelper.getBotTokens();
        for (String token : tokens) {
            try {
                botsApi.registerBot(new Bot(token));
            } catch (TelegramApiException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
