package net.earthcomputer.currentaffairs;

import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.screen.narration.NarrationPart;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class CurrentAffairsScreen extends Screen {
    private final Screen parent;
    private final Text message;
    private static final Text WONT_SHOW_AGAIN = new TranslatableText("currentaffairs.screen.wontShowAgain")
            .styled(style -> style.withColor(Formatting.GRAY).withItalic(true));

    private List<OrderedText> wrappedMessage = Collections.emptyList();
    private List<OrderedText> wrappedWontShowAgain = Collections.emptyList();

    protected CurrentAffairsScreen(Screen parent, Text message) {
        super(new TranslatableText("currentaffairs.screen.title"));
        this.parent = parent;
        this.message = message;
    }

    @Override
    protected void init() {
        super.init();
        if (width > 20) {
            wrappedMessage = textRenderer.wrapLines(message, width - 20);
            wrappedWontShowAgain = textRenderer.wrapLines(WONT_SHOW_AGAIN, width - 20);
        }
        addDrawableChild(new ButtonWidget(width / 2 - 50, 50 + textRenderer.fontHeight * (wrappedMessage.size() + wrappedWontShowAgain.size()), 100, 20, new TranslatableText("gui.done"), button -> this.close()));
    }

    @Override
    public void close() {
        Objects.requireNonNull(this.client).setScreen(parent);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            Style hoveredStyle = getHoveredStyle((int) mouseX, (int) mouseY);
            if (hoveredStyle != null && handleTextClick(hoveredStyle)) {
                return true;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta) {
        renderBackground(matrices);
        super.render(matrices, mouseX, mouseY, delta);

        int y = 20;
        for (OrderedText text : wrappedMessage) {
            textRenderer.draw(matrices, text, (float)((width - textRenderer.getWidth(text)) / 2), y, 0xffffff);
            y += textRenderer.fontHeight;
        }
        y += 10;
        for (OrderedText text : wrappedWontShowAgain) {
            textRenderer.draw(matrices, text, (float)((width - textRenderer.getWidth(text)) / 2), y, 0xffffff);
            y += textRenderer.fontHeight;
        }

        Style hoveredStyle = getHoveredStyle(mouseX, mouseY);
        if (hoveredStyle != null) {
            renderTextHoverEffect(matrices, hoveredStyle, mouseX, mouseY);
        }
    }

    @Nullable
    private Style getHoveredStyle(int mouseX, int mouseY) {
        int messageHeight = wrappedMessage.size() * textRenderer.fontHeight;
        if (mouseY < 20 || mouseY >= 20 + messageHeight) {
            return null;
        }
        OrderedText line = wrappedMessage.get((mouseY - 20) / textRenderer.fontHeight);
        int lineWidth = textRenderer.getWidth(line);
        int lineX = (this.width - lineWidth) / 2;
        if (mouseX < lineX || mouseX >= lineX + lineWidth) {
            return null;
        }
        return textRenderer.getTextHandler().getStyleAt(line, mouseX - lineX);
    }

    @Override
    protected void addElementNarrations(NarrationMessageBuilder builder) {
        builder.nextMessage().put(NarrationPart.TITLE, message);
        builder.nextMessage().put(NarrationPart.TITLE, WONT_SHOW_AGAIN);
        super.addElementNarrations(builder);
    }
}
