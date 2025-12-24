package com.atguigu.tingshu.album.service.impl;

import com.alibaba.fastjson2.JSONObject;
import com.atguigu.tingshu.album.mapper.BaseCategory1Mapper;
import com.atguigu.tingshu.album.mapper.BaseCategory2Mapper;
import com.atguigu.tingshu.album.mapper.BaseCategory3Mapper;
import com.atguigu.tingshu.album.mapper.BaseCategoryViewMapper;
import com.atguigu.tingshu.album.service.BaseCategoryService;
import com.atguigu.tingshu.model.album.BaseCategory1;
import com.atguigu.tingshu.model.album.BaseCategory2;
import com.atguigu.tingshu.model.album.BaseCategoryView;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@SuppressWarnings({"all"})
public class BaseCategoryServiceImpl extends ServiceImpl<BaseCategory1Mapper, BaseCategory1> implements BaseCategoryService {

	@Autowired
	private BaseCategory1Mapper baseCategory1Mapper;

	@Autowired
	private BaseCategory2Mapper baseCategory2Mapper;

	@Autowired
	private BaseCategory3Mapper baseCategory3Mapper;

	@Autowired
	private BaseCategoryViewMapper baseCategoryViewMapper;

	@Override
	public List<JSONObject> getBaseCategoryList() {
		List<BaseCategoryView> baseCategoryViews = baseCategoryViewMapper.selectList(null);
		Map<Long, List<BaseCategoryView>> categoryMap1List =
				baseCategoryViews.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory1Id));
		Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator_category1 = categoryMap1List.entrySet().iterator();
		ArrayList<JSONObject> categoryList = new ArrayList<>();
		while (iterator_category1.hasNext()) {
			Map.Entry<Long, List<BaseCategoryView>> category1groupByList = iterator_category1.next();
			JSONObject category1 = new JSONObject();
			category1.put("categoryId", category1groupByList.getKey());
			category1.put("categoryName", category1groupByList.getValue().get(0).getCategory1Name());
			//获取category2的列表
			Map<Long, List<BaseCategoryView>> category2MapList =
					baseCategoryViews.stream().collect(Collectors.groupingBy(BaseCategoryView::getCategory2Id));
			Iterator<Map.Entry<Long, List<BaseCategoryView>>> iterator_category2 = category2MapList.entrySet().iterator();
			ArrayList<JSONObject> category2List = new ArrayList<>();
			while (iterator_category2.hasNext()) {
				Map.Entry<Long, List<BaseCategoryView>> category2groupByList = iterator_category2.next();
				JSONObject category2 = new JSONObject();
				category2.put("categoryId", category2groupByList.getKey());
				category2.put("categoryName", category2groupByList.getValue().get(0).getCategory2Name());
				//
				List<JSONObject> category3List = category2groupByList.getValue().stream().map(baseCategoryView -> {
					JSONObject category3 = new JSONObject();
					category3.put("categoryId", baseCategoryView.getId());
					category3.put("categoryName", baseCategoryView.getCategory3Name());
					return category3;
				}).toList();
				category2.put("categoryChild", category3List);
				category2List.add(category2);
			}
			category1.put("categoryChild", category2List);
			categoryList.add(category1);
		}
		return categoryList;
	}
}
