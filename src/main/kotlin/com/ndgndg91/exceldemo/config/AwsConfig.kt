package com.ndgndg91.exceldemo.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.firehose.FirehoseAsyncClient

@Configuration
class AwsConfig(
    @Value("\${aws.region}") private val region: String,
    @Value("\${aws.credentials.access-key}") private val accessKey: String,
    @Value("\${aws.credentials.secret-key}") private val secretKey: String
) {

    @Bean
    fun firehoseAsyncClient(): FirehoseAsyncClient {
        val credentials = AwsBasicCredentials.create(accessKey, secretKey)
        return FirehoseAsyncClient.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }

    @Bean
    fun athenaAsyncClient(): software.amazon.awssdk.services.athena.AthenaAsyncClient {
        val credentials = AwsBasicCredentials.create(accessKey, secretKey)
        return software.amazon.awssdk.services.athena.AthenaAsyncClient.builder()
            .region(Region.of(region))
            .credentialsProvider(StaticCredentialsProvider.create(credentials))
            .build()
    }
}
