package kim.biryeong.textcenterer.smoke;

import java.net.URI;
import java.nio.file.*;
import java.util.*;
import java.util.function.Consumer;
import com.mojang.blaze3d.platform.InputConstants;
import kim.biryeong.textcenterer.TextCentererConfigScreen;
import kim.biryeong.textcenterer.chat.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.input.*;
import net.minecraft.client.multiplayer.chat.*;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.network.chat.*;
import net.minecraft.util.FormattedCharSequence;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

/** Runs only in the separate smoke mod, against real transformed client classes. */
public final class ClientSmoke {
    private static boolean finished;
    public static void tick(Minecraft client) {
        if (finished || client.gui.overlay() != null) return;
        if (!(client.gui.screen() instanceof TitleScreen)) client.setScreenAndShow(new TitleScreen());
        finished = true;
        try {
            runChecks(client);
            Files.writeString(Path.of("smoke-result.txt"), "PASS: 26.3 real client mixins; all alignments/link/background matrix; heads, stacking, selection; sliders/reset/panel fade; locale loading/draft return; GUI scale fitting; Shift axes; offscreen drag; negative-Y persistence\n");
            System.out.println("TEXT_CENTERER_SMOKE_PASS");
        } catch (Throwable error) {
            error.printStackTrace();
            try { Files.writeString(Path.of("smoke-result.txt"), "FAIL: " + error); } catch (Exception ignored) {}
            System.out.println("TEXT_CENTERER_SMOKE_FAIL");
        } finally { client.stop(); }
    }

    private static void runChecks(Minecraft client) throws Exception {
        int gui = client.options.guiScale().get();
        double scale = client.options.chatScale().get(), spacing = client.options.chatLineSpacing().get();
        int x = TextCentererConfig.chatXOffset(), y = TextCentererConfig.chatYOffset();
        int size = TextCentererConfig.headSize(), headY = TextCentererConfig.headYOffset();
        int textY = TextCentererConfig.chatTextYOffset();
        boolean background = TextCentererConfig.responsiveBackground();
        boolean timestamps = TextCentererConfig.timestamps(), stacking = TextCentererConfig.messageStacking();
        var mode = TextCentererConfig.headDetectionMode();
        var alignment = TextCentererConfig.textAlignment();
        double chatWidth = client.options.chatWidth().get(), focusedHeight = client.options.chatHeightFocused().get(), unfocusedHeight = client.options.chatHeightUnfocused().get();
        Path config = Path.of("config/text-centerer.properties");
        byte[] original = Files.exists(config) ? Files.readAllBytes(config) : null;
        Screen parent = client.gui.screen();
        try {
            TextCentererConfig.previewChatXOffset(0);
            TextCentererConfig.previewResponsiveBackground(true);
            if (timestamps) TextCentererConfig.toggleTimestamps();
            if (!stacking) TextCentererConfig.toggleMessageStacking();
            while (TextCentererConfig.headDetectionMode() != HeadDetection.Mode.BOTH) TextCentererConfig.cycleHeadDetectionMode();
            for (var alignmentCase : TextCentererConfig.TextAlignment.values()) {
                TextCentererConfig.previewTextAlignment(alignmentCase);
                checkGeometry(client);
            }
            TextCentererConfig.previewChatTextYOffset(6);
            checkGeometry(client);
            TextCentererConfig.previewChatTextYOffset(-6);
            checkGeometry(client);
            TextCentererConfig.previewChatTextYOffset(0);
            checkAlignedHeads(client);
            checkHeadsAndSelection(client);
            checkTranslations(client);
            checkLayoutEntry(client);
            checkLayoutYOption(client, parent);
            checkLayout(client, parent);
            PositionSmoke.check(client);
        } finally {
            client.setScreenAndShow(parent);
            client.options.guiScale().set(gui);
            client.options.chatScale().set(scale);
            client.options.chatLineSpacing().set(spacing);
            client.resizeGui();
            TextCentererConfig.previewTextAlignment(alignment);
            client.options.chatWidth().set(chatWidth);
            client.options.chatHeightFocused().set(focusedHeight);
            client.options.chatHeightUnfocused().set(unfocusedHeight);
            TextCentererConfig.previewChatXOffset(x);
            TextCentererConfig.previewChatYOffset(y);
            TextCentererConfig.previewChatTextYOffset(textY);
            TextCentererConfig.previewHeadSize(size);
            TextCentererConfig.previewHeadYOffset(headY);
            TextCentererConfig.previewResponsiveBackground(background);
            if (TextCentererConfig.timestamps() != timestamps) TextCentererConfig.toggleTimestamps();
            if (TextCentererConfig.messageStacking() != stacking) TextCentererConfig.toggleMessageStacking();
            while (TextCentererConfig.headDetectionMode() != mode) TextCentererConfig.cycleHeadDetectionMode();
            BuiltinChatHeads.processingSender = null;
            client.gui.hud.getChat().clearMessages(false);
            if (original == null) Files.deleteIfExists(config); else Files.write(config, original);
        }
    }

