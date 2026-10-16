package com.yinlin.keys;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;

import java.io.File;
import java.util.ArrayList;

/**
 * Keyboard digambar langsung di satu View (tanpa XML, tanpa banyak View).
 * Ini yang bikin ringan di HP kentang.
 */
public class KeyboardView extends View {

    public interface Listener {
        void onText(String s);
        void onDelete();
        void onEnter();
        void onCursor(int dir);
        void onToolbar(int i); // 1 tempel, 2 salin, 3 hapus semua, 4 batal (undo)
        void onSuggestion(String s);
    }

    public static final char CLIP = '\u0001';
    public static final int M_LET = 0, M_SYM = 1, M_EMO = 2;
    static final int T_CHAR = 0, T_SHIFT = 1, T_DEL = 2, T_SYM = 3, T_ENTER = 4, T_SPACE = 5, T_ABC = 6, T_TAB = 7, T_NONE = 8;
    static final int[] EMO_IDS = {R.string.emo1, R.string.emo2, R.string.emo3, R.string.emo4, R.string.emo5, R.string.emo6};

    static final class Key {
        String label, alt, u;
        int type, tab;
        float w;
        final RectF r = new RectF();

        Key(String l, String a, int t, float w) {
            label = l;
            alt = a;
            type = t;
            this.w = w;
        }
    }

    private final Prefs P;
    private Listener L;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final AudioManager am;
    private final Vibrator vib;
    private final float dens;

    private final Paint pKey = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pHint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBar = new Paint();
    private final Paint pEmoji = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pLine = new Paint();
    private final TextPaint pSug = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final RectF tmp = new RectF();
    private final Paint pBg = new Paint(Paint.FILTER_BITMAP_FLAG);

    private final ArrayList<ArrayList<Key>> rows = new ArrayList<>();
    private final ArrayList<ArrayList<String>> emo = new ArrayList<>();
    private ArrayList<String> recCache;
    private final String[] sugs = new String[3];
    private final String[] sugCache = new String[3];
    private final String[] sugTextCache = new String[3];
    private final boolean[] sugClipCache = new boolean[3];
    private final Drawable[] barIcs = new Drawable[5];
    private final String[] barLb = {"Emoji", "Tempel", "Salin", "Hapus", "Batal"};

    private int mode = M_LET, tab = 1, scrollY = 0, gridPid = -1;
    private boolean shift, caps, repeating;
    private long lastShift;
    private boolean upDirty = true;

    private int cBg, cKey, cKey2, cText, cAcc, cAccT, ka = 255;
    private int keyH, gap, tbH, sgH, nRows, padB, totalH, kbW, kbX0, gridTop, gridH;
    private float rad;
    private boolean fPopup, fHint, fHaptic, fSound;
    private int fLong, fVib;

    private Bitmap bgBmp;
    private String bgSig;

    private Drawable icShift, icShiftOn, icCaps, icBksp, icEnter, icSmile, icClip, icCopy, icTrash;
    private Drawable icClock, icThumb, icHeart, icPaw, icCup, icBulb;

    private final Key[] pk = new Key[12];
    private final int[] zone = new int[12];
    private final float[] sx = new float[12];
    private final boolean[] swiped = new boolean[12];
    private float gridDownY;
    private int gridStartScroll;
    private boolean gridMoved;
    private Key longKey, popKey;
    private int longPid;
    private String popText;

