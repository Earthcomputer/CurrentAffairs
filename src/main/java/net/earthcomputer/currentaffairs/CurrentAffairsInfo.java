package net.earthcomputer.currentaffairs;

import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

import java.util.Date;
import java.util.UUID;

public class CurrentAffairsInfo {
    public UUID uuid;
    public Text message;
    public String locale;
    public Date from;
    public Date expire;

    public CurrentAffairsInfo() {
    }

    public CurrentAffairsInfo(UUID uuid, Text message, @Nullable String locale, @Nullable Date from, @Nullable Date expire) {
        this.uuid = uuid;
        this.message = message;
        this.locale = locale;
        this.from = from;
        this.expire = expire;
    }
}
