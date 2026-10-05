package com.github.tartaricacid.touhoulittlemaid.client.book;

import com.github.tartaricacid.touhoulittlemaid.util.migrate.ScreenUtil;
import com.mojang.blaze3d.Blaze3D;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/** 自包含书壳 GUI（不依赖 Patchouli）：左侧分类 + 条目，右侧页内容。 */
public class BookScreen extends Screen {
    private static final int WIDTH = 380;
    private static final int HEIGHT = 210;
    private static final int LEFT_W = 110;
    private static final int ROWS = 8;
    private static final int ROW_H = 13;
    private static final int TEXT_COLOR = 0xFF40403F;
    private static final int LINK_COLOR = 0xFF1F6FC0;
    private static final int DIM_COLOR = 0xFF80807F;
    private static final int PANEL = 0xFFF0E6D2;
    private static final int PANEL_LEFT = 0xFFE4D8C0;
    private static final int SELECTED = 0x55A0703C;

    private final BookContent content;
    private final Screen lastScreen;

    private final List<BookRichText.ClickRegion> clickRegions = new ArrayList<>();
    private BookPage lastRenderedPage;

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
        this.clickRegions.clear();
        MultiblockPageRenderer.clearHitBox();
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

    /** 页渲染：text / image / spotlight / altar_recipe / crafting / link / entity / multiblock / item / header / separator。 */
    private void renderPage(GuiGraphicsExtractor graphics, BookPage page, int x, int y, int width) {
        if (page != this.lastRenderedPage) {
            this.lastRenderedPage = page;
            MultiblockPageRenderer.resetRotation();
        }
        String type = page.type();
        // Patchouli 允许 patchouli:xxx 前缀，这里统一剥掉
        if (type.startsWith("patchouli:")) {
            type = type.substring("patchouli:".length());
        }
        if (!"text".equals(type) && !"header".equals(type) && !"separator".equals(type) && page.has("title")) {
            graphics.text(this.font, page.text("title"), x, y, TEXT_COLOR, false);
            y += 12;
        }
        switch (type) {
            case "image" -> y = this.renderImage(graphics, page, x, y, width);
            case "spotlight" -> y = this.renderSpotlight(graphics, page, x, y, width);
            case "item" -> y = this.renderItemPage(graphics, page, x, y, width);
            case "entity" -> y = EntityPageRenderer.render(graphics, page, x, y, width);
            case "multiblock" -> y = MultiblockPageRenderer.render(graphics, page, x, y, width, this.font, TEXT_COLOR);
            case "crafting" -> {
                CraftingPageRenderer.render(graphics, page, x, y, width, this.font, TEXT_COLOR);
                return;
            }
            case "link" -> {
                this.renderLink(graphics, page, x, y, width);
                return;
            }
            case "altar_recipe" -> {
                AltarPageRenderer.render(graphics, page, x, y + 12, width, this.font, TEXT_COLOR);
                return;
            }
            case "header" -> {
                this.renderHeader(graphics, page, x, y, width);
                return;
            }
            case "separator" -> {
                this.renderSeparator(graphics, x, y, width);
                return;
            }
            default -> {
                if (!page.has("text") && !page.has("name")) {
                    graphics.text(this.font, Component.literal("<" + type + ">"), x, y, DIM_COLOR, false);
                    return;
                }
            }
        }
        this.renderBody(graphics, page, x, y, width);
    }

    /** 富文本正文（text / name 字段）：走 $() 宏解析，链接会登记成可点击区域。 */
    private void renderBody(GuiGraphicsExtractor graphics, BookPage page, int x, int y, int width) {
        String bodyKey = page.has("text") ? "text" : (page.has("name") ? "name" : null);
        if (bodyKey == null) {
            return;
        }
        BookRichText.of(page.plain(bodyKey), TEXT_COLOR, this.font, width)
                .render(graphics, this.font, x, y, this.clickRegions);
    }

    /** link 页：正文 + 居中的可点击外链。 */
    private void renderLink(GuiGraphicsExtractor graphics, BookPage page, int x, int y, int width) {
        int cy = y;
        if (page.has("text")) {
            BookRichText body = BookRichText.of(page.plain("text"), TEXT_COLOR, this.font, width);
            body.render(graphics, this.font, x, cy, this.clickRegions);
            cy += body.height() + 4;
        }
        String url = page.str("url");
        if (url.isEmpty() || !page.has("link_text")) {
            return;
        }
        String label = page.plain("link_text");
        int w = this.font.width(label);
        int lx = x + (width - w) / 2;
        MutableComponent component = Component.literal(label)
                .withStyle(style -> style.withColor(LINK_COLOR).withUnderlined(true));
        graphics.text(this.font, component, lx, cy, LINK_COLOR, false);
        this.clickRegions.add(new BookRichText.ClickRegion(lx, cy, lx + w, cy + 9, null, url));
    }

