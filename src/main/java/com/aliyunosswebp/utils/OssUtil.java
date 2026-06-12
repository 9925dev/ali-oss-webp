package com.aliyunosswebp.utils;

import com.alibaba.fastjson.JSON;
import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.comm.ResponseMessage;
import com.aliyun.oss.model.*;
import com.aliyunosswebp.vo.FileBatchUrlsVO;
import com.aliyunosswebp.vo.OssFileRespVO;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Slf4j
@Data
@Component
public class OssUtil {

    @Value("${aliyun.oss.endpoint}")
    private String endpoint;

    @Value("${aliyun.oss.accessKeyId}")
    private String accessKeyId;

    @Value("${aliyun.oss.accessKeySecret}")
    private String accessKeySecret;

    @Value("${aliyun.oss.bucketName}")
    private String bucketName;

    @Value("${aliyun.oss.cyjBucketName}")
    private String cyjBucketName;

    @Value("${aliyun.oss.directory}")
    private String directory;

    @Value("${aliyun.oss.apkBucketName}")
    private String apkBucketName;

    @Value("${aliyun.oss.apkEndpoint}")
    private String apkEndpoint;

    @Value("${aliyun.oss.original-image}")
    private String originalImage;

    @Value("${aliyun.oss.thumbnail-image}")
    private String thumbnailImage;

    @Value("${aliyun.oss.domain}")
    private String domain;

    @Value("${aliyun.oss.webp-quality:92}")
    private int webpQuality;

    private static final int THREAD_POOL_SIZE = 10;
    private static final long URL_EXPIRE_TIME = 3600 * 1000L;

