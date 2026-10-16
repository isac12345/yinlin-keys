package com.yinlin.keys;

import org.junit.Test;
import static org.junit.Assert.*;

public class DictTest {
    @Test public void saran_saya() {
        String[] out = new String[3];
        assertTrue(Dict.fill("sa", out, 0) > 0);
        assertEquals("saya", out[0]);
    }
    @Test public void saran_kosong_tidak_cocok() {
        String[] out = new String[3];
        assertEquals(0, Dict.fill("zzz", out, 0));
    }
    @Test public void default_ada_isi() {
        assertTrue(Dict.DEF.length > 0);
    }
    @Test public void kamus_cukup_banyak() {
        assertTrue(Dict.W.length >= 200);
    }
}