    /** item 页：物品图标 + 名称。 */
    private int renderItemPage(GuiGraphicsExtractor graphics, BookPage page, int x, int y, int width) {
        Identifier id = Identifier.tryParse(page.str("item").trim());
        if (id == null) {
            return y;
        }
        ItemStack stack = new ItemStack(BuiltInRegistries.ITEM.getValue(id));
        if (stack.isEmpty()) {
            return y;
        }
        int cx = x + width / 2 - 8;
        graphics.item(stack, cx, y + 2);
        graphics.itemDecorations(this.font, stack, cx, y + 2);
        graphics.centeredText(this.font, stack.getHoverName(), x + width / 2, y + 24, TEXT_COLOR);
        return y + 38;
    }

    /** header 页：居中标题 + 下划线。 */
    private void renderHeader(GuiGraphicsExtractor graphics, BookPage page, int x, int y, int width) {
        Component title = page.has("text") ? page.text("text") : Component.empty();
        graphics.centeredText(this.font, title, x + width / 2, y + 4, TEXT_COLOR);
        graphics.fill(x, y + 16, x + width, y + 17, 0x33000000);
    }

    /** separator 页：一条分隔线。 */
    private void renderSeparator(GuiGraphicsExtractor graphics, int x, int y, int width) {
        graphics.fill(x, y + 6, x + width, y + 7, 0x33000000);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mouseX = event.x();
        double mouseY = event.y();
        for (BookRichText.ClickRegion region : this.clickRegions) {
            if (!region.contains(mouseX, mouseY)) {
                continue;
            }
            if (region.url() != null && !region.url().isEmpty()) {
                this.openUrl(region.url());
                return true;
            }
            if (region.entry() != null && !region.entry().isEmpty()) {
                this.openEntry(region.entry());
                return true;
            }
        }
        int[] box = MultiblockPageRenderer.hitBox();
        if (box != null && mouseX >= box[0] && mouseX < box[2] && mouseY >= box[1] && mouseY < box[3]) {
            MultiblockPageRenderer.rotate();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    /** 书内跳转：$(l:条目) 的目标（相对 id 会补上本书命名空间，并忽略 #锚点）。 */
    private void openEntry(String target) {
        String id = target;
        int anchor = id.indexOf('#');
        if (anchor >= 0) {
            id = id.substring(0, anchor);
        }
        if (!id.contains(":")) {
            String contentId = this.content.id();
            int colon = contentId.indexOf(':');
            id = (colon >= 0 ? contentId.substring(0, colon) : contentId) + ":" + id;
        }
        for (BookEntry entry : this.content.entries()) {
            if (!entry.id().equals(id)) {
                continue;
            }
            List<BookCategory> categories = this.content.categories();
            for (int i = 0; i < categories.size(); i++) {
                if (categories.get(i).id().equals(entry.category())) {
                    this.categoryIndex = i;
                    break;
                }
            }
            List<BookEntry> current = this.currentEntries();
            for (int i = 0; i < current.size(); i++) {
                if (current.get(i).id().equals(id)) {
                    this.entryIndex = i;
                    break;
                }
            }
            this.entryScroll = Math.max(0, (this.entryIndex / ROWS) * ROWS);
            this.pageIndex = 0;
            this.rebuildWidgets();
            return;
        }
    }

    /** 外链：先弹确认框，同意后交给系统浏览器。 */
    private void openUrl(String url) {
        try {
            URI uri = URI.create(url);
            ScreenUtil.setScreen(new ConfirmLinkScreen(yes -> {
                if (yes) {
                    Blaze3D.openUri(uri);
                }
                ScreenUtil.setScreen(this);
            }, uri, true));
        } catch (Exception ignored) {
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
            // 不要再铺白底：书插图（256x256）四周本来就是透明边距（内容只在左上 200x200），
            // 铺白底会变成一大块白板，看着就像"缩放不对"。只描一圈细边即可。
            int frame = 0x55808080;
            graphics.fill(drawX - 1, y - 1, drawX + drawW + 1, y, frame);
            graphics.fill(drawX - 1, y + drawH, drawX + drawW + 1, y + drawH + 1, frame);
            graphics.fill(drawX - 1, y, drawX, y + drawH, frame);
            graphics.fill(drawX + drawW, y, drawX + drawW + 1, y + drawH, frame);
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