    private static void checkGeometry(Minecraft client) throws Exception {
        var chat = client.gui.hud.getChat();
        var click = new ClickEvent.OpenUrl(URI.create("https://example.com/"));
        Component message = Component.literal("Click link").withStyle(style -> style.withClickEvent(click));
        var render = ChatComponent.class.getDeclaredMethod("extractRenderState", ChatComponent.ChatGraphicsAccess.class,
            int.class, int.class, ChatComponent.DisplayMode.class);
        render.setAccessible(true);
        for (int gui : new int[]{3, 4}) {
            client.options.guiScale().set(gui);
            client.resizeGui();
            TextCentererConfig.previewChatXOffset(0);
            for (double scale : new double[]{0.5, 0.75, 1.0}) {
                client.options.chatScale().set(scale);
                for (int offset : new int[]{40, 77, 150}) {
                    client.options.chatLineSpacing().set(0.0);
                    TextCentererConfig.previewChatYOffset(offset);
                    chat.clearMessages(false);
                    chat.addClientSystemMessage(message);
                    int height = client.getWindow().getGuiScaledHeight();
                    int anchor = (int)Math.floor(client.getWindow().getGuiScaledWidth() / scale / 2);
                    int textX = anchor - switch (TextCentererConfig.textAlignment()) {
                        case LEFT -> 0; case CENTER -> client.font.width(message) / 2; case RIGHT -> client.font.width(message);
                    };
                    int bottom = (int)Math.floor((height - offset) / (float)scale);
                    int x = (int)Math.floor((textX + 3) * scale), y = (int)Math.floor((bottom - 5 + TextCentererConfig.chatTextYOffset()) * scale);
                    var finder = new ActiveTextCollector.ClickableStyleFinder(client.font, x, y);
                    chat.captureClickableText(finder, height, client.gui.hud.getGuiTicks(), ChatComponent.DisplayMode.FOREGROUND);
                    require(finder.result() != null && click.equals(finder.result().getClickEvent()), "click GUI=" + gui + " scale=" + scale + " y=" + offset);
                    var outside = new ActiveTextCollector.ClickableStyleFinder(client.font, (int)((textX - 8) * scale), y);
                    chat.captureClickableText(outside, height, client.gui.hud.getGuiTicks(), ChatComponent.DisplayMode.FOREGROUND);
                    require(outside.result() == null, "outside link accepted");
                }
                for (double spacing : new double[]{0, 0.5, 1}) {
                    client.options.chatLineSpacing().set(spacing);
                    chat.clearMessages(false);
                    chat.addClientSystemMessage(Component.literal("A much longer chat background test"));
                    chat.addClientSystemMessage(Component.literal("Short"));
                    for (var mode : new ChatComponent.DisplayMode[]{ChatComponent.DisplayMode.FOREGROUND, ChatComponent.DisplayMode.BACKGROUND}) {
                        var probe = new BackgroundProbe(client);
                        render.invoke(chat, probe, client.getWindow().getGuiScaledHeight(), client.gui.hud.getGuiTicks(), mode);
                        require(probe.backgrounds.size() == 2 && probe.texts.size() == 2, "missing backgrounds/text");
                        for (int i = 0; i < 2; i++) {
                            float[] bg = probe.backgrounds.get(i), text = probe.texts.get(i);
                            require(Math.abs(bg[0] - (text[0] - 4 * scale)) < 0.01 && Math.abs(bg[1] - (text[1] + 4 * scale)) < 0.01,
                                "background scale=" + scale + " spacing=" + spacing + " mode=" + mode);
                        }
                    }
                }
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static void checkHeadsAndSelection(Minecraft client) throws Exception {
        var chat = client.gui.hud.getChat();
        chat.clearMessages(false);
        client.options.chatScale().set(1.0);
        client.options.chatLineSpacing().set(0.0);
        TextCentererConfig.previewChatYOffset(77);
        UUID sender = new UUID(123, 456);
        BuiltinChatHeads.processingSender = sender;
        try {
            chat.addClientSystemMessage(Component.literal("Standalone head"));
            chat.addClientSystemMessage(Component.literal("Standalone head"));
        } finally { BuiltinChatHeads.processingSender = null; }
        var field = ChatComponent.class.getDeclaredField("trimmedMessages");
        field.setAccessible(true);
        List<GuiMessage.Line> lines = (List<GuiMessage.Line>)field.get(chat);
        require(lines.size() == 1, "identical messages did not stack");
        require(sender.equals(BuiltinChatHeads.sender(lines.getFirst())), "standalone sender lost through stacking");
        StringBuilder content = new StringBuilder();
        lines.getFirst().content().accept((index, style, point) -> { content.appendCodePoint(point); return true; });
        require(content.toString().contains("2"), "stack count missing");
        // Selection collection needs the chat screen marker, but vanilla init requires
        // a connected player. This title-screen probe tests HUD extraction only.
        var screen = new ChatScreen("unsent draft", false) {
            @Override protected void init() {
                input = new EditBox(font, 4, height - 12, width - 8, 12, Component.literal("Smoke draft"));
                input.setValue("unsent draft");
            }
            @Override protected void setInitialFocus() {}
            @Override public void removed() {}
            @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {}
        };
        client.setScreenAndShow(screen);
        var graphics = graphics(client);
        chat.extractRenderState(graphics, client.font, client.gui.hud.getGuiTicks(), 0, 0, ChatComponent.DisplayMode.FOREGROUND, false);
        var frameField = ChatTextSelection.class.getDeclaredField("frame");
        frameField.setAccessible(true);
        List<ChatTextSelection.Line> frame = (List<ChatTextSelection.Line>)frameField.get(ChatSelectionRenderer.SELECTION);
        require(!frame.isEmpty(), "real chat render did not collect selection lines");
        var line = frame.getFirst();
        require(ChatSelectionRenderer.SELECTION.begin(line.left() + 1, line.top() + 1), "selection begin");
        ChatSelectionRenderer.SELECTION.drag(line.left() + 12, line.top() + 1);
        chat.extractRenderState(graphics(client), client.font, client.gui.hud.getGuiTicks(), 0, 0, ChatComponent.DisplayMode.FOREGROUND, false);
    }

    private static void checkLayoutYOption(Minecraft client, Screen parent) throws Exception {
        TextCentererConfig.previewChatYOffset(77);
        int originalX = TextCentererConfig.chatXOffset();
        var mainScreen = new TextCentererConfigScreen(parent);
        client.setScreenAndShow(mainScreen);
        require(mainScreen.children().stream().noneMatch(widget -> widget instanceof AbstractSliderButton slider
            && slider.getMessage().getString().startsWith(Component.translatable("textcenterer.options.label.chat_y_offset").getString())), "Y option belongs only in layout");
        var screen = new TextCentererConfigScreen(parent, true);
        client.setScreenAndShow(screen);
        var slider = screen.children().stream().filter(AbstractSliderButton.class::isInstance)
            .map(AbstractSliderButton.class::cast).filter(candidate -> candidate.getMessage().getString().startsWith(
                Component.translatable("textcenterer.options.label.chat_y_offset").getString())).findFirst().orElseThrow();
        require(slider.getMessage().getString().startsWith(
            Component.translatable("textcenterer.options.label.chat_y_offset").getString()), "layout Y slider visible");
        slider.setFocused(true);
        slider.keyPressed(new KeyEvent(InputConstants.KEY_RIGHT, InputConstants.KEYCODE_RIGHT, 0));
        require(TextCentererConfig.chatYOffset() == 78 && TextCentererConfig.chatXOffset() == originalX, "layout Y adjusts independently");
        screen.onClose();
        require(Files.readString(Path.of("config/text-centerer.properties")).contains("chat_y_offset=78"), "layout Y saved on close");
        client.setScreenAndShow(screen);
        var reset = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
            .filter(b -> b.getHeight() == 28 && b.getMessage().getString().equals(Component.translatable("controls.reset").getString()))
            .findFirst().orElseThrow();
        reset = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
            .filter(b -> b.getHeight() == 28 && b.getMessage().getString().equals(Component.translatable("controls.reset").getString()))
            .toList().get(3);
        reset.visible = true;
        reset.onPress(new KeyEvent(InputConstants.KEY_RETURN, InputConstants.KEYCODE_RETURN, 0));
        Thread.sleep(110);
        screen.tick();
        require(TextCentererConfig.chatYOffset() == 77 && TextCentererConfig.chatXOffset() == originalX, "layout Y reset independent");
        screen.onClose();
    }

    private static void checkLayout(Minecraft client, Screen parent) throws Exception {
        client.options.guiScale().set(2);
        client.resizeGui();
        var constructor = TextCentererConfigScreen.class.getDeclaredConstructor(Screen.class, boolean.class);
        constructor.setAccessible(true);
        Screen screen = constructor.newInstance(parent, true);
        client.setScreenAndShow(screen);
        List<AbstractSliderButton> sliders = screen.children().stream().filter(AbstractSliderButton.class::isInstance).map(AbstractSliderButton.class::cast).toList();
        List<Button> resets = screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).filter(b -> b.getHeight() == 28 && b.getMessage().getString().equals(Component.translatable("controls.reset").getString())).toList();
        require(sliders.size() == 5 && resets.size() == 5, "five layout sliders and inline resets required");
        int[] defaults = {8, 0, 0, 77, 0};
        TextCentererConfig.previewChatTextYOffset(0);
        TextCentererConfig.previewHeadSize(8);
        TextCentererConfig.previewHeadYOffset(0);
        TextCentererConfig.previewChatXOffset(0);
        TextCentererConfig.previewChatYOffset(77);
        for (int i = 0; i < 5; i++) {
            var slider = sliders.get(i);
            slider.setFocused(true);
            require(slider.keyPressed(new KeyEvent(InputConstants.KEY_RIGHT, InputConstants.KEYCODE_RIGHT, 0)), "slider arrow ignored");
            require(values()[i] == defaults[i] + 1, "integer arrow not one unit");
            slider.setFocused(false);
            int[] before = values();
            var reset = resets.get(i);
            reset.visible = true;
            reset.onPress(new KeyEvent(InputConstants.KEY_RETURN, InputConstants.KEYCODE_RETURN, 0));
            require(values()[i] == before[i], "animated reset fired before delay");
            screen.extractRenderState(graphics(client), reset.getX() + 1, reset.getY() + 1, 0);
            Thread.sleep(110);
            screen.tick();
            require(values()[i] == defaults[i], "inline reset failed");
            for (int j = 0; j < 5; j++) if (j != i) require(values()[j] == before[j], "reset changed unrelated slider");
            require(screen.children().contains(slider), "reset rebuilt widgets");
        }
        var slider = sliders.getFirst();
        slider.visible = true;
        var event = new MouseButtonEvent(slider.getRight() - 4, slider.getY() + 10, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0));
        slider.mouseClicked(event, false);
        slider.mouseReleased(event);
        require(TextCentererConfig.headSize() == 32, "mouse slider live update");
        screen.extractRenderState(graphics(client), 0, 0, 0);
        // Fade only while the preview overlaps the panel during a drag.
        var draggingField = TextCentererConfigScreen.class.getDeclaredField("dragging"); draggingField.setAccessible(true);
        var panelLeftField = TextCentererConfigScreen.class.getDeclaredField("panelLeft"); panelLeftField.setAccessible(true);
        var rowsTopField = TextCentererConfigScreen.class.getDeclaredField("rowsTop"); rowsTopField.setAccessible(true);
        int panelLeft = panelLeftField.getInt(screen), rowsTop = rowsTopField.getInt(screen);
        TextCentererConfig.previewChatXOffset(panelLeft + 100 - screen.width / 2);
        TextCentererConfig.previewChatYOffset(screen.height - rowsTop - 45);
        draggingField.setBoolean(screen, true);
        screen.extractRenderState(graphics(client), 0, 0, 0);
        require(Math.abs(slider.getAlpha() - 0.35f) < 0.001 && Math.abs(resets.getFirst().getAlpha() - 0.35f) < 0.001, "overlapping panel opacity");
        draggingField.setBoolean(screen, false);
        screen.extractRenderState(graphics(client), 0, 0, 0);
        require(slider.getAlpha() == 1.0f && resets.getFirst().getAlpha() == 1.0f, "panel opacity restoration");
        screen.onClose();
        require(Files.readString(Path.of("config/text-centerer.properties")).contains("head_size=32"), "save on close");
    }

    private static void checkTranslations(Minecraft client) throws Exception {
        require(!Component.translatable("textcenterer.options.position").getString().equals("textcenterer.options.position"), "translations not loaded");
        for (String locale : List.of("en_us", "ko_kr", "zh_cn", "zh_tw")) {
            var resource = client.getResourceManager().getResource(net.minecraft.resources.Identifier.fromNamespaceAndPath("text-centerer", "lang/" + locale + ".json"));
            require(resource.isPresent(), "missing locale " + locale);
        }
    }

    private static void checkLayoutEntry(Minecraft client) throws Exception {
        // Only connection-dependent initialization is excluded from this title-screen harness.
        var chatScreen = new ChatScreen("initial draft", false) {
            @Override protected void init() {
                input = new EditBox(font, 4, height - 12, width - 8, 12, Component.empty());
                input.setValue(initial);
            }
            @Override protected void setInitialFocus() {}
            @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float delta) {}
        };
        client.setScreenAndShow(chatScreen);
        var inputField = ChatScreen.class.getDeclaredField("input"); inputField.setAccessible(true);
        ((EditBox)inputField.get(chatScreen)).setValue("unsent 한글 draft");
        var hook = Arrays.stream(ChatScreen.class.getDeclaredMethods()).filter(m -> m.getName().contains("textCenterer$restore") && Arrays.equals(m.getParameterTypes(), new Class<?>[]{org.spongepowered.asm.mixin.injection.callback.CallbackInfo.class})).findFirst().orElseThrow();
        hook.setAccessible(true);
        hook.invoke(chatScreen, new org.spongepowered.asm.mixin.injection.callback.CallbackInfo("init", false));
        Button button = chatScreen.children().stream().filter(Button.class::isInstance).map(Button.class::cast).findFirst().orElseThrow();
        require(chatScreen.mouseClicked(new MouseButtonEvent(button.getX() + 2, button.getY() + 2,
            new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false), "layout button click");
        require(client.gui.screen() instanceof TextCentererConfigScreen, "layout editor did not open");
        client.gui.screen().onClose();
        require(client.gui.screen() == chatScreen && ((EditBox)inputField.get(chatScreen)).getValue().equals("unsent 한글 draft"), "layout draft lost");
    }

    private static void checkAlignedHeads(Minecraft client) throws Exception {
        var chat = client.gui.hud.getChat();
        client.options.chatScale().set(0.75);
        client.options.chatLineSpacing().set(0.0);
        TextCentererConfig.previewChatXOffset(0);
        TextCentererConfig.previewChatYOffset(77);
        TextCentererConfig.previewHeadSize(16);
        var render = ChatComponent.class.getDeclaredMethod("extractRenderState", ChatComponent.ChatGraphicsAccess.class,
            int.class, int.class, ChatComponent.DisplayMode.class);
        render.setAccessible(true);
        var click = new ClickEvent.OpenUrl(URI.create("https://example.com/"));
        var message = Component.literal("Head test").withStyle(style -> style.withClickEvent(click));
        for (var alignment : TextCentererConfig.TextAlignment.values()) for (boolean heads : new boolean[]{false, true})
            for (boolean responsive : new boolean[]{false, true}) {
                TextCentererConfig.previewTextAlignment(alignment);
                TextCentererConfig.previewResponsiveBackground(responsive);
                chat.clearMessages(false);
                BuiltinChatHeads.processingSender = heads ? new UUID(123, 456) : null;
                try { chat.addClientSystemMessage(message); } finally { BuiltinChatHeads.processingSender = null; }
                int width = client.font.width(message), head = heads ? BuiltinChatHeads.width() : 0;
                double scale = client.options.chatScale().get();
                int anchor = (int)Math.floor(client.getWindow().getGuiScaledWidth() / 2.0 / scale);
                int textX = anchor + switch(alignment) { case LEFT -> head; case CENTER -> -width / 2; case RIGHT -> -width; };
                int fixed = (int)Math.ceil(ChatComponent.getWidth(client.options.chatWidth().get()) / scale);
                int bgX = responsive ? textX : anchor + switch(alignment) {case LEFT -> 0; case CENTER -> -fixed / 2; case RIGHT -> -fixed;};
                for (var display : new ChatComponent.DisplayMode[]{ChatComponent.DisplayMode.FOREGROUND, ChatComponent.DisplayMode.BACKGROUND}) {
                    var probe = new BackgroundProbe(client);
                    render.invoke(chat, probe, client.getWindow().getGuiScaledHeight(), client.gui.hud.getGuiTicks(), display);
                    require(probe.texts.size() == 1 && probe.backgrounds.size() == 1, "head matrix line count");
                    require(Math.abs(probe.texts.getFirst()[0] - textX * scale) < 0.01, "head/text edge " + alignment);
                    require(Math.abs(probe.backgrounds.getFirst()[0] - (bgX - 4 - (responsive ? head : 0)) * scale) < 0.01, "head background left");
                    require(Math.abs(probe.backgrounds.getFirst()[1] - (bgX + (responsive ? width : fixed) + 4) * scale) < 0.01, "head background right");
                }
                int bottom = (int)Math.floor((client.getWindow().getGuiScaledHeight() - 77) / (float)scale);
                var finder = new ActiveTextCollector.ClickableStyleFinder(client.font, (int)((textX + 3) * scale), (int)((bottom - 5) * scale));
                chat.captureClickableText(finder, client.getWindow().getGuiScaledHeight(), client.gui.hud.getGuiTicks(), ChatComponent.DisplayMode.FOREGROUND);
                require(finder.result() != null && click.equals(finder.result().getClickEvent()), "head link hit testing");
            }
        TextCentererConfig.previewHeadSize(8);
        TextCentererConfig.previewTextAlignment(TextCentererConfig.TextAlignment.CENTER);
        TextCentererConfig.previewResponsiveBackground(true);
    }

    private static int[] values() { return new int[]{TextCentererConfig.headSize(), TextCentererConfig.headYOffset(), TextCentererConfig.chatXOffset(), TextCentererConfig.chatYOffset(), TextCentererConfig.chatTextYOffset()}; }
    private static GuiGraphicsExtractor graphics(Minecraft client) { return new GuiGraphicsExtractor(client, new GuiRenderState(), 0, 0); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }

    private static final class BackgroundProbe implements ChatComponent.ChatGraphicsAccess {
        final Minecraft client;
        final Matrix3x2f pose = new Matrix3x2f();
        final ArrayList<float[]> backgrounds = new ArrayList<>(), texts = new ArrayList<>();
        BackgroundProbe(Minecraft client) { this.client = client; }
        public void updatePose(Consumer<Matrix3x2f> update) { update.accept(pose); }
        public void fill(int l, int t, int r, int b, int color) {
            backgrounds.add(new float[]{pose.transformPosition(l, t, new Vector2f()).x, pose.transformPosition(r, b, new Vector2f()).x});
        }
        public boolean handleMessage(int top, float opacity, FormattedCharSequence message) {
            texts.add(new float[]{pose.transformPosition(0, top, new Vector2f()).x, pose.transformPosition(client.font.width(message), top, new Vector2f()).x});
            return false;
        }
        public void handleTag(int l, int t, int r, int b, float opacity, GuiMessageTag tag) {}
        public void handleTagIcon(int l, int b, boolean force, GuiMessageTag tag, GuiMessageTag.Icon icon) {}
    }
}
