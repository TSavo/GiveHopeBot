package com.tsavo.givehopebot;

import de.btobastian.javacord.ImplDiscordAPI;
import de.btobastian.javacord.utils.ThreadPool;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

/**
 * Created by evilg on 10/12/2017.
 */
@Service
public class DiscordAPI extends ImplDiscordAPI{


    public DiscordAPI() {
        super(new ThreadPool());

    }

    @PostConstruct
    public void init(){

        setToken(System.getenv("DISCORD_TOKEN"), true);
        setAutoReconnect(true);
        connectBlocking();
    }
}
