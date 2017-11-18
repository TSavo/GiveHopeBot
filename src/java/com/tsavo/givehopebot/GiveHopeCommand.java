package com.tsavo.givehopebot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.btobastian.javacord.entities.Channel;
import de.btobastian.javacord.entities.message.embed.EmbedBuilder;
import de.btobastian.sdcf4j.Command;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;

/**
 * Created by evilg on 10/12/2017.
 */
@Service
public class GiveHopeCommand extends AbstractCommand {

    @Autowired
    DiscordBot discordBot;

    @Command(aliases = {"givehope", "hope", "gethope", "balance", "stake", "pool", "status"}, description = "Gives HOPE to everyone.", usage = "#givehope")
    public void addPivot(Channel channel) throws IOException, URISyntaxException {
        //create a singular HttpClient object
        String url = DiscordBot.DMD_INFO_URL;
        URI uri = new URI(url);
        HttpGet httpget = new HttpGet(uri);

        HttpClient httpclient = HttpClientBuilder.create().build();

        HttpResponse response = httpclient.execute(httpget);
        // check response headers.
        String reasonPhrase = response.getStatusLine().getReasonPhrase();
        int statusCode = response.getStatusLine().getStatusCode();


        HttpEntity entity = response.getEntity();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(entity.getContent());
        String balance = node.get("balance").toString();
        String firstHalf = balance.substring(0, balance.length()-8);
        String secondHalf = balance.substring(balance.length()-8);
        BigDecimal price = discordBot.getPrice();
        BigDecimal value = price.multiply(new BigDecimal(firstHalf + "." + secondHalf)).setScale(8, BigDecimal.ROUND_HALF_EVEN);
        channel.sendMessage("", new EmbedBuilder().setTitle("The HOPE Diamond Staking Pool").setUrl(DiscordBot.DMD_EXPLORER_URL).setImage(DiscordBot.DMD_IMG_URL).setColor(Color.BLUE).addField("Balance", new DecimalFormat("###,###").format(Integer.parseInt(firstHalf)) + "." + secondHalf + " DMD", true).addField("Price", discordBot.getPrice().toPlainString() + " BTC", false).addField("Value", value.toPlainString(), true));
    }
}
