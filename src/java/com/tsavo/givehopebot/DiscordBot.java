package com.tsavo.givehopebot;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.util.concurrent.RateLimiter;
import de.btobastian.javacord.entities.message.embed.EmbedBuilder;
import de.btobastian.sdcf4j.CommandExecutor;
import de.btobastian.sdcf4j.CommandHandler;
import de.btobastian.sdcf4j.handler.JavacordHandler;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.bittrex.v1.BittrexExchange;
import org.knowm.xchange.currency.CurrencyPair;
import org.knowm.xchange.dto.marketdata.OrderBook;
import org.knowm.xchange.dto.trade.LimitOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Created by evilg on 9/19/2017.
 */
@Service
public class DiscordBot implements CommandExecutor {
    public static final String NOT_REGISTERED_MESSAGE = "You are not registered yet. Use the !register command to register yourself.";
    public static final String NO_CREDENTIALS_MESSAGE = "You don't have any exchange credentials entered. Check the !help, and use the !credentials command to add some.";
    public static final String DMD_INFO_URL = "https://chainz.cryptoid.info/explorer/address.summary.dws?coin=dmd&id=12829&r=25294&fmt.js";
    public static final String DMD_EXPLORER_URL = "https://chainz.cryptoid.info/dmd/address.dws?dH4bKCoyNj9BzLuyU4JvhwvhYs7cnogDVb.htm";
    public static final String DMD_IMG_URL = "https://chainz.cryptoid.info/logo/dmd.png";
    static RateLimiter rateLimiter = RateLimiter.create(1);
    @Autowired
    public de.btobastian.javacord.DiscordAPI api;
    ExecutorService executor = Executors.newFixedThreadPool(5);

    CommandHandler handler;

    @Autowired
    GiveHopeCommand giveHopeCommand;


    HashMap<String, JsonNode> txs = new HashMap<>();

    Exchange exchange;
    @PostConstruct
    public void init() throws IOException, URISyntaxException {
        handler = new JavacordHandler(api);
        handler.registerCommand(giveHopeCommand);
        String url = DMD_INFO_URL;
        URI uri = new URI(url);
        HttpGet httpget = new HttpGet(uri);

        HttpClient httpclient = new DefaultHttpClient();

        HttpResponse response = httpclient.execute(httpget);
        // check response headers.
        String reasonPhrase = response.getStatusLine().getReasonPhrase();
        int statusCode = response.getStatusLine().getStatusCode();


        HttpEntity entity = response.getEntity();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(entity.getContent());
        node.get("tx").forEach(tx -> {
            if(!txs.containsKey(tx.get(1).toString())){
                txs.put(tx.get(1).toString(), tx);
            }
        });

        exchange = new BittrexExchange();
        ExchangeSpecification exchangeSpecification = exchange.getDefaultExchangeSpecification();
        exchange.applySpecification(exchangeSpecification);
    }


    @Scheduled(fixedDelay = 30000)
    public void stake() throws IOException, URISyntaxException {
        String url = DMD_INFO_URL;
        URI uri = new URI(url);
        HttpGet httpget = new HttpGet(uri);

        HttpClient httpclient = new DefaultHttpClient();

        HttpResponse response = httpclient.execute(httpget);
        // check response headers.
        String reasonPhrase = response.getStatusLine().getReasonPhrase();
        int statusCode = response.getStatusLine().getStatusCode();

        OrderBook orderBook = exchange.getMarketDataService().getOrderBook(new CurrencyPair("DMD", "BTC"));
        BigDecimal askPrice = orderBook.getAsks().stream()
                .sorted((x, y) -> x.getLimitPrice().compareTo(y.getLimitPrice())).findFirst().get().getLimitPrice();
        BigDecimal bidPrice = orderBook.getBids().stream()
                .sorted((x, y) -> y.getLimitPrice().compareTo(x.getLimitPrice())).findFirst().get().getLimitPrice();

        BigDecimal price = askPrice.add(bidPrice).divide(new BigDecimal(2), 8, BigDecimal.ROUND_HALF_EVEN).setScale(8, BigDecimal.ROUND_HALF_EVEN);

        HttpEntity entity = response.getEntity();

        ObjectMapper mapper = new ObjectMapper();
        JsonNode node = mapper.readTree(entity.getContent());
        node.get("tx").forEach(tx -> {
            if(!txs.containsKey(tx.get(1).toString())){
                BigDecimal amount = new BigDecimal(tx.get(4).toString());
                BigDecimal balance = new BigDecimal(tx.get(5).toString());
                BigDecimal value = price.multiply(balance).setScale(8, BigDecimal.ROUND_HALF_EVEN);
                broadcast("", new EmbedBuilder().setTitle("The HOPE Diamond Staking Pool just staked a new DMD Diamond!").setUrl("https://chainz.cryptoid.info/dmd/address.dws?dH4bKCoyNj9BzLuyU4JvhwvhYs7cnogDVb.htm").setImage("https://chainz.cryptoid.info/logo/dmd.png").setColor(Color.GREEN).addField("Stake Reward" , amount.setScale(8, BigDecimal.ROUND_HALF_EVEN).toPlainString(), false).addField("Balance", new DecimalFormat("###,###.########").format(balance) + " DMD", true).addField("Price", price.toPlainString() + " BTC", true).addField("Value", value.toPlainString(), true));
                txs.put(tx.get(1).toString(), tx);
            }
        });
    }

    public BigDecimal getPrice() throws IOException {
        OrderBook orderBook = exchange.getMarketDataService().getOrderBook(new CurrencyPair("DMD", "BTC"));
        BigDecimal askPrice = orderBook.getAsks().stream()
                .sorted((x, y) -> x.getLimitPrice().compareTo(y.getLimitPrice())).findFirst().get().getLimitPrice();
        BigDecimal bidPrice = orderBook.getBids().stream()
                .sorted((x, y) -> y.getLimitPrice().compareTo(x.getLimitPrice())).findFirst().get().getLimitPrice();

        BigDecimal price = askPrice.add(bidPrice).divide(new BigDecimal(2), 8, BigDecimal.ROUND_HALF_EVEN).setScale(8, BigDecimal.ROUND_HALF_EVEN);
    return price;
    }

    public void broadcast(String message, EmbedBuilder embedBuilder){
        api.getChannels().forEach(channel -> {channel.sendMessage(message, embedBuilder); rateLimiter.acquire();});
    }


}
