package com.atguigu.tingshu.album.api;

import com.atguigu.tingshu.album.service.AlbumInfoService;
import com.atguigu.tingshu.common.result.Result;
import com.atguigu.tingshu.common.util.AuthContextHolder;
import com.atguigu.tingshu.query.album.AlbumInfoQuery;
import com.atguigu.tingshu.vo.album.AlbumInfoVo;
import com.atguigu.tingshu.vo.album.AlbumListVo;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Tag(name = "专辑管理")
@RestController
@RequestMapping("api/album")
@SuppressWarnings({"all"})
public class AlbumInfoApiController {

	@Autowired
	private AlbumInfoService albumInfoService;

	@Operation(summary = "内容创作者或者平台运营人员-保存专辑")
	@PostMapping("/albumInfo/saveAlbumInfo")
	public Result saveAlbumInfo(@RequestBody AlbumInfoVo albumInfoVo) {
		Long userId = AuthContextHolder.getUserId() == null ? 1L : AuthContextHolder.getUserId();
		return albumInfoService.saveAlbumInfo(userId, albumInfoVo);
	}


	/**
	 * TODO 该接口登录才可以访问
	 * 分页条件查询当前登录用户发布专辑
	 *
	 * @param page
	 * @param limit
	 * @param albumInfoQuery
	 * @return
	 */
	@Operation(summary = "分页条件查询当前登录用户发布专辑")
	@PostMapping("/albumInfo/findUserAlbumPage/{page}/{limit}")
	public Result<Page<AlbumListVo>> findUserAlbumPage(@PathVariable Long page,
													   @PathVariable Long limit,
													   @RequestBody AlbumInfoQuery albumInfoQuery) {
		//1.获取当前用户登录ID
		Long userId = AuthContextHolder.getUserId() == null ? 1L : AuthContextHolder.getUserId();
		albumInfoQuery.setUserId(userId);
		//2.控制层封装分页参数：页码、页大小
		Page<AlbumListVo> pageParam = new Page<>(page, limit);
		//3.调用业务逻辑层完成分页查询 封装：总记录数，总页数，当前页数据
		pageParam = albumInfoService.findUserAlbumPage(pageParam, albumInfoQuery);
		return Result.ok(pageParam);
	}

}

