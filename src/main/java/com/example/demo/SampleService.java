package com.example.demo;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.S3ClientOptions;
import com.amazonaws.services.s3.model.*;
import com.amazonaws.util.IOUtils;

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
    AmazonS3 s3Client;

    @PostConstruct
    public void initializeS3() {
        logger.info("Viettel S3");
        logger.info("Endpoint : " + this.endpointUrl);
        logger.info("Bucket Name : " + this.bucketName);
        logger.info("Access Key: " + this.accessKey);
        logger.info("Secret Key: " + this.secretKey);
        this.s3Client = initS3Client();
    }

    private AmazonS3 initS3Client() {
        AWSCredentials credentials = new BasicAWSCredentials(
                accessKey,
                secretKey
            );
            ClientConfiguration config = new ClientConfiguration();
            config.setSignerOverride("S3SignerType");
            AmazonS3 s3Client =  AmazonS3ClientBuilder
                .standard()
                .withCredentials(new AWSStaticCredentialsProvider(credentials))
                .withEndpointConfiguration(
                    new AwsClientBuilder.EndpointConfiguration(
                        endpointUrl,
                        null
                    )
                )
                .enablePathStyleAccess()
                .withClientConfiguration(config)
                .build();
            return s3Client;
    }

    public String uploadFile(MultipartFile multipartFile) {
        File convertedFile = null;
        try {
            convertedFile = convertMultiPartToFile(multipartFile);
        } catch (IOException e) {
            return "Converted fail";
        }
        String fileName = generateFileName(multipartFile);
        boolean isUpload = uploadUsingS3Upload(convertedFile, fileName);
        convertedFile.delete();
        if (!isUpload) {
            return "Upload fail";
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
    
    public byte[] getFileObject(String keyName) throws IOException {
    	byte[] content = null;
        S3Object s3Object = s3Client.getObject(bucketName, keyName);
        S3ObjectInputStream is = s3Object.getObjectContent();
        content = IOUtils.toByteArray(is);
        s3Object.close();
        return content;
    }

    public String deleteFileOnS3(String keyName) {
        s3Client.deleteObject(bucketName, keyName);
        return keyName;
    }
}
