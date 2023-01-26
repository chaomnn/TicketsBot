package bot.commands;

import bot.BotHelper;
import db.DatabaseManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class StatsCommand extends BotCommand {

    private static final String MAIL_COMMAND = "stats";
    private static final String DESCRIPTION = "Display usage statistics";

    private static final String STATISTICS = "Статистика\nКоличество подписчиков бота: ";

    public StatsCommand() {
        super(MAIL_COMMAND, DESCRIPTION);
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {
        var chatId = chat.getId();
        if (chatId.equals(BotHelper.getBotAdmin()) || chatId.equals(BotHelper.getBotOwner())) {
            try {
                absSender.execute(SendMessage.builder()
                        .text(STATISTICS + DatabaseManager.getInstance().getAllSubscribers().size())
                        .chatId(chatId)
                        .build());
            } catch (TelegramApiException e) {
                BotHelper.reportException(absSender, e);
                e.printStackTrace();
            }
        } else {
            Logger.getRootLogger().log(Level.WARN, "User " + chatId + " is not authorized to use /mail command");
        }
    }
}
