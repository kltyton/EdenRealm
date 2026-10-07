package com.kltyton.eden_realm.client.world;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

/** Serves the packaged terrain workbench assets to its local WebView2 page. */
public final class TerrainWorkbenchServer implements AutoCloseable {
    private static final String ASSET_PATH_PREFIX = "kltytonui";
    private static final String RUNTIME_PATH_PREFIX = "kltytonui/kltytonui/runtime/";
    private static final String WORKBENCH_PATH_PREFIX = "kltytonui/eden_realm/";
    private static final int COMMAND_CAPACITY = 64;

    private final HttpServer server;
    private final ExecutorService requests;
    private final Map<String, byte[]> assets;
    private final ArrayBlockingQueue<String> commands = new ArrayBlockingQueue<>(COMMAND_CAPACITY);
    private volatile byte[] state = new byte[0];
    private boolean closed;

    public TerrainWorkbenchServer(Minecraft game) throws IOException {
        ResourceManager resources = game.getResourceManager();
        Map<String, byte[]> loadedAssets = new HashMap<>();
        for (Map.Entry<Identifier, Resource> entry : resources.listResources("kltytonui", id ->
                "kltytonui".equals(id.getNamespace())
                        && (id.getPath().startsWith(RUNTIME_PATH_PREFIX)
                        || id.getPath().startsWith(WORKBENCH_PATH_PREFIX))).entrySet()) {
            String urlPath = entry.getKey().getPath().substring(ASSET_PATH_PREFIX.length());
            try (InputStream input = entry.getValue().open()) {
                loadedAssets.put(urlPath, input.readAllBytes());
            }
        }
        for (String font : new String[]{"minecraft_ten.otf", "minecraft_seven.otf",
                "minecraft_five.otf", "minecraft_five_bold.otf"}) {
            String path = "/kltytonui/runtime/mcui/fonts/" + font;
            if (!loadedAssets.containsKey(path)) throw new IOException("Missing workbench font: " + path);
        }
        assets = Map.copyOf(loadedAssets);

        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        requests = Executors.newVirtualThreadPerTaskExecutor();
        server.setExecutor(requests);
        server.createContext("/", this::handle);
        server.start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort() + "/eden_realm/terrain.html";
    }

    public void publish(String json) {
        state = json.getBytes(StandardCharsets.UTF_8);
    }

    public String pollCommands() {
        return commands.poll();
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            exchange.getResponseHeaders().set("Cache-Control", "no-store");
            String origin = exchange.getRequestHeaders().getFirst("Origin");
            if (origin != null && !origin.equals(origin())) {
                respond(exchange, 403, "text/plain; charset=utf-8", "forbidden".getBytes(StandardCharsets.UTF_8));
                return;
            }

            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if ("/state.json".equals(path)) {
                if (!"GET".equals(method)) {
                    respond(exchange, 405, "text/plain; charset=utf-8", "method not allowed".getBytes(StandardCharsets.UTF_8));
                    return;
                }
                byte[] current = state;
                if (current.length == 0) {
                    respond(exchange, 204, null, null);
                } else {
                    respond(exchange, 200, "application/json; charset=utf-8", current);
                }
                return;
            }

            if ("/commands".equals(path)) {
                if (!"POST".equals(method)) {
                    respond(exchange, 405, "text/plain; charset=utf-8", "method not allowed".getBytes(StandardCharsets.UTF_8));
                    return;
                }
                String command;
                try (InputStream input = exchange.getRequestBody()) {
                    command = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                }
                if (!commands.offer(command)) {
                    respond(exchange, 429, "text/plain; charset=utf-8", "command queue full".getBytes(StandardCharsets.UTF_8));
                } else {
                    respond(exchange, 204, null, null);
                }
                return;
            }

            if ("GET".equals(method)) {
                byte[] asset = assets.get(path);
                if (asset == null) {
                    respond(exchange, 404, "text/plain; charset=utf-8", "not found".getBytes(StandardCharsets.UTF_8));
                } else {
                    respond(exchange, 200, contentType(path), asset);
                }
                return;
            }

            respond(exchange, 405, "text/plain; charset=utf-8", "method not allowed".getBytes(StandardCharsets.UTF_8));
        } finally {
            exchange.close();
        }
    }

    private String origin() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    private static String contentType(String path) {
        int extensionAt = path.lastIndexOf('.');
        String extension = extensionAt < 0 ? "" : path.substring(extensionAt + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "js" -> "text/javascript; charset=utf-8";
            case "css" -> "text/css; charset=utf-8";
            case "html" -> "text/html; charset=utf-8";
            case "otf" -> "font/otf";
            case "ttf" -> "font/ttf";
            case "woff" -> "font/woff";
            case "woff2" -> "font/woff2";
            case "svg" -> "image/svg+xml";
            case "png" -> "image/png";
            case "txt" -> "text/plain; charset=utf-8";
            default -> "application/octet-stream";
        };
    }

    private static void respond(HttpExchange exchange, int status, String contentType, byte[] body) throws IOException {
        if (contentType != null) exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.sendResponseHeaders(status, body == null ? -1 : body.length);
        if (body != null) {
            try (OutputStream output = exchange.getResponseBody()) {
                output.write(body);
            }
        }
    }

    @Override
    public synchronized void close() {
        if (closed) return;
        closed = true;
        try {
            server.stop(0);
        } finally {
            requests.shutdownNow();
        }
    }
}
