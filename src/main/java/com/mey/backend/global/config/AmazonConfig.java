package com.mey.backend.global.config;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Getter
public class AmazonConfig {
    private AWSCredentials credentials;

    @Value("${cloud.aws.credentials.accessKey}")
    private String accessKey;

    private String secretKey;

    private String region;

    private String bucket;

    @PostConstruct
    public void init() {
        this.credentials = new BasicAWSCredentials("accessKey", "secretKey");
    }

    @Bean
    public AmazonS3 s3Client() {
        BasicAWSCredentials awsCredentials = new BasicAWSCredentials("accessKey", "secretKey");
        return AmazonS3ClientBuilder.standard()
                .withRegion("ap-northeast-2")
                .withCredentials(new AWSStaticCredentialsProvider(awsCredentials))
                .build();
    }

    @Bean
    public AWSCredentialsProvider awsCredentialsProvider() {
        return new AWSStaticCredentialsProvider(this.credentials);
    }

}
