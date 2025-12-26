package com.atguigu.tingshu.album.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.cloud.commons.lang.StringUtils;
import com.atguigu.tingshu.album.config.VodConstantProperties;
import com.atguigu.tingshu.album.mapper.AlbumInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackInfoMapper;
import com.atguigu.tingshu.album.mapper.TrackStatMapper;
import com.atguigu.tingshu.album.service.TrackInfoService;
import com.atguigu.tingshu.album.service.VodService;
import com.atguigu.tingshu.common.constant.SystemConstant;
import com.atguigu.tingshu.common.execption.GuiguException;
import com.atguigu.tingshu.common.util.UploadFileUtil;
import com.atguigu.tingshu.model.album.AlbumInfo;
import com.atguigu.tingshu.model.album.TrackInfo;
import com.atguigu.tingshu.model.album.TrackStat;
import com.atguigu.tingshu.query.album.TrackInfoQuery;
import com.atguigu.tingshu.vo.album.TrackInfoVo;
import com.atguigu.tingshu.vo.album.TrackListVo;
import com.atguigu.tingshu.vo.album.TrackMediaInfoVo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.qcloud.vod.VodUploadClient;
import com.qcloud.vod.model.VodUploadRequest;
import com.qcloud.vod.model.VodUploadResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Random;

@Slf4j
@Service
@SuppressWarnings({"all"})
public class TrackInfoServiceImpl extends ServiceImpl<TrackInfoMapper, TrackInfo> implements TrackInfoService {

	@Autowired
	private TrackInfoMapper trackInfoMapper;

	@Autowired
	private AlbumInfoMapper albumInfoMapper;

	@Autowired
	private VodService vodService;

	@Autowired
	private TrackStatMapper trackStatMapper;
	/**
	 * 保存声音
	 *
	 * @param trackInfoVo
	 * @param userId
	 */
	@Transactional(rollbackFor = Exception.class)
	@Override
	public void saveTrackInfo(TrackInfoVo trackInfoVo, Long userId) {
		//1.根据所属专辑ID查询专辑信息 得到封面图片，用于后续更新
		Long albumId = trackInfoVo.getAlbumId();
		AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
		if (albumInfo == null) {
			log.error("专辑:{}不存在", albumId);
			throw new GuiguException(404, "专辑不存在");
		}
		//2.新增声音记录
		//2.1 将声音VO转为PO
		TrackInfo trackInfo = BeanUtil.copyProperties(trackInfoVo, TrackInfo.class);
		//2.2 给属性赋值
		//2.2.1 设置用户ID
		trackInfo.setUserId(userId);
		//2.2.2 设置声音序号 要求从1开始递增TODO:OrderNum注意点
		trackInfo.setOrderNum(albumInfo.getIncludeTrackCount() + 1);
		//2.2.3 调用点播平台获取音频详情信息：时长、大小、类型
		TrackMediaInfoVo trackMediaInfoVo = vodService.getTrackMediaInfo(trackInfo.getMediaFileId());
		if (trackMediaInfoVo != null) {
			trackInfo.setMediaDuration(BigDecimal.valueOf(trackMediaInfoVo.getDuration()));
			trackInfo.setMediaSize(trackMediaInfoVo.getSize());
			trackInfo.setMediaType(trackMediaInfoVo.getType());
		}
		//2.2.4 来源：用户上传
		trackInfo.setSource(SystemConstant.TRACK_SOURCE_USER);
		//2.2.5 状态：待审核
		trackInfo.setStatus(SystemConstant.TRACK_STATUS_NO_PASS);
		//2.2.6 封面图片 如果未提交使用所属专辑封面
		String coverUrl = trackInfo.getCoverUrl();
		if(StringUtils.isBlank(coverUrl)){
			trackInfo.setCoverUrl(albumInfo.getCoverUrl());
		}
		//2.3 新增声音记录
		trackInfoMapper.insert(trackInfo);
		Long trackId = trackInfo.getId();

		//3. 更新专辑信息：包含声音数量TODO:IncludeTrackCount注意点
		albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() + 1);
		albumInfoMapper.updateById(albumInfo);

		//4.新增声音统计记录
		this.saveTrackStat(trackId, SystemConstant.TRACK_STAT_PLAY, 0);
		this.saveTrackStat(trackId, SystemConstant.TRACK_STAT_COLLECT, 0);
		this.saveTrackStat(trackId, SystemConstant.TRACK_STAT_PRAISE, 0);
		this.saveTrackStat(trackId, SystemConstant.TRACK_STAT_COMMENT, 0);

