package com.yinlin.keys;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.inputmethodservice.InputMethodService;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.widget.Toast;

public class YinlinIME extends InputMethodService implements KeyboardView.Listener {

    private KeyboardView kv;
    private Prefs P;
    private ClipboardManager cm;
    private final Handler h = new Handler(Looper.getMainLooper());
    private int inputType, imeOpts;
    private boolean pw;
    private String clip = "", lastPasted = "";
    // papan salin: 20 item terakhir yang dicOPY dari keyboard ini (dipakai user, bukan bocor ke luar)
    private static final int CLIP_N = 20;
    private final String[] clipHist = new String[CLIP_N];
    private int clipLen, clipNext;
    // undo: teks yang diketik sejak tombol Batal terakhir (maks 200 huruf)
    private final StringBuilder undoBuf = new StringBuilder(200);
    private static final int UNDO_MAX = 200;

    private final Runnable sugRun = new Runnable() {
        @Override
        public void run() {
            updateSug();
        }
    };

    private final ClipboardManager.OnPrimaryClipChangedListener clipListener = new ClipboardManager.OnPrimaryClipChangedListener() {
        @Override
        public void onPrimaryClipChanged() {
            readClip();
            postSug();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        P = new Prefs(this);
        cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        try {
            if (cm != null) cm.addPrimaryClipChangedListener(clipListener);
        } catch (Throwable ignored) {
        }
        readClip();
    }

    @Override
    public void onDestroy() {
        try {
            if (cm != null) cm.removePrimaryClipChangedListener(clipListener);
        } catch (Throwable ignored) {
        }
        h.removeCallbacks(sugRun);
        super.onDestroy();
    }

    @Override
    public View onCreateInputView() {
        kv = new KeyboardView(this, P);
        kv.setListener(this);
        return kv;
    }

    @Override
    public boolean onEvaluateFullscreenMode() {
        return false;
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        inputType = info.inputType;
        imeOpts = info.imeOptions;
        int cls = inputType & InputType.TYPE_MASK_CLASS;
        int var = inputType & InputType.TYPE_MASK_VARIATION;
        pw = (cls == InputType.TYPE_CLASS_TEXT && (var == InputType.TYPE_TEXT_VARIATION_PASSWORD
                || var == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                || var == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD))
                || (cls == InputType.TYPE_CLASS_NUMBER && var == InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        readClip();
        if (kv == null) return;
        kv.reload();
        boolean num = cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE || cls == InputType.TYPE_CLASS_DATETIME;
        kv.setMode(num ? KeyboardView.M_SYM : KeyboardView.M_LET);
        updateCaps();
        updateSug();
    }

    @Override
    public void onFinishInputView(boolean finishingInput) {
        super.onFinishInputView(finishingInput);
        if (kv != null) kv.cancelAll();
    }

    @Override
    public void onUpdateSelection(int oldSelStart, int oldSelEnd, int newSelStart, int newSelEnd, int candStart, int candEnd) {
        super.onUpdateSelection(oldSelStart, oldSelEnd, newSelStart, newSelEnd, candStart, candEnd);
        postSug();
    }

    // ------------------------------------------------------------ Listener

    @Override
    public void onText(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || s == null || s.isEmpty()) return;
        ic.commitText(s, 1);
        // catat buat tombol Batal (maks 200 huruf supaya RAM kecil)
        for (int i = 0; i < s.length() && undoBuf.length() < UNDO_MAX; i++) undoBuf.append(s.charAt(i));
        char c = s.charAt(0);
        if (c == ' ' || c == '\n') updateCaps();
        postSug();
    }

    @Override
    public void onDelete() {
        if (undoBuf.length() > 0) undoBuf.setLength(undoBuf.length() - 1);
        sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
        updateCaps();
        postSug();
    }

    /** Batal: hapus semua yang diketik sejak tombol Batal terakhir. */
    private void doUndo() {
        if (undoBuf.length() == 0) {
            toast("Tidak ada yang perlu dibatalkan");
            return;
        }
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        ic.beginBatchEdit();
        for (int i = 0; i < undoBuf.length(); i++) sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
        ic.endBatchEdit();
        undoBuf.setLength(0);
        updateCaps();
        postSug();
    }

    @Override
    public void onEnter() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        int act = imeOpts & EditorInfo.IME_MASK_ACTION;
        if ((imeOpts & EditorInfo.IME_FLAG_NO_ENTER_ACTION) == 0
                && act != EditorInfo.IME_ACTION_NONE && act != EditorInfo.IME_ACTION_UNSPECIFIED) {
            ic.performEditorAction(act);
        } else {
            sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
        }
        updateCaps();
        postSug();
    }

    @Override
    public void onCursor(int dir) {
        sendDownUpKeyEvents(dir < 0 ? KeyEvent.KEYCODE_DPAD_LEFT : KeyEvent.KEYCODE_DPAD_RIGHT);
    }

