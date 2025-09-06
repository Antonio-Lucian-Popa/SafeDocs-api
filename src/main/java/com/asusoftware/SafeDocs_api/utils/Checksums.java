package com.asusoftware.SafeDocs_api.utils;

import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.util.HexFormat;

public final class Checksums {
    private Checksums() {}
    public static String sha256(InputStream in) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        in.transferTo(new java.security.DigestOutputStream(OutputStream.nullOutputStream(), md));
        byte[] digest = md.digest();
        return HexFormat.of().formatHex(digest);
    }
}