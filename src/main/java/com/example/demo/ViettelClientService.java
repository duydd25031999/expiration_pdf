package com.example.demo;

import com.amazonaws.*;
import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.AmazonS3Exception;
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
import java.util.Map;

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
    private Logger logger = LoggerFactory.getLogger(ViettelClientService.class);

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
            boolean isUpload = uploadFileToS3bucket(fileName, convertedFile);
            if (!isUpload) {
            	fileName = "Upload fail";
            }
            convertedFile.delete();
        } catch (AmazonServiceException ase) {
            logger.info("Caught an AmazonServiceException from GET requests, rejected reasons:");
            logger.info("Error Message:    " + ase.getMessage());
            logger.info("HTTP Status Code: " + ase.getStatusCode());
            logger.info("AWS Error Code:   " + ase.getErrorCode());
            logger.info("Error Type:       " + ase.getErrorType());
            logger.info("Request ID:       " + ase.getRequestId());
            logger.info("Http Reques");
            Map<String, String> httpHeaders = ase.getHttpHeaders();
            for (String key : httpHeaders.keySet()) {
            	logger.info(key + " = " + httpHeaders.get(key));
            }
            logger.info("Response:       " + ase.getRawResponseContent());
            
            return "Request fail";
        }
        catch (AmazonClientException ace) {
            logger.info("Caught an AmazonClientException: ");
            logger.info("Error Message: " + ace.getMessage());
            return "Request fail";
        }
        catch (IOException ioe) {
            logger.info("IOE Error Message: " + ioe.getMessage());
            return "Request fail";
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
//        System.setProperty(SDKGlobalConfiguration.DISABLE_CERT_CHECKING_SYSTEM_PROPERTY, "true");
        AWSCredentials credentials = new BasicAWSCredentials(
            accessKey,
            secretKey
        );
//                AmazonS3 s3Client = new AmazonS3Client(credentials);
//                s3Client.setEndpoint(endpointUrl);
        ClientConfiguration config = new ClientConfiguration();
        config.setSignerOverride("S3SignerType");
        AmazonS3 s3Client =  AmazonS3ClientBuilder
            .standard()
            .withCredentials(new AWSStaticCredentialsProvider(credentials))
//            .withRegion(Regions.AP_SOUTHEAST_1)
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

    private boolean uploadFileToS3bucket(String fileName, File file) {
    	try {
    		 PutObjectRequest uploadRequest = new PutObjectRequest(bucketName, fileName, file);
    		 ObjectMetadata md = new ObjectMetadata();
    		 md.setCacheControl("max-age=680400");
    		 md.setContentType("application/x-www-form-urlencoded");
    		 uploadRequest.setMetadata(md);
//           FileInputStream fis = new FileInputStream(file);
//           PutObjectRequest uploadRequest = new PutObjectRequest(bucketName, fileName, fis, new ObjectMetadata());
           s3Client.putObject(uploadRequest);
    	}
    	catch (AmazonS3Exception ase) {
    		logger.info("Caught an AmazonServiceException from GET requests, rejected reasons:");
            logger.info("Error Message:    " + ase.getMessage());
            logger.info("HTTP Status Code: " + ase.getStatusCode());
            logger.info("AWS Error Code:   " + ase.getErrorCode());
            logger.info("Error Type:       " + ase.getErrorType());
            logger.info("Request ID:       " + ase.getRequestId());
            logger.info("Http Reques");
            Map<String, String> httpHeaders = ase.getHttpHeaders();
            for (String key : httpHeaders.keySet()) {
            	logger.info(key + " = " + httpHeaders.get(key));
            }
            logger.info("Response:       " + ase.getRawResponseContent());
			return false;
		}
    	catch (Exception e) {
			e.printStackTrace();
			return false;
		}
       
        return true;
    }
   
}
