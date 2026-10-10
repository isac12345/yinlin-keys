package com.yinlin.keys;

import android.content.Context;
import android.content.SharedPreferences;

/** Pengaturan tersimpan (SharedPreferences), dibaca ringan tanpa library. */
final class Prefs {
    static final String[] THEME_NAMES = {"Yinlin", "Terang", "Gelap", "Laut", "Hutan", "Amoled", "Sakura", "Pasir"};
    // kbg, key, key2, text, accent, accentText
    static final int[][] THEMES = {
        {0xFF1A1030, 0xFF2B1D4A, 0xFF1C1235, 0xFFF4ECFF, 0xFFB061FF, 0xFFFFFFFF},
        {0xFFD5DBE2, 0xFFFFFFFF, 0xFFB9C2CC, 0xFF16202A, 0xFF2F6FED, 0xFFFFFFFF},
        {0xFF1A2027, 0xFF343E49, 0xFF252D36, 0xFFEEF2F6, 0xFF4DA3FF, 0xFF10202A},
        {0xFF0D3846, 0xFF1C5A6F, 0xFF134658, 0xFFE6FBFF, 0xFF2FE0C2, 0xFF10202A},
        {0xFF1F2E24, 0xFF355241, 0xFF294034, 0xFFE9F5EC, 0xFF8BD450, 0xFF10202A},
        {0xFF000000, 0xFF1C1C1E, 0xFF101012, 0xFFF2F2F7, 0xFF9D7BFF, 0xFFFFFFFF},
        {0xFFF3D9E1, 0xFFFFF7FA, 0xFFE3B7C6, 0xFF4A1F30, 0xFFE0457B, 0xFFFFFFFF},
        {0xFFE6DCC8, 0xFFFFFAF0, 0xFFCDBF9F, 0xFF3A3020, 0xFFB8742A, 0xFFFFFFFF}
    };

    final SharedPreferences sp;

    Prefs(Context c) {
        sp = c.getApplicationContext().getSharedPreferences("yk", Context.MODE_PRIVATE);
    }

    private static int cl(int v, int a, int b) {
        return v < a ? a : (v > b ? b : v);
    }

    int theme() { return cl(sp.getInt("theme", 0), 0, THEMES.length - 1); }
    /** 0 aksi, 1 potret, 2 galeri, 3 polos */
    int bg() { return cl(sp.getInt("bg", 0), 0, 3); }
    int dim() { return cl(sp.getInt("dim", 35), 0, 80); }
    int keyOpacity() { return cl(sp.getInt("ko", 62), 30, 100); }
    int size() { return cl(sp.getInt("size", 1), 0, 2); }
    int radius() { return cl(sp.getInt("rad", 10), 0, 26); }
    int gap() { return cl(sp.getInt("gap", 4), 0, 10); }
    int bottom() { return cl(sp.getInt("bot", 0), 0, 40); }
    /** 0 mati, 1 kiri, 2 kanan */
    int one() { return cl(sp.getInt("one", 0), 0, 2); }
    int longMs() { return cl(sp.getInt("lp", 380), 200, 700); }
    int vibMs() { return cl(sp.getInt("vi", 12), 4, 40); }

    boolean nums() { return sp.getBoolean("nums", true); }
    boolean haptic() { return sp.getBoolean("haptic", false); }
    boolean sound() { return sp.getBoolean("sound", false); }
    boolean popup() { return sp.getBoolean("popup", true); }
    boolean sug() { return sp.getBoolean("sug", true); }
    boolean paste() { return sp.getBoolean("paste", true); }
    boolean cap() { return sp.getBoolean("cap", true); }
    boolean hint() { return sp.getBoolean("hint", true); }
    boolean bold() { return sp.getBoolean("bold", false); }
}
