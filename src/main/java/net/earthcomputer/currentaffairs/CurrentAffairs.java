package net.earthcomputer.currentaffairs;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.metadata.CustomValue;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class CurrentAffairs {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final Gson GSON = new GsonBuilder()
            .registerTypeAdapter(Text.class, (JsonDeserializer<Text>) (json, typeOfT, context) -> Text.Serializer.fromJson(json))
            .create();
    private static final Path configFile = FabricLoader.getInstance().getConfigDir().resolve("current-affairs").resolve("seen-messages.txt");

    private static boolean hasApplied = false;

    public static Screen apply(Screen oldScreen) {
        if (hasApplied) {
            return oldScreen;
        }
        hasApplied = true;

        List<URL> updateUrls = collectUpdateURLs();
        Set<UUID> seenMessages = getSeenMessages();
        CurrentAffairsInfo info = getCurrentAffairsInfo(updateUrls, seenMessages);
        if (info == null) {
            return oldScreen;
        }

        seenMessages = new HashSet<>(seenMessages); // ensure it's mutable
        seenMessages.add(info.uuid);
        saveSeenMessages(seenMessages);

        return new CurrentAffairsScreen(oldScreen, info.message);
    }

    private static List<URL> collectUpdateURLs() {
        List<URL> updateUrls = new ArrayList<>();

        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            CustomValue customValue = mod.getMetadata().getCustomValue("current-affairs");
            if (customValue != null && customValue.getType() == CustomValue.CvType.STRING) {
                URL url;
                try {
                    url = new URL(customValue.getAsString());
                } catch (MalformedURLException e) {
                    LOGGER.warn("Mod {} has invalid current-affairs URL: {}", mod.getMetadata().getId(), customValue.getAsString());
                    continue;
                }
                if (!"https".equals(url.getProtocol())) {
                    LOGGER.warn("Mod {} tried to add a current-affairs URL with an invalid protocol {}", mod.getMetadata().getId(), url.getProtocol());
                    continue;
                }
                updateUrls.add(url);
            }
        }

        return updateUrls;
    }

    private static Set<UUID> getSeenMessages() {
        try (BufferedReader reader = Files.newBufferedReader(configFile)) {
            return reader.lines().map(String::trim).filter(str -> !str.isEmpty()).map(UUID::fromString).collect(Collectors.toSet());
        } catch (NoSuchFileException e) {
            LOGGER.debug("Config file {} not found", configFile);
            return Collections.emptySet();
        } catch (IOException | IllegalArgumentException e) {
            // IllegalArgumentException is thrown by UUID.fromString for invalid UUIDs
            LOGGER.warn(() -> "Unable to read current affairs config file " + configFile, e);
            return Collections.emptySet();
        }
    }

    private static void saveSeenMessages(Set<UUID> seenMessages) {
        try {
            Files.createDirectories(configFile.getParent());
        } catch (IOException e) {
            LOGGER.warn("Could not save seen current affairs messages", e);
            return;
        }

        try (PrintWriter writer = new PrintWriter(Files.newBufferedWriter(configFile))) {
            for (UUID seenMessage : seenMessages) {
                writer.println(seenMessage);
            }
        } catch (IOException e) {
            LOGGER.warn("Could not save seen current affairs messages", e);
        }
    }

    @Nullable
    private static CurrentAffairsInfo getCurrentAffairsInfo(List<URL> updateUrls, Set<UUID> seenMessages) {
        for (URL url : updateUrls) {
            List<CurrentAffairsInfo> infos;
            try (Reader is = new InputStreamReader(url.openStream(), StandardCharsets.UTF_8)) {
                infos = GSON.fromJson(is, new TypeToken<List<CurrentAffairsInfo>>(){}.getType());
            } catch (UnknownHostException e) {
                LOGGER.warn("Unknown host: {}", url);
                continue;
            } catch (JsonParseException e) {
                LOGGER.warn("Invalid JSON: {}", e.toString());
                continue;
            } catch (IOException e) {
                LOGGER.warn(() -> "I/O error occurred reading from URL: " + url, e);
                continue;
            }

            if (infos == null) {
                LOGGER.warn("Read null from URL {}", url);
                continue;
            }

            for (CurrentAffairsInfo info : infos) {
                if (info.uuid == null) {
                    LOGGER.warn("Current affairs info has null uuid in {}", url);
                    continue;
                }
                if (info.message == null) {
                    LOGGER.warn("Current affairs info {} has null message in {}", info.uuid, url);
                    continue;
                }

                if (seenMessages.contains(info.uuid)) {
                    LOGGER.debug("Already seen message {}", info.uuid);
                    continue;
                }

                if (info.locale != null) {
                    Locale currentLocale = Locale.getDefault();
                    if (info.locale.contains("-")) {
                        String[] parts = info.locale.split("-", 2);
                        String language = new Locale(parts[0]).getLanguage();
                        if (!language.equals(currentLocale.getLanguage())) {
                            LOGGER.debug("Language {} doesn't match {} for current affairs info {} in {}", language, currentLocale.getLanguage(), info.uuid, url);
                            continue;
                        }
                        String country = new Locale(parts[0], parts[1]).getCountry();
                        if (!country.equals(currentLocale.getCountry())) {
                            LOGGER.debug("Country {} doesn't match {} for current affairs info {} in {}", country, currentLocale.getCountry(), info.uuid, url);
                            continue;
                        }
                    } else if (!info.locale.equalsIgnoreCase(currentLocale.getCountry())) {
                        LOGGER.debug("Country {} doesn't match {} for current affairs info {} in {}", info.locale, currentLocale.getCountry(), info.uuid, url);
                        continue;
                    }
                }

                if (info.from != null || info.expire != null) {
                    Date now = new Date();
                    if (info.from != null && info.from.after(now)) {
                        LOGGER.debug("Too early to display this message ({} < {}) for current affairs info {} in {}", now, info.from, info.uuid, url);
                        continue;
                    }
                    if (info.expire != null && info.expire.before(now)) {
                        LOGGER.debug("Too late to display this message ({} > {}) for current affairs info {} in {}", now, info.expire, info.uuid, url);
                        continue;
                    }
                }

                return info;
            }
        }

        return null;
    }
}
