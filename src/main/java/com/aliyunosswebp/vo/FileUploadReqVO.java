package com.aliyunosswebp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
@Schema(description = "文件上传请求")
public class FileUploadReqVO {

    @Schema(description = "上传文件", type = "string", format = "binary", requiredMode = Schema.RequiredMode.REQUIRED)
    private MultipartFile file;

    @Schema(description = "自定义存储路径或文件名", example = "avatar.png")
    private String path;

    @Schema(description = "文件业务类型", example = "AVATAR")
    private String type;
}