    public String upload(MultipartFile file, String dir) {
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
        String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
        String objectName = dir + "/" + fileName;

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            ossClient.putObject(bucketName, objectName, inputStream, metadata);
            return objectName;
        } catch (IOException e) {
            log.error("上传文件失败", e);
            throw new RuntimeException("上传文件失败");
        } finally {
            ossClient.shutdown();
        }
    }

    public String uploadByBase64(byte[] bytes, String targetBucketName, String dir, String originalFilename) {
        String suffix = ".png";
        String fileName = StringUtils.isBlank(originalFilename)
                ? UUID.randomUUID().toString().replace("-", "") + suffix
                : originalFilename + suffix;
        String objectName = dir + "/" + fileName;

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            InputStream inputStream = new ByteArrayInputStream(bytes);
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType("application/octet-stream");
            metadata.setContentLength(bytes.length);
            ossClient.putObject(targetBucketName, objectName, inputStream, metadata);
            return objectName;
        } catch (Exception e) {
            log.error("上传文件失败", e);
            throw new RuntimeException("上传文件失败");
        } finally {
            ossClient.shutdown();
        }
    }

    public String uploadByBase64(byte[] bytes, String dir) {
        return uploadByBase64(bytes, bucketName, dir, null);
    }

    public String getFileUrl(String objectName, long expiration) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            Date expireDate = new Date(System.currentTimeMillis() + expiration * 1000);
            GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, objectName);
            request.setExpiration(expireDate);
            URL url = ossClient.generatePresignedUrl(request);
            return url.toString();
        } finally {
            ossClient.shutdown();
        }
    }

    public void deleteFile(String objectName) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try {
            ossClient.deleteObject(bucketName, objectName);
        } finally {
            ossClient.shutdown();
        }
    }

    public byte[] getFileBytes(String objectName) {
        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try (InputStream inputStream = ossClient.getObject(bucketName, objectName).getObjectContent()) {
            return inputStream.readAllBytes();
        } catch (Exception e) {
            log.error("获取OSS文件字节数组失败: {}", e.getMessage(), e);
            return null;
        } finally {
            ossClient.shutdown();
        }
    }

    public Map<String, String> generateUrlsMultiThread(List<String> fileIds) {
        Map<String, String> urlMap = new ConcurrentHashMap<>();
        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        List<Future<Map<String, String>>> futures = new ArrayList<>();

        int batchSize = 100;
        for (int i = 0; i < fileIds.size(); i += batchSize) {
            List<String> batch = fileIds.subList(i, Math.min(i + batchSize, fileIds.size()));
            futures.add(executor.submit(() -> {
                OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
                Map<String, String> batchUrls = new HashMap<>();
                try {
                    for (String fileId : batch) {
                        batchUrls.put(fileId, generateSignedUrl(ossClient, fileId));
                    }
                } finally {
                    ossClient.shutdown();
                }
                return batchUrls;
            }));
        }

        for (Future<Map<String, String>> future : futures) {
            try {
                urlMap.putAll(future.get());
            } catch (Exception e) {
                log.error("批量生成签名URL失败", e);
            }
        }

        executor.shutdown();
        return urlMap;
    }

    public List<FileBatchUrlsVO> getUrlsList(List<String> fileIds) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return Collections.emptyList();
        }
        Map<String, String> urlMap = generateUrlsMultiThread(fileIds);
        if (urlMap.isEmpty()) {
            return Collections.emptyList();
        }
        List<FileBatchUrlsVO> result = new ArrayList<>();
        for (Map.Entry<String, String> entry : urlMap.entrySet()) {
            FileBatchUrlsVO vo = new FileBatchUrlsVO();
            vo.setId(entry.getKey());
            vo.setUrl(entry.getValue());
            result.add(vo);
        }
        return result;
    }

    public String generateSignedUrl(OSS ossClient, String fileId) {
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(bucketName, fileId);
        request.setExpiration(new Date(System.currentTimeMillis() + URL_EXPIRE_TIME));
        URL url = ossClient.generatePresignedUrl(request);
        return url.toString();
    }

    public String getPublicUrl(String ossFilePath) {
        return String.format("https://%s.%s/%s", bucketName, endpoint, ossFilePath);
    }

    public List<FileBatchUrlsVO> getPublicUrlsList(List<String> fileIds) {
        if (CollectionUtils.isEmpty(fileIds)) {
            return Collections.emptyList();
        }
        List<FileBatchUrlsVO> result = new ArrayList<>();
        for (String fileId : fileIds) {
            if (StringUtils.isBlank(fileId)) {
                continue;
            }
            FileBatchUrlsVO vo = new FileBatchUrlsVO();
            vo.setId(fileId);
            vo.setUrl(getPublicUrl(fileId));
            result.add(vo);
        }
        return result;
    }

    public String uploadApk(MultipartFile file, String dir) {
        String originalFilename = file.getOriginalFilename();
        String suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
        String fileName = UUID.randomUUID().toString().replace("-", "") + suffix;
        String objectName = dir + "/" + fileName;
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSupportCname(true);
        OSS ossClient = new OSSClientBuilder().build("https://" + apkEndpoint, accessKeyId, accessKeySecret, clientBuilderConfiguration);

        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            metadata.setContentLength(file.getSize());
            log.info("上传文件的桶是: {}", apkBucketName);
            ossClient.putObject(apkBucketName, objectName, inputStream, metadata);
            return objectName;
        } catch (IOException e) {
            log.error("apk上传文件失败", e);
            throw new RuntimeException("上传文件失败");
        } finally {
            ossClient.shutdown();
        }
    }

    public String getPublicUrlApk(String ossFilePath) {
        return String.format("https://%s/%s", apkEndpoint, ossFilePath);
    }

    /**
     * 上传原图并通过 OSS 图片处理等质量压缩为 WebP
     */
    public OssFileRespVO uploadImageZip(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        log.info("将图片压缩并上传开始 originalFilename={}", originalFilename);
        if (originalFilename == null || !originalFilename.matches("(?i).*\\.(jpg|jpeg|png|gif|bmp|webp)$")) {
            throw new IllegalArgumentException("仅支持jpg、jpeg、png、gif、bmp、webp格式");
        }

        String uuid = UUID.randomUUID().toString().replace("-", "");
        String originalExt = getFileExt(originalFilename);
        String originalOssKey = originalImage + uuid + originalExt;
        String webpOssKey = thumbnailImage + uuid + ".webp";
        log.info("将图片压缩并上传路径 originalOssKey={} webpOssKey={}", originalOssKey, webpOssKey);

        OSS ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
        try (InputStream inputStream = file.getInputStream()) {
            ObjectMetadata metadata = new ObjectMetadata();
            metadata.setContentType(file.getContentType());
            PutObjectResult putObjectResult = ossClient.putObject(bucketName, originalOssKey, inputStream, metadata);
            log.info("将图片压缩并上传原始图片返回结果 putObjectResult={}", JSON.toJSONString(putObjectResult));
            ResponseMessage response = putObjectResult.getResponse();
            if (response != null && response.getStatusCode() != 200) {
                throw new RuntimeException("OSS上传原始图片失败");
            }

            String encodedKey = OSSBase64Utils.encode(webpOssKey);
            String encodedBucket = OSSBase64Utils.encode(bucketName);
            int quality = Math.min(100, Math.max(1, webpQuality));
            String processRule = String.format(
                    "image/quality,q_%d/format,webp|sys/saveas,o_%s,b_%s",
                    quality,
                    encodedKey,
                    encodedBucket
            );

            ProcessObjectRequest processRequest = new ProcessObjectRequest(bucketName, originalOssKey, processRule);
            GenericResult result = ossClient.processObject(processRequest);
            log.info("将图片压缩并上传压缩图片返回结果 result={}", JSON.toJSONString(result));

            if (result.getResponse().getStatusCode() != 200) {
                throw new RuntimeException("OSS生成WebP失败，状态码：" + result.getResponse().getStatusCode());
            }

            OssFileRespVO ossFileRespVO = new OssFileRespVO();
            ossFileRespVO.setOriginalUrl(domain + "/" + originalOssKey);
            ossFileRespVO.setThumbnailUrl(domain + "/" + webpOssKey);
            ossFileRespVO.setOriginalPath(originalOssKey);
            ossFileRespVO.setThumbnailPath(webpOssKey);
            ossFileRespVO.setOriginalFilename(originalOssKey);
            ossFileRespVO.setThumbnailFilename(webpOssKey);

            log.info("将图片压缩并上传返回结果 ossFileRespVO={}", JSON.toJSONString(ossFileRespVO));
            return ossFileRespVO;
        } catch (Exception e) {
            log.error("将图片压缩并上传失败", e);
            throw new RuntimeException("上传文件失败");
        } finally {
            ossClient.shutdown();
        }
    }

    private String getFileExt(String filename) {
        return filename.substring(filename.lastIndexOf('.'));
    }
}
