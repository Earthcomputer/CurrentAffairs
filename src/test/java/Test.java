import net.earthcomputer.currentaffairs.CurrentAffairs;
import net.earthcomputer.currentaffairs.CurrentAffairsInfo;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.util.Formatting;

import java.util.UUID;

public class Test {
    public static void main(String[] args) {
        System.out.println(CurrentAffairs.toJson(new CurrentAffairsInfo(
                UUID.randomUUID(),
                new LiteralText("Help the Ukrainians ").append(new LiteralText("here").styled(style -> style.withUnderline(true).withColor(Formatting.BLUE).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, "https://donate.redcross.org.uk/appeal/ukraine-crisis-appeal")))),
                null,
                null,
                null
        )));
    }
}
