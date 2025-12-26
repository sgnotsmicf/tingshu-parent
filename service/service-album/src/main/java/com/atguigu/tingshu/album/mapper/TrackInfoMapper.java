package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface TrackInfoMapper extends BaseMapper<TrackInfo> {

    /**
     * 条件分页查询当前用户声音列表（包含声音统计信息）
     * @param pageInfo
     * @param trackInfoQuery
     * @return
     */
    Page<TrackListVo> getUserTrackPage(Page<TrackListVo> pageInfo, @Param("vo") TrackInfoQuery trackInfoQuery);
}
