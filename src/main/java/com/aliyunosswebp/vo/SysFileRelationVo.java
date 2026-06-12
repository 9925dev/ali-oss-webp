package com.aliyunosswebp.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

@Data
@Schema(description = "系统文件关联信息")
public class SysFileRelationVo implements Serializable {

    private static final long serialVersionUID = 1L;

    @Schema(description = "文件关联 ID")
    private String id;

    @Schema(description = "文件名称")
    private String fileName;

    @Schema(description = "文件 ID")
    private String fileId;

    @Schema(description = "文件路径")
    private String filePath;

    @Schema(description = "关联文件 ID")
    private String relatedFileId;

    @Schema(description = "关联类型")
    private String relationType;

    @Schema(description = "文件大小（字节）")
    private Long fileSize;

    @Schema(description = "关联名称")
    private String relationName;

    @Schema(description = "关联描述")
    private String description;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "MIME 类型")
    private String mimeType;

    @Schema(description = "OSS 文件 ID（对象路径）", example = "dir/abc123.jpg")
    private String ossFileId;

    @Schema(description = "业务类型")
    private String businessType;

    @Schema(description = "业务 ID")
    private String businessId;

    @Schema(description = "创建时间")
    private Date createTime;

    @Schema(description = "更新时间")
    private Date updateTime;

    @Schema(description = "创建人")
    private String createBy;

    @Schema(description = "更新人")
    private String updateBy;

    @Schema(description = "OSS 路径")
    private String ossPath;

    @Schema(description = "状态：0-禁用，1-启用")
    private Integer status;

    @Schema(description = "临时访问 URL")
    private String avatarUrl;
}
