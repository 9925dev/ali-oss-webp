package com.aliyunosswebp.service;

import com.aliyunosswebp.vo.FileBatchUrlsVO;
import com.aliyunosswebp.vo.FileUploadReqVO;
import com.aliyunosswebp.vo.OssFileRespVO;
import com.aliyunosswebp.vo.SysFileRelationVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface OssFileService {

    SysFileRelationVo upload(FileUploadReqVO uploadReqVO);

    String uploadStr(FileUploadReqVO uploadReqVO);

    String getUrl(String fileName);

    List<FileBatchUrlsVO> getUrls(List<String> fileNames);

    List<FileBatchUrlsVO> getPermanentUrl(List<String> fileNames);

    String uploadByBase64(String base64Str, String fileType);

    String uploadByBase64(String base64Data, String bucketName, String dir, String fileName, String fileType);

    String uploadStrApk(FileUploadReqVO uploadReqVO);

    OssFileRespVO uploadZip(FileUploadReqVO uploadReqVO);

    String uploadZipFile(MultipartFile file);

    String uploadZipBase64Method(String base64Data, String fileType);

    String uploadZipBase64(String base64Data, String fileType);
}