		//5.TODO 对点播平台音频文件进行审核（异步审核）
	}

	@Override
	public void saveTrackStat(Long trackId, String statType, int statNum) {
		TrackStat trackStat = new TrackStat();
		trackStat.setTrackId(trackId);
		trackStat.setStatType(statType);
		if (statNum == 0) {
			trackStat.setStatNum(new Random().nextInt(10000) + 1);
		}else {
			trackStat.setStatNum(statNum);
		}
		trackStatMapper.insert(trackStat);
	}


	/**
	 * 条件分页查询当前用户声音列表（包含声音统计信息）
	 *
	 * @param pageInfo 分页对象
	 * @param trackInfoQuery 查询条件（用户ID，关键字，审核状态）
	 * @return
	 */
	@Override
	public Page<TrackListVo> getUserTrackPage(Page<TrackListVo> pageInfo, TrackInfoQuery trackInfoQuery) {
		return trackInfoMapper.getUserTrackPage(pageInfo, trackInfoQuery);
	}

	/**
	 * 修改声音信息
	 *
	 * @param id
	 * @param trackInfo
	 * @return
	 */
	@Override
	public void updateTrackInfo(TrackInfo trackInfo) {
		//1.判断音频文件是否变更
		//1.1 根据声音ID查询声音记录得到“旧”的音频文件标识
		TrackInfo oldTrackInfo = trackInfoMapper.selectById(trackInfo.getId());
		//1.2 判断文件是否被更新
		if (!trackInfo.getMediaFileId().equals(oldTrackInfo.getMediaFileId())) {
			//1.3 如果文件被更新，再次获取新音频文件信息更新：时长，大小，类型
			TrackMediaInfoVo mediaInfoVo = vodService.getTrackMediaInfo(trackInfo.getMediaFileId());
			if (mediaInfoVo != null) {
				trackInfo.setMediaType(mediaInfoVo.getType());
				trackInfo.setMediaDuration(BigDecimal.valueOf(mediaInfoVo.getDuration()));
				trackInfo.setMediaSize(mediaInfoVo.getSize());
				trackInfo.setStatus(SystemConstant.TRACK_STATUS_NO_PASS);

				// 音频文件发生更新后，必须再次进行审核
				//4. 开启音视频任务审核；更新声音表：审核任务ID-后续采用定时任务检查审核结果
				//4.1 启动审核任务得到任务ID
				//String reviewTaskId = vodService.reviewMediaTask(trackInfo.getMediaFileId());
				//4.2 更新声音表：审核任务ID，状态（审核中）

				//4.2 更新声音表：审核任务ID，状态（审核中）
				//trackInfo.setReviewTaskId(reviewTaskId);
				//trackInfo.setStatus(SystemConstant.TRACK_STATUS_REVIEW_ING);
				//trackInfoMapper.updateById(trackInfo);
			}
			//1.4 从点播平台删除旧的音频文件
			vodService.deleteMedia(oldTrackInfo.getMediaFileId());
		}
		//2.更新声音信息
		trackInfoMapper.updateById(trackInfo);
	}

	/**
	 * 删除声音记录
	 *
	 * @param id
	 * @return
	 */
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void removeTrackInfo(Long id) {
		TrackInfo trackInfo = trackInfoMapper.selectById(id);
		if (trackInfo != null) {
			Long albumId = trackInfo.getAlbumId();
			Integer orderNum = trackInfo.getOrderNum();
			trackInfoMapper.update(null, new LambdaUpdateWrapper<TrackInfo>()
					.setSql("order_num = order_num - 1")
					.eq(TrackInfo::getAlbumId, albumId)
					.gt(TrackInfo::getOrderNum, orderNum));
			trackInfoMapper.deleteById(id);
			trackStatMapper.delete(new LambdaQueryWrapper<TrackStat>().eq(TrackStat::getTrackId, id));
			//5.更新专辑包含声音数量
			AlbumInfo albumInfo = albumInfoMapper.selectById(albumId);
			albumInfo.setIncludeTrackCount(albumInfo.getIncludeTrackCount() - 1);
			albumInfoMapper.updateById(albumInfo);

			//6.删除点播平台音频文件
			vodService.deleteMedia(trackInfo.getMediaFileId());
		}


	}


}
