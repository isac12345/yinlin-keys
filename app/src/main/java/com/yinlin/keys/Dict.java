package com.yinlin.keys;

/** Kamus mini untuk saran kata (sengaja kecil agar ringan). */
final class Dict {
    static final String[] DEF = {"saya", "yang", "dan"};
    static final String[] W = {
        "saya", "yang", "dan", "aku", "kamu", "dia", "kami", "kita",
        "mereka", "anda", "kalian", "nya", "ku", "mu", "atau", "dengan",
        "untuk", "dari", "ke", "di", "ini", "itu", "ada", "karena",
        "tapi", "kalau", "jadi", "harus", "boleh", "saja", "juga", "kah",
        "pun", "sampai", "agar", "tidak", "bisa", "mau", "sudah", "belum",
        "lagi", "pergi", "datang", "lihat", "dengar", "bicara", "tunggu", "buka",
        "tutup", "kirim", "baca", "tulis", "cari", "pakai", "taruh", "ambil",
        "beri", "simpan", "hapus", "ubah", "gerak", "duduk", "tidur", "makan",
        "minum", "kerja", "main", "pulang", "kembali", "naik", "turun", "keluar",
        "masuk", "lanjut", "berhenti", "bagus", "keren", "ringan", "berat", "cepat",
        "lambat", "mudah", "sulit", "cantik", "jernih", "benar", "salah", "baru",
        "lama", "tinggi", "rendah", "luas", "kecil", "besar", "jauh", "dekat",
        "banget", "agak", "mohon", "silakan", "maaf", "terima", "kasih", "makasih",
        "tolong", "selamat", "semoga", "mungkin", "pasti", "sedikit", "banyak", "semua",
        "hampir", "hari", "jam", "menit", "detik", "pagi", "siang", "sore",
        "malam", "nanti", "sekarang", "besok", "kemarin", "lusa", "tadi", "segera",
        "sambil", "sebelum", "sesudah", "waktu", "tanggal", "rumah", "sekolah", "kampus",
        "kantor", "toko", "warung", "cafe", "pasar", "jalan", "tempat", "negara",
        "kota", "desa", "dunia", "kamar", "meja", "kursi", "pintu", "jendela",
        "kuliah", "belajar", "bermain", "game", "tugas", "ujian", "kelas", "latihan",
        "hp", "aplikasi", "pesan", "foto", "video", "musik", "lagu", "film",
        "berita", "uang", "harga", "mobil", "motor", "sepeda", "baju", "sepatu",
        "topi", "tas", "buku", "kertas", "botol", "chat", "klik", "scroll",
        "login", "password", "akun", "server", "internet", "sinyal", "baterai", "email",
        "hello", "hi", "thanks", "please", "sorry", "good", "morning", "night",
        "today", "tomorrow", "yesterday", "what", "when", "where", "why", "how",
        "with", "from", "have", "that", "this", "just", "really", "okay",
        "yeah", "nice", "cool", "love", "like", "want", "need", "make",
        "take", "give", "help", "time", "day", "home", "food", "water",
        "money", "work", "play", "read", "write", "send", "open", "close",
        "start", "stop", "yes", "no", "ok", "done",
    };

    static int fill(String p, String[] out, int n) {
        for (int i = 0; i < W.length && n < out.length; i++) {
            String w = W[i];
            if (w.length() > p.length() && w.startsWith(p)) out[n++] = w;
        }
        return n;
    }
}