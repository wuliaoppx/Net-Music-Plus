package com.wuliaoppx.netmusicplus.client;

import com.github.tartaricacid.netmusic.NetMusic;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wuliaoppx.netmusicplus.NetMusicPlus;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fetches and parses NetEase lyrics for a song id, reusing NetMusic's own web API client so the cookies and the
 * endpoints stay exactly what NetMusic already talks to.
 * <p>
 * The request happens on the vanilla background executor and the result is cached per song id; rendering only ever
 * reads the cache, so a slow network can never stall a frame.
 */
public final class Mp3Lyrics {
    /** {@code [mm:ss.xx]text}, the usual LRC line. */
    private static final Pattern LRC = Pattern.compile("\\[(\\d+):(\\d+)[.:](\\d+)\\](.*)");
    /** {@code https://music.163.com/song/media/outer/url?id=123.mp3} - the same pattern NetMusic matches. */
    private static final Pattern SONG_ID = Pattern.compile("^.*?\\?id=(\\d+)\\.mp3$");

    private static final Map<Long, CompletableFuture<List<Line>>> CACHE = new ConcurrentHashMap<>();

    /** One lyric line: when it starts (in milliseconds) plus the original and the translated text. */
    public record Line(int millis, String text, String translation) {
    }

    private Mp3Lyrics() {
    }

    /** The NetEase song id inside a streaming url, or {@code -1} when the url is not a NetEase one. */
    public static long songId(String url) {
        if (url == null || !url.startsWith("https://music.163.com/")) {
            return -1L;
        }
        Matcher matcher = SONG_ID.matcher(url);
        if (!matcher.find()) {
            return -1L;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException exception) {
            return -1L;
        }
    }

    /** Starts loading the lyrics for a song id (no-op when they are already loaded or loading). */
    public static void prefetch(long songId) {
        if (songId < 0) {
            return;
        }
        CACHE.computeIfAbsent(songId, id -> CompletableFuture.supplyAsync(() -> fetch(id)));
    }

    /** The lyrics of a song id, or {@code null} while they are still loading (or unavailable). */
    public static List<Line> lines(long songId) {
        CompletableFuture<List<Line>> future = CACHE.get(songId);
        if (future == null || !future.isDone() || future.isCompletedExceptionally()) {
            return null;
        }
        return future.getNow(List.of());
    }

    private static List<Line> fetch(long songId) {
        try {
            String json = NetMusic.NET_EASE_WEB_API.lyric(songId);
            return parse(json);
        } catch (Exception exception) {
            NetMusicPlus.LOGGER.warn("Cannot load the lyrics of song {}", songId, exception);
            return List.of();
        }
    }

    /** Parses the JSON returned by the NetEase lyric endpoint; accepts both the {@code lrc} and {@code original} keys. */
    private static List<Line> parse(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        JsonObject root;
        try {
            JsonElement element = JsonParser.parseString(json);
            if (!element.isJsonObject()) {
                return List.of();
            }
            root = element.getAsJsonObject();
        } catch (Exception exception) {
            return List.of();
        }
        Map<Integer, String> original = lrc(root, "lrc", "original");
        Map<Integer, String> translation = lrc(root, "tlyric", "translation");
        List<Line> lines = new ArrayList<>(original.size());
        original.forEach((millis, text) -> lines.add(new Line(millis, text, translation.getOrDefault(millis, ""))));
        lines.sort((a, b) -> Integer.compare(a.millis(), b.millis()));
        return List.copyOf(lines);
    }

    private static Map<Integer, String> lrc(JsonObject root, String first, String second) {
        JsonObject holder = object(root, first);
        if (holder == null) {
            holder = object(root, second);
        }
        Map<Integer, String> result = new HashMap<>();
        if (holder == null) {
            return result;
        }
        JsonElement lyric = holder.get("lyric");
        if (lyric == null || !lyric.isJsonPrimitive()) {
            return result;
        }
        for (String raw : lyric.getAsString().split("\n")) {
            Matcher matcher = LRC.matcher(raw.trim());
            if (!matcher.find()) {
                continue;
            }
            String text = matcher.group(4).trim();
            if (text.isEmpty()) {
                continue;
            }
            int minutes = Integer.parseInt(matcher.group(1));
            int seconds = Integer.parseInt(matcher.group(2));
            String fraction = matcher.group(3);
            int millis = minutes * 60_000 + seconds * 1_000 + fractionValue(fraction);
            result.put(millis, text);
        }
        return result;
    }

    /** {@code "5"} means 500 ms, {@code "50"} means 500 ms as well; LRC uses hundredths but some files use millis. */
    private static int fractionValue(String fraction) {
        int value = Integer.parseInt(fraction);
        return fraction.length() >= 3 ? value : value * 10;
    }

    private static JsonObject object(JsonObject root, String key) {
        JsonElement element = root.get(key);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }
}
