package kim.biryeong.textcenterer.mixin.client;

import com.mojang.blaze3d.platform.InputConstants;
import kim.biryeong.textcenterer.ChatSearchScreen;
import kim.biryeong.textcenterer.chat.ChatHistoryAccess;
import kim.biryeong.textcenterer.chat.ChatSelectionRenderer;
import net.minecraft.client.gui.ActiveTextCollector;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ChatScreen.class)
abstract class ChatScreenSelectionMixin extends Screen {
    @Shadow protected EditBox input;
    @Shadow protected String initial;
    @Shadow private ChatComponent.DisplayMode displayMode;
    @Shadow private boolean handleComponentClicked(Style style, boolean insertion) { throw new AssertionError(); }
    @Unique private Style textCenterer$pressedStyle;
    @Unique private boolean textCenterer$insertion;
    @Unique private net.minecraft.client.gui.components.Button textCenterer$layoutButton;
    protected ChatScreenSelectionMixin(Component title) { super(title); }

    @Inject(method="mouseClicked", at=@At("HEAD"), cancellable=true)
    private void textCenterer$clear(MouseButtonEvent event, boolean twice, CallbackInfoReturnable<Boolean> cir) {
        ChatSelectionRenderer.SELECTION.clear();
        textCenterer$pressedStyle = null;
        if (textCenterer$layoutButton != null && textCenterer$layoutButton.mouseClicked(event, twice))
            cir.setReturnValue(true);
    }
    // Command suggestions have already had first refusal at this point.
    @Inject(method="mouseClicked", at=@At(value="INVOKE", target="Lnet/minecraft/client/Minecraft;getWindow()Lcom/mojang/blaze3d/platform/Window;"), cancellable=true)
    private void textCenterer$select(MouseButtonEvent event, boolean twice, CallbackInfoReturnable<Boolean> cir) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && event.y() < height - 14
                && ChatSelectionRenderer.SELECTION.begin(event.x(), event.y())) {
            textCenterer$insertion = minecraft.hasShiftDown();
            var finder = new ActiveTextCollector.ClickableStyleFinder(font, (int)event.x(), (int)event.y())
                .includeInsertions(textCenterer$insertion);
            minecraft.gui.hud.getChat().captureClickableText(finder, minecraft.getWindow().getGuiScaledHeight(),
                minecraft.gui.hud.getGuiTicks(), displayMode);
            textCenterer$pressedStyle = finder.result();
            cir.setReturnValue(true);
        }
    }
    @Override public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && ChatSelectionRenderer.SELECTION.drag(event.x(), event.y())) return true;
        return super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(MouseButtonEvent event) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && ChatSelectionRenderer.SELECTION.pressed()) {
            ChatSelectionRenderer.SELECTION.drag(event.x(), event.y());
            boolean dragged = ChatSelectionRenderer.SELECTION.release();
            Style style = textCenterer$pressedStyle;
            textCenterer$pressedStyle = null;
            if (!dragged && style != null && handleComponentClicked(style, textCenterer$insertion)) initial = input.getValue();
            return true;
        }
        return super.mouseReleased(event);
    }
    @Inject(method="keyPressed", at=@At("HEAD"), cancellable=true)
    private void textCenterer$key(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.key() == InputConstants.KEY_F && event.hasControlDown()) {
            minecraft.setScreenAndShow(new ChatSearchScreen(input.getValue()));
            cir.setReturnValue(true);
        } else if (event.isCopy() && ChatSelectionRenderer.SELECTION.hasSelection()) {
            minecraft.keyboardHandler.setClipboard(ChatSelectionRenderer.SELECTION.clipboardText());
            cir.setReturnValue(true);
        } else if (event.key() != InputConstants.KEY_LSHIFT && event.key() != InputConstants.KEY_RSHIFT
                && event.key() != InputConstants.KEY_LCONTROL && event.key() != InputConstants.KEY_RCONTROL) {
            ChatSelectionRenderer.SELECTION.clear();
        }
    }
    @Inject(method={"removed", "init"}, at=@At("HEAD"))
    private void textCenterer$reset(CallbackInfo ci) {
        textCenterer$pressedStyle = null;
        ChatSelectionRenderer.SELECTION.clear();
        ChatSelectionRenderer.SELECTION.frame(java.util.List.of());
    }
    @Inject(method="init", at=@At("TAIL"))
    private void textCenterer$restore(CallbackInfo ci) {
        ((ChatHistoryAccess)minecraft.gui.hud.getChat()).textCenterer$restoreHistory();
        Component label = Component.translatable("textcenterer.options.position");
        int buttonWidth = Math.min(width - 8, font.width(label) + 16);
        textCenterer$layoutButton = addRenderableWidget(net.minecraft.client.gui.components.Button.builder(label, button -> {
            initial = input.getValue();
            minecraft.setScreenAndShow(new kim.biryeong.textcenterer.TextCentererConfigScreen(this, true));
        }).bounds(width - buttonWidth - 4, 4, buttonWidth, 20).build());
    }
    @Inject(method="extractRenderState", at=@At("TAIL"))
    private void textCenterer$hint(GuiGraphicsExtractor graphics, int x, int y, float delta, CallbackInfo ci) {
        if (ChatSelectionRenderer.SELECTION.hasSelection())
            graphics.text(font, Component.translatable("textcenterer.chat.copy_hint"), 4, 4, 0xFFFFFF55, true);
    }
}
