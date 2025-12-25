package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;


@Slf4j
@Service
@SuppressWarnings({"all"})
public class AlbumInfoServiceImpl extends ServiceImpl<AlbumInfoMapper, AlbumInfo> implements AlbumInfoService {

	@Autowired
	private AlbumInfoMapper albumInfoMapper;

	@Autowired
	private AlbumStatMapper albumStatMapper;

	@Autowired
	private AlbumAttributeValueMapper albumAttributeValueMapper;

	/**
	 * 内容创作者或者平台运营人员-保存专辑
	 * @param userId 用户Id
	 * @param albumInfoVo 前端返回的专辑VO
	 * @return 成功与否，加上事务
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public Result saveAlbumInfo(Long userId, AlbumInfoVo albumInfoVo) {
		//1.保存专辑信息
		AlbumInfo albumInfo = new AlbumInfo();
		BeanUtils.copyProperties(albumInfoVo, albumInfo);
		albumInfo.setUserId(userId); //TODO:别忘了用户id
		if (!(albumInfoVo.getPayType() == SystemConstant.ALBUM_PAY_TYPE_FREE)) {
			//付费专辑
			albumInfo.setTracksForFree(1); //免费听一集吧
		}
		//暂时手动设置通过
		albumInfo.setStatus(SystemConstant.ALBUM_STATUS_NO_PASS);
		albumInfoMapper.insert(albumInfo);

		//2.保存专辑的统计数据
		saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_PLAY);
		saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_SUBSCRIBE);
		saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_BUY);
		saveAlbumStat(albumInfo.getId(), SystemConstant.ALBUM_STAT_COMMENT);

		//保存专辑属性值
//		albumInfoVo.getAlbumAttributeValueVoList().forEach(albumAttributeValueVo -> {
//			AlbumAttributeValue albumAttributeValue = new AlbumAttributeValue();
//			BeanUtils.copyProperties(albumAttributeValueVo, albumAttributeValue);
//			albumAttributeValue.setAlbumId(albumInfo.getId());
//			albumAttributeValueMapper.insert(albumAttributeValue);
//		});
		List<AlbumAttributeValueVo> albumAttributeValueVoList = albumInfoVo.getAlbumAttributeValueVoList();
		if (CollUtil.isNotEmpty(albumAttributeValueVoList)) {
			List<AlbumAttributeValue> albumAttributeValueList = albumAttributeValueVoList.stream().map(albumAttributeValueVo -> {
				AlbumAttributeValue albumAttributeValue = BeanUtil.copyProperties(albumAttributeValueVo, AlbumAttributeValue.class);
				albumAttributeValue.setAlbumId(albumInfo.getId());
				return albumAttributeValue;
			}).toList();
			int resultLine = albumAttributeValueMapper.insertList(albumAttributeValueList);
			if (resultLine == 0){
				throw new GuiguException(400,"保存albumAttributeValue异常~~~");
			}
		}
		return Result.ok();
	}

	/**
	 * 保存专辑统计信息
	 * @param id 专辑id
	 * @param trackStatPlay 统计类型：声音统计 0701-播放量 0702-收藏量 0703-点赞量 0704-评论数
	 */
	private void saveAlbumStat(Long id, String trackStatPlay) {
		AlbumStat albumStat = new AlbumStat();
		albumStat.setAlbumId(id);
		albumStat.setStatType(trackStatPlay);
		albumStat.setStatNum(new Random().nextInt(10000));
		albumStatMapper.insert(albumStat);
	}


	/**
	 * Copy string literal text to the clipboard
	 * @param pageParam 分页插件参数
	 * @param albumInfoQuery 查询条件实体
	 * @return
	 */
	@Override
	public Page<AlbumListVo> findUserAlbumPage(Page<AlbumListVo> pageParam, AlbumInfoQuery albumInfoQuery) {
		//明确，需要查询AlbumInfo、AlbumStat这两张表
		return albumInfoMapper.findUserAlbumPage(pageParam, albumInfoQuery);
	}
}
