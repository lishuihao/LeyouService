package com.leyou.upload.service;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.leyou.common.enums.ExceptionEnums;
import com.leyou.common.exception.LyException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class UploadService {

    private static final List<String> ALLOW_TYPES= Arrays.asList("image/jpeg","image/png","image/bmp");

    @Autowired
    FastFileStorageClient storageClient;

    public String uploadImage(MultipartFile file) {
        try {
            //校验文件类型
            String contentType = file.getContentType();
            if(!ALLOW_TYPES.contains(contentType)){
                throw new LyException(ExceptionEnums.FILE_TYPE_NOT_MATCH_ERROR);
            }

            //校验文件内容
            BufferedImage read = ImageIO.read(file.getInputStream());
            if(read == null){
                throw new LyException(ExceptionEnums.FILE_TYPE_NOT_MATCH_ERROR);
            }

            //获取后缀名
            String extension = StringUtils.substringAfterLast(file.getOriginalFilename(), ".");
            //上传到FASTDFS
            StorePath storePath = this.storageClient.uploadFile(
                    file.getInputStream(), file.getSize(), extension, null);
            return "http://image.leyou.com/"+ storePath.getFullPath();
        } catch (IOException e) {
            log.error("文件上传失败",e);
            throw new LyException(ExceptionEnums.FILE_UPLOAD_ERROR);
        }
    }
}
