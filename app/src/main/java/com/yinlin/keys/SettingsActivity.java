package com.yinlin.keys;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.drawable.GradientDrawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class SettingsActivity extends Activity {

    private static final int REQ_BG = 11;
    private static final int BG = 0xFF120B22, CARD = 0xFF1E1535, TXT = 0xFFF4ECFF, SUB = 0xFFB9A9D6, ACC = 0xFFB061FF;
    private static final int MATCH = ViewGroup.LayoutParams.MATCH_PARENT, WRAP = ViewGroup.LayoutParams.WRAP_CONTENT;

    private interface Cb {
        void on(int v);
    }

    private static final class Chips {
        final TextView[] v;
        Chips(TextView[] v) {
            this.v = v;
        }
    }

    private Prefs P;
    private KeyboardView kv;
    private EditText et;
    private Chips bgChips;
    private float dens;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        P = new Prefs(this);
        dens = getResources().getDisplayMetrics().density;
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (kv != null) kv.reload();
    }

    private int dp(int v) {
        return Math.round(v * dens);
    }

    private TextView txt(String s, int size, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(size);
        t.setTextColor(color);
        return t;
    }

    private LinearLayout.LayoutParams lp(int top) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(MATCH, WRAP);
        p.topMargin = dp(top);
        return p;
    }

    private void head(LinearLayout p, String s) {
        TextView t = txt(s, 13, ACC);
        t.setAllCaps(true);
        t.setLetterSpacing(0.08f);
        p.addView(t, lp(22));
    }

    private void style(TextView t, boolean on) {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(10));
        g.setColor(on ? 0x33B061FF : CARD);
        g.setStroke(dp(on ? 2 : 1), on ? ACC : 0xFF3A2D5C);
        t.setBackground(g);
        t.setTextColor(on ? 0xFFFFFFFF : SUB);
    }

    private void select(Chips c, int sel) {
        for (int i = 0; i < c.v.length; i++) style(c.v[i], i == sel);
    }

    private Chips chips(LinearLayout parent, String[] labels, int sel, int perRow, final Cb cb) {
        final TextView[] tv = new TextView[labels.length];
        final Chips ch = new Chips(tv);
        LinearLayout row = null;
        for (int i = 0; i < labels.length; i++) {
            if (i % perRow == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                parent.addView(row, lp(6));
            }
            TextView t = txt(labels[i], 14, SUB);
            t.setGravity(Gravity.CENTER);
            t.setPadding(dp(6), dp(10), dp(6), dp(10));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, WRAP, 1f);
            p.rightMargin = dp(6);
            row.addView(t, p);
            final int idx = i;
            t.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    select(ch, idx);
                    cb.on(idx);
                }
            });
            tv[i] = t;
        }
        select(ch, sel);
        return ch;
    }

    private void slider(LinearLayout p, final String label, final int min, int max, int val, final String unit, final Cb cb) {
        final TextView t = txt(label + ": " + val + unit, 14, TXT);
        p.addView(t, lp(12));
        SeekBar s = new SeekBar(this);
        s.setMax(max - min);
        s.setProgress(val - min);
        s.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int pr, boolean user) {
                t.setText(label + ": " + (pr + min) + unit);
            }

            @Override
            public void onStartTrackingTouch(SeekBar sb) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar sb) {
                cb.on(sb.getProgress() + min);
            }
        });
        p.addView(s, new LinearLayout.LayoutParams(MATCH, WRAP));
    }

    private void sw(LinearLayout p, String label, boolean on, final Cb cb) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setTextColor(TXT);
        s.setTextSize(15);
        s.setChecked(on);
        s.setPadding(0, dp(8), 0, dp(8));
        s.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton b, boolean checked) {
                cb.on(checked ? 1 : 0);
            }
        });
        p.addView(s, new LinearLayout.LayoutParams(MATCH, WRAP));
    }

    private void setI(String k, int v) {
        P.sp.edit().putInt(k, v).apply();
        kv.reload();
    }

    private void setB(String k, boolean v) {
        P.sp.edit().putBoolean(k, v).apply();
        kv.reload();
    }

    private Button btn(String s, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(s);
        b.setAllCaps(false);
        b.setTextColor(0xFFFFFFFF);
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(12));
        g.setColor(0xFF4A2F86);
        b.setBackground(g);
        b.setOnClickListener(l);
        return b;
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private void build() {
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(BG);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(20), dp(16), dp(40));
        sv.addView(root, new ViewGroup.LayoutParams(MATCH, WRAP));
        setContentView(sv);

        // header
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        ImageView icon = new ImageView(this);
        icon.setImageResource(R.mipmap.ic_launcher);
        icon.setScaleType(ImageView.ScaleType.CENTER_CROP);
        top.addView(icon, new LinearLayout.LayoutParams(dp(56), dp(56)));
        LinearLayout tt = new LinearLayout(this);
        tt.setOrientation(LinearLayout.VERTICAL);
        tt.setPadding(dp(12), 0, 0, 0);
        tt.addView(txt("Yinlin Keys", 22, TXT));
        tt.addView(txt("Keyboard super ringan", 13, SUB));
        top.addView(tt);
        root.addView(top);

        // langkah awal
        head(root, "Langkah awal");
        root.addView(btn("1. Aktifkan keyboard", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
            }
        }), lp(8));
        root.addView(btn("2. Pilih Yinlin Keys sebagai keyboard", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showInputMethodPicker();
            }
        }), lp(6));

        // preview
        head(root, "Coba ketik di sini");
        et = new EditText(this);
        et.setHint("Ketik pakai preview di bawah…");
        et.setHintTextColor(SUB);
        et.setTextColor(TXT);
        et.setTextSize(16);
        et.setGravity(Gravity.TOP);
        et.setMinLines(3);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        et.setShowSoftInputOnFocus(false);
        et.setPadding(dp(12), dp(10), dp(12), dp(10));
        GradientDrawable eg = new GradientDrawable();
        eg.setCornerRadius(dp(12));
        eg.setColor(CARD);
        et.setBackground(eg);
        root.addView(et, lp(8));

        kv = new KeyboardView(this, P);
        kv.setSuggestions(new String[]{"saya", "yang", "dan"});
        kv.setListener(new KeyboardView.Listener() {
            @Override
            public void onText(String s) {
                ins(s);
            }

            @Override
            public void onDelete() {
                int a = Math.max(0, et.getSelectionStart()), b = Math.max(0, et.getSelectionEnd());
                int lo = Math.min(a, b), hi = Math.max(a, b);
                if (hi > lo) {
                    et.getText().delete(lo, hi);
                    et.setSelection(lo);
                } else if (lo > 0) {
                    et.getText().delete(lo - 1, lo);
                    et.setSelection(lo - 1);
                }
            }

            @Override
            public void onEnter() {
                ins("\n");
            }

            @Override
            public void onCursor(int d) {
                int p = Math.max(0, Math.min(et.length(), et.getSelectionEnd() + d));
                et.setSelection(p);
            }

            @Override
            public void onToolbar(int i) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm == null) return;
                if (i == 1) {
                    ClipData d = cm.getPrimaryClip();
                    if (d != null && d.getItemCount() > 0) {
                        CharSequence t = d.getItemAt(0).coerceToText(SettingsActivity.this);
                        if (t != null && t.length() > 0) ins(t.toString());
                        else toast("Clipboard kosong");
                    } else {
                        toast("Clipboard kosong");
                    }
                } else if (i == 2) {
                    String t = et.getText().toString();
                    if (t.isEmpty()) {
                        toast("Tidak ada teks");
                    } else {
                        cm.setPrimaryClip(ClipData.newPlainText("Yinlin Keys", t));
                        toast("Teks disalin");
                    }
                } else if (i == 3) {
                    et.setText("");
                }
            }

            @Override
            public void onSuggestion(String s) {
                if (s.length() > 0 && s.charAt(0) == KeyboardView.CLIP) ins(s.substring(1));
                else ins(s + " ");
            }
        });
        root.addView(kv, new LinearLayout.LayoutParams(MATCH, WRAP));

        // latar
        head(root, "Latar gambar");
        bgChips = chips(root, new String[]{"Yinlin aksi", "Yinlin potret", "Foto galeri", "Polos"}, P.bg(), 2, new Cb() {
            @Override
            public void on(int v) {
                if (v == 2 && !new File(getFilesDir(), "bg.jpg").exists()) {
                    pickPhoto();
                    return;
                }
                setI("bg", v);
            }
        });
        root.addView(btn("Pilih foto dari galeri", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pickPhoto();
            }
        }), lp(10));
        slider(root, "Gelapkan latar", 0, 80, P.dim(), "%", new Cb() {
            @Override
            public void on(int v) {
                setI("dim", v);
            }
        });

        // tema
        head(root, "Tema warna");
        chips(root, Prefs.THEME_NAMES, P.theme(), 4, new Cb() {
            @Override
            public void on(int v) {
                setI("theme", v);
            }
        });

        // ukuran
        head(root, "Ukuran keyboard");
        chips(root, new String[]{"Kecil", "Sedang", "Besar"}, P.size(), 3, new Cb() {
            @Override
            public void on(int v) {
                setI("size", v);
            }
        });

        // fitur cepat
        head(root, "Fitur cepat");
        sw(root, "Saran kata", P.sug(), new Cb() {
            @Override
            public void on(int v) {
                setB("sug", v == 1);
            }
        });
        sw(root, "Tempel otomatis dari clipboard", P.paste(), new Cb() {
            @Override
            public void on(int v) {
                setB("paste", v == 1);
            }
        });
        sw(root, "Getar", P.haptic(), new Cb() {
            @Override
            public void on(int v) {
                setB("haptic", v == 1);
            }
        });
        sw(root, "Suara klik", P.sound(), new Cb() {
            @Override
            public void on(int v) {
                setB("sound", v == 1);
            }
        });

        // lanjutan
        final LinearLayout adv = new LinearLayout(this);
        adv.setOrientation(LinearLayout.VERTICAL);
        adv.setVisibility(View.GONE);
        final TextView advH = txt("▸ Pengaturan lanjutan", 15, TXT);
        advH.setPadding(0, dp(14), 0, dp(14));
        advH.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                boolean show = adv.getVisibility() != View.VISIBLE;
                adv.setVisibility(show ? View.VISIBLE : View.GONE);
                advH.setText(show ? "▾ Pengaturan lanjutan" : "▸ Pengaturan lanjutan");
            }
        });
        root.addView(advH, lp(20));
        root.addView(adv, new LinearLayout.LayoutParams(MATCH, WRAP));

        slider(adv, "Transparansi tombol", 30, 100, P.keyOpacity(), "%", new Cb() {
            @Override
            public void on(int v) {
                setI("ko", v);
            }
        });
        slider(adv, "Lengkung tombol", 0, 26, P.radius(), "dp", new Cb() {
            @Override
            public void on(int v) {
                setI("rad", v);
            }
        });
        slider(adv, "Jarak antar tombol", 0, 10, P.gap(), "dp", new Cb() {
            @Override
            public void on(int v) {
                setI("gap", v);
            }
        });
        slider(adv, "Jarak bawah keyboard", 0, 40, P.bottom(), "dp", new Cb() {
            @Override
            public void on(int v) {
                setI("bot", v);
            }
        });
        slider(adv, "Waktu tahan tombol", 200, 700, P.longMs(), " ms", new Cb() {
            @Override
            public void on(int v) {
                setI("lp", v);
            }
        });
        slider(adv, "Kekuatan getar", 4, 40, P.vibMs(), " ms", new Cb() {
            @Override
            public void on(int v) {
                setI("vi", v);
            }
        });
        adv.addView(txt("Mode satu tangan", 14, TXT), lp(14));
        chips(adv, new String[]{"Mati", "Kiri", "Kanan"}, P.one(), 3, new Cb() {
            @Override
            public void on(int v) {
                setI("one", v);
            }
        });
        sw(adv, "Baris angka", P.nums(), new Cb() {
            @Override
            public void on(int v) {
                setB("nums", v == 1);
            }
        });
        sw(adv, "Huruf kapital otomatis", P.cap(), new Cb() {
            @Override
            public void on(int v) {
                setB("cap", v == 1);
            }
        });
        sw(adv, "Pratinjau tombol saat ditekan", P.popup(), new Cb() {
            @Override
            public void on(int v) {
                setB("popup", v == 1);
            }
        });
        sw(adv, "Petunjuk karakter tahan", P.hint(), new Cb() {
            @Override
            public void on(int v) {
                setB("hint", v == 1);
            }
        });
        sw(adv, "Huruf tebal", P.bold(), new Cb() {
            @Override
            public void on(int v) {
                setB("bold", v == 1);
            }
        });

        root.addView(btn("Reset semua pengaturan", new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                P.sp.edit().clear().apply();
                recreate();
            }
        }), lp(20));
        TextView note = txt("Tips: tahan tombol baris atas untuk angka, tahan koma atau titik untuk tanda baca, geser di spasi untuk memindah kursor, ketuk shift dua kali untuk caps lock.", 12, SUB);
        root.addView(note, lp(14));
    }

    private void ins(String s) {
        int a = Math.max(0, et.getSelectionStart()), b = Math.max(0, et.getSelectionEnd());
        int lo = Math.min(a, b), hi = Math.max(a, b);
        et.getText().replace(lo, hi, s);
        et.setSelection(lo + s.length());
    }

    private void pickPhoto() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("image/*");
        i.addCategory(Intent.CATEGORY_OPENABLE);
        try {
            startActivityForResult(i, REQ_BG);
        } catch (Throwable e) {
            toast("Tidak ada aplikasi galeri");
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_BG || res != RESULT_OK || data == null || data.getData() == null) return;
        final Uri u = data.getData();
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean ok = saveBg(u);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (ok) {
                            P.sp.edit().putInt("bg", 2).apply();
                            select(bgChips, 2);
                            kv.reload();
                            toast("Latar dari galeri dipasang");
                        } else {
                            toast("Gagal memuat foto");
                        }
                    }
                });
            }
        }).start();
    }

    private boolean saveBg(Uri u) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream in = getContentResolver().openInputStream(u);
            if (in == null) return false;
            BitmapFactory.decodeStream(in, null, o);
            in.close();
            int s = 1;
            while (Math.max(o.outWidth, o.outHeight) / (s * 2) >= 1280) s *= 2;
            o.inJustDecodeBounds = false;
            o.inSampleSize = s;
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            in = getContentResolver().openInputStream(u);
            if (in == null) return false;
            Bitmap b = BitmapFactory.decodeStream(in, null, o);
            in.close();
            if (b == null) return false;
            int rot = 0;
            if (Build.VERSION.SDK_INT >= 24) {
                try {
                    in = getContentResolver().openInputStream(u);
                    if (in != null) {
                        ExifInterface ex = new ExifInterface(in);
                        int or = ex.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
                        in.close();
                        rot = or == ExifInterface.ORIENTATION_ROTATE_90 ? 90
                                : or == ExifInterface.ORIENTATION_ROTATE_180 ? 180
                                : or == ExifInterface.ORIENTATION_ROTATE_270 ? 270 : 0;
                    }
                } catch (Throwable ignored) {
                }
            }
            if (rot != 0) {
                Matrix m = new Matrix();
                m.postRotate(rot);
                Bitmap r = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
                b.recycle();
                b = r;
            }
            FileOutputStream out = new FileOutputStream(new File(getFilesDir(), "bg.jpg"));
            b.compress(Bitmap.CompressFormat.JPEG, 88, out);
            out.close();
            b.recycle();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }
}
