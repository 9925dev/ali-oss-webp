package com.aliyunosswebp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "批量 URL 响应项")
public class FileBatchUrlsVO {

    @Schema(description = "文件 ID 或 OSS 对象路径", example = "dir/abc123.jpg")
    private String id;

    @Schema(description = "访问 URL", example = "https://bucket.oss-cn-beijing.aliyuncs.com/dir/abc123.jpg")
    private String url;
}
