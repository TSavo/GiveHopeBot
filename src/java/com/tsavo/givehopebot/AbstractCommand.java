package com.tsavo.givehopebot;

import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.Server;
import de.btobastian.javacord.utils.ratelimits.RateLimitType;
import de.btobastian.sdcf4j.CommandExecutor;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Created by evilg on 10/1/2017.
 */
public abstract class AbstractCommand implements CommandExecutor {


    @Autowired
    DiscordAPI api;

    public void sendMessage(Server server, Channel channel, String message) {
        sendMessage(server, channel, message, 0);
    }

    public void sendMessage(Server server, Channel channel, String message, int retry) {

        try {
            synchronized (this) {
                while (api.getRateLimitManager().isRateLimited(RateLimitType.SERVER_MESSAGE, server, channel) ||
                        api.getRateLimitManager().isRateLimited(RateLimitType.SERVER_MESSAGE) ||
                        api.getRateLimitManager().isRateLimited(RateLimitType.SERVER_MESSAGE, server)) {
                    Thread.sleep(1000);
                }
                channel.sendMessage(message).get();
            }
        } catch (Exception e) {
            if (retry > 5) {
                e.printStackTrace();
                return;
            }
            try {
                Thread.sleep(500);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            sendMessage(server, channel, message, retry + 1);
        }
    }

    public void sendMessage(String serverId, String channelId, String message) {
        Server server = api.getServerById(serverId);
        Channel channel =  server.getChannelById(channelId);
        sendMessage(server, channel, message);
    }


}
