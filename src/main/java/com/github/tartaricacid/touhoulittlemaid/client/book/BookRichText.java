package com.github.tartaricacid.touhoulittlemaid.client.book;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 书的富文本：把 Patchouli 的 <code>$(...)</code> 宏解析成「带样式的行」，负责折行、绘制，
 * 并顺带收集可点击区域交给 BookScreen 处理。
 * <p>
 * 本书正文里实际出现的宏只有 {@code $(br2) $(br) $(li) $() $(#RRGGBB) $(l:目标) $(/l)} 以及旧版
 * 颜色/格式码 {@code $(4) $(5) $(l)}，这里按 Patchouli 的完整语法实现，以后换文本不必再动代码。
 * 注意：中文没有空格，折行必须逐字处理；ASCII 单词则整体不拆。
 */
public final class BookRichText {
    public static final int LINE_HEIGHT = 10;
    private static final int MAX_CACHE = 256;
    private static final Map<String, BookRichText> CACHE = new HashMap<>();

    /** 一段统一样式的文字；linkEntry / linkUrl 至多一个非空表示可点击。 */
    private record Span(String text, int color, boolean bold, boolean underline, boolean italic, boolean strike,
                        String linkEntry, String linkUrl) {
    }

    /** 一行：若干 span + 缩进。 */
    private record Line(List<Span> spans, int indent) {
    }

    /** 渲染时收集的可点击矩形（屏幕坐标）。 */
    public record ClickRegion(int x1, int y1, int x2, int y2, String entry, String url) {
        public boolean contains(double mx, double my) {
            return mx >= this.x1 && mx < this.x2 && my >= this.y1 && my < this.y2;
        }
    }

    private final List<Line> lines;

    private BookRichText(List<Line> lines) {
        this.lines = lines;
    }

    public int height() {
        return this.lines.size() * LINE_HEIGHT;
    }

    /** 解析并缓存（同一段文本 + 同一宽度只算一次）。 */
    public static BookRichText of(String raw, int defaultColor, Font font, int maxWidth) {
        String key = maxWidth + "|" + defaultColor + "|" + raw;
        BookRichText cached = CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        BookRichText result = new Builder(raw, defaultColor, font, Math.max(8, maxWidth)).build();
        if (CACHE.size() > MAX_CACHE) {
            CACHE.clear();
        }
        CACHE.put(key, result);
        return result;
    }

    public void render(GuiGraphicsExtractor graphics, Font font, int x, int y, List<ClickRegion> clicks) {
        int cy = y;
        for (Line line : this.lines) {
            int cx = x + line.indent();
            for (Span span : line.spans()) {
                String text = span.text();
                if (text.isEmpty()) {
                    continue;
                }
                MutableComponent component = Component.literal(text).withStyle(style -> style
                        .withColor(span.color())
                        .withBold(span.bold())
                        .withUnderlined(span.underline())
                        .withItalic(span.italic())
                        .withStrikethrough(span.strike()));
                graphics.text(font, component, cx, cy, span.color(), false);
                int w = font.width(text);
                if (clicks != null && (span.linkEntry() != null || span.linkUrl() != null)) {
                    clicks.add(new ClickRegion(cx, cy, cx + w, cy + 9, span.linkEntry(), span.linkUrl()));
                }
                cx += w;
            }
            cy += LINE_HEIGHT;
        }
    }

    /** 解析 + 排版。 */
    private static final class Builder {
        private final List<Line> lines = new ArrayList<>();
        private final List<Span> line = new ArrayList<>();
        private final String raw;
        private final Font font;
        private final int maxWidth;
        private final int defaultColor;

        private int lineWidth;
        private int indent;
        private int color;
        private boolean bold;
        private boolean underline;
        private boolean italic;
        private boolean strike;
        private String linkEntry;
        private String linkUrl;

        private Builder(String raw, int defaultColor, Font font, int maxWidth) {
            this.raw = raw;
            this.font = font;
            this.maxWidth = maxWidth;
            this.defaultColor = defaultColor;
            this.color = defaultColor;
        }

        private BookRichText build() {
            int i = 0;
            int n = this.raw.length();
            while (i < n) {
                int next = this.raw.indexOf("$(", i);
                if (next < 0) {
                    this.addText(this.raw.substring(i));
                    break;
                }
                if (next > i) {
                    this.addText(this.raw.substring(i, next));
                }
                int end = this.raw.indexOf(')', next + 2);
                if (end < 0) {
                    this.addText(this.raw.substring(next));
                    break;
                }
                this.macro(this.raw.substring(next + 2, end));
                i = end + 1;
            }
            if (!this.line.isEmpty() || this.lines.isEmpty()) {
                this.endLine();
            }
            return new BookRichText(List.copyOf(this.lines));
        }

        private void endLine() {
            this.lines.add(new Line(List.copyOf(this.line), this.indent));
            this.line.clear();
            this.lineWidth = 0;
            this.indent = 0;
        }

        private void addText(String text) {
            for (String atom : splitAtoms(text)) {
                this.addAtom(atom);
            }
        }

        private void addAtom(String atom) {
            int w = this.font.width(atom);
            if (this.lineWidth > 0 && this.lineWidth + w > this.maxWidth) {
                this.endLine();
            }
            if (this.line.isEmpty() && atom.isBlank()) {
                return;
            }
            this.line.add(new Span(atom, this.color, this.bold, this.underline, this.italic, this.strike,
                    this.linkEntry, this.linkUrl));
            this.lineWidth += w;
        }

        private void resetStyle() {
            this.color = this.defaultColor;
            this.bold = false;
            this.underline = false;
            this.italic = false;
            this.strike = false;
            this.linkEntry = null;
            this.linkUrl = null;
        }

        private void macro(String macro) {
            if (macro.isEmpty() || macro.equals("r")) {
                this.resetStyle();
            } else if (macro.equals("br")) {
                this.endLine();
            } else if (macro.equals("br2")) {
                this.endLine();
                this.endLine();
            } else if (macro.equals("li")) {
                if (!this.line.isEmpty()) {
                    this.endLine();
                }
                this.addAtom("•");
                this.addAtom(" ");
            } else if (macro.startsWith("#")) {
                int rgb = parseHex(macro.substring(1));
                if (rgb >= 0) {
                    this.color = 0xFF000000 | rgb;
                }
            } else if (macro.startsWith("l:")) {
                String target = macro.substring(2).trim();
                if (target.startsWith("http://") || target.startsWith("https://")) {
                    this.linkUrl = target;
                } else {
                    this.linkEntry = target;
                }
            } else if (macro.equals("/l")) {
                this.linkEntry = null;
                this.linkUrl = null;
            } else if (macro.equals("l")) {
                this.bold = true;
            } else if (macro.equals("n")) {
                this.underline = true;
            } else if (macro.equals("o")) {
                this.italic = true;
            } else if (macro.equals("m")) {
                this.strike = true;
            } else if (macro.length() == 1) {
                int rgb = legacyColor(macro.charAt(0));
                if (rgb >= 0) {
                    this.color = 0xFF000000 | rgb;
                }
            }
        }
    }

    private static List<String> splitAtoms(String text) {
        List<String> atoms = new ArrayList<>();
        StringBuilder word = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int cp = text.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == ' ') {
                flushWord(atoms, word);
                atoms.add(" ");
            } else if (isWordChar(cp)) {
                word.appendCodePoint(cp);
            } else {
                flushWord(atoms, word);
                atoms.add(new String(Character.toChars(cp)));
            }
        }
        flushWord(atoms, word);
        return atoms;
    }

    private static void flushWord(List<String> atoms, StringBuilder word) {
        if (word.length() > 0) {
            atoms.add(word.toString());
            word.setLength(0);
        }
    }

    /** ASCII 单词字符可以整体换行；CJK / 标点则逐字换行。 */
    private static boolean isWordChar(int cp) {
        return (cp >= 'a' && cp <= 'z') || (cp >= 'A' && cp <= 'Z') || (cp >= '0' && cp <= '9')
                || cp == '_' || cp == '-' || cp == '\'' || cp == '/' || cp == '.' || cp == ':' || cp == '%';
    }

    private static int parseHex(String hex) {
        try {
            if (hex.length() == 3) {
                int r = Integer.parseInt(hex.substring(0, 1), 16);
                int g = Integer.parseInt(hex.substring(1, 2), 16);
                int b = Integer.parseInt(hex.substring(2, 3), 16);
                return (r * 17 << 16) | (g * 17 << 8) | (b * 17);
            }
            if (hex.length() == 6) {
                return Integer.parseInt(hex, 16);
            }
        } catch (NumberFormatException ignored) {
        }
        return -1;
    }

    private static int legacyColor(char code) {
        return switch (Character.toLowerCase(code)) {
            case '0' -> 0x000000;
            case '1' -> 0x0000AA;
            case '2' -> 0x00AA00;
            case '3' -> 0x00AAAA;
            case '4' -> 0xAA0000;
            case '5' -> 0xAA00AA;
            case '6' -> 0xFFAA00;
            case '7' -> 0xAAAAAA;
            case '8' -> 0x555555;
            case '9' -> 0x5555FF;
            case 'a' -> 0x55FF55;
            case 'b' -> 0x55FFFF;
            case 'c' -> 0xFF5555;
            case 'd' -> 0xFF55FF;
            case 'e' -> 0xFFFF55;
            case 'f' -> 0xFFFFFF;
            default -> -1;
        };
    }
}
