package com.example.demo;


import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.URL;

@RestController
public class DemoController {
    private final static String PDF_URL_1 = "https://www.w3docs.com/uploads/media/default/0001/01/540cb75550adf33f281f29132dddd14fded85bfc.pdf";
    private final static String PDF_URL_2 = "https://docs.spring.io/spring-boot/docs/current/reference/pdf/spring-boot-reference.pdf";

    @Autowired
    private UrlService urlService;

    @Autowired
    private AmazonClientService s3ClientService;

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

    @PostMapping("/upload_file")
    public String uploadFile(@RequestParam("file") MultipartFile file) {
        return s3ClientService.uploadFile(file);
    }

    @GetMapping("/view_pdf")
    public void viewPdf(@RequestParam(value = "key", required=false)  String key, HttpServletResponse response) {
        String keyName = key;
        if (keyName == null) {
            keyName = "pdf_1.pdf-1597573243502";
        }
        S3Object fileObject = s3ClientService.getFileObject(keyName);
        S3ObjectInputStream is = fileObject.getObjectContent();
        try {
            int nRead;
            while ((nRead = is.read()) != -1) {
                response.getWriter().write(nRead);
            }
            fileObject.close();
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @GetMapping("/dowload_file")
    public ResponseEntity<byte[]> downloadFile(@RequestParam(value = "key", required=false)  String key) {
        String keyName = key;
        if (keyName == null) {
            keyName = "pdf_1.pdf-1597573243502";
        }
        byte[] content = null;
        S3Object fileObject = s3ClientService.getFileObject(keyName);
        S3ObjectInputStream is = fileObject.getObjectContent();
        try {
            content = IOUtils.toByteArray(is);
            fileObject.close();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Exception".getBytes());
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(content);
    }

    @GetMapping("/delete_file")
    public ResponseEntity<String> deleteFile(@RequestParam(value = "key", required=false)  String key) {
        String keyName = key;
        if (keyName == null) {
            keyName = "pdf_1.pdf-1597573243502";
        }
        s3ClientService.deleteFileOnS3(keyName);
        return ResponseEntity.ok().body(keyName);
    }

    private String getPdfUrl(String id) {
        if (id.equalsIgnoreCase("1")) {
            return PDF_URL_1;
        }
        return PDF_URL_2;
    }
}
