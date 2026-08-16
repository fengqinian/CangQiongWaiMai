package com.sky.config;

import com.sky.properties.AliOssProperties;
import com.sky.utils.AliOssUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云OSS配置
 */
@Configuration
@Slf4j
public class OSSConfiguration {

    private final String accessKeyId = System.getenv("OSS_ACCESS_KEY_ID");
    private final String accessKeySecret = System.getenv("OSS_ACCESS_KEY_SECRET");

    @Bean
    @ConditionalOnMissingBean
    public AliOssUtil aliOssUtil(AliOssProperties aliOssProperties) {
        log.info("开始创建阿里云文件上传工具类对象: {}", aliOssProperties);
        return new AliOssUtil(aliOssProperties.getEndpoint(),
                accessKeyId,
                accessKeySecret,
                aliOssProperties.getBucketName());
    }
}
