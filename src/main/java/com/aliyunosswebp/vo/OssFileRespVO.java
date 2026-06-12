package com.aliyunosswebp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "图片上传并压缩响应")
public class OssFileRespVO {

    @Schema(description = "原图访问 URL", example = "https://oss-prod.oss-cn-beijing.aliyuncs.com/original_image/xxx.jpg")
    private String originalUrl;

    @Schema(description = "原图 OSS 路径", example = "original_image/xxx.jpg")
    private String originalPath;

    @Schema(description = "WebP 压缩图访问 URL", example = "https://oss-prod.oss-cn-beijing.aliyuncs.com/thumbnail_image/xxx.webp")
    private String thumbnailUrl;

    @Schema(description = "WebP 压缩图 OSS 路径", example = "thumbnail_image/xxx.webp")
    private String thumbnailPath;

    @Schema(description = "原图文件名")
    private String originalFilename;

    @Schema(description = "WebP 压缩图文件名")
    private String thumbnailFilename;
}
