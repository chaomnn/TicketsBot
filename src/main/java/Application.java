import bot.Bot;
import bot.BotHelper;
import db.DatabaseManager;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Application {

    public static void main(String[] args) {
        try {
            DatabaseManager.getInstance().connect();
            var botsApi = new TelegramBotsApi(DefaultBotSession.class);
            var properties = BotHelper.getProperties();
            if (properties != null) {
                botsApi.registerBot(new Bot(properties.getProperty("username"), properties.getProperty("token")));
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
