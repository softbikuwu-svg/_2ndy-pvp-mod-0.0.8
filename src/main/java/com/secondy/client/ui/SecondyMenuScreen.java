package com.secondy.client.ui;

import com.secondy.client.module.ArmorBreakModule;
import com.secondy.client.module.Category;
import com.secondy.client.module.CrosshairModule;
import com.secondy.client.module.HitSoundModule;
import com.secondy.client.module.HudModule;
import com.secondy.client.module.KeystrokesModule;
import com.secondy.client.module.Module;
import com.secondy.client.module.ModuleManager;
import com.secondy.client.module.TargetHudModule;
import com.secondy.client.module.ZoomModule;
import com.secondy.client.settings.ActionSetting;
import com.secondy.client.settings.BoolSetting;
import com.secondy.client.settings.ColorSetting;
import com.secondy.client.settings.CycleSetting;
import com.secondy.client.settings.FileSetting;
import com.secondy.client.settings.KeybindSetting;
import com.secondy.client.settings.ModeSetting;
import com.secondy.client.settings.NumberSetting;
import com.secondy.client.settings.PixelGridSetting;
import com.secondy.client.settings.Setting;
import com.secondy.client.util.Config;
import com.secondy.client.util.Input;
import com.secondy.client.util.Theme;
import com.secondy.client.util.Ui;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Module menu, three columns:
 * left - categories, middle - search + module cards, right - settings of the selected module.
 * Click a card to toggle it, "Settings" (or right-click) to show its settings on the right.
 * Just start typing to search; Right Shift closes the menu.
 */
public class SecondyMenuScreen extends Screen {
    // Size
    private static final int PW_MAX = 560, PH_MAX = 312, PAD = 10, RADIUS = 12;
    private static final int CARD_H = 42, CARD_GAP = 4, HDR_H = 14;

    // Colors (ARGB)
    private static final int PANEL        = 0xE8101114;
    private static final int PANEL_BORDER = 0x2EFFFFFF;
    private static final int SEPARATOR    = 0x1AFFFFFF;
    private static final int CARD         = 0x14FFFFFF;
    private static final int CARD_HOVER   = 0x20FFFFFF;
    private static final int TEXT         = 0xFFFFFFFF;
    private static final int TEXT_DIM     = 0xFF8E8E93;
    private static final int SW_OFF       = 0xFF39393D;

    private static final int[] PRESETS = {
            0x0A84FF, 0x30D158, 0xFF453A, 0xFF9F0A, 0xFFD60A, 0xFF375F, 0xBF5AF2, 0x64D2FF
    };
    private static final int[] SET_PRESETS = {
            0x0A84FF, 0x30D158, 0xFF453A, 0xFF9F0A, 0xFFD60A, 0xFF375F, 0xBF5AF2, 0x64D2FF, 0xFFFFFF
    };

    /** Modules with a "NEW" badge. */
    private static final Set<String> NEW_MODULES = Set.of("Custom Crosshair");

    /** Shown in the "Popular" section under the modules of the current category. */
    private static final List<Class<? extends Module>> POPULAR = List.of(
            KeystrokesModule.class, TargetHudModule.class, HitSoundModule.class, ArmorBreakModule.class);

    // Special rows of the settings panel
    private static final Object ROW_LABEL = new Object();
    private static final Object ROW_CROSS = new Object();
    private static final Object ROW_HUD_BG = new Object();
    private static final Object ROW_HUD_RESET = new Object();

    // Remembered between openings
    private static Category lastCat = Category.VISUAL;
    private static Module lastMod;
    private static String versionLine;

    private record Entry(String header, List<Module> mods) {}

    // State
    private Category selected = lastCat;
    private Module selMod;
    private String query = "";
    private boolean onlyEnabled;

    private boolean wasLeft, wasRight, shiftWasDown;
    private long openedAt, lastFrame;
    private int scrollRow, setScroll;

    private final Map<Module, Float> anim = new HashMap<>();
    // Text layout caches (measuring text every frame is what makes menus slow)
    private final Map<String, String> boldCache = new HashMap<>();
    private final Map<String, String[]> wrapCache = new HashMap<>();

    // Keybind capture
    private KeybindSetting listening;
    private boolean bindArmed;
    private final Map<Object, Float> swAnim = new HashMap<>();
    private float hudSw;

    // Dragging
    private NumberSetting dragNum;
    private int dragNumX, dragNumW;
    private ColorSetting dragCol;
    private int dragCh, dragColX, dragColW;
    private int dragAccent = -1;

    // Search typing (polled through GLFW, like the rest of the input)
    private final boolean[] keyState = new boolean[GLFW.GLFW_KEY_LAST + 1];
    private long backSince, backLast;

    private final CrosshairDesigner designer = new CrosshairDesigner();

    // Geometry of the current frame
    private int px, py, pw, ph, sbw, rw;

    public SecondyMenuScreen() {
        super(Text.literal("_2ndy Client"));
    }

    @Override
    protected void init() {
        wasLeft = Input.mouse(0);
        wasRight = Input.mouse(1);
        shiftWasDown = Input.key(GLFW.GLFW_KEY_RIGHT_SHIFT);
        openedAt = System.currentTimeMillis();
        lastFrame = System.nanoTime();
        hudSw = Theme.hudAccent ? 1f : 0f;
        dragNum = null;
        dragCol = null;
        dragAccent = -1;
        designer.sync(wasLeft, wasRight);
        syncKeys();
        if (selMod == null) selMod = lastMod != null ? lastMod : pickDefault(selected);
    }

    @Override
    public boolean shouldPause() { return false; }

    /** Esc cancels key capture instead of closing the menu. */
    @Override
    public boolean shouldCloseOnEsc() { return listening == null; }

