package DiscordApiStuff;

import Admin.Database;
import Admin.User;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.TextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.MessageAuthor;
import org.javacord.api.entity.server.Server;

import java.sql.SQLException;
import java.util.concurrent.atomic.AtomicReference;

public class HandleMessages {
    private DiscordApi discordApi;

    public HandleMessages(DiscordApi discordApi) {
        this.discordApi = discordApi;
    }

    public void startHandlingMessages() {
        listenForMessageEdit();
        listenForMessageDelete();
    }

    private void listenForMessageDelete() {
        discordApi.addMessageDeleteListener(messageDelete -> {
            try {
                long serverId = messageDelete.getServer().get().getId();
                long messageId = messageDelete.getMessageId();
                // connect to database for this guild
                Database db = new Database(serverId, User.BOT);
                db.delete.message(messageId);
                db.closeConnection();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void listenForMessageEdit() {
        discordApi.addMessageEditListener(messageEdit -> {
            try {
                long serverId = messageEdit.getServer().get().getId();
                long messageId = messageEdit.getMessageId();
                long messageTimestamp = messageEdit.getMessage().get().getLastEditTimestamp().get().getEpochSecond();
                // convert long timestamp to string datetime
                String content = messageEdit.getNewContent();
                // connect to database for this guild
                Database db = new Database(serverId, User.BOT);
                db.update.message(messageId, content, messageTimestamp);
                db.closeConnection();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static void insertMessage(long serverId, Message message, Server server) throws SQLException {
        // connect to database for this guild
        Database db = new Database(serverId, User.BOT);

        // first create the channel
        long channelDatabaseDiscordId = insertChannel(message.getChannel(), db);
        // then create the author
        long authorDatabaseDiscordId = insertAuthor(message.getAuthor(), db, server);
        long messageId = message.getId();
        String content = message.getContent();

        // insert the message
        db.create.message(messageId, authorDatabaseDiscordId, channelDatabaseDiscordId, content);
        db.closeConnection();
    }

    private static long insertChannel(TextChannel channel, Database db) throws SQLException {
        long channelId = channel.getId();
        String channelName = channel.asServerTextChannel().get().getName();
        db.create.channel(channelId, channelName);
        return channelId;
    }

    private static long insertAuthor(MessageAuthor author, Database db, Server server) throws SQLException {
        long authorId = author.getId();
        AtomicReference<String> authorName = new AtomicReference<>(author.getDisplayName());
        author.asUser().ifPresent(user -> {
            if(user.getNickname(server).isPresent()) {
                authorName.set(user.getNickname(server).get());
            }
        });
        db.create.author(authorId, authorName.get());
        return authorId;
    }
}
