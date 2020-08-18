package com.example.demo;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.amazonaws.util.IOUtils;
import io.minio.errors.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@RestController
public class ViettelController {
    @Autowired
    private ViettelClientService s3ClientService;

//    @Autowired
//    private MinIOService s3ClientService;

    @PostMapping("/viettel/upload_file")
    public String uploadFile(@RequestParam("file") MultipartFile file) throws IOException, InvalidResponseException, InvalidKeyException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, InsufficientDataException, ErrorResponseException {
        return s3ClientService.uploadFile(file);
    }

//    @GetMapping("/viettel/download_pdf")
//    public ResponseEntity<byte[]> downloadFile(@RequestParam(value = "key", required=false)  String key) {
//        String keyName = key;
//        if (keyName == null) {
//            keyName = "pdf_1.pdf-1597573243502";
//        }
//        byte[] content = null;
//        S3Object fileObject = s3ClientService.getFileObject(keyName);
//        S3ObjectInputStream is = fileObject.getObjectContent();
//        try {
//            content = IOUtils.toByteArray(is);
//            fileObject.close();
//        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Exception".getBytes());
//        }
//        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).body(content);
//    }
//
//    @GetMapping("/viettel/delete_file")
//    public ResponseEntity<String> deleteFile(@RequestParam(value = "key", required=false)  String key) {
//        String keyName = key;
//        if (keyName == null) {
//            keyName = "pdf_1.pdf-1597573243502";
//        }
//        s3ClientService.deleteFileOnS3(keyName);
//        return ResponseEntity.ok().body(keyName);
//    }
}
