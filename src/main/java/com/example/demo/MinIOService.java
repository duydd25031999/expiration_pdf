package com.example.demo;

import com.amazonaws.SDKGlobalConfiguration;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

@Service
public class MinIOService {
    @Value("${viettelProperties.endpointUrl}")
    private String endpointUrl;
    @Value("${viettelProperties.bucketName}")
    private String bucketName;
    @Value("${viettelProperties.accessKey}")
    private String accessKey;
    @Value("${viettelProperties.secretKey}")
    private String secretKey;

    private MinioClient s3Client;
    private Logger logger = LoggerFactory.getLogger(MinIOService.class);

    @PostConstruct
    public void initializeS3() {
        this.s3Client = initS3Client();
    }

    public String uploadFile(MultipartFile multipartFile) throws IOException, InvalidResponseException, InvalidKeyException, NoSuchAlgorithmException, ServerException, ErrorResponseException, XmlParserException, InvalidBucketNameException, InsufficientDataException, InternalException {
        String fileName = "";
        File convertedFile = convertMultiPartToFile(multipartFile);
        fileName = generateFileName(multipartFile);
        logger.info("File Name: " + fileName);
        uploadFileToS3bucket(fileName, convertedFile);
        convertedFile.delete();
        return fileName;
    }

    private MinioClient initS3Client() {
        MinioClient s3Client = MinioClient.builder()
            .endpoint(endpointUrl)
            .credentials(accessKey, secretKey)
            .build();
        return s3Client;
    }

    private File convertMultiPartToFile(MultipartFile multipartFile) throws IOException {
        File convertedFile = new File(multipartFile.getOriginalFilename());
        FileOutputStream fileOutputStream = new FileOutputStream(convertedFile);
        fileOutputStream.write(multipartFile.getBytes());
        fileOutputStream.close();
        return convertedFile;
    }

    private String generateFileName(MultipartFile multiPart) {
        return new Date().getTime()  + "-" + multiPart.getOriginalFilename();
    }

    private void uploadFileToS3bucket(String fileName, File file) throws IOException, InvalidKeyException, InvalidResponseException, InsufficientDataException, NoSuchAlgorithmException, ServerException, InternalException, XmlParserException, InvalidBucketNameException, ErrorResponseException {
        FileInputStream fis = new FileInputStream(file);
        PutObjectArgs args = PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(fileName)
                                .stream(fis, file.length(), -1)
                                .contentType(MediaType.APPLICATION_PDF_VALUE)
                                .build();
        s3Client.putObject(args);
    }
}