    public KeyboardView(Context c, Prefs p) {
        super(c);
        P = p;
        am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        vib = (Vibrator) c.getSystemService(Context.VIBRATOR_SERVICE);
        dens = c.getResources().getDisplayMetrics().density;
        pText.setTextAlign(Paint.Align.CENTER);
        pHint.setTextAlign(Paint.Align.RIGHT);
        pLabel.setTextAlign(Paint.Align.LEFT);
        pEmoji.setTextAlign(Paint.Align.CENTER);
        pSug.setTextAlign(Paint.Align.CENTER);
        pLine.setStrokeWidth(1f);
        icShift = ic(R.drawable.ic_shift);
        icShiftOn = ic(R.drawable.ic_shift_on);
        icCaps = ic(R.drawable.ic_caps);
        icBksp = ic(R.drawable.ic_bksp);
        icEnter = ic(R.drawable.ic_enter);
        icSmile = ic(R.drawable.ic_smile);
        icClip = ic(R.drawable.ic_clip);
        icCopy = ic(R.drawable.ic_copy);
        icTrash = ic(R.drawable.ic_trash);
        barIcs[0] = icSmile; barIcs[1] = icClip; barIcs[2] = icCopy; barIcs[3] = icTrash; barIcs[4] = icBksp;
        icClock = ic(R.drawable.ic_clock);
        icThumb = ic(R.drawable.ic_thumb);
        icHeart = ic(R.drawable.ic_heart);
        icPaw = ic(R.drawable.ic_paw);
        icCup = ic(R.drawable.ic_cup);
        icBulb = ic(R.drawable.ic_bulb);
        reload();
    }

    private Drawable ic(int id) {
        return getResources().getDrawable(id, null).mutate();
    }

    private float dp(float v) {
        return v * dens;
    }

    // ---------------------------------------------------------------- API

    public void setListener(Listener l) {
        L = l;
    }

    public void setShift(boolean s) {
        if (caps) return;
        if (shift != s) {
            shift = s;
            invalidate();
        }
    }

    public void setSuggestions(String[] s) {
        for (int i = 0; i < 3; i++) sugs[i] = (s != null && i < s.length) ? s[i] : null;
        invalidate();
    }

    public void setMode(int m) {
        mode = m;
        if (m == M_EMO) {
            tab = recents().isEmpty() ? 1 : 0;
            scrollY = 0;
        }
        buildKeys();
        layoutKeys();
        invalidate();
    }

