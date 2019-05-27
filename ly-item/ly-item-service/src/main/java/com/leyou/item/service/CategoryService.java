package com.leyou.item.service;

import com.leyou.common.enums.ExceptionEnums;
import com.leyou.common.exception.LyException;
import com.leyou.item.mapper.Brand_CategoryMapper;
import com.leyou.item.pojo.Brand_Category;
import com.leyou.item.pojo.Category;
import com.leyou.item.mapper.CategoryMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class CategoryService {


    @Autowired
    private Brand_CategoryMapper brandCategoryMapper;

    @Autowired
    private CategoryMapper categoryMapper;

    public List<Category> queryCategoryListByPid(Long pid) {
        //查询条件，mapper会把对象中的非空属性作为查询条件
        Category t=new Category();
        t.setParentId(pid);
        List<Category> categories = categoryMapper.select(t);
        //判断查询结果
        if(CollectionUtils.isEmpty(categories)){
            throw new LyException(ExceptionEnums.CATEGORY_NOT_FOUND);
        }
        return categories;
    }

    public List<Category> queryByIds(List<Long> ids){
        List<Category> categories = categoryMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(categories)){
            throw new LyException(ExceptionEnums.CATEGORY_NOT_FOUND);
        }
        return categories;
    }


    public List<Category> queryCategoryListByBid(Long bid) {
        Brand_Category brandCategory = new Brand_Category();
        brandCategory.setBrandId(bid);
        List<Brand_Category> list = brandCategoryMapper.select(brandCategory);
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnums.CATEGORY_NOT_FOUND);
        }
        List<Long> list1 = new ArrayList();
        for (Brand_Category brand_category : list) {
            list1.add(brand_category.getCategoryId());
        }
        List<Category> list2 = categoryMapper.selectByIdList(list1);
        return list2;
    }
}
