package com.aliyunosswebp.controller;

import com.aliyunosswebp.common.CommonResult;
import com.aliyunosswebp.service.OssFileService;
import com.aliyunosswebp.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;

@Slf4j
@Validated
@RestController
@RequestMapping("/infra/oss")
@Tag(name = "管理后台 - OSS 文件存储", description = "OSS 上传、URL 获取及 WebP 压缩相关接口")
public class OssFileController {

    private final OssFileService ossFileService;

    @Value("${spring.servlet.multipart.max-file-size:200MB}")
    private String maxFileSize;

    public OssFileController(OssFileService ossFileService) {
        this.ossFileService = ossFileService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件", description = "上传文件到 OSS，返回永久访问 URL")
    public CommonResult<String> uploadFile(@ParameterObject FileUploadReqVO uploadReqVO) {
        long size = uploadReqVO.getFile().getSize();
        if (size > parseMaxFileSizeBytes()) {
            return CommonResult.error(400, "文件大小不能超过" + maxFileSize);
        }
        return CommonResult.success(ossFileService.uploadStr(uploadReqVO));
    }

    @PostMapping(value = "/upload-obj", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件（返回对象）", description = "上传文件到 OSS，返回包含临时 URL 的文件关联对象")
    public CommonResult<SysFileRelationVo> uploadFileObj(@ParameterObject FileUploadReqVO uploadReqVO) {
        return CommonResult.success(ossFileService.upload(uploadReqVO));
    }

    @GetMapping("/url")
    @Operation(summary = "获取文件临时 URL", description = "根据 OSS 对象路径生成带签名的临时访问 URL（有效期 24 小时）")
    public CommonResult<String> getUrl(
            @Parameter(description = "OSS 对象路径", example = "dir/abc123.jpg", required = true)
            @RequestParam("file") String file) {
        return CommonResult.success(ossFileService.getUrl(file));
    }

    @PostMapping("/urls")
    @Operation(summary = "批量获取临时 URL", description = "根据 OSS 对象路径列表批量生成签名临时 URL")
    public CommonResult<List<FileBatchUrlsVO>> getUrls(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "OSS 对象路径列表",
                    required = true,
                    content = @Content(schema = @Schema(example = "[\"dir/a.jpg\", \"dir/b.png\"]"))
            )
            @RequestBody List<String> fileNames) {
        return CommonResult.success(ossFileService.getUrls(fileNames));
    }

    @PostMapping("/urls-permanent")
    @Operation(summary = "批量获取永久 URL", description = "根据 OSS 对象路径列表生成公共读永久访问 URL")
    public CommonResult<List<FileBatchUrlsVO>> getPermanentUrl(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "OSS 对象路径列表",
                    required = true,
                    content = @Content(schema = @Schema(example = "[\"dir/a.jpg\", \"dir/b.png\"]"))
            )
            @RequestBody List<String> fileNames) {
        return CommonResult.success(ossFileService.getPermanentUrl(fileNames));
    }

    @PostMapping(value = "/upload/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件（MultipartFile）", description = "直接上传 multipart 文件，返回永久 URL")
    public CommonResult<String> uploadFileByPart(
            @Parameter(description = "待上传文件", required = true)
            @RequestPart("file") MultipartFile file) {
        FileUploadReqVO vo = new FileUploadReqVO();
        vo.setFile(file);
        vo.setPath(file.getOriginalFilename());
        return CommonResult.success(ossFileService.uploadStr(vo));
    }

    @PostMapping(value = "/uploadWxProfilePicture", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "微信头像上传", description = "上传微信头像文件，返回永久 URL")
    public CommonResult<String> uploadWxProfilePicture(@ParameterObject FileUploadReqVO uploadReqVO) {
        return CommonResult.success(ossFileService.uploadStr(uploadReqVO));
    }

    @PostMapping(value = "/uploadApk", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传 APK 文件", description = "上传 APK 到专用 Bucket，返回永久 URL")
    public CommonResult<String> uploadFileApk(@ParameterObject FileUploadReqVO uploadReqVO) {
        long size = uploadReqVO.getFile().getSize();
        if (size > parseMaxFileSizeBytes()) {
            return CommonResult.error(400, "文件大小不能超过" + maxFileSize);
        }
        return CommonResult.success(ossFileService.uploadStrApk(uploadReqVO));
    }

    @PostMapping(value = "/upload/zip", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传并压缩为 WebP", description = "上传原图到 original_image/，由 OSS 等质量压缩后持久化到 thumbnail_image/，返回原图与 WebP 地址")
    public CommonResult<OssFileRespVO> uploadFileZip(@ParameterObject FileUploadReqVO uploadReqVO) {
        long size = uploadReqVO.getFile().getSize();
        if (size > parseMaxFileSizeBytes()) {
            return CommonResult.error(500, "文件大小不能超过" + maxFileSize);
        }
        return CommonResult.success(ossFileService.uploadZip(uploadReqVO));
    }

    @PostMapping(value = "/upload/zip1", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传并压缩（仅返回 WebP URL）", description = "上传图片并压缩为 WebP，仅返回压缩图永久 URL")
    public CommonResult<String> uploadFileZipSimple(
            @Parameter(description = "待压缩图片", required = true)
            @RequestParam("file") MultipartFile file) {
        long size = file.getSize();
        if (size > parseMaxFileSizeBytes()) {
            return CommonResult.error(500, "文件大小不能超过" + maxFileSize);
        }
        return CommonResult.success(ossFileService.uploadZipFile(file));
    }

    @GetMapping("/base64")
    @Operation(summary = "文本 Base64 编码", description = "将文本进行 URL 安全的 Base64 编码")
    public CommonResult<String> base64(
            @Parameter(description = "待编码文本", example = "hello", required = true)
            @RequestParam("text") String text) {
        String encodedContent = Base64.getEncoder().encodeToString(text.getBytes(StandardCharsets.UTF_8))
                .replace("+", "-").replace("/", "_").replace("=", "");
        return CommonResult.success(encodedContent);
    }

    @PostMapping("/getUrlWatermark")
    @Operation(summary = "Base64 上传并加水印", description = "Base64 图片上传、转 WebP 后拼接 OSS 水印处理参数")
    public String getUrlWatermark(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Base64 图片数据", required = true)
            @RequestBody GetUrlWatermarkReqVO getUrlWatermarkReqVO) {
        String result = ossFileService.uploadZipBase64Method(getUrlWatermarkReqVO.getBase64Data(), "getUrlWatermark");
        return result + "?x-oss-process=image/watermark,image_ZGlyLzczYTllMzk2M2YwMDQ0NTNiNjcyZTg2NTcwYmVjMzFiLnBuZw";
    }

    private long parseMaxFileSizeBytes() {
        return Long.parseLong(maxFileSize.replaceAll("MB", "")) * 1024L * 1024L;
    }
}