    public void reload() {
        int[] t = Prefs.THEMES[P.theme()];
        cBg = t[0];
        cKey = t[1];
        cKey2 = t[2];
        cText = t[3];
        cAcc = t[4];
        cAccT = t[5];
        fPopup = P.popup();
        fHint = P.hint();
        fHaptic = P.haptic();
        fSound = P.sound();
        fLong = P.longMs();
        fVib = P.vibMs();
        pText.setTypeface(P.bold() ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
        computeDims();
        buildKeys();
        requestLayout();
        if (getWidth() > 0) {
            layoutKeys();
            buildBg();
        }
        invalidate();
    }

    private void refreshUp() {
        boolean up = (shift || caps) && mode == M_LET;
        for (int i = 0; i < pk.length; i++) {
            Key k = pk[i];
            if (k != null) k.u = up ? (k.label == null ? null : k.label.toUpperCase()) : k.label;
        }
        upDirty = false;
    }

    public void cancelAll() {
        for (int i = 0; i < pk.length; i++) {
            pk[i] = null;
            zone[i] = 0;
        }
        stopRepeat();
        handler.removeCallbacks(longRun);
        handler.removeCallbacks(delRun);
        longKey = null;
        popKey = null;
        popText = null;
        gridPid = -1;
        lastShift = 0;
        shift = false;
        caps = false;
        upDirty = true;
        invalidate();
    }

    // ---------------------------------------------------------------- layout

    private void computeDims() {
        boolean land = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        int[] hs = {44, 52, 60};
        keyH = (int) (hs[P.size()] * dens * (land ? 0.8f : 1f));
        gap = Math.round(P.gap() * dens);
        rad = P.radius() * dens;
        tbH = (int) (36 * dens);
        sgH = (P.sug() || P.paste()) ? (int) (34 * dens) : 0;
        nRows = P.nums() ? 5 : 4;
        padB = Math.round(P.bottom() * dens) + gap;
        totalH = tbH + sgH + gap + nRows * (keyH + gap) + padB;
    }

    @Override
    protected void onMeasure(int ws, int hs) {
        computeDims();
        setMeasuredDimension(MeasureSpec.getSize(ws), totalH);
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        layoutKeys();
        buildBg();
        for (int i = 0; i < 3; i++) sugCache[i] = null;
    }

    private ArrayList<Key> chars(String s, String alts) {
        ArrayList<Key> l = new ArrayList<>();
        for (int i = 0; i < s.length(); i++) {
            l.add(new Key(String.valueOf(s.charAt(i)), alts == null ? null : String.valueOf(alts.charAt(i)), T_CHAR, 1f));
        }
        return l;
    }

    private Key spacer() {
        return new Key("", null, T_NONE, 0.5f);
    }

    private void buildKeys() {
        rows.clear();
        upDirty = true;
        boolean nums = P.nums();
        if (mode == M_EMO) {
            ArrayList<Key> t = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                Key k = new Key("", null, T_TAB, 1f);
                k.tab = i;
                t.add(k);
            }
            rows.add(t);
            ArrayList<Key> e = new ArrayList<>();
            e.add(new Key("ABC", null, T_ABC, 1.5f));
            e.add(new Key(" ", null, T_SPACE, 5f));
            e.add(new Key("", null, T_DEL, 1.5f));
            rows.add(e);
            return;
        }
        if (mode == M_LET) {
            if (nums) rows.add(chars("1234567890", null));
            rows.add(chars("qwertyuiop", nums ? null : "1234567890"));
            ArrayList<Key> r2 = chars("asdfghjkl", null);
            r2.add(0, spacer());
            r2.add(spacer());
            rows.add(r2);
            ArrayList<Key> r3 = chars("zxcvbnm", null);
            r3.add(0, new Key("", null, T_SHIFT, 1.5f));
            r3.add(new Key("", null, T_DEL, 1.5f));
            rows.add(r3);
        } else {
            rows.add(chars("1234567890", null));
            rows.add(chars("@#$%&-+()/", null));
            if (nums) {
                ArrayList<Key> c = chars("*\"':;!?=", null);
                c.add(0, spacer());
                c.add(spacer());
                rows.add(c);
                ArrayList<Key> d = chars("_~^<>[]{}", null);
                d.add(new Key("", null, T_DEL, 1.5f));
                rows.add(d);
            } else {
                ArrayList<Key> c = chars("*\"':;!?=", null);
                c.add(new Key("", null, T_DEL, 1.5f));
                rows.add(c);
            }
        }
        ArrayList<Key> b = new ArrayList<>();
        b.add(new Key(mode == M_LET ? "?123" : "ABC", null, T_SYM, 1.5f));
        b.add(new Key(",", mode == M_LET ? "!" : null, T_CHAR, 1f));
        b.add(new Key(" ", null, T_SPACE, 5f));
        b.add(new Key(".", mode == M_LET ? "?" : null, T_CHAR, 1f));
        b.add(new Key("", null, T_ENTER, 1.5f));
        rows.add(b);
    }

    private void layoutKeys() {
        int w = getWidth();
        if (w <= 0) return;
        kbW = P.one() == 0 ? w : (int) (w * 0.82f);
        kbX0 = P.one() == 2 ? w - kbW : 0;
        for (int i = 0; i < rows.size(); i++) {
            ArrayList<Key> row = rows.get(i);
            float sum = 0;
            for (Key k : row) sum += k.w;
            float unit = (kbW - gap * (row.size() + 1)) / sum;
            int slot = (mode == M_EMO && i == 1) ? nRows - 1 : i;
            float y = tbH + sgH + gap + slot * (keyH + gap);
            float x = kbX0 + gap;
            for (Key k : row) {
                float kw = k.w * unit;
                k.r.set(x, y, x + kw, y + keyH);
                x += kw + gap;
            }
        }
        gridTop = tbH + sgH + gap + (keyH + gap);
        gridH = (nRows - 2) * (keyH + gap) - gap;
    }

    // ---------------------------------------------------------------- latar