    /** No vanilla background blur (it costs a lot of FPS): just a light dim. */
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float deltaTicks) {
        float o = Ui.ease((System.currentTimeMillis() - openedAt) / 220f);
        ctx.fill(0, 0, this.width, this.height, Ui.alpha(0x88000000, o));
    }

    @Override
    public void close() {
        listening = null;
        lastCat = selected;
        lastMod = selMod;
        Config.save();
        super.close();
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int dir = (int) Math.signum(verticalAmount);
        if (mouseX >= px + pw - rw) {
            setScroll = Math.max(0, setScroll - dir);
        } else {
            scrollRow = Math.max(0, scrollRow - dir);
        }
        return true;
    }

    // ───────────────────────── Helpers ─────────────────────────

    private static boolean hasOptions(Module m) {
        return !m.settings.isEmpty() || m instanceof HudModule;
    }

    private Module pickDefault(Category c) {
        Module first = null;
        for (Module m : ModuleManager.ALL) {
            if (m.category != c) continue;
            if (first == null) first = m;
            if (!m.settings.isEmpty()) return m;
        }
        return first;
    }

    private void select(Module m) {
        if (m != selMod) {
            listening = null;
            selMod = m;
            setScroll = 0;
            dragNum = null;
            dragCol = null;
        }
    }

    private static float step(float cur, float target, float dt) {
        return cur + Math.signum(target - cur) * Math.min(Math.abs(target - cur), dt * 7f);
    }

    private static boolean in(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private int categoryCount(Category c) {
        int n = 0;
        for (Module m : ModuleManager.ALL) if (m.category == c) n++;
        return n;
    }

    /** Fits a bold string (bold glyphs are about 1px wider each). */
    private String fitBold(String s, int maxWidth) {
        if (maxWidth <= 0) return "";
        String key = maxWidth + "|" + s;
        String cached = boldCache.get(key);
        if (cached != null) return cached;
        int end = s.length();
        while (end > 0 && this.textRenderer.getWidth(s.substring(0, end)) + end > maxWidth) end--;
        String res;
        if (end == s.length()) res = s;
        else res = end > 2 ? s.substring(0, end - 1) + ".." : s.substring(0, end);
        if (boldCache.size() > 300) boldCache.clear();
        boldCache.put(key, res);
        return res;
    }

    private void bold(DrawContext ctx, String s, int x, int y, int color) {
        Ui.text(ctx, this.textRenderer, Text.literal(s).formatted(Formatting.BOLD), x, y, color);
    }

    /** Splits text into two lines; the second one may have a different width limit and is cut with "..". */
    private String[] wrap2(String s, int w1, int w2) {
        String key = w1 + "," + w2 + "|" + s;
        String[] cached = wrapCache.get(key);
        if (cached != null) return cached;
        String[] res = wrap2Raw(s, w1, w2);
        if (wrapCache.size() > 300) wrapCache.clear();
        wrapCache.put(key, res);
        return res;
    }

    private String[] wrap2Raw(String s, int w1, int w2) {
        String[] out = { "", "" };
        String[] words = s.split(" ");
        StringBuilder line = new StringBuilder();
        int i = 0;
        for (; i < words.length; i++) {
            String t = line.length() == 0 ? words[i] : line + " " + words[i];
            if (this.textRenderer.getWidth(t) <= w1) line = new StringBuilder(t); else break;
        }
        if (line.length() == 0) {
            out[0] = Ui.fit(this.textRenderer, s, w1);
            return out;
        }
        out[0] = line.toString();
        StringBuilder rest = new StringBuilder();
        for (; i < words.length; i++) {
            if (rest.length() > 0) rest.append(' ');
            rest.append(words[i]);
        }
        out[1] = Ui.fit(this.textRenderer, rest.toString(), w2);
        return out;
    }

    private static String versions() {
        if (versionLine == null) {
            FabricLoader fl = FabricLoader.getInstance();
            String mod = fl.getModContainer("secondy_client")
                    .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("dev");
            String mc = fl.getModContainer("minecraft")
                    .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("");
            versionLine = "v" + mod + (mc.isEmpty() ? "" : " \u00b7 " + mc);
        }
        return versionLine;
    }

    // ───────────────────────── Search typing ─────────────────────────

    private void syncKeys() {
        for (int k = GLFW.GLFW_KEY_A; k <= GLFW.GLFW_KEY_Z; k++) keyState[k] = Input.key(k);
        for (int k = GLFW.GLFW_KEY_0; k <= GLFW.GLFW_KEY_9; k++) keyState[k] = Input.key(k);
        keyState[GLFW.GLFW_KEY_SPACE] = Input.key(GLFW.GLFW_KEY_SPACE);
        keyState[GLFW.GLFW_KEY_MINUS] = Input.key(GLFW.GLFW_KEY_MINUS);
        keyState[GLFW.GLFW_KEY_BACKSPACE] = Input.key(GLFW.GLFW_KEY_BACKSPACE);
    }

    private void pollTyping() {
        if (Input.key(GLFW.GLFW_KEY_LEFT_CONTROL) || Input.key(GLFW.GLFW_KEY_RIGHT_CONTROL)) {
            syncKeys();
            return;
        }
        for (int k = GLFW.GLFW_KEY_A; k <= GLFW.GLFW_KEY_Z; k++) typeKey(k, (char) ('a' + k - GLFW.GLFW_KEY_A));
        for (int k = GLFW.GLFW_KEY_0; k <= GLFW.GLFW_KEY_9; k++) typeKey(k, (char) ('0' + k - GLFW.GLFW_KEY_0));
        typeKey(GLFW.GLFW_KEY_SPACE, ' ');
        typeKey(GLFW.GLFW_KEY_MINUS, '-');

        boolean bs = Input.key(GLFW.GLFW_KEY_BACKSPACE);
        long now = System.currentTimeMillis();
        if (bs) {
            if (!keyState[GLFW.GLFW_KEY_BACKSPACE]) {
                backspace();
                backSince = now;
                backLast = now;
            } else if (now - backSince > 400 && now - backLast > 45) {
                backspace();
                backLast = now;
            }
        }
        keyState[GLFW.GLFW_KEY_BACKSPACE] = bs;
    }

    private void typeKey(int k, char c) {
        boolean d = Input.key(k);
        if (d && !keyState[k] && query.length() < 24) {
            boolean okSpace = c != ' ' || (!query.isEmpty() && !query.endsWith(" "));
            if (okSpace) {
                query += c;
                scrollRow = 0;
            }
        }
        keyState[k] = d;
    }

    private void backspace() {
        if (!query.isEmpty()) {
            query = query.substring(0, query.length() - 1);
            scrollRow = 0;
        }
    }

    /** Captures the next key / mouse button for the setting being rebound. */
    private void pollBind() {
        int hit = -1;
        if (Input.key(GLFW.GLFW_KEY_ESCAPE)) hit = -2;                      // cancel
        else if (Input.key(GLFW.GLFW_KEY_BACKSPACE)) hit = -3;              // reset to default
        else {
            for (int k = GLFW.GLFW_KEY_SPACE; k <= GLFW.GLFW_KEY_LAST; k++) {
                if (k == GLFW.GLFW_KEY_RIGHT_SHIFT) continue;               // menu key
                if (Input.key(k)) { hit = k; break; }
            }
            if (hit < 0) {
                for (int b = 2; b <= 7; b++) {                              // middle + side buttons
                    if (Input.mouse(b)) { hit = Input.MOUSE_BASE + b; break; }
                }
            }
        }
        if (!bindArmed) {                                                   // wait until everything is released first
            if (hit == -1) bindArmed = true;
            return;
        }
        if (hit == -1) return;
        if (hit == -3) listening.reset();
        else if (hit >= 0) listening.set(hit);
        listening = null;
        Config.save();
    }

    // ───────────────────────── Render ─────────────────────────

    @Override
    public void render(DrawContext ctx, int mx, int my, float delta) {
        super.render(ctx, mx, my, delta);

        boolean shift = Input.key(GLFW.GLFW_KEY_RIGHT_SHIFT);
        if (shift && !shiftWasDown && listening == null) { close(); return; }
        shiftWasDown = shift;

        boolean left = Input.mouse(0), right = Input.mouse(1);
        boolean clicked = left && !wasLeft;
        boolean rclicked = right && !wasRight;
        wasLeft = left;
        wasRight = right;

        long nowN = System.nanoTime();
        float dt = Math.min(0.1f, (nowN - lastFrame) / 1_000_000_000f);
        lastFrame = nowN;

        float open = Ui.ease((System.currentTimeMillis() - openedAt) / 220f);

        pw = Math.max(340, Math.min(PW_MAX, this.width - 12));
        ph = Math.max(200, Math.min(PH_MAX, this.height - 12));
        sbw = pw >= 470 ? 96 : 74;
        rw = pw >= 470 ? 184 : (pw >= 400 ? 150 : 128);
        px = (this.width - pw) / 2;
        py = (this.height - ph) / 2 + Math.round((1f - open) * 10f);

        Ui.glow(ctx, px, py, pw, ph, RADIUS, Ui.alpha(0xFF000000, open), 4);
        Ui.round(ctx, px, py, pw, ph, RADIUS, Ui.alpha(PANEL, open));
        Ui.roundOutline(ctx, px, py, pw, ph, RADIUS, Ui.alpha(PANEL_BORDER, open));
        ctx.fill(px + sbw, py + 8, px + sbw + 1, py + ph - 8, Ui.alpha(SEPARATOR, open));
        ctx.fill(px + pw - rw, py + 8, px + pw - rw + 1, py + ph - 8, Ui.alpha(SEPARATOR, open));

        if (listening != null) {
            pollBind();
            syncKeys(); // keys pressed while binding must not end up in the search box
        } else {
            pollTyping();
        }

        renderSidebar(ctx, mx, my, clicked, open);
        renderCenter(ctx, mx, my, clicked, rclicked, left, dt, open);
        renderPanel(ctx, mx, my, left, right, clicked, dt, open);

        if (!left) {
            if (dragNum != null || dragCol != null || dragAccent >= 0) Config.save();
            dragNum = null;
            dragCol = null;
            dragAccent = -1;
        }
    }

    // ───────────────────────── Left: sidebar ─────────────────────────

    private void renderSidebar(DrawContext ctx, int mx, int my, boolean clicked, float open) {
        int acc = Theme.accentColor(0f);

        // Logo + title + version
        Ui.round(ctx, px + PAD, py + PAD, 16, 16, 5, Ui.alpha(acc, open));
        Ui.round(ctx, px + PAD + 3, py + PAD + 3, 6, 6, 2, Ui.alpha(0x55FFFFFF, open));
        int tx = px + PAD + 21, tw = sbw - PAD - 21 - 4;
        bold(ctx, fitBold("_2ndy Client", tw), tx, py + PAD, Ui.alpha(Theme.color(0f), open));
        Ui.text(ctx, this.textRenderer, Ui.fit(this.textRenderer, versions(), tw), tx, py + PAD + 10,
                Ui.alpha(0xFF6C6C70, open));

        // Categories
        Category[] cats = Category.values();
        int y = py + 44;
        for (Category c : cats) {
            int ix = px + 6, iw = sbw - 12;
            boolean sel = c == selected;
            boolean hv = in(mx, my, ix, y, iw, 18);
            if (sel) {
                Ui.glow(ctx, ix, y, iw, 18, 6, Ui.alpha(acc, 0.55f * open), 3);
                Ui.round(ctx, ix, y, iw, 18, 6, Ui.alpha(acc, 0.95f * open));
            } else if (hv) {
                Ui.round(ctx, ix, y, iw, 18, 6, Ui.alpha(0x14FFFFFF, open));
            }
            Ui.round(ctx, ix + 5, y + 4, 10, 10, 3, Ui.alpha(sel ? 0x66FFFFFF : 0x22FFFFFF, open));

            String cnt = c == Category.COLOR ? "-" : String.valueOf(categoryCount(c));
            int cw = this.textRenderer.getWidth(cnt) + 8;
            int cxp = ix + iw - 5 - cw;
            Ui.round(ctx, cxp, y + 4, cw, 10, 5, Ui.alpha(sel ? 0x33FFFFFF : 0x1AFFFFFF, open));
            Ui.text(ctx, this.textRenderer, cnt, cxp + 4, y + 5, Ui.alpha(sel ? TEXT : TEXT_DIM, open));

            Ui.text(ctx, this.textRenderer, Ui.fit(this.textRenderer, c.label, cxp - (ix + 20) - 3),
                    ix + 20, y + 5, Ui.alpha(sel ? TEXT : 0xFFA0A0A8, open));

            if (clicked && hv && !sel) {
                selected = c;
                scrollRow = 0;
                if (c != Category.COLOR && (selMod == null || selMod.category != c)) select(pickDefault(c));
            }
            y += 22;
        }

        // Key hints at the bottom
        String[][] hints = { { "R Shift", "close" }, { "T", "move & resize HUD" },
                { Input.keyName(ModuleManager.get(ZoomModule.class).key.get()), "zoom" } };
        int hy = py + ph - PAD - hints.length * 14 + 3;
        if (y + 2 < hy) {
            for (String[] h : hints) {
                int kw = this.textRenderer.getWidth(h[0]) + 8;
                Ui.round(ctx, px + PAD, hy, kw, 11, 3, Ui.alpha(0x2EFFFFFF, open));
                Ui.text(ctx, this.textRenderer, h[0], px + PAD + 4, hy + 2, Ui.alpha(0xFFD0D0D6, open));
                Ui.text(ctx, this.textRenderer, Ui.fit(this.textRenderer, h[1], sbw - PAD - kw - 4 - 4),
                        px + PAD + kw + 4, hy + 2, Ui.alpha(0xFF6C6C70, open));
                hy += 14;
            }
        }
    }

    // ───────────────────────── Middle: search + cards ─────────────────────────

    private boolean matches(Module m, String q) {
        return m.name.toLowerCase(Locale.ROOT).contains(q) || m.description.toLowerCase(Locale.ROOT).contains(q);
    }

    private void addSection(List<Entry> out, String header, List<Module> mods, int cols) {
        if (mods.isEmpty()) return;
        out.add(new Entry(header, null));
        for (int i = 0; i < mods.size(); i += cols) {
            out.add(new Entry(null, new ArrayList<>(mods.subList(i, Math.min(mods.size(), i + cols)))));
        }
    }

    private List<Entry> buildEntries(int cols) {
        List<Entry> out = new ArrayList<>();
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (!q.isEmpty()) {
            List<Module> found = new ArrayList<>();
            for (Module m : ModuleManager.ALL) {
                if (matches(m, q) && (!onlyEnabled || m.isEnabled())) found.add(m);
            }
            addSection(out, "RESULTS", found, cols);
            return out;
        }
        List<Module> own = new ArrayList<>();
        for (Module m : ModuleManager.ALL) {
            if (m.category == selected && (!onlyEnabled || m.isEnabled())) own.add(m);
        }
        addSection(out, selected.label.toUpperCase(Locale.ROOT), own, cols);

        List<Module> pop = new ArrayList<>();
        for (Class<? extends Module> cls : POPULAR) {
            Module m = ModuleManager.get(cls);
            if (m.category != selected && (!onlyEnabled || m.isEnabled())) pop.add(m);
        }
        addSection(out, "POPULAR", pop, cols);
        return out;
    }

    private void renderCenter(DrawContext ctx, int mx, int my, boolean clicked, boolean rclicked, boolean down,
                              float dt, float open) {
        int x0 = px + sbw + PAD, x1 = px + pw - rw - PAD, cw = x1 - x0;
        int y0 = py + PAD;
        int acc = Theme.accentColor(0f);

        // Filter chips (All / Enabled)
        String[] chips = { "All", "Enabled" };
        int[] chipW = new int[2];
        int chipsTotal = 0;
        for (int i = 0; i < 2; i++) {
            chipW[i] = this.textRenderer.getWidth(chips[i]) + 14;
            chipsTotal += chipW[i] + 4;
        }
        int searchW = cw - chipsTotal;

        // Search field
        Ui.round(ctx, x0, y0, searchW, 18, 6, Ui.alpha(0x1FFFFFFF, open));
        Ui.roundOutline(ctx, x0, y0, searchW, 18, 6, Ui.alpha(acc, (query.isEmpty() ? 0.25f : 0.6f) * open));
        if (query.isEmpty()) {
            Ui.text(ctx, this.textRenderer, Ui.fit(this.textRenderer, "Type to search modules...", searchW - 14),
                    x0 + 7, y0 + 5, Ui.alpha(0xFF6C6C70, open));
        } else {
            String shown = query;
            while (shown.length() > 1 && this.textRenderer.getWidth(shown) > searchW - 20) shown = shown.substring(1);
            Ui.text(ctx, this.textRenderer, shown, x0 + 7, y0 + 5, Ui.alpha(TEXT, open));
            if ((System.currentTimeMillis() / 500) % 2 == 0) {
                int cxp = x0 + 7 + this.textRenderer.getWidth(shown) + 1;
                ctx.fill(cxp, y0 + 4, cxp + 1, y0 + 14, Ui.alpha(TEXT, open));
            }
        }

        int cxp = x0 + searchW + 4;
        for (int i = 0; i < 2; i++) {
            boolean active = (i == 1) == onlyEnabled;
            boolean hv = in(mx, my, cxp, y0, chipW[i], 18);
            Ui.round(ctx, cxp, y0, chipW[i], 18, 6,
                    active ? Ui.alpha(acc, 0.22f * open) : Ui.alpha(hv ? 0x2EFFFFFF : 0x1FFFFFFF, open));
            Ui.text(ctx, this.textRenderer, chips[i], cxp + 7, y0 + 5, Ui.alpha(active ? acc : TEXT, open));
            if (clicked && hv) {
                onlyEnabled = i == 1;
                scrollRow = 0;
            }
            cxp += chipW[i] + 4;
        }

        int top = y0 + 18 + 8;
        int bottom = py + ph - PAD + 2;

        if (selected == Category.COLOR && query.trim().isEmpty()) {
            renderAccent(ctx, mx, my, clicked, down, dt, open, x0, top, cw);
            return;
        }

        int cols = cw >= 240 ? 2 : 1;
        int cardW = (cw - CARD_GAP * (cols - 1)) / cols;
        List<Entry> entries = buildEntries(cols);

        if (entries.isEmpty()) {
            String msg = onlyEnabled ? "No enabled modules here" : "No modules found";
            Ui.text(ctx, this.textRenderer, msg, x0 + (cw - this.textRenderer.getWidth(msg)) / 2, top + 30,
                    Ui.alpha(TEXT_DIM, open));
            return;
        }

        int avail = bottom - top;
        int maxScroll = 0, sum = 0;
        for (int i = entries.size() - 1; i >= 0; i--) {
            sum += entryH(entries.get(i));
            if (sum > avail) { maxScroll = i + 1; break; }
        }
        scrollRow = Math.max(0, Math.min(maxScroll, scrollRow));

        int y = top, shownH = 0, end = scrollRow;
        while (end < entries.size() && y + entryH(entries.get(end)) <= bottom + CARD_GAP) {
            Entry e = entries.get(end);
            if (e.header() != null) {
                Ui.text(ctx, this.textRenderer, e.header(), x0 + 2, y + 2, Ui.alpha(0xFF8E8E93, open));
            } else {
                for (int i = 0; i < e.mods().size(); i++) {
                    drawCard(ctx, e.mods().get(i), x0 + i * (cardW + CARD_GAP), y, cardW, mx, my, clicked, rclicked,
                            dt, open);
                }
            }
            y += entryH(e);
            shownH = y - top;
            end++;
        }

        if (maxScroll > 0) {
            int barH = Math.max(12, avail * (end - scrollRow) / entries.size());
            int barY = top + (avail - barH) * scrollRow / maxScroll;
            Ui.round(ctx, x1 + 5, barY, 2, barH, 1, Ui.alpha(0x55FFFFFF, open));
        }
    }

    private static int entryH(Entry e) {
        return e.header() != null ? HDR_H : CARD_H + CARD_GAP;
    }

    private void drawCard(DrawContext ctx, Module m, int x, int y, int w, int mx, int my, boolean clicked,
                          boolean rclicked, float dt, float open) {
        int acc = Theme.accentColor(0f);
        boolean on = m.isEnabled();
        boolean sel = m == selMod;
        boolean hover = in(mx, my, x, y, w, CARD_H);

        // Card background: tinted with the accent color when enabled
        int fill = on ? Ui.alpha(acc, hover ? 0.24f : 0.16f) : (hover ? CARD_HOVER : CARD);
        Ui.round(ctx, x, y, w, CARD_H, 7, Ui.alpha(fill, open));
        Ui.roundOutline(ctx, x, y, w, CARD_H, 7,
                Ui.alpha(on ? Ui.alpha(acc, 0.55f) : 0x1AFFFFFF, open));
        if (sel) {
            Ui.roundOutline(ctx, x, y, w, CARD_H, 7, Ui.alpha(acc, open));
            Ui.glow(ctx, x, y, w, CARD_H, 7, Ui.alpha(acc, open), 3);
        }

        // Switch (top right)
        int tgw = 24, tgh = 12, tgx = x + w - 7 - tgw, tgy = y + 5;
        float target = on ? 1f : 0f;
        float cur = step(anim.getOrDefault(m, target), target, dt);
        anim.put(m, cur);
        drawSwitch(ctx, tgx, tgy, tgw, tgh, Ui.ease(cur), open);

        // Title (+ NEW badge)
        boolean isNew = NEW_MODULES.contains(m.name);
        int badgeW = isNew ? this.textRenderer.getWidth("NEW") + 8 : 0;
        int titleMax = tgx - 4 - (x + 7) - (isNew ? badgeW + 4 : 0);
        String title = fitBold(m.name, titleMax);
        bold(ctx, title, x + 7, y + 5, Ui.alpha(TEXT, open));
        if (isNew) {
            int bx = x + 7 + this.textRenderer.getWidth(title) + title.length() + 4;
            Ui.round(ctx, bx, y + 5, badgeW, 10, 3, Ui.alpha(0x33FF9F0A, open));
            Ui.text(ctx, this.textRenderer, "NEW", bx + 4, y + 6, Ui.alpha(0xFFFF9F0A, open));
        }

        // Settings button (bottom right)
        boolean hasBtn = hasOptions(m);
        int btnW = this.textRenderer.getWidth("Settings") + 10;
        int btnX = x + w - 6 - btnW, btnY = y + CARD_H - 5 - 11;
        boolean btnHit = hasBtn && in(mx, my, btnX, btnY, btnW, 11);

        // Description (2 lines; the second one stays clear of the button)
        int dw = w - 14;
        String[] lines = wrap2(m.description, dw, hasBtn ? dw - btnW - 6 : dw);
        Ui.text(ctx, this.textRenderer, lines[0], x + 7, y + 17, Ui.alpha(TEXT_DIM, open));
        if (!lines[1].isEmpty()) Ui.text(ctx, this.textRenderer, lines[1], x + 7, y + 26, Ui.alpha(TEXT_DIM, open));

        if (hasBtn) {
            Ui.round(ctx, btnX, btnY, btnW, 11, 4, Ui.alpha(btnHit ? 0x4AFFFFFF : 0x2EFFFFFF, open));
            Ui.text(ctx, this.textRenderer, "Settings", btnX + 5, btnY + 2, Ui.alpha(TEXT, open));
        }

        if (clicked && hover) {
            if (btnHit) {
                select(m);
            } else {
                m.toggle();
                select(m);
                Config.save();
            }
        } else if (rclicked && hover) {
            select(m);
        }
    }

    // ───────────────────────── Middle: accent color tab ─────────────────────────

    private void renderAccent(DrawContext ctx, int mx, int my, boolean clicked, boolean down, float dt, float open,
                              int x, int y, int w) {
        int cardH = 150;
        Ui.round(ctx, x, y, w, cardH, 7, Ui.alpha(CARD, open));

        int rgb = Theme.accent & 0xFFFFFF;
        int[] ch = { (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255 };

        Ui.round(ctx, x + 10, y + 8, 14, 14, 7, Ui.alpha(Theme.accent, open));
        bold(ctx, "Accent color", x + 30, y + 11, Ui.alpha(TEXT, open));
        String hex = String.format("#%06X", rgb);
        Ui.text(ctx, this.textRenderer, hex, x + w - 10 - this.textRenderer.getWidth(hex), y + 11,
                Ui.alpha(TEXT_DIM, open));

        int swD = 14, swY = y + 32;
        int stp = Math.min(26, (w - 20 - swD) / (PRESETS.length - 1));
        for (int i = 0; i < PRESETS.length; i++) {
            int sxp = x + 10 + i * stp;
            Ui.round(ctx, sxp, swY, swD, swD, swD / 2, Ui.alpha(0xFF000000 | PRESETS[i], open));
            if (PRESETS[i] == rgb) {
                Ui.roundOutline(ctx, sxp - 3, swY - 3, swD + 6, swD + 6, (swD + 6) / 2, Ui.alpha(0xFFFFFFFF, open));
            }
            if (clicked && in(mx, my, sxp - 2, swY - 2, swD + 4, swD + 4)) {
                Theme.accent = 0xFF000000 | PRESETS[i];
                Config.save();
            }
        }

        String[] names = { "R", "G", "B" };
        int[] labelCol = { 0xFFFF453A, 0xFF30D158, 0xFF0A84FF };
        int trackX = x + 28, trackW = w - 28 - 40;
        for (int i = 0; i < 3; i++) {
            int sy = y + 58 + i * 22;
            Ui.text(ctx, this.textRenderer, names[i], x + 12, sy + 5, Ui.alpha(labelCol[i], open));
            drawRgbTrack(ctx, ch, i, trackX, sy + 6, trackW, open);
            int kx = trackX + Math.round(ch[i] / 255f * (trackW - 1));
            Ui.round(ctx, kx - 5, sy + 3, 10, 10, 5, Ui.alpha(0xFFFFFFFF, open));
            Ui.roundOutline(ctx, kx - 5, sy + 3, 10, 10, 5, Ui.alpha(0x55000000, open));
            String val = String.valueOf(ch[i]);
            Ui.text(ctx, this.textRenderer, val, x + w - 10 - this.textRenderer.getWidth(val), sy + 5,
                    Ui.alpha(TEXT_DIM, open));
            if (clicked && in(mx, my, trackX - 6, sy, trackW + 12, 18)) dragAccent = i;
        }
        if (down && dragAccent >= 0) {
            int v = Math.round((mx - trackX) / (float) (trackW - 1) * 255f);
            ch[dragAccent] = Math.max(0, Math.min(255, v));
            Theme.accent = 0xFF000000 | (ch[0] << 16) | (ch[1] << 8) | ch[2];
        }

        int ry = y + 58 + 3 * 22 + 6;
        ctx.fill(x + 10, ry - 3, x + w - 10, ry - 2, Ui.alpha(SEPARATOR, open));
        boolean hover = in(mx, my, x, ry - 2, w, 22);
        Ui.text(ctx, this.textRenderer, Ui.fit(this.textRenderer, "Tint HUD text with this color", w - 56),
                x + 10, ry + 6, Ui.alpha(TEXT, open));
        hudSw = step(hudSw, Theme.hudAccent ? 1f : 0f, dt);
        drawSwitch(ctx, x + w - 24 - 10, ry + 4, 24, 12, Ui.ease(hudSw), open);
        if (clicked && hover) {
            Theme.hudAccent = !Theme.hudAccent;
            Config.save();
        }
    }

    /** Gradient track drawn in 4px steps (about 25 fills instead of one per pixel column). */
    private void drawRgbTrack(DrawContext ctx, int[] ch, int idx, int x, int y, int w, float open) {
        final int seg = 4;
        for (int c = 0; c < w; c += seg) {
            int x1 = Math.min(w, c + seg);
            int mid = Math.min(w - 1, (c + x1) / 2);
            int col = trackColor(ch, idx, mid * 255 / Math.max(1, w - 1), open);
            int x0 = c;
            if (c == 0) {
                ctx.fill(x, y + 1, x + 1, y + 4, col);
                x0 = 1;
            }
            if (x1 == w) {
                ctx.fill(x + w - 1, y + 1, x + w, y + 4, trackColor(ch, idx, 255, open));
                x1 = w - 1;
            }
            if (x1 > x0) ctx.fill(x + x0, y, x + x1, y + 5, col);
        }
    }

    private static int trackColor(int[] ch, int idx, int v, float open) {
        int r = idx == 0 ? v : ch[0], g = idx == 1 ? v : ch[1], b = idx == 2 ? v : ch[2];
        return Ui.alpha(0xFF000000 | (r << 16) | (g << 8) | b, open);
    }

    // ───────────────────────── Right: settings panel ─────────────────────────

    private List<Object> panelRows(Module m) {
        List<Object> rows = new ArrayList<>();
        if (m instanceof CrosshairModule) rows.add(ROW_CROSS);
        if (hasOptions(m)) rows.add(ROW_LABEL);
        if (m instanceof HudModule) {
            rows.add(ROW_HUD_BG);
            rows.add(ROW_HUD_RESET);
        }
        for (Setting s : m.settings) {
            if (m instanceof CrosshairModule && s instanceof PixelGridSetting) continue; // drawn inline above
            rows.add(s);
        }
        return rows;
    }

    private int rowH(Object o, int cell) {
        if (o == ROW_LABEL) return 14;
        if (o == ROW_CROSS) return CrosshairDesigner.height(cell) + 6;
        if (o == ROW_HUD_BG || o == ROW_HUD_RESET) return 22;
        if (o instanceof ColorSetting) return 116;
        if (o instanceof NumberSetting) return 28;
        return 22;
    }

    private void renderPanel(DrawContext ctx, int mx, int my, boolean left, boolean right, boolean clicked,
                             float dt, float open) {
        int x0 = px + pw - rw + PAD, iw = rw - PAD * 2, top = py + PAD;

        if (selMod == null) {
            String msg = "Select a module";
            Ui.text(ctx, this.textRenderer, msg, x0 + (iw - this.textRenderer.getWidth(msg)) / 2, py + ph / 2 - 4,
                    Ui.alpha(TEXT_DIM, open));
            return;
        }
        Module m = selMod;

        // Header: name, switch, description
        int tgw = 24, tgh = 12, tgx = x0 + iw - tgw, tgy = top;
        bold(ctx, fitBold(m.name, iw - tgw - 6), x0, top + 1, Ui.alpha(Theme.color(0f), open));
        float target = m.isEnabled() ? 1f : 0f;
        float cur = step(anim.getOrDefault(m, target), target, dt);
        anim.put(m, cur);
        drawSwitch(ctx, tgx, tgy, tgw, tgh, Ui.ease(cur), open);
        if (clicked && in(mx, my, tgx - 3, tgy - 2, tgw + 6, tgh + 4)) {
            m.toggle();
            Config.save();
        }
        String[] dl = wrap2(m.description, iw, iw);
        Ui.text(ctx, this.textRenderer, dl[0], x0, top + 15, Ui.alpha(TEXT_DIM, open));
        if (!dl[1].isEmpty()) Ui.text(ctx, this.textRenderer, dl[1], x0, top + 24, Ui.alpha(TEXT_DIM, open));

        int rowsTop = top + 40;
        int bottom = py + ph - PAD;
        int avail = bottom - rowsTop;

        List<Object> rows = panelRows(m);
        if (rows.isEmpty()) {
            Ui.text(ctx, this.textRenderer, "No extra settings", x0, rowsTop + 4, Ui.alpha(0xFF6C6C70, open));
            return;
        }

        int cell = Math.max(5, Math.min(Math.min(11, iw / PixelGridSetting.N), (avail - 14 - 92) / PixelGridSetting.N));

        int maxScroll = 0, sum = 0;
        for (int i = rows.size() - 1; i >= 0; i--) {
            sum += rowH(rows.get(i), cell);
            if (sum > avail) { maxScroll = i + 1; break; }
        }
        setScroll = Math.max(0, Math.min(maxScroll, setScroll));

        int used = 0, end = setScroll;
        while (end < rows.size() && used + rowH(rows.get(end), cell) <= avail) {
            used += rowH(rows.get(end), cell);
            end++;
        }
        if (end == setScroll && end < rows.size()) end++; // always show at least one row

        int ry = rowsTop;
        for (int i = setScroll; i < end; i++) {
            Object row = rows.get(i);
            int h = rowH(row, cell);
            drawRow(ctx, m, row, mx, my, left, right, clicked, dt, open, x0, ry, iw, h, cell);
            ry += h;
        }

        if (maxScroll > 0) {
            int barH = Math.max(12, avail * (end - setScroll) / rows.size());
            int barY = rowsTop + (avail - barH) * setScroll / maxScroll;
            Ui.round(ctx, px + pw - 6, barY, 2, barH, 1, Ui.alpha(0x55FFFFFF, open));
        }

        // Slider dragging
        if (left) {
            if (dragNum != null) dragNum.setFraction((mx - dragNumX) / (float) dragNumW);
            if (dragCol != null) {
                int v = Math.round((mx - dragColX) / (float) (dragColW - 1) * 255f);
                v = Math.max(0, Math.min(255, v));
                int rgb = dragCol.rgb();
                int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
                if (dragCh == 0) r = v; else if (dragCh == 1) g = v; else b = v;
                dragCol.setRgb((r << 16) | (g << 8) | b);
            }
        }
    }

    private void drawRow(DrawContext ctx, Module m, Object row, int mx, int my, boolean left, boolean right,
                         boolean clicked, float dt, float open, int x, int y, int w, int h, int cell) {
        if (row == ROW_LABEL) {
            Ui.text(ctx, this.textRenderer, "OPTIONS", x, y + 3, Ui.alpha(0xFF8E8E93, open));
        } else if (row == ROW_CROSS) {
            CrosshairModule cm = (CrosshairModule) m;
            designer.render(ctx, this.textRenderer, cm.design(), mx, my, left, right, clicked, open, x, y, w, cell);
        } else if (row == ROW_HUD_BG) {
            HudModule hm = (HudModule) m;
            drawBoolRow(ctx, "Background", hm.showBackground, hm, mx, my, clicked, dt, open, x, y, w, h);
            if (clicked && in(mx, my, x, y, w, h)) {
                hm.showBackground = !hm.showBackground;
                Config.save();
            }
        } else if (row == ROW_HUD_RESET) {
            HudModule hm = (HudModule) m;
            boolean hv = in(mx, my, x, y, w, h);
            Ui.round(ctx, x, y + 3, w, 16, 6, Ui.alpha(hv ? 0x4AFFFFFF : 0x2EFFFFFF, open));
            String t = Ui.fit(this.textRenderer, "Reset size (now " + Math.round(hm.scale * 100) + "%)", w - 8);
            Ui.text(ctx, this.textRenderer, t, x + (w - this.textRenderer.getWidth(t)) / 2, y + 7,
                    Ui.alpha(TEXT, open));
            if (clicked && hv) {
                hm.scale = 1f;
                Config.save();
            }
        } else if (row instanceof Setting s) {
            drawSetting(ctx, s, mx, my, clicked, dt, open, x, y, w, h);
        }
    }

    /** Label on the left, switch on the right. The click itself is handled by the caller. */
    private void drawBoolRow(DrawContext ctx, String label, boolean value, Object key, int mx, int my,
                             boolean clicked, float dt, float open, int x, int y, int w, int h) {
        Ui.text(ctx, this.textRenderer, Ui.fit(this.textRenderer, label, w - 34), x, y + 7, Ui.alpha(TEXT, open));
        float t = value ? 1f : 0f;
        float k = step(swAnim.getOrDefault(key, t), t, dt);
        swAnim.put(key, k);
        drawSwitch(ctx, x + w - 24, y + 5, 24, 12, Ui.ease(k), open);
    }

    private void drawSetting(DrawContext ctx, Setting st, int mx, int my, boolean clicked, float dt, float open,
                             int x, int y, int w, int h) {
        int acc = Theme.accentColor(0f);
        if (st instanceof BoolSetting b) {
            drawBoolRow(ctx, b.name, b.get(), b, mx, my, clicked, dt, open, x, y, w, h);
            if (clicked && in(mx, my, x, y, w, h)) {
                b.toggle();
                Config.save();
            }
        } else if (st instanceof NumberSetting n) {
            String val = n.display();
            int vw = this.textRenderer.getWidth(val);
            Ui.text(ctx, this.textRenderer, Ui.fit(this.textRenderer, n.name, w - vw - 8), x, y + 3,
                    Ui.alpha(TEXT, open));
            Ui.text(ctx, this.textRenderer, val, x + w - vw, y + 3, Ui.alpha(TEXT_DIM, open));
            int trackY = y + 17, trackW = w - 4, trackX = x + 2;
            int fw = Math.round(n.fraction() * trackW);
            Ui.round(ctx, trackX, trackY, trackW, 4, 2, Ui.alpha(0x33FFFFFF, open));
            Ui.round(ctx, trackX, trackY, Math.max(3, fw), 4, 2, Ui.alpha(acc, open));
            Ui.round(ctx, trackX + fw - 4, trackY - 3, 9, 9, 4, Ui.alpha(0xFFFFFFFF, open));
            Ui.roundOutline(ctx, trackX + fw - 4, trackY - 3, 9, 9, 4, Ui.alpha(0x55000000, open));
            if (clicked && in(mx, my, trackX - 5, y + 11, trackW + 10, 16)) {
                dragNum = n;
                dragNumX = trackX;
                dragNumW = trackW;
            }
        } else if (st instanceof KeybindSetting kb) {
            boolean cap = listening == kb;
            drawPillRow(ctx, kb.name, cap ? "Press a key..." : kb.display(), mx, my, open, x, y, w, h);
            if (cap) {
                int pw2 = this.textRenderer.getWidth("Press a key...") + 14;
                Ui.roundOutline(ctx, x + w - pw2, y + 3, pw2, 16, 6, Ui.alpha(acc, open));
            }
            if (clicked && in(mx, my, x, y, w, h)) {
                listening = cap ? null : kb;
                bindArmed = false;
            }
        } else if (st instanceof CycleSetting cy) {
            drawPillRow(ctx, cy.name, cy.display(), mx, my, open, x, y, w, h);
            if (clicked && in(mx, my, x, y, w, h)) {
                cy.next();
                Config.save();
            }
        } else if (st instanceof ModeSetting md) {
            drawPillRow(ctx, md.name, md.get(), mx, my, open, x, y, w, h);
            if (clicked && in(mx, my, x, y, w, h)) {
                md.next();
                Config.save();
            }
        } else if (st instanceof FileSetting fs) {
            drawPillRow(ctx, fs.name, fs.display(), mx, my, open, x, y, w, h);
            if (clicked && in(mx, my, x, y, w, h)) {
                fs.next();
                Config.save();
            }
        } else if (st instanceof ActionSetting a) {
            boolean hv = in(mx, my, x, y, w, h);
            Ui.round(ctx, x, y + 3, w, 16, 6, Ui.alpha(Ui.alpha(acc, hv ? 0.55f : 0.35f), open));
            String an = Ui.fit(this.textRenderer, a.name, w - 10);
            Ui.text(ctx, this.textRenderer, an, x + (w - this.textRenderer.getWidth(an)) / 2, y + 7,
                    Ui.alpha(TEXT, open));
            if (clicked && hv) {
                a.run();
                Config.save();
            }
        } else if (st instanceof ColorSetting c) {
            drawColorSetting(ctx, c, mx, my, clicked, dt, open, x, y, w);
        }
    }

    private void drawPillRow(DrawContext ctx, String label, String value, int mx, int my, float open,
                             int x, int y, int w, int h) {
        String val = Ui.fit(this.textRenderer, value, w * 55 / 100);
        int pillW = this.textRenderer.getWidth(val) + 14;
        int pxx = x + w - pillW;
        Ui.text(ctx, this.textRenderer, Ui.fit(this.textRenderer, label, pxx - x - 6), x, y + 7,
                Ui.alpha(TEXT, open));
        boolean hv = in(mx, my, x, y, w, h);
        Ui.round(ctx, pxx, y + 3, pillW, 16, 6, Ui.alpha(hv ? 0x4AFFFFFF : 0x2EFFFFFF, open));
        Ui.text(ctx, this.textRenderer, val, pxx + 7, y + 7, Ui.alpha(TEXT, open));
    }

    private void drawColorSetting(DrawContext ctx, ColorSetting c, int mx, int my, boolean clicked, float dt,
                                  float open, int x, int y, int w) {
        int rgb = c.rgb();
        int[] ch = { (rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255 };

        String hex = String.format("#%06X", rgb);
        int hexW = this.textRenderer.getWidth(hex);
        Ui.text(ctx, this.textRenderer, Ui.fit(this.textRenderer, c.name, w - 12 - 6 - hexW - 8), x, y + 3,
                Ui.alpha(TEXT, open));
        Ui.round(ctx, x + w - 12, y + 1, 12, 12, 6, Ui.alpha(c.get(), open));
        Ui.text(ctx, this.textRenderer, hex, x + w - 12 - 6 - hexW, y + 3, Ui.alpha(TEXT_DIM, open));

        // Preset colors
        int swY = y + 20, swD = 12;
        int stp = Math.min(20, (w - swD) / (SET_PRESETS.length - 1));
        for (int i = 0; i < SET_PRESETS.length; i++) {
            int sxp = x + i * stp;
            Ui.round(ctx, sxp, swY, swD, swD, swD / 2, Ui.alpha(0xFF000000 | SET_PRESETS[i], open));
            if (SET_PRESETS[i] == rgb) {
                Ui.roundOutline(ctx, sxp - 2, swY - 2, swD + 4, swD + 4, (swD + 4) / 2, Ui.alpha(0xFFFFFFFF, open));
            }
            if (clicked && in(mx, my, sxp - 1, swY - 1, swD + 2, swD + 2)) {
                c.setRgb(SET_PRESETS[i]);
                Config.save();
            }
        }

        // R, G, B sliders
        String[] names = { "R", "G", "B" };
        int[] labelCol = { 0xFFFF453A, 0xFF30D158, 0xFF0A84FF };
        int trackX = x + 12, trackW = w - 12 - 22;
        for (int i = 0; i < 3; i++) {
            int sy = y + 38 + i * 16;
            Ui.text(ctx, this.textRenderer, names[i], x, sy + 2, Ui.alpha(labelCol[i], open));
            drawRgbTrack(ctx, ch, i, trackX, sy + 3, trackW, open);
            int kx = trackX + Math.round(ch[i] / 255f * (trackW - 1));
            Ui.round(ctx, kx - 4, sy + 1, 9, 9, 4, Ui.alpha(0xFFFFFFFF, open));
            Ui.roundOutline(ctx, kx - 4, sy + 1, 9, 9, 4, Ui.alpha(0x55000000, open));
            String val = String.valueOf(ch[i]);
            Ui.text(ctx, this.textRenderer, val, x + w - this.textRenderer.getWidth(val), sy + 2,
                    Ui.alpha(TEXT_DIM, open));
            if (clicked && in(mx, my, trackX - 5, sy, trackW + 10, 13)) {
                dragCol = c;
                dragCh = i;
                dragColX = trackX;
                dragColW = trackW;
            }
        }

        // Rainbow
        int rowY = y + 90;
        Ui.text(ctx, this.textRenderer, "Rainbow", x, rowY + 5, Ui.alpha(TEXT, open));
        float t = c.rainbow ? 1f : 0f;
        float k = step(swAnim.getOrDefault(c, t), t, dt);
        swAnim.put(c, k);
        drawSwitch(ctx, x + w - 24, rowY + 3, 24, 12, Ui.ease(k), open);
        if (clicked && in(mx, my, x, rowY, w, 20)) {
            c.rainbow = !c.rainbow;
            Config.save();
        }
    }

    // ───────────────────────── Common ─────────────────────────

    private void drawSwitch(DrawContext ctx, int x, int y, int w, int h, float k, float open) {
        Ui.round(ctx, x, y, w, h, h / 2, Ui.alpha(Ui.lerp(SW_OFF, Theme.accentColor(0f), k), open));
        int kd = h - 4;
        int kx = x + 2 + Math.round(k * (w - kd - 4));
        Ui.round(ctx, kx, y + 2, kd, kd, kd / 2, Ui.alpha(0xFFFFFFFF, open));
    }
}
