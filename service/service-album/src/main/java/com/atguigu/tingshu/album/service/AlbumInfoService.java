package com.atguigu.tingshu.album.service;

import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AlbumInfoService extends IService<AlbumInfo> {


    Result saveAlbumInfo(Long userId, AlbumInfoVo albumInfoVo);

    Page<AlbumListVo> findUserAlbumPage(Page<AlbumListVo> pageParam, AlbumInfoQuery albumInfoQuery);

    void removeAlbumInfo(Long id);

    AlbumInfo getAlbumInfo(Long id);

    void updateAlbumInfo(Long id, AlbumInfoVo albumInfoVo);

    List<AlbumInfo> getUserAllAlbumList(Long userId);
}