    @Override
    public void onToolbar(int i) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (i == 1) {
            readClip();
            if (clip.isEmpty()) {
                toast("Clipboard kosong");
                return;
            }
            ic.commitText(clip, 1);
            lastPasted = clip;
            updateCaps();
            postSug();
        } else if (i == 2) {
            CharSequence sel = ic.getSelectedText(0);
            String t;
            if (sel != null && sel.length() > 0) {
                t = sel.toString();
            } else {
                CharSequence b = ic.getTextBeforeCursor(50000, 0);
                CharSequence a = ic.getTextAfterCursor(50000, 0);
                t = (b == null ? "" : b.toString()) + (a == null ? "" : a.toString());
            }
            if (t.isEmpty()) {
                toast("Tidak ada teks");
                return;
            }
            try {
                cm.setPrimaryClip(ClipData.newPlainText("Yinlin Keys", t));
                pushClip(t);
                toast("Teks disalin");
            } catch (Throwable e) {
                toast("Gagal menyalin");
            }
        } else if (i == 3) {
            CharSequence b = ic.getTextBeforeCursor(50000, 0);
            CharSequence a = ic.getTextAfterCursor(50000, 0);
            ic.beginBatchEdit();
            ic.deleteSurroundingText(b == null ? 0 : b.length(), a == null ? 0 : a.length());
            ic.endBatchEdit();
            undoBuf.setLength(0);
            updateCaps();
            postSug();
        } else if (i == 4) {
            doUndo();
        }
    }

    @Override
    public void onSuggestion(String s) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || s == null || s.isEmpty()) return;
        if (s.charAt(0) == KeyboardView.CLIP) {
            String t = s.substring(1);
            ic.commitText(t, 1);
            lastPasted = t;
        } else {
            String w = currentWord(ic);
            if (!w.isEmpty()) ic.deleteSurroundingText(w.length(), 0);
            String r = s;
            if (!w.isEmpty() && Character.isUpperCase(w.charAt(0))) {
                r = Character.toUpperCase(r.charAt(0)) + r.substring(1);
            }
            ic.commitText(r + " ", 1);
        }
        updateCaps();
        postSug();
    }

    // ------------------------------------------------------------ util

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------ papan salin

    /** Tambah teks ke papan salin internal (maks 20, yang lama dibuang). */
    private void pushClip(String t) {
        if (t == null || t.isEmpty()) return;
        for (int i = 0; i < clipLen; i++) {
            if (t.equals(clipHist[i])) { // jangan dobel
                clipHist[i] = clipHist[clipNext];
                clipHist[clipNext] = t;
                return;
            }
        }
        clipHist[clipNext] = t;
        clipNext = (clipNext + 1) % CLIP_N;
        if (clipLen < CLIP_N) clipLen++;
    }

    /** Ambil teks ke-i dari papan salin (0 = terbaru). */
    private String getClip(int i) {
        if (i < 0 || i >= clipLen) return null;
        return clipHist[(clipNext - 1 - i + CLIP_N * 2) % CLIP_N];
    }

    private void readClip() {
        clip = "";
        try {
            if (cm == null) return;
            ClipData d = cm.getPrimaryClip();
            if (d != null && d.getItemCount() > 0) {
                CharSequence t = d.getItemAt(0).coerceToText(this);
                if (t != null) clip = t.toString();
            }
        } catch (Throwable ignored) {
        }
    }

    private void updateCaps() {
        if (kv == null) return;
        InputConnection ic = getCurrentInputConnection();
        int cls = inputType & InputType.TYPE_MASK_CLASS;
        if (ic == null || pw || cls != InputType.TYPE_CLASS_TEXT || !P.cap()) {
            kv.setShift(false);
            return;
        }
        kv.setShift(ic.getCursorCapsMode(inputType) != 0);
    }

    private String currentWord(InputConnection ic) {
        CharSequence b = ic.getTextBeforeCursor(24, 0);
        if (b == null) return "";
        int i = b.length();
        while (i > 0 && Character.isLetter(b.charAt(i - 1))) i--;
        return b.subSequence(i, b.length()).toString();
    }

    private void postSug() {
        h.removeCallbacks(sugRun);
        h.postDelayed(sugRun, 60);
    }

    private void updateSug() {
        if (kv == null) return;
        String[] out = new String[3];
        InputConnection ic = getCurrentInputConnection();
        if (pw || ic == null) {
            kv.setSuggestions(out);
            return;
        }
        String w = currentWord(ic);
        int n = 0;
        if (w.isEmpty() && P.paste() && !clip.isEmpty() && !clip.equals(lastPasted)) {
            out[n++] = KeyboardView.CLIP + clip;
        }
        if (P.sug()) {
            if (w.isEmpty()) {
                for (int i = 0; i < Dict.DEF.length && n < out.length; i++) out[n++] = Dict.DEF[i];
            } else {
                Dict.fill(w.toLowerCase(), out, n);
            }
        }
        // papan salin: kalau sisa slot, isi dengan riwayat copy (terbaru dulu)
        for (int i = 0; n < out.length && i < clipLen; i++) {
            String c = getClip(i);
            if (c != null && !c.equals(lastPasted)) out[n++] = KeyboardView.CLIP + c;
        }
        kv.setSuggestions(out);
    }
}
