Telegram bot created for the purpose of helping people to buy tickets for certain events online and notifying them
about new events. Also forwards all non-command messages from users to the bot admin.
Before starting the bot, first create a bot.properties file in the root project directory and write admin and owner
chat id, bot id and bot token there.

Main functionality:

/start — displays a greeting message and subscribes the user to notifications from bot

/stop  — unsubscribes user

Admin functionality:

/setgreeting — when used as a reply to a message, sets the new greeting message from the bot

/setbutton — when used as a reply to a message, sets the new text of the message from the bot that is displayed when
user clicks the button under the greeting message

/reply *arguments* — when used as a reply to a forwarded message from a specific user, sends a message with provided
arguments to that user

/stats — display usage statistics

/mail — when used as a reply to message, mails the message to all bot subscribers.
Buttons can be attached to the mailing message with /button command, example:

*sample mailing message text*

/button link

https://...

/button some text button

this is text which should be displayed when user clicks the "some text button" button

... etc
