package com.github.tartaricacid.touhoulittlemaid.client.gui.item;

import cn.sh1rocu.touhoulittlemaid.mixin.accessor.ScreenAccessor;
import com.github.tartaricacid.touhoulittlemaid.init.InitDataComponent;
import com.github.tartaricacid.touhoulittlemaid.network.message.PortableAudioControlPackage;
import com.github.tartaricacid.touhoulittlemaid.util.migrate.ScreenUtil;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.screen.v1.Screens;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 手提音响的播放界面：一个链接输入框 + 播放/停止按钮。
 */
public class PortableAudioGui extends Screen {
    private final ItemStack stack;
    private EditBox urlField;

    private PortableAudioGui(ItemStack stack) {
        super(Component.translatable("gui.touhou_little_maid.portable_audio.title"));
        this.stack = stack;
    }

    public static void open(ItemStack stack) {
        ScreenUtil.setScreen(new PortableAudioGui(stack));
    }

    @Override
    protected void init() {
        this.clearWidgets();
        int middleX = this.width / 2;
        int middleY = this.height / 2;

        this.urlField = new EditBox(Screens.getFont(this), middleX - 150, middleY - 30, 300, 20,
                Component.translatable("gui.touhou_little_maid.portable_audio.url"));
        this.urlField.setMaxLength(PortableAudioControlPackage.MAX_URL_LENGTH);
        this.urlField.setValue(this.stack.getOrDefault(InitDataComponent.PORTABLE_AUDIO_URL, ""));
        this.addWidget(this.urlField);
        this.setInitialFocus(this.urlField);

        this.addRenderableWidget(Button.builder(Component.translatable("gui.touhou_little_maid.portable_audio.play"), b -> send(true))
                .pos(middleX - 150, middleY).size(96, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.touhou_little_maid.portable_audio.stop"), b -> send(false))
                .pos(middleX - 48, middleY).size(96, 20).build());
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), b -> onClose())
                .pos(middleX + 54, middleY).size(96, 20).build());
    }

    private void send(boolean play) {
        String url = this.urlField.getValue().trim();
        if (url.isEmpty()) {
            return;
        }
        ClientPlayNetworking.send(new PortableAudioControlPackage(url, play));
    }

    @Override
    public void resize(int width, int height) {
        String value = this.urlField == null ? "" : this.urlField.getValue();
        super.resize(width, height);
        if (this.urlField != null) {
            this.urlField.setValue(value);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        this.urlField.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        for (Renderable renderable : ((ScreenAccessor) this).tlm$getRenderables()) {
            renderable.extractRenderState(graphics, mouseX, mouseY, partialTicks);
        }
        graphics.centeredText(font, this.title, this.width / 2, this.height / 2 - 55, 0xFFFFFFFF);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (this.urlField.mouseClicked(event, doubleClick)) {
            this.setFocused(this.urlField);
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    protected void insertText(String text, boolean overwrite) {
        if (overwrite) {
            this.urlField.setValue(text);
        } else {
            this.urlField.insertText(text);
        }
    }
}
