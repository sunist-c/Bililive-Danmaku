package cn.sunist.project.bililivedanma.service;

import cn.sunist.project.bililivedanma.model.*;
import cn.sunist.project.bililivedanma.ui.Danmaku;
import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;

import static java.nio.charset.StandardCharsets.UTF_8;

public class Server {
    private HttpServer httpServer;
    private Danmaku danmaku;

    public boolean Started() {
        return httpServer != null;
    }

    private void giftHandler(@NotNull HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        // Method Filter
        if (method.equals("POST")) {
            InputStream is = exchange.getRequestBody();
            byte[] bytes = is.readAllBytes();
            String message = new String(bytes, UTF_8);
            is.close();

            Gson gson = new Gson();
            GiftMessage giftMessage = gson.fromJson(message, GiftMessage.class);

            // Bad Json Format
            if (StringUtils.isBlank(giftMessage.gift_name) || StringUtils.isBlank(giftMessage.u_uname) || StringUtils.isBlank(giftMessage.action)) {
                exchange.sendResponseHeaders(400, 0);
                exchange.getResponseBody().close();
                return;
            }

            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
            danmaku.SendGiftMessage(giftMessage.u_uname, giftMessage.gift_name, giftMessage.action, giftMessage.price, giftMessage.number);
        }

        // Method Not Allowed
        else {
            exchange.sendResponseHeaders(405, 0);
            exchange.getResponseBody().close();
        }
    }

    private void dmHandler(@NotNull HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        // Method Filter
        if (method.equals("POST")) {
            InputStream is = exchange.getRequestBody();
            byte[] bytes = is.readAllBytes();
            String message = new String(bytes, UTF_8);
            is.close();

            Gson gson = new Gson();
            DanmuMessage danmuMessage = gson.fromJson(message, DanmuMessage.class);

            // Bad Json Format
            if (StringUtils.isBlank(danmuMessage.uname) || StringUtils.isBlank(danmuMessage.text)) {
                exchange.sendResponseHeaders(400, 0);
                exchange.getResponseBody().close();
                return;
            }

            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
            danmaku.SendDanmaMessage(danmuMessage.uname, danmuMessage.text);
        }

        // Method Not Allowed
        else {
            exchange.sendResponseHeaders(405, 0);
            exchange.getResponseBody().close();
        }
    }

    private void customMessageHandler(@NotNull HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        // Method Filter
        if (method.equals("POST")) {
            InputStream is = exchange.getRequestBody();
            byte[] bytes = is.readAllBytes();
            String message = new String(bytes, UTF_8);
            is.close();

            Gson gson = new Gson();
            CustomMessage customMessage = gson.fromJson(message, CustomMessage.class);

            // Bad Json Format
            if (StringUtils.isBlank(customMessage.message)) {
                exchange.sendResponseHeaders(400, 0);
                exchange.getResponseBody().close();
                return;
            }

            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
            danmaku.SendCustomMessage(customMessage.message);
        }

        // Method Not Allowed
        else {
            exchange.sendResponseHeaders(405, 0);
            exchange.getResponseBody().close();
        }
    }

    private void welcomeHandler(@NotNull HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();

        // Method Filter
        if (method.equals("POST")) {
            InputStream is = exchange.getRequestBody();
            byte[] bytes = is.readAllBytes();
            String message = new String(bytes, UTF_8);
            is.close();

            Gson gson = new Gson();
            WelcomeMessage welcomeMessage = gson.fromJson(message, WelcomeMessage.class);

            // Bad Json Format
            if (StringUtils.isBlank(welcomeMessage.user_name) || StringUtils.isBlank(welcomeMessage.title)) {
                exchange.sendResponseHeaders(400, 0);
                exchange.getResponseBody().close();
                return;
            }

            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().close();
            danmaku.SendAudienceCome(welcomeMessage.user_name);
        }

        // Method Not Allowed
        else {
            exchange.sendResponseHeaders(405, 0);
            exchange.getResponseBody().close();
        }
    }

    private void bindHandlers() {
        httpServer.createContext("/gift", this::giftHandler);
        httpServer.createContext("/dm", this::dmHandler);
        httpServer.createContext("/custom_message", this::customMessageHandler);
        httpServer.createContext("/welcome", this::welcomeHandler);
    }

    public Server(int port, Danmaku danmaku) {
        this.danmaku = danmaku;

        try {
            httpServer = HttpServer.create(new InetSocketAddress(port), 0);
        }
        catch (Exception e) {
            httpServer = null;
            System.out.println(e.getMessage());
        }

        bindHandlers();

        System.out.println("success start server");
    }

    public boolean Start() {
        if (httpServer != null) {
            httpServer.start();
            return true;
        } else {
            return false;
        }
    }

    public void Close() {
        if (httpServer != null) {
            httpServer.stop(0);
        }
    }
}
