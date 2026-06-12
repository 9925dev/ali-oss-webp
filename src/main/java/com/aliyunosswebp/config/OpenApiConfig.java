package com.aliyunosswebp.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Value("${server.port:8081}")
    private int serverPort;

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("阿里云 OSS 文件服务 API")
                        .description("提供文件上传、批量获取 URL、图片等质量压缩转 WebP 等能力。")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("infra-server")
                                .url("https://help.aliyun.com/zh/oss/user-guide/convert-image-formats-2")))
                .addServersItem(new Server()
                        .url("http://localhost:" + serverPort)
                        .description("本地开发环境"));
    }
}
