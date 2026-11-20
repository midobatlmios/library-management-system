package com.library.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.util.HtmlUtils;

/** Small helper for the few HTML pages the backend serves itself (email links, dev pages). */
final class SimpleHtml {

    private SimpleHtml() {
    }

    static ResponseEntity<String> page(HttpStatus status, String title, String bodyHtml, String csp) {
        String html = """
                <!doctype html>
                <html lang="en"><head><meta charset="utf-8">
                <meta name="viewport" content="width=device-width, initial-scale=1">
                <title>%s</title>
                <style>
                  body{font-family:system-ui,sans-serif;background:#f4f5f7;color:#1d2129;margin:0;padding:24px 16px}
                  main{max-width:480px;margin:0 auto;background:#fff;border-radius:12px;padding:24px;box-shadow:0 1px 3px #0002}
                  h1{font-size:1.5rem;margin-top:0} .ok{color:#137333} .bad{color:#b3261e} .muted{color:#5f6673;font-size:.9rem}
                  pre{background:#f4f5f7;padding:12px;border-radius:8px;white-space:pre-wrap;word-break:break-all;font-size:.8rem}
                </style></head>
                <body><main>%s</main></body></html>
                """.formatted(esc(title), bodyHtml);
        return ResponseEntity.status(status)
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.noStore())
                .header("Content-Security-Policy", csp)
                .header("Referrer-Policy", "no-referrer")
                .body(html);
    }

    static ResponseEntity<String> message(HttpStatus status, boolean ok, String title, String text, String linkUrl) {
        String body = "<h1 class=\"" + (ok ? "ok" : "bad") + "\">" + esc(title) + "</h1><p>" + esc(text) + "</p>"
                + (linkUrl == null ? "" : "<p><a href=\"" + esc(linkUrl) + "\">Go to the library</a></p>");
        return page(status, title, body, "default-src 'none'; style-src 'unsafe-inline'");
    }

    static String esc(String s) {
        return s == null ? "" : HtmlUtils.htmlEscape(s);
    }
}
