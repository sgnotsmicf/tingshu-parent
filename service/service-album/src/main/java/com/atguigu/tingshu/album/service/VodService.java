package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface VodService {

    Map<String, String> uploadTrack(MultipartFile file);

    TrackMediaInfoVo getTrackMediaInfo(String mediaFileId);
    /**
     * 删除音视频文件
     * @param mediaFileId
     */
    void deleteMedia(String mediaFileId);
}
