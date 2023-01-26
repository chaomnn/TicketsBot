package bot.commands;

import bot.Bot;
import bot.BotHelper;
import bot.BotUtils;
import bot.utils.MarkupUtils;
import db.DatabaseManager;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.telegram.telegrambots.extensions.bots.commandbot.commands.BotCommand;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class MailCommand extends BotCommand {

    private static final String MAIL_COMMAND = "mail";
    private static final String DESCRIPTION = "Forward the message to subscribers";
    private static final String ATTACH_BUTTON = "/button";
    private static final String LINK_PREFIX = "http";

    private static final String MAIL_CONFIRMATION = "Рассылка отправлена стольким людям: ";
    private static final String HELP_MSG =
            "Чтобы разослать сообщение, ответь на него командой /mail. Чтобы прикрепить к сообщению кнопку(и), " +
                    "допиши /button *название кнопки* в конце сообщения рассылки, и напиши ее текст или ссылку " +
                    "на следущей строке";

    public MailCommand() {
        super(MAIL_COMMAND, DESCRIPTION);
    }

    @Override
    public void processMessage(AbsSender absSender, Message message, String[] arguments) {
        super.processMessage(absSender, message, arguments);
        var replyToMessage = message.getReplyToMessage();
        var chatId = message.getChatId();
        Logger.getRootLogger().log(Level.INFO, "MailCommand, chatId: " + chatId);
        if (chatId.equals(BotUtils.ID_BOT_ADMIN) || chatId.equals(BotUtils.ID_BOT_OWNER)) {
            try {
                if (replyToMessage == null) {
                    absSender.execute(SendMessage.builder()
                            .chatId(chatId)
                            .text(HELP_MSG)
                            .build());
                    return;
                }
                var subscribers = DatabaseManager.getInstance().getAllSubscribers();
                subscribers.remove(BotUtils.ID_BOT_ADMIN);
                subscribers.remove(BotUtils.ID_BOT_OWNER);
                InputFile inputFile = null;
                var inlineButtons = new InlineKeyboardMarkup();

                if (replyToMessage.hasPhoto()) {
                    // Get the picture from message
                    var photoSizes = replyToMessage.getPhoto();
                    var filePath = absSender
                            .execute(new GetFile(photoSizes.get(photoSizes.size() - 1).getFileId())).getFilePath();
                    inputFile = new InputFile(((Bot) absSender).downloadFile(filePath));
                }
                var mailingText = replyToMessage.hasPhoto() ? replyToMessage.getCaption() : replyToMessage.getText();
                var texts = mailingText.split("(^|\\n)/button: *");
                if (mailingText.contains(ATTACH_BUTTON)) {
                    // Attach button(s) to message
                    List<List<InlineKeyboardButton>> buttonsList = new ArrayList<>();
                    var textsWithMarkup =
                            MarkupUtils.getMessageTextWithMarkup(replyToMessage).split("(^|\\n)/button: *");
                    for (var i = 1; i < textsWithMarkup.length; i++) {
                        var inlineButton = new InlineKeyboardButton();
                        var buttonContents = textsWithMarkup[i].split("\n", 2);
                        var name = buttonContents[0].trim();
                        inlineButton.setText(name);
                        var content = buttonContents[1].trim();
                        if (content.startsWith(LINK_PREFIX)) {
                            // It's a link button
                            inlineButton.setUrl(content);
                        } else {
                            // It's a button with message callback
                            DatabaseManager.getInstance().setConstant(name, content);
                            inlineButton.setCallbackData(name);
                        }
                        buttonsList.add(Collections.singletonList(inlineButton));
                    }
                    inlineButtons.setKeyboard(buttonsList);
                }
                var entities = replyToMessage.hasPhoto() ?
                        replyToMessage.getCaptionEntities() : replyToMessage.getEntities();
                entities = entities
                        .stream()
                        .filter(e -> e.getOffset() < texts[0].length())
                        .peek(e -> {
                            if (e.getOffset() + e.getLength() > texts[0].length()) {
                                var diff = e.getOffset() + e.getLength() - texts[0].length();
                                e.setLength(e.getLength() - diff);
                            }
                        })
                        .collect(Collectors.toList());
                // Send message to subscribers
                Logger.getRootLogger().log(Level.INFO, "Starting to mail message to subscribers");
                for (var subscriberId : subscribers) {
                    if (replyToMessage.hasPhoto() && inputFile != null) {
                        absSender.execute(SendPhoto.builder()
                                .chatId(subscriberId)
                                .photo(inputFile)
                                .captionEntities(entities)
                                .replyMarkup(inlineButtons)
                                .caption(texts[0])
                                .build());
                    } else {
                        absSender.execute(SendMessage.builder()
                                .entities(entities)
                                .chatId(subscriberId)
                                .text(texts[0])
                                .replyMarkup(inlineButtons)
                                .build());
                    }
                }
                absSender.execute(SendMessage.builder()
                        .text(MAIL_CONFIRMATION + subscribers.size())
                        .chatId(message.getChatId())
                        .build());
            } catch (TelegramApiException e) {
                BotHelper.reportException(absSender, e);
                e.printStackTrace();
            }
        } else {
            Logger.getRootLogger().log(Level.WARN, "User " + chatId + " is not authorized to use /mail command");
        }
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {}
}
