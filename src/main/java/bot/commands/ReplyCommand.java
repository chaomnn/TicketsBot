package bot.commands;

import bot.BotHelper;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class ReplyCommand extends BotCommand {

    private static final String REPLY_COMMAND = "reply";
    private static final String DESCRIPTION = "Reply to user's message";
    private static final String HELP_MSG =
            "Команда /reply должна быть использована в качестве ответа на сообщение пользователя";
    private static final String REPLY_PREFIX = "Ответ администратора: ";

    public ReplyCommand() {
        super(REPLY_COMMAND, DESCRIPTION);
    }

    @Override
    public void processMessage(AbsSender absSender, Message message, String[] arguments) {
        super.processMessage(absSender, message, arguments);
        var chatId = message.getChatId();
        var replyToMessage = message.getReplyToMessage();
        if (chatId.equals(BotHelper.getBotAdmin()) || chatId.equals(BotHelper.getBotOwner())) {
            try {
                if (replyToMessage == null) {
                    absSender.execute(SendMessage.builder()
                            .text(HELP_MSG)
                            .chatId(chatId)
                            .build());
                    return;
                }
                absSender.execute(SendMessage.builder()
                        .text(REPLY_PREFIX + message.getText().substring(REPLY_COMMAND.length() + 2))
                        .chatId(replyToMessage.getForwardFrom().getId())
                        .build());
            } catch (TelegramApiException e) {
                BotHelper.reportException(absSender, e);
                e.printStackTrace();
            }
        } else {
            Logger.getRootLogger().log(Level.WARN, "User " + chatId + " is not authorized to use /reply command");
        }
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {}
}
