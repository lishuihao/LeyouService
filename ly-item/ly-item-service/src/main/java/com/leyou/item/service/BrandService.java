package com.leyou.item.service;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.leyou.common.enums.ExceptionEnums;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.BrandMapper;
import com.leyou.item.mapper.Brand_CategoryMapper;
import com.leyou.item.pojo.Brand;
import com.leyou.item.pojo.Brand_Category;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import javax.persistence.Id;
import java.util.ArrayList;
import java.util.List;

@Service
public class BrandService {
    @Autowired
    BrandMapper brandMapper;
    @Autowired
    Brand_CategoryMapper brandCategoryMapper;

    public PageResult<Brand> queryBrandByPageAndSort(  Integer page, Integer rows,
      String sortBy, Boolean desc, String key){
        //开始分页
        PageHelper.startPage(page,rows);
        //过滤
        Example example = new Example(Brand.class);
        if(StringUtils.isNotBlank(key)){
            example.createCriteria()
                    .andLike("name","%"+key+"%")
                    .orEqualTo("letter",key);
        }
        if(StringUtils.isNotBlank(sortBy)){
            String orderByClause=sortBy+(desc?"DESC":"ASC");
            example.setOrderByClause(orderByClause);
        }
        //开始查询
        Page<Brand> pageinfo = (Page<Brand>) brandMapper.selectByExample(example);
        //返回结果
        return new PageResult(pageinfo.getTotal(),pageinfo);
    }

    @Transactional
    public void saveBrand(Brand brand, Long[] categories) {
        //新增品牌
        int i = brandMapper.insert(brand);
        if(i!=1){
            throw new LyException(ExceptionEnums.BRAND_SAVE_ERROR);
        }
        //新增中间表
        for (Long category : categories) {
            int count = brandMapper.insertCategoryBrand(category, brand.getId());
            if(count != 1){
                throw new LyException(ExceptionEnums.BRAND_SAVE_ERROR);
            }
        }
    }

    public Brand queryById(Long id){
        Brand brand = brandMapper.selectByPrimaryKey(id);
        if(brand ==null){
            new LyException(ExceptionEnums.BRAND_NOT_FOUND);
        }
        return brand;
    }

    /**
     * 根据Cid查询品牌
     * @param cid
     * @return
     */
    public List<Brand> queryBrandByCid(Long cid) {

        Brand_Category brandCategory = new Brand_Category();
        brandCategory.setCategoryId(cid);
        List<Brand_Category> brandIds = brandCategoryMapper.select(brandCategory);
        if(CollectionUtils.isEmpty(brandIds)){
            throw new LyException(ExceptionEnums.CATEGORY_NOT_FOUND);
        }
        List<Brand> list = new ArrayList<>();
        for (Brand_Category brandId : brandIds) {
            Brand brand = brandMapper.selectByPrimaryKey(brandId.getBrandId());
            list.add(brand);
        }
        if(CollectionUtils.isEmpty(list)){
            throw new LyException(ExceptionEnums.BRAND_NOT_FOUND);
        }
        return list;
    }

    public List<Brand> queryByIds(List<Long> ids) {
        List<Brand> brands = brandMapper.selectByIdList(ids);
        if(CollectionUtils.isEmpty(brands)){
            throw new LyException(ExceptionEnums.BRAND_NOT_FOUND);
        }
        return brands;
    }
}
