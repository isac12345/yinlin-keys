package com.yinlin.keys;

/** Kamus mini untuk saran kata (sengaja kecil agar ringan). */
final class Dict {
    static final String[] DEF = {"saya", "yang", "dan"};
    static final String[] W = {
        "saya", "aku", "kamu", "dia", "kami", "kita", "mereka", "yang", "dan", "atau", "dengan", "untuk",
        "dari", "ke", "di", "ini", "itu", "ada", "tidak", "bisa", "mau", "sudah", "belum", "lagi", "banget",
        "makasih", "terima", "kasih", "tolong", "maaf", "selamat", "pagi", "siang", "sore", "malam",
        "kapan", "dimana", "kenapa", "bagaimana", "gimana", "bagus", "keren", "oke", "nanti", "sekarang",
        "besok", "kemarin", "hari", "jam", "rumah", "kerja", "makan", "minum", "tidur", "main", "game",
        "halo", "hai", "teman", "sama", "juga", "karena", "tapi", "kalau", "jadi", "harus", "boleh",
        "kembali", "pulang", "pergi", "datang", "lihat", "dengar", "bicara", "tunggu", "cepat", "lambat",
        "ringan", "berat", "lancar", "kustom", "keyboard", "aplikasi", "pesan", "telepon", "video", "foto",
        "musik", "lagu", "film", "belajar", "sekolah", "kuliah", "kantor", "uang", "harga", "murah",
        "mahal", "beli", "jual", "kirim", "terima", "balas", "baca", "tulis", "cari", "buka", "tutup",
        "hello", "thanks", "please", "sorry", "good", "morning", "night", "today", "tomorrow", "what",
        "when", "where", "why", "how", "with", "from", "have", "that", "this", "just", "really", "okay"
    };

    static int fill(String p, String[] out, int n) {
        for (int i = 0; i < W.length && n < out.length; i++) {
            String w = W[i];
            if (w.length() > p.length() && w.startsWith(p)) out[n++] = w;
        }
        return n;
    }
}
