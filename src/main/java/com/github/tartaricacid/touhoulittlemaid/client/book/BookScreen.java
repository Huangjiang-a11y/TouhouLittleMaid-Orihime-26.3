package com.github.tartaricacid.touhoulittlemaid.client.book;

import com.github.tartaricacid.touhoulittlemaid.util.migrate.ScreenUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.TextAlignment;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.MultiLineLabel;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.List;

/** 自包含书壳 GUI（不依赖 Patchouli）：左侧分类 + 条目，右侧页内容。 */
public class BookScreen extends Screen {
    private static final int WIDTH = 380;
    private static final int HEIGHT = 210;
    private static final int LEFT_W = 110;
    private static final int ROWS = 8;
    private static final int ROW_H = 13;
    private static final int TEXT_COLOR = 0xFF40403F;
    private static final int DIM_COLOR = 0xFF80807F;
    private static final int PANEL = 0xFFF0E6D2;
    private static final int PANEL_LEFT = 0xFFE4D8C0;
    private static final int SELECTED = 0x55A0703C;

    private final BookContent content;
    private final Screen lastScreen;

    private int categoryIndex;
    private int entryScroll;
    private int entryIndex;
    private int pageIndex;

    public BookScreen(BookContent content, Screen lastScreen) {
        super(content.name());
        this.content = content;
        this.lastScreen = lastScreen;
    }

    public static void open(BookContent content) {
        ScreenUtil.setScreen(new BookScreen(content, ScreenUtil.getScreen()));
    }

    private List<BookEntry> currentEntries() {
        List<BookCategory> categories = this.content.categories();
        if (categories.isEmpty()) {
            return this.content.entries();
        }
        return this.content.entriesOf(categories.get(Math.min(this.categoryIndex, categories.size() - 1)).id());
    }

    /** 当前条目（可能为 null：分类下没条目，或还没选）。 */
    private BookEntry currentEntry() {
        List<BookEntry> entries = this.currentEntries();
        if (entries.isEmpty()) {
            return null;
        }
        return entries.get(Math.min(this.entryIndex, entries.size() - 1));
    }

    private BookPage currentPage() {
        BookEntry entry = this.currentEntry();
        if (entry == null || entry.pages().isEmpty()) {
            return null;
        }
        return entry.pages().get(Math.min(this.pageIndex, entry.pages().size() - 1));
    }

    @Override
    protected void init() {
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        List<BookCategory> categories = this.content.categories();
        List<BookEntry> entries = this.currentEntries();
        BookEntry entry = this.currentEntry();

        // 分类按钮（横排）
        int cw = LEFT_W / Math.max(1, categories.size()) - 2;
        for (int i = 0; i < categories.size(); i++) {
            int index = i;
            this.addRenderableWidget(Button.builder(categories.get(i).name(), b -> {
                this.categoryIndex = index;
                this.entryScroll = 0;
                this.entryIndex = 0;
                this.pageIndex = 0;
                this.rebuildWidgets();
            }).bounds(left + 3 + i * (cw + 2), top + 4, cw, 16).build());
        }

        // 条目列表翻页
        this.addRenderableWidget(Button.builder(Component.literal("▲"), b -> {
            this.entryScroll = Math.max(0, this.entryScroll - ROWS);
            this.rebuildWidgets();
        }).bounds(left + 3, top + 26, 24, 14).build());
        this.addRenderableWidget(Button.builder(Component.literal("▼"), b -> {
            if (this.entryScroll + ROWS < entries.size()) {
                this.entryScroll += ROWS;
            }
            this.rebuildWidgets();
        }).bounds(left + 3 + 26, top + 26, 24, 14).build());

        // 条目行按钮
        for (int row = 0; row < ROWS; row++) {
            int index = this.entryScroll + row;
            if (index >= entries.size()) {
                break;
            }
            int rowIndex = index;
            BookEntry e = entries.get(index);
            this.addRenderableWidget(Button.builder(e.name(), b -> {
                this.entryIndex = rowIndex;
                this.pageIndex = 0;
                this.rebuildWidgets();
            }).bounds(left + 3, top + 42 + row * ROW_H, LEFT_W - 6, 12).build());
        }

        // 翻页
        boolean hasPages = entry != null && entry.pages().size() > 1;
        Button prev = Button.builder(Component.literal("<"), b -> {
            this.pageIndex = Math.max(0, this.pageIndex - 1);
            this.rebuildWidgets();
        }).bounds(this.width / 2 + 60, top + HEIGHT - 22, 20, 16).build();
        Button next = Button.builder(Component.literal(">"), b -> {
            if (!hasPages || this.pageIndex < entry.pages().size() - 1) {
                this.pageIndex = this.pageIndex + 1;
            }
            this.rebuildWidgets();
        }).bounds(this.width / 2 + 84, top + HEIGHT - 22, 20, 16).build();
        prev.active = this.pageIndex > 0;
        next.active = hasPages && this.pageIndex < (entry == null ? 0 : entry.pages().size() - 1);
        this.addRenderableWidget(prev);
        this.addRenderableWidget(next);

        this.addRenderableWidget(Button.builder(CommonComponents.GUI_BACK, b -> ScreenUtil.setScreen(this.lastScreen))
                .bounds(left + 3, top + HEIGHT - 22, 46, 16).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int pMouseX, int pMouseY, float pPartialTick) {
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, PANEL);
        graphics.fill(left, top, left + LEFT_W, top + HEIGHT, PANEL_LEFT);

        BookEntry entry = this.currentEntry();
        int textX = left + LEFT_W + 8;
        int textW = WIDTH - LEFT_W - 16;
        int textY = top + 8;

        if (entry == null) {
            graphics.text(this.font, this.content.landingText(), textX, textY, TEXT_COLOR, false);
            if (this.content.entries().isEmpty()) {
                graphics.text(this.font, Component.literal("[no content loaded]"), textX, textY + 20, DIM_COLOR, false);
            }
            super.extractRenderState(graphics, pMouseX, pMouseY, pPartialTick);
            return;
        }

        // 条目标题 + 页码
        graphics.text(this.font, entry.name(), textX, textY, TEXT_COLOR, false);
        String pageText = (this.pageIndex + 1) + "/" + entry.pages().size();
        graphics.text(this.font, Component.literal(pageText), left + WIDTH - 8 - this.font.width(pageText), textY, DIM_COLOR, false);
        graphics.fill(textX, textY + 12, left + WIDTH - 8, textY + 13, 0x33000000);

        BookPage page = this.currentPage();
        if (page != null) {
            this.renderPage(graphics, page, textX, textY + 18, textW);
        }
        super.extractRenderState(graphics, pMouseX, pMouseY, pPartialTick);
    }

