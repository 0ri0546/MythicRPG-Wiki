package com.mythicrpg.titles;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class TitleTextFormatter {
    private static final Identifier UNIFORM_FONT = Identifier.ofVanilla("uniform");

    private TitleTextFormatter() {
    }

    public static MutableText format(
            String localizedTitle,
            TitleColor primary,
            TitleColor secondary,
            boolean gradient,
            TitleFinish finish,
            boolean compactBorderFallback
    ) {
        String displayed = finish == TitleFinish.UPPERCASE
                ? localizedTitle.toUpperCase(Locale.ROOT)
                : localizedTitle;

        List<String> glyphs = displayed.codePoints()
                .mapToObj(codePoint -> new String(Character.toChars(codePoint)))
                .toList();

        int coloredGlyphCount = (int) displayed.codePoints()
                .filter(codePoint -> !Character.isWhitespace(codePoint))
                .count();
        int coloredIndex = 0;
        int lastColor = primary.rgb();

        MutableText result = Text.empty();
        for (String glyph : glyphs) {
            int codePoint = glyph.codePointAt(0);
            if (!Character.isWhitespace(codePoint)) {
                float progress = coloredGlyphCount <= 1
                        ? 0.0F
                        : (float) coloredIndex / (float) (coloredGlyphCount - 1);
                lastColor = gradient
                        ? interpolate(primary.rgb(), secondary.rgb(), progress)
                        : primary.rgb();
                coloredIndex++;
            }

            Style style = Style.EMPTY.withColor(lastColor);
            if (finish == TitleFinish.BOLD || (finish == TitleFinish.BORDER && compactBorderFallback)) {
                style = style.withBold(true);
            } else if (finish == TitleFinish.ITALIC) {
                style = style.withItalic(true);
            } else if (finish == TitleFinish.UNIFORM_FONT) {
                style = style.withFont(UNIFORM_FONT);
            }

            result.append(Text.literal(glyph).setStyle(style));
        }

        return result;
    }

    public static MutableText prefix(
            String localizedTitle,
            TitleColor primary,
            TitleColor secondary,
            boolean gradient,
            TitleFinish finish
    ) {
        MutableText result = format(localizedTitle, primary, secondary, gradient, finish, true);
        // The trailing separator owns an empty style so title formatting never leaks
        // into the vanilla player name appended by the scoreboard team.
        result.append(Text.literal(" ").setStyle(Style.EMPTY));
        return result;
    }

    public static List<PaletteChoice> paletteChoices() {
        List<PaletteChoice> choices = new ArrayList<>();
        for (TitleColor color : TitleColor.values()) {
            choices.add(new PaletteChoice(color, color, false));
        }
        for (TitleColor left : TitleColor.values()) {
            for (TitleColor right : TitleColor.values()) {
                if (left != right) {
                    choices.add(new PaletteChoice(left, right, true));
                }
            }
        }
        return List.copyOf(choices);
    }

    private static int interpolate(int start, int end, float progress) {
        int startRed = (start >> 16) & 0xFF;
        int startGreen = (start >> 8) & 0xFF;
        int startBlue = start & 0xFF;
        int endRed = (end >> 16) & 0xFF;
        int endGreen = (end >> 8) & 0xFF;
        int endBlue = end & 0xFF;

        int red = Math.round(startRed + (endRed - startRed) * progress);
        int green = Math.round(startGreen + (endGreen - startGreen) * progress);
        int blue = Math.round(startBlue + (endBlue - startBlue) * progress);
        return (red << 16) | (green << 8) | blue;
    }

    public record PaletteChoice(TitleColor primary, TitleColor secondary, boolean gradient) {
    }
}
