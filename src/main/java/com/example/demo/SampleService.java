package com.example.demo;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

@Service
public class SampleService {
    @Value("${viettelProperties.endpointUrl}")
    private String endpointUrl;
    @Value("${viettelProperties.bucketName}")
    private String bucketName;
    @Value("${viettelProperties.accessKey}")
    private String accessKey;
    @Value("${viettelProperties.secretKey}")
    private String secretKey;

    private Logger logger = LoggerFactory.getLogger(SampleService.class);
    AmazonS3Client s3Client;

    @PostConstruct
    public void initializeS3() {
        logger.info("Viettel S3");
        logger.info("Endpoint : " + this.endpointUrl);
        logger.info("Bucket Name : " + this.bucketName);
        logger.info("Access Key: " + this.accessKey);
        logger.info("Secret Key: " + this.secretKey);
        this.s3Client = initS3Client();
    }

    private AmazonS3Client initS3Client() {
        System.getProperty("javax.net.ssl.trustStore", "true");
        AmazonS3Client s3Client = new AmazonS3Client(new BasicAWSCredentials(accessKey, secretKey));
        S3ClientOptions options = new S3ClientOptions();
        options.setPathStyleAccess(true);
        s3Client.setEndpoint(endpointUrl);
        s3Client.setS3ClientOptions(options);
        return  s3Client;
    }

    public String uploadFile(MultipartFile multipartFile) {
        File convertedFile = null;
        try {
            convertedFile = convertMultiPartToFile(multipartFile);
        } catch (IOException e) {
            return "Request fail";
        }
        String fileName = generateFileName(multipartFile);
        boolean isUpload = uploadUsingS3Upload(convertedFile, fileName);
        convertedFile.delete();
        if (!isUpload) {
            return "Request fail";
        }
        return fileName;
    }

    private boolean uploadUsingS3Upload(File file, String keyName) {
        logger.info("Upload file : " + keyName);
        ArrayList<PartETag> partETags = new ArrayList<PartETag>();
        InitiateMultipartUploadResult initResponse = null;
        try {
            // Step 1: Initialize.
            logger.info("Step 1 : Initialize");
            InitiateMultipartUploadRequest initRequest = new
                InitiateMultipartUploadRequest(bucketName, keyName);
            initResponse = s3Client.initiateMultipartUpload(initRequest);
            logger.info("Upload ID :" + initResponse.getUploadId());

            // Step 2: Upload parts.
            logger.info("Step 2: Upload parts.");
            long filePosition = 0;
            long contentLength = file.length();
            long partSize = 5242880;
            for (int i = 1; filePosition < contentLength; i++) {
                // Last part can be less than 5 MB. Adjust part size.
                partSize = Math.min(partSize, (contentLength - filePosition));

                // Create request to upload a part.
                UploadPartRequest uploadRequest = new UploadPartRequest()
                    .withBucketName(bucketName).withKey(keyName)
                    .withUploadId(initResponse.getUploadId()).withPartNumber(i)
                    .withFileOffset(filePosition)
                    .withFile(file)
                    .withPartSize(partSize);

                logger.info("File position :" + filePosition);
                logger.info("Part Size :" + partSize);

                // Upload part and add response to our list.
                UploadPartResult result = s3Client.uploadPart(uploadRequest);

                partETags.add(result.getPartETag());

                filePosition += partSize;
            }

            // Step 3: Complete.
            logger.info("Step 3: Complete.");
            CompleteMultipartUploadRequest compRequest = new CompleteMultipartUploadRequest(
                bucketName,
                keyName,
                initResponse.getUploadId(),
                partETags);
            s3Client.completeMultipartUpload(compRequest);
        } catch (Exception e) {
            if (initResponse != null) {
                s3Client.abortMultipartUpload(new AbortMultipartUploadRequest(
                    bucketName, keyName, initResponse.getUploadId()));
            }
            logger.info("Uploaded failed");
            e.printStackTrace();
            return false;
        }

        logger.info("Uploaded successfully");
        return true;
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
}