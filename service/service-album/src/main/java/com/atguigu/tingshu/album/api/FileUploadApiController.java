package com.atguigu.tingshu.album.api;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import com.atguigu.tingshu.album.config.MinioConstantProperties;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import io.minio.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
@Slf4j
@Tag(name = "上传管理接口")
@RestController
@RequestMapping("api/album")
public class FileUploadApiController {

    @Autowired
    private MinioConstantProperties minioConstantProperties;

    @Operation(summary = "minio图片（封面、头像）文件上传")
    @PostMapping("/fileUpload")
    @SneakyThrows
    public Result fileUpload(@RequestParam("file") MultipartFile file){
        try {
            MinioClient minioClient =
                    MinioClient.builder()
                            .endpoint(minioConstantProperties.getEndpointUrl())
                            .credentials(minioConstantProperties.getAccessKey(), minioConstantProperties.getSecreKey())
                            .build();
            String OriginalFilename = file.getOriginalFilename();
            String bucketName = minioConstantProperties.getBucketName();
            //filename是 唯一标识  /时间/uuid+.后缀
            String fileName = "/" + DateUtil.today() + "/" +
                             UUID.randomUUID().toString(true)
                            + OriginalFilename.substring(OriginalFilename.lastIndexOf("."));

            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
            }
            //putObject 是网络上传  uploadObject是本地文件上传
            minioClient.putObject(
                    PutObjectArgs.builder().bucket(bucketName).object(fileName).stream(
                                    file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build());
            log.info("执行返回流程，返回地址:{}", minioConstantProperties.getEndpointUrl() + "/" + bucketName + fileName);
            return Result.ok(minioConstantProperties.getEndpointUrl() + "/" + bucketName + fileName);
        } catch (Exception e) {
            log.error("MinIO文件上传失败，原始异常：", e);
            throw new GuiguException(500, "文件上传失败");
        }
    }

}
