package com.aliyunosswebp.service;

import com.aliyunosswebp.utils.FileTypeUtils;
import com.aliyunosswebp.utils.OSSBase64Utils;
import com.aliyunosswebp.utils.OssUtil;
import com.aliyunosswebp.vo.FileBatchUrlsVO;
import com.aliyunosswebp.vo.FileUploadReqVO;
import com.aliyunosswebp.vo.OssFileRespVO;
import com.aliyunosswebp.vo.SysFileRelationVo;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.GenericResult;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.ProcessObjectRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class OssFileServiceImpl implements OssFileService {

    private final OssUtil ossUtil;

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    @Value("${aliyun.oss.directory}")
    private String directory;

    @Value("${aliyun.oss.original-image}")
    private String originalImage;

    @Value("${aliyun.oss.thumbnail-image}")
    private String thumbnailImage;

    @Value("${aliyun.oss.domain}")
    private String domain;

    @Value("${aliyun.oss.webp-quality:92}")
    private int webpQuality;

    public OssFileServiceImpl(OssUtil ossUtil) {
        this.ossUtil = ossUtil;
    }

    @Override
    public SysFileRelationVo upload(FileUploadReqVO uploadReqVO) {
        try {
            String ossPath = ossUtil.upload(uploadReqVO.getFile(), directory);
            String fileUrl = ossUtil.getFileUrl(ossPath, 24 * 60 * 60);
            SysFileRelationVo vo = new SysFileRelationVo();
            vo.setOssFileId(ossPath);
            vo.setAvatarUrl(fileUrl);
            return vo;
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new RuntimeException("上传头像失败");
        }
    }

    @Override
    public String uploadByBase64(String base64Data, String bucketName, String dir, String fileName, String fileType) {
        try {
            if (base64Data.startsWith("data:")) {
                base64Data = base64Data.substring(base64Data.indexOf(',') + 1);
            }
            byte[] decodedBytes = Base64.getDecoder().decode(base64Data);
            String ossPath = ossUtil.uploadByBase64(decodedBytes, bucketName, dir, fileName);
            return ossUtil.getPublicUrl(ossPath);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new RuntimeException("上传头像失败");
        }
    }

    @Override
    public String uploadByBase64(String base64Data, String fileType) {
        return uploadByBase64(base64Data, bucketName, directory, null, fileType);
    }

    @Override
    public String uploadStr(FileUploadReqVO uploadReqVO) {
        try {
            String ossPath = ossUtil.upload(uploadReqVO.getFile(), directory);
            log.info("uploadStr上传文件ossPath:{}", ossPath);
            return ossUtil.getPublicUrl(ossPath);
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new RuntimeException("上传头像失败");
        }
    }

    @Override
    public String getUrl(String fileName) {
        return ossUtil.getFileUrl(fileName, 24 * 60 * 60);
    }

    @Override
    public List<FileBatchUrlsVO> getUrls(List<String> fileNames) {
        return ossUtil.getUrlsList(fileNames);
    }

    @Override
    public List<FileBatchUrlsVO> getPermanentUrl(List<String> fileNames) {
        return ossUtil.getPublicUrlsList(fileNames);
    }

    @Override
    public String uploadStrApk(FileUploadReqVO uploadReqVO) {
        try {
            String ossPath = ossUtil.uploadApk(uploadReqVO.getFile(), directory);
            log.info("apk uploadStrApk上传文件ossPath:{}", ossPath);
            return ossUtil.getPublicUrlApk(ossPath);
        } catch (Exception e) {
            log.error("apk上传文件失败", e);
            throw new RuntimeException("上传头像失败");
        }
    }

    @Override
    public OssFileRespVO uploadZip(FileUploadReqVO uploadReqVO) {
        try {
            return uploadZipFileMethod(uploadReqVO.getFile());
        } catch (Exception e) {
            log.error("上传文件并压缩失败", e);
            throw new RuntimeException("上传文件并压缩失败");
        }
    }

    @Override
    public String uploadZipFile(MultipartFile file) {
        return uploadZipFileMethod(file).getThumbnailUrl();
    }

    public OssFileRespVO uploadZipFileMethod(MultipartFile file) {
        return ossUtil.uploadImageZip(file);
    }

    @Override
    public String uploadZipBase64Method(String base64Data, String fileType) {
        return uploadBase64AndConvertToWebp(base64Data, fileType).getThumbnailUrl();
    }

    @Override
    public String uploadZipBase64(String base64Data, String fileType) {
        OssFileRespVO ossFileRespVO = uploadBase64AndConvertToWebp(base64Data, fileType);
        log.info("=======返回图片地址是：{}======", ossFileRespVO.getOriginalUrl());
        return ossFileRespVO.getOriginalUrl();
    }

    private OssFileRespVO uploadBase64AndConvertToWebp(String base64Data, String fileType) {
        ParsedBase64Image parsed = parseBase64Image(base64Data);
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String originalOssKey = originalImage + uuid + parsed.extension;
        String webpOssKey = thumbnailImage + uuid + ".webp";
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try (InputStream inputStream = new ByteArrayInputStream(parsed.bytes)) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(parsed.contentType);
            ossClient.putObject(bucketName, originalOssKey, inputStream, metadata);

            String processRule = buildWebpSaveAsProcessRule(webpOssKey);
            ProcessObjectRequest processRequest = new ProcessObjectRequest(bucketName, originalOssKey, processRule);
            GenericResult result = ossClient.processObject(processRequest);
            if (result.getResponse().getStatusCode() != 200) {
                throw new RuntimeException("base64DataOSS生成WebP失败，状态码：" + result.getResponse().getStatusCode());
            }

            OssFileRespVO ossFileRespVO = new OssFileRespVO();
            ossFileRespVO.setOriginalUrl(domain + "/" + originalOssKey);
            ossFileRespVO.setThumbnailUrl(domain + "/" + webpOssKey);
            ossFileRespVO.setOriginalPath(originalOssKey);
            ossFileRespVO.setThumbnailPath(webpOssKey);
            ossFileRespVO.setOriginalFilename(originalOssKey);
            ossFileRespVO.setThumbnailFilename(webpOssKey);
            return ossFileRespVO;
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("base64Data上传文件失败", e);
            throw new RuntimeException("base64Data上传文件失败");
        } finally {
            ossClient.shutdown();
        }
    }

    private String buildWebpSaveAsProcessRule(String webpOssKey) {
        int quality = Math.min(100, Math.max(1, webpQuality));
        String encodedKey = OSSBase64Utils.encode(webpOssKey);
        String encodedBucket = OSSBase64Utils.encode(bucketName);
        return String.format("image/quality,q_%d/format,webp|sys/saveas,o_%s,b_%s", quality, encodedKey, encodedBucket);
    }

    private ParsedBase64Image parseBase64Image(String base64Data) {
        String contentType = null;
        String payload = base64Data;
        if (base64Data.startsWith("data:")) {
            int comma = base64Data.indexOf(',');
            if (comma < 0) {
                throw new IllegalArgumentException("Base64 Data URL 格式非法");
            }
            String meta = base64Data.substring(5, comma);
            int semi = meta.indexOf(';');
            contentType = semi >= 0 ? meta.substring(0, semi) : meta;
            payload = base64Data.substring(comma + 1);
        }
        byte[] bytes = Base64.getDecoder().decode(payload);
        if (StringUtils.isBlank(contentType)) {
            contentType = FileTypeUtils.getMineType(bytes);
        }
        String extension = extensionFromContentType(contentType);
        return new ParsedBase64Image(bytes, contentType, extension);
    }

    private static String extensionFromContentType(String contentType) {
        if (contentType == null) {
            return ".jpg";
        }
        return switch (contentType) {
            case "image/jpeg", "image/jpg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/gif" -> ".gif";
            case "image/bmp" -> ".bmp";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    private static final class ParsedBase64Image {
        private final byte[] bytes;
        private final String contentType;
        private final String extension;

        private ParsedBase64Image(byte[] bytes, String contentType, String extension) {
            this.bytes = bytes;
            this.contentType = contentType;
            this.extension = extension;
        }
    }
}
