package bot.utils;

import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;

public class MarkupUtils {

    private static final String ENTITY_TYPE_LINK = "text_link";
    private static final String ENTITY_TYPE_BOLD = "bold";
    private static final String ENTITY_TYPE_ITALIC = "italic";

    public static String getMessageTextWithMarkup(Message message) {
        var editedText = message.hasPhoto() ? message.getCaption() : message.getText();
        var entities = message.hasPhoto() ? message.getCaptionEntities() : message.getEntities();
        if (entities != null && !entities.isEmpty()) {
            for (MessageEntity e : entities) {
                var text = e.getText().trim();
                switch (e.getType()) {
                    case ENTITY_TYPE_LINK:
                        editedText = editedText.replaceFirst(text,
                                "<a href=\"" + e.getUrl() + "\">" + text + "</a>");
                        continue;
                    case ENTITY_TYPE_BOLD:
                        editedText = editedText.replace(text, "<b>" + text + "</b>");
                        continue;
                    case ENTITY_TYPE_ITALIC:
                        editedText = editedText.replace(text, "<i>" + text + "</i>");
                        continue;
                    default:
                }
            }
        }
        return editedText;
    }
}
