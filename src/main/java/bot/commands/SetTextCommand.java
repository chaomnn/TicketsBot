package bot.commands;

import bot.Bot;
import bot.BotHelper;
import bot.utils.MarkupUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class SetTextCommand extends BotCommand {

    private static final String SET_GREETING_COMMAND = "setgreeting";
    private static final String SET_BUTTON_COMMAND = "setbutton";
    private static final String SUCCESS_MESSAGE = "Текст успешно изменен";
    private static final String ERROR_GREETING_EMPTY =
            "Чтобы поменять приветствие, ответь на сообщение командой /setgreeting," +
                    "а чтобы поменять инструкцию по покупке билетов - командой /setbutton";

    public SetTextCommand(String commandIdentifier, String description) {
        super(commandIdentifier, description);
    }

    @Override
    public void processMessage(AbsSender absSender, Message message, String[] arguments) {
        super.processMessage(absSender, message, arguments);
        var replyToMessage = message.getReplyToMessage();
        var chatId = message.getChatId();
        Logger.getRootLogger().log(Level.INFO, "SetTextCommand, chatId: " + chatId);
        if (chatId.equals(BotHelper.getBotAdmin()) || chatId.equals(BotHelper.getBotOwner())) {
            try {
                if (replyToMessage == null) {
                    absSender.execute(SendMessage.builder()
                            .chatId(chatId)
                            .text(ERROR_GREETING_EMPTY)
                            .build());
                    return;
                }
                Logger.getRootLogger().log(Level.INFO, "Setting new bot text");
                ((Bot) absSender).getDatabaseManager().setConstant(getCommandIdentifier().equals(SET_GREETING_COMMAND)
                                ? SET_GREETING_COMMAND : SET_BUTTON_COMMAND,
                        MarkupUtils.getMessageTextWithMarkup(replyToMessage));
                absSender.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text(SUCCESS_MESSAGE)
                        .build());
            } catch (TelegramApiException e) {
                BotHelper.reportException(absSender, e);
                e.printStackTrace();
            }
        } else {
            Logger.getRootLogger().log(Level.WARN, "User " + chatId + " is not authorized to use setText command");
        }
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {}
}
