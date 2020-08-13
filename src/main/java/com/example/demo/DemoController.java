package com.example.demo;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;

@RestController
public class DemoController {
    private final static String PDF_URL_1 = "https://www.w3docs.com/uploads/media/default/0001/01/540cb75550adf33f281f29132dddd14fded85bfc.pdf";
    private final static String PDF_URL_2 = "https://docs.spring.io/spring-boot/docs/current/reference/pdf/spring-boot-reference.pdf";

    @Autowired
    private UrlService urlService;

    @GetMapping("/generate")
    public ResponseEntity<String> generateUrl(@RequestParam(value = "order", required=false)  String order) {
        String id = null;
        if (order == null) {
            id = "1";
        } else {
            id = order;
        }
        String url = urlService.generateDemoUrl(id);
        return ResponseEntity.ok(url);
    }

    @GetMapping("/pdf/{encrypt}")
    public void getPdf(@PathVariable String encrypt, HttpServletResponse response) {
        String file = urlService.getDataFromUrl(encrypt);
        if (file == null) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }
        try {
            String pdfUrl = getPdfUrl(file);
            response.setContentType("application/pdf");
            URL url = new URL(pdfUrl);
            InputStream is = url.openStream();
            int nRead;
            while ((nRead = is.read()) != -1) {
                response.getWriter().write(nRead);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }

    }

    private String getPdfUrl(String id) {
        if (id.equalsIgnoreCase("1")) {
            return PDF_URL_1;
        }
        return PDF_URL_2;
    }
}