    private void buildBg() {
        doBuildBg();
        ka = bgBmp != null ? P.keyOpacity() * 255 / 100 : 255;
    }

    private static int sample(int sw, int sh, int w, int h) {
        int s = 1;
        while (sw / (s * 2) >= w && sh / (s * 2) >= h) s *= 2;
        return s;
    }

    private void dropBg() {
        if (bgBmp != null) { bgBmp.recycle(); bgBmp = null; }
    }

    private void doBuildBg() {
        int w = getWidth(), h = getHeight();
        if (w <= 0 || h <= 0) return;
        int m = P.bg();
        if (m == 3) {
            dropBg();
            bgSig = "3";
            return;
        }
        File f = new File(getContext().getFilesDir(), "bg.jpg");
        String sig = m + "|" + P.dim() + "|" + w + "x" + h + "|" + (m == 2 ? f.lastModified() : 0);
        if (sig.equals(bgSig)) return;
        bgSig = sig;
        Bitmap src = null;
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            o.inJustDecodeBounds = true;
            if (m == 2 && f.exists()) {
                BitmapFactory.decodeFile(f.getPath(), o);
                o.inSampleSize = sample(o.outWidth, o.outHeight, w, h);
                o.inJustDecodeBounds = false;
                src = BitmapFactory.decodeFile(f.getPath(), o);
            } else {
                int id = m == 1 ? R.drawable.bg_potret : R.drawable.bg_aksi;
                BitmapFactory.decodeResource(getResources(), id, o);
                o.inSampleSize = sample(o.outWidth, o.outHeight, w, h);
                o.inJustDecodeBounds = false;
                src = BitmapFactory.decodeResource(getResources(), id, o);
            }
        } catch (Throwable t) {
            src = null;
        }
        if (src == null) {
            dropBg();
            return;
        }
        try {
            Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.RGB_565);
            Canvas c = new Canvas(out);
            float s = Math.max(w / (float) src.getWidth(), h / (float) src.getHeight());
            float dw = src.getWidth() * s, dh = src.getHeight() * s;
            float l = (w - dw) / 2f, t = (h - dh) * 0.25f;
            tmp.set(l, t, l + dw, t + dh);
            c.drawBitmap(src, null, tmp, pBg);
            c.drawColor(Color.argb(P.dim() * 255 / 100, 0, 0, 0));
            dropBg();
            bgBmp = out;
        } catch (Throwable t) {
            dropBg();
        }
        src.recycle();
    }

    // ---------------------------------------------------------------- gambar

    private static int mix(int a, int b, float t) {
        int r = (int) (Color.red(a) + (Color.red(b) - Color.red(a)) * t);
        int g = (int) (Color.green(a) + (Color.green(b) - Color.green(a)) * t);
        int bl = (int) (Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t);
        return Color.rgb(r, g, bl);
    }

    private static int withAlpha(int c, int a) {
        return (a << 24) | (c & 0x00FFFFFF);
    }

    private void drawIcon(Canvas c, Drawable d, float cx, float cy, float size, int color) {
        d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
        int s = (int) size;
        d.setBounds((int) (cx - s / 2f), (int) (cy - s / 2f), (int) (cx + s / 2f), (int) (cy + s / 2f));
        d.draw(c);
    }

    private void drawText(Canvas c, String s, float cx, float cy, float size, int color, int alpha) {
        pText.setTextSize(size);
        pText.setColor(color);
        pText.setAlpha(alpha);
        c.drawText(s, cx, cy - (pText.ascent() + pText.descent()) / 2f, pText);
    }

    private Drawable tabIcon(int t) {
        switch (t) {
            case 0: return icClock;
            case 1: return icSmile;
            case 2: return icThumb;
            case 3: return icHeart;
            case 4: return icPaw;
            case 5: return icCup;
            default: return icBulb;
        }
    }

    private boolean isPressed(Key k) {
        for (int i = 0; i < pk.length; i++) if (pk[i] == k) return true;
        return false;
    }

    @Override
    protected void onDraw(Canvas c) {
        if (bgBmp != null) c.drawBitmap(bgBmp, 0, 0, null);
        else c.drawColor(cBg);
        drawBars(c, getWidth());
        if (mode == M_EMO) drawGrid(c);
        for (ArrayList<Key> row : rows) {
            for (Key k : row) {
                if (k.type != T_NONE) drawKey(c, k, isPressed(k));
            }
        }
        drawPop(c);
    }

    private void drawBars(Canvas c, int w) {
        pBar.setColor(Color.argb(60, 0, 0, 0));
        c.drawRect(0, 0, w, tbH + sgH, pBar);
        float cw = w / barLb.length, is = tbH * 0.5f, cy = tbH / 2f;
        pLabel.setTextSize(tbH * 0.34f);
        for (int i = 0; i < barLb.length; i++) {
            int col = (i == 0 && mode == M_EMO) ? cAcc : cText;
            pLabel.setColor(col);
            float tw = pLabel.measureText(barLb[i]);
            float gw = is + dp(5) + tw;
            float x = i * cw + (cw - gw) / 2f;
            drawIcon(c, barIcs[i], x + is / 2f, cy, is, col);
            c.drawText(barLb[i], x + is + dp(5), cy - (pLabel.ascent() + pLabel.descent()) / 2f, pLabel);
        }
        if (sgH > 0) {
            pLine.setColor(withAlpha(cText, 50));
            float slot = w / 3f;
            c.drawLine(slot, tbH + sgH * 0.2f, slot, tbH + sgH * 0.8f, pLine);
            c.drawLine(slot * 2, tbH + sgH * 0.2f, slot * 2, tbH + sgH * 0.8f, pLine);
            pSug.setTextSize(sgH * 0.44f);
            pSug.setColor(cText);
            float scy = tbH + sgH / 2f, si = sgH * 0.46f;
            for (int i = 0; i < 3; i++) {
                String s = sugs[i];
                if (s == null) continue;
                boolean clip = s.length() > 0 && s.charAt(0) == CLIP;
                String t;
                if (s.equals(sugCache[i]) && clip == sugClipCache[i]) {
                    t = sugTextCache[i];
                } else {
                    t = (clip ? s.substring(1) : s).replace('\n', ' ');
                    float maxW = slot - dp(16) - (clip ? si + dp(4) : 0);
                    t = TextUtils.ellipsize(t, pSug, maxW, TextUtils.TruncateAt.END).toString();
                    sugCache[i] = s; sugClipCache[i] = clip; sugTextCache[i] = t;
                }
                float tw = pSug.measureText(t);
                float gw = tw + (clip ? si + dp(4) : 0);
                float x = i * slot + (slot - gw) / 2f;
                if (clip) {
                    drawIcon(c, icClip, x + si / 2f, scy, si, cAcc);
                    x += si + dp(4);
                }
                c.drawText(t, x + tw / 2f, scy - (pSug.ascent() + pSug.descent()) / 2f, pSug);
            }
        }
    }

    private void drawKey(Canvas c, Key k, boolean pr) {
        boolean acc = false;
        if (k.type == T_ENTER) acc = true;
        else if (k.type == T_SHIFT) acc = shift || caps;
        else if (k.type == T_TAB) acc = (k.tab == tab);
        boolean special = k.type != T_CHAR;
        int col = acc ? cAcc : (special ? cKey2 : cKey);
        int a = (acc || pr) ? 255 : ka;
        if (pr) col = acc ? mix(col, Color.BLACK, 0.25f) : mix(col, cAcc, 0.5f);
        pKey.setColor(col);
        pKey.setAlpha(a);
        c.drawRoundRect(k.r, rad, rad, pKey);
        float cx = k.r.centerX(), cy = k.r.centerY();
        int fg = acc ? cAccT : cText;
        switch (k.type) {
            case T_CHAR: {
                if (upDirty) refreshUp();
                String s = k.u != null ? k.u : k.label;
                drawText(c, s, cx, cy, keyH * 0.40f, fg, 255);
                if (k.alt != null && fHint && !pr) {
                    pHint.setTextSize(keyH * 0.22f);
                    pHint.setColor(fg);
                    pHint.setAlpha(120);
                    c.drawText(k.alt, k.r.right - dp(5), k.r.top + keyH * 0.28f, pHint);
                }
                break;
            }
            case T_SPACE:
                drawText(c, "Indonesia", cx, cy, keyH * 0.26f, fg, 170);
                break;
            case T_SYM:
            case T_ABC:
                drawText(c, k.label, cx, cy, keyH * 0.30f, fg, 255);
                break;
            case T_SHIFT:
                drawIcon(c, caps ? icCaps : (shift ? icShiftOn : icShift), cx, cy, keyH * 0.5f, fg);
                break;
            case T_DEL:
                drawIcon(c, icBksp, cx, cy, keyH * 0.5f, fg);
                break;
            case T_ENTER:
                drawIcon(c, icEnter, cx, cy, keyH * 0.5f, fg);
                break;
            case T_TAB:
                drawIcon(c, tabIcon(k.tab), cx, cy, keyH * 0.46f, fg);
                break;
            default:
                break;
        }
    }

    private void drawPop(Canvas c) {
        if (!fPopup || popKey == null || mode == M_EMO) return;
        if (upDirty) refreshUp();
        String s = popText != null ? popText : (popKey != null && popKey.u != null ? popKey.u : popKey != null ? popKey.label : null);
        RectF r = popKey.r;
        float bw = Math.max(r.width() * 1.3f, keyH * 0.95f), bh = keyH * 1.1f;
        float l = r.centerX() - bw / 2f;
        if (l < 0) l = 0;
        if (l + bw > getWidth()) l = getWidth() - bw;
        float t = r.top - bh - dp(4);
        if (t < 0) t = 0;
        tmp.set(l, t, l + bw, t + bh);
        pKey.setColor(cKey);
        pKey.setAlpha(255);
        c.drawRoundRect(tmp, rad, rad, pKey);
        drawText(c, s, tmp.centerX(), tmp.centerY(), keyH * 0.6f, cText, 255);
    }

    // ---------------------------------------------------------------- emoji

    private ArrayList<String> recents() {
        if (recCache == null) {
            recCache = new ArrayList<>();
            String s = P.sp.getString("rec", "");
            for (String e : s.split(" ")) {
                if (!e.isEmpty()) recCache.add(e);
            }
        }
        return recCache;
    }

    private void addRecent(String e) {
        ArrayList<String> l = recents();
        l.remove(e);
        l.add(0, e);
        while (l.size() > 24) l.remove(l.size() - 1);
        P.sp.edit().putString("rec", TextUtils.join(" ", l)).apply();
    }

    private ArrayList<String> emoList(int t) {
        if (t == 0) return recents();
        while (emo.size() < 7) emo.add(null);
        ArrayList<String> l = emo.get(t);
        if (l == null) {
            l = new ArrayList<>();
            String raw = getResources().getString(EMO_IDS[t - 1]);
            for (String s : raw.trim().split("\\s+")) {
                if (s.isEmpty()) continue;
                if (Build.VERSION.SDK_INT < 23 || pEmoji.hasGlyph(s)) l.add(s);
            }
            emo.set(t, l);
        }
        return l;
    }

    private int maxScroll() {
        int n = emoList(tab).size();
        int rowsN = (n + 7) / 8;
        return Math.max(0, rowsN * keyH - gridH);
    }

    private void drawGrid(Canvas c) {
        ArrayList<String> l = emoList(tab);
        float cw = kbW / 8f;
        c.save();
        c.clipRect(kbX0, gridTop, kbX0 + kbW, gridTop + gridH);
        if (l.isEmpty()) {
            drawText(c, "Belum ada emoji terbaru", kbX0 + kbW / 2f, gridTop + gridH / 2f, keyH * 0.3f, cText, 160);
        } else {
            pEmoji.setTextSize(keyH * 0.62f);
            int ms = maxScroll();
            if (scrollY > ms) scrollY = ms;
            int rowsN = (l.size() + 7) / 8;
            int r0 = scrollY / keyH;
            int r1 = Math.min(rowsN - 1, (scrollY + gridH) / keyH);
            float off = -(pEmoji.ascent() + pEmoji.descent()) / 2f;
            for (int r = r0; r <= r1; r++) {
                for (int col = 0; col < 8; col++) {
                    int i = r * 8 + col;
                    if (i >= l.size()) break;
                    c.drawText(l.get(i), kbX0 + col * cw + cw / 2f, gridTop + r * keyH - scrollY + keyH / 2f + off, pEmoji);
                }
            }
        }
        c.restore();
    }

    private String gridHit(float x, float y) {
        if (x < kbX0 || x >= kbX0 + kbW) return null;
        int col = (int) ((x - kbX0) / (kbW / 8f));
        int row = (int) ((y - gridTop + scrollY) / keyH);
        int i = row * 8 + col;
        ArrayList<String> l = emoList(tab);
        return (i >= 0 && i < l.size()) ? l.get(i) : null;
    }

    // ---------------------------------------------------------------- sentuhan

    private Key hit(float x, float y) {
        int slot = (int) ((y - tbH - sgH - gap / 2f) / (keyH + gap));
        if (y < tbH + sgH) return null;
        if (slot < 0) slot = 0;
        if (slot >= nRows) return null;
        int ri = slot;
        if (mode == M_EMO) ri = slot == 0 ? 0 : (slot == nRows - 1 ? 1 : -1);
        if (ri < 0 || ri >= rows.size()) return null;
        if (x < kbX0 || x > kbX0 + kbW) return null;
        Key last = null;
        for (Key k : rows.get(ri)) {
            if (k.type == T_NONE) continue;
            last = k;
            if (x < k.r.right + gap / 2f) return k;
        }
        return last;
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {
                int i = e.getActionIndex();
                down(e.getPointerId(i), e.getX(i), e.getY(i));
                return true;
            }
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < e.getPointerCount(); i++) move(e.getPointerId(i), e.getX(i), e.getY(i));
                return true;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP: {
                int i = e.getActionIndex();
                up(e.getPointerId(i), e.getX(i), e.getY(i));
                return true;
            }
            case MotionEvent.ACTION_CANCEL:
                cancelAll();
                return true;
            default:
                return super.onTouchEvent(e);
        }
    }

    private void down(int id, float x, float y) {
        if (id < 0 || id >= pk.length) return;
        pk[id] = null;
        zone[id] = 0;
        int w = getWidth();
        if (y < tbH) {
            int i = Math.min(barLb.length - 1, (int) (x * barLb.length / w));
            feedback();
            if (i == 0) setMode(mode == M_EMO ? M_LET : M_EMO);
            else if (L != null) L.onToolbar(i);
            return;
        }
        if (sgH > 0 && y < tbH + sgH) {
            int i = Math.min(2, (int) (x * 3 / w));
            if (sugs[i] != null) {
                feedback();
                if (L != null) L.onSuggestion(sugs[i]);
            }
            return;
        }
        if (mode == M_EMO && y >= gridTop && y < gridTop + gridH) {
            zone[id] = 3;
            gridPid = id;
            gridDownY = y;
            gridStartScroll = scrollY;
            gridMoved = false;
            return;
        }
        Key k = hit(x, y);
        if (k == null) return;
        pk[id] = k;
        zone[id] = 2;
        feedback();
        switch (k.type) {
            case T_CHAR:
                typeChar(k);
                popKey = k;
                popText = null;
                if (k.alt != null) {
                    handler.removeCallbacks(longRun);
                    longKey = k;
                    longPid = id;
                    handler.postDelayed(longRun, fLong);
                }
                break;
            case T_DEL:
                del();
                startRepeat();
                break;
            case T_SHIFT:
                toggleShift();
                break;
            case T_SYM:
                setMode(mode == M_LET ? M_SYM : M_LET);
                break;
            case T_ABC:
                setMode(M_LET);
                break;
            case T_ENTER:
                if (L != null) L.onEnter();
                break;
            case T_SPACE:
                sx[id] = x;
                swiped[id] = false;
                break;
            case T_TAB:
                tab = k.tab;
                scrollY = 0;
                break;
            default:
                break;
        }
        invalidate();
    }

    private void move(int id, float x, float y) {
        if (id < 0 || id >= pk.length) return;
        if (zone[id] == 3 && gridPid == id) {
            float dy = gridDownY - y;
            if (Math.abs(dy) > dp(8)) gridMoved = true;
            if (gridMoved) {
                int s = gridStartScroll + (int) dy;
                int ms = maxScroll();
                scrollY = s < 0 ? 0 : (s > ms ? ms : s);
                invalidate();
            }
        } else if (pk[id] != null && pk[id].type == T_SPACE) {
            float step = dp(16);
            float dx = x - sx[id];
            while (Math.abs(dx) >= step) {
                int dir = dx > 0 ? 1 : -1;
                if (L != null) L.onCursor(dir);
                sx[id] += dir * step;
                dx = x - sx[id];
                swiped[id] = true;
            }
        }
    }

    private void up(int id, float x, float y) {
        if (id < 0 || id >= pk.length) return;
        Key k = pk[id];
        if (zone[id] == 3) {
            if (!gridMoved) {
                String e = gridHit(x, y);
                if (e != null) {
                    feedback();
                    if (L != null) L.onText(e);
                    addRecent(e);
                }
            }
            gridPid = -1;
        } else if (k != null) {
            if (k.type == T_DEL) stopRepeat();
            if (k.type == T_SPACE && !swiped[id] && L != null) L.onText(" ");
            if (k == longKey) {
                longKey = null;
                handler.removeCallbacks(longRun);
            }
        }
        pk[id] = null;
        zone[id] = 0;
        popKey = null;
        popText = null;
        invalidate();
    }

    private void typeChar(Key k) {
        String s = k.label;
        if (mode == M_LET && (shift || caps)) s = s.toUpperCase();
        if (L != null) L.onText(s);
        if (mode == M_LET && shift && !caps) shift = false;
    }

    private void del() {
        if (L != null) L.onDelete();
    }

    private void toggleShift() {
        long now = SystemClock.uptimeMillis();
        if (caps) {
            caps = false;
            shift = false;
        } else if (shift && now - lastShift < 350) {
            caps = true;
        } else {
            shift = !shift;
        }
        lastShift = now;
        upDirty = true;
    }

    private final Runnable longRun = new Runnable() {
        @Override
        public void run() {
            Key k = longKey;
            if (k == null || k.alt == null || longPid < 0 || longPid >= pk.length || pk[longPid] != k) return;
            if (L != null) {
                L.onDelete();
                L.onText(k.alt);
            }
            popText = k.alt;
            feedback();
            invalidate();
        }
    };

    private final Runnable delRun = new Runnable() {
        @Override
        public void run() {
            if (!repeating) return;
            del();
            handler.postDelayed(this, 45);
        }
    };

    private void startRepeat() {
        repeating = true;
        handler.removeCallbacks(delRun);
        handler.postDelayed(delRun, 380);
    }

    private void stopRepeat() {
        repeating = false;
        handler.removeCallbacks(delRun);
    }

    @SuppressWarnings("deprecation")
    private void feedback() {
        if (fHaptic && vib != null) {
            if (Build.VERSION.SDK_INT >= 26) {
                vib.vibrate(VibrationEffect.createOneShot(fVib, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vib.vibrate(fVib);
            }
        }
        if (fSound && am != null) am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        cancelAll();
        if (bgBmp != null) { bgBmp.recycle(); bgBmp = null; }
        bgSig = null;
    }
}
