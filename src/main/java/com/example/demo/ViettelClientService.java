package com.example.demo;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.util.AwsHostNameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.*;
import java.util.Date;

@Service
public class ViettelClientService {
    @Value("${viettelProperties.endpointUrl}")
    private String endpointUrl;
    @Value("${viettelProperties.bucketName}")
    private String bucketName;
    @Value("${viettelProperties.accessKey}")
    private String accessKey;
    @Value("${viettelProperties.secretKey}")
    private String secretKey;

    private AmazonS3 s3Client;
    private Logger logger = LoggerFactory.getLogger(AmazonClientService.class);

    @PostConstruct
    public void initializeS3() {
        logger.info("Viettel S3");
        logger.info("Endpoint : " + this.endpointUrl);
        logger.info("Bucket Name : " + this.bucketName);
        logger.info("Access Key: " + this.accessKey);
        logger.info("Secret Key: " + this.secretKey);
        this.s3Client = initS3Client();
    }

    public String uploadFile(MultipartFile multipartFile) {
        String fileName = "";
        try {
            File convertedFile = convertMultiPartToFile(multipartFile);
            fileName = generateFileName(multipartFile);
            logger.info("File Name: " + fileName);
            uploadFileToS3bucket(fileName, convertedFile);
            convertedFile.delete();
        } catch (AmazonServiceException ase) {
            logger.info("Caught an AmazonServiceException from GET requests, rejected reasons:");
            logger.info("Error Message:    " + ase.getMessage());
            logger.info("HTTP Status Code: " + ase.getStatusCode());
            logger.info("AWS Error Code:   " + ase.getErrorCode());
            logger.info("Error Type:       " + ase.getErrorType());
            logger.info("Request ID:       " + ase.getRequestId());
        }
//        catch (AmazonClientException ace) {
//            logger.info("Caught an AmazonClientException: ");
//            logger.info("Error Message: " + ace.getMessage());
//        }
        catch (IOException ioe) {
            logger.info("IOE Error Message: " + ioe.getMessage());

        }
        return fileName;
    }

    public S3Object getFileObject(String keyName) {
        S3Object s3Object = s3Client.getObject(bucketName, keyName);
        return s3Object;
    }

    public String deleteFileOnS3(String keyName) {
        s3Client.deleteObject(bucketName, keyName);
        return keyName;
    }

    private AmazonS3 initS3Client() {
        AWSCredentials credentials = new BasicAWSCredentials(
            accessKey,
            secretKey
        );
        //        AmazonS3 s3Client = new AmazonS3Client(credentials);
        AmazonS3 s3Client =  AmazonS3ClientBuilder
            .standard()
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
//            .withRegion(Regions.AP_SOUTHEAST_1)
            .withEndpointConfiguration(
                new AwsClientBuilder.EndpointConfiguration(
                    endpointUrl,
                    Regions.US_EAST_1.getName()
                )
            ).withPathStyleAccessEnabled(true)
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

    private void uploadFileToS3bucket(String fileName, File file) {

        PutObjectRequest uploadRequest = new PutObjectRequest(bucketName, fileName, file);
//        FileInputStream fis = new FileInputStream(file);
//        PutObjectRequest uploadRequest = new PutObjectRequest(bucketName, fileName, fis, new ObjectMetadata());
        s3Client.putObject(uploadRequest);
    }
}
