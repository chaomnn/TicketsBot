package bot.commands;

import bot.BotHelper;
import db.DatabaseManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;

public class StartCommand extends BotCommand {

    private static final String START_COMMAND = "start";
    private static final String DESCRIPTION = "Start the bot";
    private static final String BUY_TICKET_CALLBACK = "buy_ticket";
    private static final String GREETING_KEY = "setgreeting";

    private static final String DEFAULT_MESSAGE = "На данный момент нет доступных к покупке билетов.";
    private static final String BUY_TICKET = "Купить билет";

    public StartCommand() {
        super(START_COMMAND, DESCRIPTION);
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
        var userId = user.getId();
        Logger.getRootLogger().log(Level.INFO, "StartCommand from user: userId: " + userId +
                ", first name " + user.getFirstName() + ", last name: " + user.getLastName());
        if (!DatabaseManager.getInstance().isUserSubscribed(userId)) {
            // Add user to DB
            DatabaseManager.getInstance().manageUserSubscription(userId, true);
        }
        var buttonsList = new ArrayList<InlineKeyboardButton>();
        buttonsList.add(InlineKeyboardButton.builder()
                .text(BUY_TICKET)
                .callbackData(BUY_TICKET_CALLBACK)
                .build());
        try {
            var greeting = DatabaseManager.getInstance().getConstant(GREETING_KEY);
            absSender.execute(SendMessage.builder()
                    .chatId(chat.getId().toString())
                    .text(greeting.isEmpty() ? DEFAULT_MESSAGE : greeting)
                    .replyMarkup(InlineKeyboardMarkup.builder()
                            .keyboardRow(buttonsList)
                            .build())
                    .build());
        } catch (TelegramApiException e) {
            BotHelper.reportException(absSender, e);
            e.printStackTrace();
        }
    }
}
