package com.asusoftware.SafeDocs_api.utils;


import org.apache.tika.Tika;

public final class MimeSniffer {
    private static final Tika TIKA = new Tika();
    private MimeSniffer() {}
    public static String detect(byte[] content, String fallback) {
        try { return TIKA.detect(content); } catch (Exception e) { return fallback; }
    }
}
