package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import com.atguigu.tingshu.album.mapper.AlbumAttributeValueMapper;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.AlbumStatMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.model.album.AlbumAttributeValue;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.AlbumStat;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumAttributeValueVo;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static com.atguigu.tingshu.common.constant.SystemConstant.ALBUM_STATUS_NO_PASS;


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

	@Autowired
	private TrackInfoMapper trackInfoMapper;
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
		albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
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

	//这个id是专辑id
	@Transactional(rollbackFor = Exception.class)
	@Override
	public void removeAlbumInfo(Long id) {
		//1.根据专辑ID查询声音表判断该专辑下是否关联声音 如果存在 不允许删除
		Long count = trackInfoMapper.selectCount(
				new LambdaQueryWrapper<TrackInfo>()
						.eq(TrackInfo::getAlbumId, id)
		);
		if (count > 0) {
			throw new GuiguException(500, "该专辑下存在关联声音");
		}
		//根据专辑id删除专辑
		albumInfoMapper.deleteById(id);
		//根据专辑id去删除统计信息
		albumStatMapper.delete(new LambdaQueryWrapper<AlbumStat>().eq(AlbumStat::getAlbumId, id));
		//根据专辑id去删除属性值
		albumAttributeValueMapper.delete(new LambdaQueryWrapper<AlbumAttributeValue>().eq(AlbumAttributeValue::getAlbumId, id));
		//5.TODO 基于MQ删除存在Elasticsearch（全文搜索引擎）中数据（待写）
	}

	/**
	 * 根据专辑ID查询专辑信息（包含专辑标签列表）
	 *
	 * @param id 专辑ID
	 * @return
	 */
	@Override
	public AlbumInfo getAlbumInfo(Long id) {
		//1.根据ID查询专辑
		AlbumInfo albumInfo = albumInfoMapper.selectById(id);
		//2.根据专辑ID查询专辑标签关系列表
		if (albumInfo != null) {
			List<AlbumAttributeValue> albumAttributeValues = albumAttributeValueMapper
					.selectList(
							new LambdaQueryWrapper<AlbumAttributeValue>()
									.eq(AlbumAttributeValue::getAlbumId, id)
					);
			albumInfo.setAlbumAttributeValueVoList(albumAttributeValues);
		}
		return albumInfo;
	}


	/**
	 * 更新专辑信息(自己写)
	 * @param id
	 * @param albumInfoVo
	 */
	@Override
	public void updateAlbumInfo(Long id, AlbumInfoVo albumInfoVo) {
		AlbumInfo albumInfo = albumInfoMapper.selectById(id);
		if (BeanUtil.isEmpty(albumInfo)) {
			throw new GuiguException(400,"专辑不存在，但是要修改~！");
		}
		//1.更新专辑信息 状态：未审核
		BeanUtils.copyProperties(albumInfoVo, albumInfo);
		albumInfo.setStatus(ALBUM_STATUS_NO_PASS);
		albumInfo.setId(id);
		albumInfo.setUserId(AuthContextHolder.getUserId() == null ? 1L : AuthContextHolder.getUserId());
		albumInfoMapper.updateById(albumInfo);
		//统计信息肯定是不用动的，只修改属性信息
		albumStatMapper.delete(new LambdaQueryWrapper<AlbumStat>().eq(AlbumStat::getAlbumId, id));
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
	}

	/**
	 * 获取当前用户全部专辑列表
	 * @param userId
	 * @return
	 */
	@Override
	public List<AlbumInfo> getUserAllAlbumList(Long userId) {
		//模拟做一个分页，先只查询50条
		int pageNum = 1;
		int pageSize = 50;
		Page<AlbumInfo> albumInfoPage = albumInfoMapper.selectPage(new Page<AlbumInfo>(pageNum, pageSize),
				new LambdaQueryWrapper<AlbumInfo>().eq(AlbumInfo::getUserId, userId)
						.orderByDesc(AlbumInfo::getId));

//		List<AlbumInfo> albumInfoList = albumInfoPage.getRecords().stream().map(albumInfo -> {
//			albumInfo
//					.setAlbumAttributeValueVoList(albumAttributeValueMapper
//							.selectList(new LambdaQueryWrapper<AlbumAttributeValue>()
//									.eq(AlbumAttributeValue::getAlbumId, albumInfo.getId())));
//			return albumInfo;
//		}).collect(Collectors.toList()); //不需要，白忙活了。

		return albumInfoPage.getRecords();
	}
}
