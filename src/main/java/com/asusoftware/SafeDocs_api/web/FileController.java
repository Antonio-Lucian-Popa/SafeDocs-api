package com.asusoftware.SafeDocs_api.web;

import com.asusoftware.SafeDocs_api.auth.CurrentUser;
import com.asusoftware.SafeDocs_api.auth.SimplePrincipal;
import com.asusoftware.SafeDocs_api.config.AppProperties;
import com.asusoftware.SafeDocs_api.domain.Document;
import com.asusoftware.SafeDocs_api.repo.DocumentRepository;
import com.asusoftware.SafeDocs_api.repo.FolderShareRepository;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
public class FileController {
    private final DocumentRepository docs;
    private final CurrentUser currentUser;
    private final AppProperties appProps;
    private final FolderShareRepository folderShares;

    @GetMapping("/{documentId}/view")
    public ResponseEntity<Resource> view(@AuthenticationPrincipal SimplePrincipal me,
                                         @PathVariable UUID documentId,
                                         @RequestHeader(value = "Range", required = false) String rangeHeader) throws IOException {
        var user = currentUser.require(me);
        Document d = docs.findById(documentId).orElseThrow();

        // owner sau share READ
        if (!d.getUser().getId().equals(user.getId())) {
            boolean shared = d.getFolder() != null &&
                    folderShares.existsByFolder_IdAndSharedWith_Id(d.getFolder().getId(), user.getId());
            if (!shared) return ResponseEntity.status(403).build();
        }

        Path abs = Paths.get(appProps.getStorage().getLocation()).resolve(d.getFilePath()).normalize();
        FileSystemResource resource = new FileSystemResource(abs);
        if (!resource.exists()) return ResponseEntity.notFound().build();

        String mime = d.getMimeType() != null ? d.getMimeType() : "application/octet-stream";

        // Range (video/audio)
        if (rangeHeader != null && (mime.startsWith("video/") || mime.startsWith("audio/"))) {
            long fileLength = resource.contentLength();
            var ranges = org.springframework.http.HttpRange.parseRanges(rangeHeader);
            if (!ranges.isEmpty()) {
                HttpRange r = ranges.get(0);
                long start = r.getRangeStart(fileLength);
                long end   = r.getRangeEnd(fileLength);
                long len   = end - start + 1;

                try (InputStream is = Files.newInputStream(abs)) {
                    is.skip(start);
                    byte[] bytes = is.readNBytes((int) len);
                    return ResponseEntity.status(206)
                            .header(HttpHeaders.CONTENT_TYPE, mime)
                            .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                            .header(HttpHeaders.CONTENT_RANGE, "bytes " + start + "-" + end + "/" + fileLength)
                            .contentLength(len)
                            .body(new ByteArrayResource(bytes));
                }
            }
        }

        // Inline pentru restul
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, mime)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(abs.getFileName().toString())
                        .build().toString())
                .body(resource);
    }

    @GetMapping("/{documentId}/thumbnail")
    public ResponseEntity<byte[]> thumbnail(@AuthenticationPrincipal SimplePrincipal me,
                                            @PathVariable UUID documentId,
                                            @RequestParam(defaultValue = "300") int w,
                                            @RequestParam(defaultValue = "200") int h) throws Exception {
        var user = currentUser.require(me);
        Document d = docs.findById(documentId).orElseThrow();
        if (!d.getUser().getId().equals(user.getId())) {
            boolean shared = d.getFolder() != null &&
                    folderShares.existsByFolder_IdAndSharedWith_Id(d.getFolder().getId(), user.getId());
            if (!shared) return ResponseEntity.status(403).build();
        }

        Path abs = Paths.get(appProps.getStorage().getLocation()).resolve(d.getFilePath()).normalize();
        if (!Files.exists(abs)) return ResponseEntity.notFound().build();

        String mime = d.getMimeType() != null ? d.getMimeType() : "application/octet-stream";

        BufferedImage thumb;
        if (mime.startsWith("image/")) {
            var img = javax.imageio.ImageIO.read(abs.toFile());
            thumb = scale(img, w, h);
        } else if ("application/pdf".equalsIgnoreCase(mime)) {
            try (PDDocument pdf = Loader.loadPDF(abs.toFile())) {
                PDFRenderer renderer = new PDFRenderer(pdf);
                BufferedImage first = renderer.renderImageWithDPI(0, 110); // DPI 110
                thumb = scale(first, w, h);
            }
        } else {
            thumb = placeholder(w, h, d.getTitle());
        }

        try (java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream()) {
            javax.imageio.ImageIO.write(thumb, "png", baos);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_TYPE, "image/png")
                    .body(baos.toByteArray());
        }
    }

    private BufferedImage scale(BufferedImage src, int w, int h) {
        var out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        var g = out.createGraphics();
        g.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return out;
    }

    private BufferedImage placeholder(int w, int h, String text) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0,0,w,h);
        g.setColor(Color.GRAY);
        g.drawRect(0,0,w-1,h-1);
        g.setFont(new Font("SansSerif", Font.PLAIN, Math.max(12, w/15)));
        FontMetrics fm = g.getFontMetrics();
        String display = text == null ? "" : (text.length() > 30 ? text.substring(0,30) + "..." : text);
        int tx = 8;
        int ty = h/2 + fm.getAscent()/2 - fm.getDescent();
        g.drawString(display, tx, ty);
        g.dispose();
        return out;
    }

    @GetMapping("/{documentId}/download")
    public ResponseEntity<FileSystemResource> download(@AuthenticationPrincipal SimplePrincipal me,
                                                       @PathVariable UUID documentId) throws IOException {
        var user = currentUser.require(me);
        Document d = docs.findById(documentId).orElseThrow();
        if (!d.getUser().getId().equals(user.getId())) return ResponseEntity.status(403).build();

        Path abs = Paths.get(appProps.getStorage().getLocation()).resolve(d.getFilePath()).normalize();
        var resource = new FileSystemResource(abs);
        if (!resource.exists()) return ResponseEntity.notFound().build();

        String filename = abs.getFileName().toString();
        var cd = ContentDisposition.attachment().filename(filename).build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentDisposition(cd);
        headers.setContentType(MediaType.parseMediaType(d.getMimeType() != null ? d.getMimeType() : MimeTypeUtils.APPLICATION_OCTET_STREAM_VALUE));
        headers.setContentLength(resource.contentLength());

        return ResponseEntity.ok().headers(headers).body(resource);
    }
}
