package bot.commands;

import bot.Bot;
import bot.BotHelper;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class StopCommand extends BotCommand {

    private static final String STOP_COMMAND = "stop";
    private static final String DESCRIPTION = "Stop the bot";
    private static final String STOP_MESSAGE = "Вы отписались от рассылки.";

    public StopCommand() {
        super(STOP_COMMAND, DESCRIPTION);
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
        var userId = user.getId();
        // Remove user from DB
        ((Bot) absSender).getDatabaseManager().manageUserSubscription(userId, false);
        try {
            absSender.execute(SendMessage.builder()
                    .chatId(chat.getId().toString())
                    .text(STOP_MESSAGE)
                    .build());
        } catch (TelegramApiException e) {
            BotHelper.reportException(absSender, e);
            e.printStackTrace();
        }
    }
}
