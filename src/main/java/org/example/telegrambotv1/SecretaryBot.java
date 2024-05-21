package org.example.telegrambotv1;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.ForwardMessage;
import org.telegram.telegrambots.meta.api.methods.GetUserProfilePhotos;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.UserProfilePhotos;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@Component
public class SecretaryBot extends TelegramLongPollingBot implements Serializable {

    private final long adminChatId = 5371435224L; // Replace with the actual numerical chat ID of the admin
    private final String botUsername = "gapirchibot"; // Replace with your bot's username
    private final String botToken = "6878352676:AAFArUv9CSayL_wy0_Rcrxgkzv6tHKKKL0U"; // Replace with your bot's token
    private final String adminUsername = "abdurahmonabdukarim";
    // Map to keep track of which user requested to talk to the admin
    private final Map<String, Long> userRequests = new HashMap<>();
    private List<Long> botUsers = new ArrayList<>();
    // Channel ID
    private final long channelId = -1001864182569L; // Replace with your channel's ID


    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            if (messageText.equals("/start")) {
                sendInitialMessage(chatId);
            }
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            String username = update.getCallbackQuery().getFrom().getUserName();

            handleCallbackQuery(callbackData, chatId, messageId, username);
        }else if (update.hasChannelPost()) {
            Message channelMessage = update.getChannelPost();
            forwardMessageToBotUsers(channelMessage);
        }
    }
    private void registerUser(long chatId) {
        if (!botUsers.contains(chatId)) {
            botUsers.add(chatId);
            saveUsers();
        }
    }

    private void forwardMessageToBotUsers(Message message) {
        for (Long userId : botUsers) {
            forwardMessageToUser(userId, message);
        }
    }

    private void forwardMessageToUser(Long userId, Message message) {
        ForwardMessage forwardMessage = new ForwardMessage();
        forwardMessage.setChatId(userId.toString());
        forwardMessage.setFromChatId(message.getChatId().toString());
        forwardMessage.setMessageId(message.getMessageId());

        try {
            execute(forwardMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }


    private void sendInitialMessage(long chatId) {
        String messageText = "Assalomu alaykum! Do you want to take Abdurahmon's username?";
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData("request_username");
        row.add(yesButton);

        buttons.add(row);
        markup.setKeyboard(buttons);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(messageText);
        message.setReplyMarkup(markup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleCallbackQuery(String callbackData, long chatId, long messageId, String username) {
        if (callbackData.equals("request_username")) {
            notifyAdmin(chatId, username);
        } else if (callbackData.startsWith("admin_response:")) {
            handleAdminResponse(callbackData, chatId, messageId);
        }
    }

    private void notifyAdmin(long requesterChatId, String requesterUsername) {
        try {
            GetUserProfilePhotos getUserProfilePhotos = new GetUserProfilePhotos();
            getUserProfilePhotos.setUserId((Long) requesterChatId);
            UserProfilePhotos photos = execute(getUserProfilePhotos);

            if (photos.getTotalCount() > 0) {
                PhotoSize photo = photos.getPhotos().get(0).get(0); // Get the first photo
                String fileId = photo.getFileId();

                SendPhoto sendPhoto = new SendPhoto();
                sendPhoto.setChatId(String.valueOf(adminChatId));
                sendPhoto.setPhoto(new org.telegram.telegrambots.meta.api.objects.InputFile(fileId));
                sendPhoto.setCaption("User @" + requesterUsername + " wants to talk with you. Would you like to share your username?");

                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
                List<InlineKeyboardButton> row = new ArrayList<>();

                InlineKeyboardButton yesButton = new InlineKeyboardButton();
                yesButton.setText("Yes");
                yesButton.setCallbackData("admin_response:yes:" + requesterChatId);
                row.add(yesButton);

                InlineKeyboardButton noButton = new InlineKeyboardButton();
                noButton.setText("No");
                noButton.setCallbackData("admin_response:no:" + requesterChatId);
                row.add(noButton);

                buttons.add(row);
                markup.setKeyboard(buttons);
                sendPhoto.setReplyMarkup(markup);

                // Store the requester's chat ID in the map
                userRequests.put(requesterUsername, requesterChatId);

                execute(sendPhoto);
            } else {
                sendTextNotificationToAdmin(requesterChatId, requesterUsername);
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
            sendTextNotificationToAdmin(requesterChatId, requesterUsername);
        }
    }
    private void sendTextNotificationToAdmin(long requesterChatId, String requesterUsername) {
        String messageText = "User @" + requesterUsername + " wants to talk with you. Would you like to share your username?";

        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> buttons = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton yesButton = new InlineKeyboardButton();
        yesButton.setText("Yes");
        yesButton.setCallbackData("admin_response:yes:" + requesterChatId);
        row.add(yesButton);

        InlineKeyboardButton noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData("admin_response:no:" + requesterChatId);
        row.add(noButton);

        buttons.add(row);
        markup.setKeyboard(buttons);

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(adminChatId)); // Use numerical chat ID
        message.setText(messageText);
        message.setReplyMarkup(markup);

        // Store the requester's chat ID in the map
        userRequests.put(requesterUsername, requesterChatId);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void handleAdminResponse(String callbackData, long chatId, long messageId) {
        String[] parts = callbackData.split(":");
        if (parts.length < 3) {
            // Log an error or handle the case where callback data is malformed
            System.err.println("Invalid callback data: " + callbackData);
            return;
        }

        String response = parts[1];
        long requesterChatId;
        try {
            requesterChatId = Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            // Log an error or handle the case where the chat ID is not a valid number
            System.err.println("Invalid requester chat ID: " + parts[2]);
            return;
        }

        try {
            // Delete the original message
            execute(new DeleteMessage(String.valueOf(chatId), (int) messageId));

            // Send a new message with the updated text
            String newMessageText = "Javobingiz qabul qilindi ❕❕❕";

            SendMessage newMessage = new SendMessage();
            newMessage.setChatId(String.valueOf(chatId));
            newMessage.setText(newMessageText);

            execute(newMessage);

            // Notify the requester
            if (response.equals("yes")) {
                sendMessageToRequester(requesterChatId, "username: @" + adminUsername +"\n"+
                        "time to disturb 08:00|24:00");
            } else {
                sendMessageToRequester(requesterChatId, "Sorry, your request could not be processed in a timely manner. Please try again later.");
            }
        } catch (TelegramApiException e) {
            e.printStackTrace();
            // Log an error with more detailed context
            System.err.println("Failed to handle admin response. Chat ID: " + chatId + ", Message ID: " + messageId);
        }
    }

    private void sendMessageToRequester(long requesterChatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(requesterChatId));
        message.setText(text);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
    private void loadUsers() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream("users.dat"))) {
            botUsers = (List<Long>) ois.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private void saveUsers() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("users.dat"))) {
            oos.writeObject(botUsers);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
