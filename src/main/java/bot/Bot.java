package bot;

import bot.commands.MailCommand;
import bot.commands.ReplyCommand;
import bot.commands.SetTextCommand;
import bot.commands.StartCommand;
import bot.commands.StatsCommand;
import bot.commands.StopCommand;
import db.DatabaseManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class Bot extends TelegramLongPollingCommandBot {

    private final String username;
    private final String token;

    private static final String BUY_TICKET_CALLBACK = "buy_ticket";
    private static final String SET_GREETING_COMMAND = "setgreeting";
    private static final String DESCRIPTION_GREETING = "Set the greeting";
    private static final String SET_BUTTON_COMMAND = "setbutton";
    private static final String DESCRIPTION_BUTTON  = "Set the button text";
    private static final String HTML = "HTML";

    private static final String CONFIRMATION = "Ваше сообщение было переслано организаторам.";

    public Bot(String username, String token) {
        super();
        this.username = username;
        this.token = token;
        register(new StartCommand());
        register(new StopCommand());
        register(new SetTextCommand(SET_GREETING_COMMAND, DESCRIPTION_GREETING));
        register(new SetTextCommand(SET_BUTTON_COMMAND, DESCRIPTION_BUTTON));
        register(new MailCommand());
        register(new StatsCommand());
        register(new ReplyCommand());
    }

    @Override
    public String getBotUsername() {
        return username;
    }

    @Override
    public String getBotToken() {
        return token;
    }

    @Override
    public void processNonCommandUpdate(Update update) {
        var message = update.getMessage();
        try {
            if (update.hasMessage() && message.isUserMessage()) {
                // Forward message to the admin
                var chatId = message.getChatId();
                if (!chatId.equals(BotHelper.getBotOwner())) {
                    execute(ForwardMessage.builder()
                            .messageId(message.getMessageId())
                            .fromChatId(chatId)
                            .chatId(BotHelper.getBotAdmin())
                            .build());
                    execute(SendMessage.builder()
                            .chatId(chatId)
                            .text(CONFIRMATION)
                            .build());
                    var user = update.getMessage().getFrom();
                    Logger.getRootLogger().log(Level.INFO, " Forwarding message from user: first name: " +
                            user.getFirstName() + ", last name: " +
                            user.getLastName() + ", userId " +
                            user.getId() + " to bot admin");
                }
            } else if (update.hasCallbackQuery()) {
                // Display instructions
                var callbackData = update.getCallbackQuery().getData();
                if (callbackData.equals(BUY_TICKET_CALLBACK)) {
                    execute(SendMessage.builder()
                            .chatId(update.getCallbackQuery().getMessage().getChatId())
                            .text(DatabaseManager.getInstance().getConstant(SET_BUTTON_COMMAND))
                            .parseMode(HTML)
                            .disableWebPagePreview(true)
                            .build());
                } else {
                    execute(SendMessage.builder()
                            .chatId(update.getCallbackQuery().getMessage().getChatId())
                            .text(callbackData)
                            .parseMode(HTML)
                            .disableWebPagePreview(true)
                            .build());
                }
            }
        } catch (TelegramApiException e) {
            BotHelper.reportException(this, e);
            e.printStackTrace();
        }
    }
}
