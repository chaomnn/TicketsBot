package bot.commands;

import bot.Bot;
import bot.BotHelper;
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
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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

    private static final int LIMIT = 30;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private static final int delay = 2;

    public MailCommand() {
        super(MAIL_COMMAND, DESCRIPTION);
    }

    @Override
    public void processMessage(AbsSender absSender, Message message, String[] arguments) {
        super.processMessage(absSender, message, arguments);
        var replyToMessage = message.getReplyToMessage();
        var chatId = message.getChatId();
        Logger.getRootLogger().log(Level.INFO, "MailCommand, chatId: " + chatId);
        if (chatId.equals(BotHelper.getBotAdmin()) || chatId.equals(BotHelper.getBotOwner())) {
            try {
                if (replyToMessage == null) {
                    absSender.execute(SendMessage.builder()
                            .chatId(chatId)
                            .text(HELP_MSG)
                            .build());
                    return;
                }
                var subscribers = DatabaseManager.getInstance().getAllSubscribers();
                InputFile inputFile = null;
                var inlineButtons = new InlineKeyboardMarkup();
                inlineButtons.setKeyboard(new ArrayList<>());

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
                            String buttonId = UUID.randomUUID().toString();
                            DatabaseManager.getInstance().setConstant(buttonId, content);
                            inlineButton.setCallbackData(buttonId);
                        }
                        buttonsList.add(Collections.singletonList(inlineButton));
                    }
                    inlineButtons.setKeyboard(buttonsList);
                }
                var entities = replyToMessage.hasPhoto() ?
                        replyToMessage.getCaptionEntities() : replyToMessage.getEntities();
                // Filter entities so they don't include button entities
                if (entities != null && texts.length > 1) {
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
                }
                // Split users into lists of 30 elements
                List<List<Long>> listOfLists = new ArrayList<>();
                if (subscribers.size() < LIMIT) {
                    listOfLists.add(subscribers);
                } else {
                    var amount = subscribers.size() / LIMIT;
                    var end = 0;
                    for (var begin = 0; begin < amount; begin++) {
                        end += 30;
                        listOfLists.add(subscribers.subList(begin*LIMIT, end));
                    }
                    if (end < subscribers.size()) {
                        listOfLists.add(subscribers.subList(end, subscribers.size()));
                    }
                }
                var file = inputFile;
                var textEntities = entities;
                // Send message to subscribers with 2 seconds delay per 30 users because of Telegram API limitations
                Logger.getRootLogger().log(Level.INFO, "Starting to mail message to subscribers");
                for (var i = 0; i < listOfLists.size(); i++) {
                    var ind = i;
                    scheduler.schedule(() ->
                                    sendMessageToSubscribers(listOfLists.get(ind), absSender, replyToMessage.hasPhoto(),
                                            file, textEntities, inlineButtons, texts[0], replyToMessage.getMessageId()),
                            (long) delay*i,TimeUnit.SECONDS);
                }
                scheduler.schedule(() -> absSender.execute(SendMessage.builder()
                        .text(MAIL_CONFIRMATION + subscribers.size())
                        .chatId(message.getChatId())
                        .build()), (long) delay *listOfLists.size(), TimeUnit.SECONDS);
            } catch (TelegramApiException e) {
                BotHelper.reportException(absSender, e);
                e.printStackTrace();
            }
        } else {
            Logger.getRootLogger().log(Level.WARN, "User " + chatId + " is not authorized to use /mail command");
        }
    }

    private void sendMessageToSubscribers(List<Long> subscribers, AbsSender absSender, boolean isPhoto,
                                          InputFile photo, List<MessageEntity> entities,
                                          InlineKeyboardMarkup inlineButtons, String text, int messageId) {
        for (var subscriberId : subscribers) {
            if (!DatabaseManager.getInstance().hasUserBeenMailed(subscriberId, messageId)) {
                try {
                    if (isPhoto && photo != null) {
                        absSender.execute(SendPhoto.builder()
                                .chatId(subscriberId)
                                .photo(photo)
                                .captionEntities(entities)
                                .replyMarkup(inlineButtons)
                                .caption(text)
                                .build());
                        DatabaseManager.getInstance().userMailedSuccess(subscriberId, messageId);
                    } else {
                        absSender.execute(SendMessage.builder()
                                .entities(entities)
                                .chatId(subscriberId)
                                .text(text)
                                .replyMarkup(inlineButtons)
                                .build());
                        DatabaseManager.getInstance().userMailedSuccess(subscriberId, messageId);
                    }
                } catch (TelegramApiException e) {
                    e.printStackTrace();
                    BotHelper.reportException(absSender, e);
                }
            }
        }
    }

    @Override
    public void execute(AbsSender absSender, User user, Chat chat, String[] arguments) {}
}
