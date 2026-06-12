package com.aliyunosswebp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "Base64 水印上传请求")
public class GetUrlWatermarkReqVO {

    @Schema(description = "Base64 图片数据，支持 data:image/png;base64,... 格式", requiredMode = Schema.RequiredMode.REQUIRED)
    private String base64Data;
}
