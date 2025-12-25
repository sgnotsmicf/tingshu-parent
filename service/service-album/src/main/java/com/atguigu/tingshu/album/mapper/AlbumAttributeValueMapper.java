package com.atguigu.tingshu.album.mapper;

import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AlbumAttributeValueMapper extends BaseMapper<AlbumAttributeValue> {

    int insertList(@Param("albumAttributeValueList") List<AlbumAttributeValue> albumAttributeValueList);
}