    /** 页渲染：text / image / spotlight 已实现，其余先回退成文本。 */
    private void renderPage(GuiGraphicsExtractor graphics, BookPage page, int x, int y, int width) {
        String type = page.type();
        if (!"text".equals(type) && page.has("title")) {
            graphics.text(this.font, page.text("title"), x, y, TEXT_COLOR, false);
            y += 12;
        }
        if ("image".equals(type)) {
            y = this.renderImage(graphics, page, x, y, width);
        } else if ("spotlight".equals(type)) {
            y = this.renderSpotlight(graphics, page, x, y, width);
        } else if ("altar_recipe".equals(type)) {
            AltarPageRenderer.render(graphics, page, x, y + 12, width, this.font, TEXT_COLOR);
            return;
        }
        String bodyKey = page.has("text") ? "text" : (page.has("name") ? "name" : null);
        if (bodyKey != null) {
            MultiLineLabel label = MultiLineLabel.create(this.font, page.text(bodyKey), width);
            label.visitLines(TextAlignment.LEFT, x, y, 9, graphics.textRenderer());
        } else if (!"image".equals(type) && !"spotlight".equals(type)) {
            graphics.text(this.font, Component.literal("<" + type + ">"), x, y, DIM_COLOR, false);
        }
    }

    /** 右侧内容区可用下边界（避开底部按钮行）。 */
    private int contentBottom() {
        return (this.height - HEIGHT) / 2 + HEIGHT - 26;
    }

    /**
     * image 页：按贴图真实宽高等比缩放到页面内**完整**显示（不裁剪、不放大），居中 + 描边 + 白底。
     * 注意：必须用带源区域参数的 blit（12 参数版），
     * 只有它会在「源区域 -&gt; 目标尺寸」之间做缩放；10 参数版是 1:1 像素裁剪。
     */
    private int renderImage(GuiGraphicsExtractor graphics, BookPage page, int x, int y, int width) {
        List<String> images = page.strList("images");
        if (images.isEmpty() && page.has("image")) {
            images = List.of(page.str("image"));
        }
        for (String raw : images) {
            Identifier id = Identifier.tryParse(raw);
            if (id == null) {
                continue;
            }
            int[] size = BookImages.size(id);
            int texW = Math.max(1, size[0]);
            int texH = Math.max(1, size[1]);
            int availH = Math.max(24, this.contentBottom() - y);
            int wantW = page.integer("width", 0);
            int wantH = page.integer("height", 0);
            int drawW;
            int drawH;
            if (wantW > 0 && wantH > 0) {
                drawW = Math.min(wantW, width);
                drawH = Math.min(wantH, availH);
            } else {
                int boxW = wantW > 0 ? Math.min(wantW, width) : width;
                float scale = Math.min((float) boxW / texW, (float) availH / texH);
                scale = Math.min(scale, 1.0F);
                drawW = Math.max(1, Math.round(texW * scale));
                drawH = Math.max(1, Math.round(texH * scale));
            }
            int drawX = x + (width - drawW) / 2;
            graphics.fill(drawX - 1, y - 1, drawX + drawW + 1, y + drawH + 1, 0xFF8A8A85);
            graphics.fill(drawX, y, drawX + drawW, y + drawH, 0xFFFFFFFF);
            graphics.blit(RenderPipelines.GUI_TEXTURED, id, drawX, y, 0.0F, 0.0F,
                    drawW, drawH, texW, texH, texW, texH);
            y += drawH + 6;
        }
        return y;
    }

    /** spotlight 页：物品图标（item 支持逗号分隔多个，取第一个显示）。 */
    private int renderSpotlight(GuiGraphicsExtractor graphics, BookPage page, int x, int y, int width) {
        String raw = page.str("item");
        if (raw.isEmpty()) {
            return y;
        }
        Identifier id = Identifier.tryParse(raw.split(",")[0].trim());
        if (id == null) {
            return y;
        }
        Item item = BuiltInRegistries.ITEM.getValue(id);
        ItemStack stack = new ItemStack(item);
        int iconX = x + width / 2 - 8;
        graphics.item(stack, iconX, y);
        graphics.itemDecorations(this.font, stack, iconX, y);
        return y + 22;
    }
}
