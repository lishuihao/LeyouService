package com.leyou.item.service;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.leyou.common.enums.ExceptionEnums;
import com.leyou.common.exception.LyException;
import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.mapper.SpuDetailMapper;
import com.leyou.item.mapper.SpuMapper;
import com.leyou.item.mapper.StockMapper;
import com.leyou.item.pojo.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import tk.mybatis.mapper.entity.Example;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoodsService {

    @Autowired
    SpuMapper spuMapper;
    @Autowired
    SpuDetailMapper spuDetailMapper;
    @Autowired
    CategoryService categoryService;
    @Autowired
    BrandService brandService;
    @Autowired
    SkuMapper skuMapper;
    @Autowired
    StockMapper stockMapper;
    @Autowired
    AmqpTemplate template;
    public PageResult<Spu> querySpuByPage(Integer page, Integer rows, Boolean saleable, Boolean desc, String key) {
        //开始分页
        PageHelper.startPage(page,rows);
        //过滤
        Example example = new Example(Spu.class);
        Example.Criteria criteria = example.createCriteria();
        if(StringUtils.isNotBlank(key)){
            criteria.andLike("title","%"+key+"%");
        }
        //上下架过滤
        if(saleable!=null){
            criteria.andEqualTo("saleable",saleable);
        }
        //排序
        String clause="id"+" "+(desc?"DESC":"ASC");
        example.setOrderByClause(clause);
        //查询
        List<Spu> spus = spuMapper.selectByExample(example);
        if(CollectionUtils.isEmpty(spus)){
            throw new LyException(ExceptionEnums.GOODS_NOT_FOUND);
        }

        //解析分类与品牌的名称
        loadCategoryAndBrandName(spus);


        //解析分页结果
        PageInfo<Spu> info = new PageInfo<>(spus);
        return new PageResult<>(info.getTotal(),spus);

    }

    private void loadCategoryAndBrandName(List<Spu> spus) {

        for (Spu spu : spus) {
            //处理分类名称
            List<String> names = categoryService.queryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()))
                    .stream().map(Category::getName).collect(Collectors.toList());
            spu.setCname(StringUtils.join(names,","));

            //处理品牌名称
            String name = brandService.queryById(spu.getBrandId()).getName();
            spu.setBname(name);
        }
        
    }

    @Transactional
    public void saveGoodsInfo(Spu spu) {
        //保存Spu
        spu.setSaleable(true);
        spu.setValid(true);
        spu.setCreateTime(new Date());
        spu.setLastUpdateTime(spu.getCreateTime());
        int i = spuMapper.insert(spu);
        if(i != 1){
            throw new LyException(ExceptionEnums.GOODS_SAVE_ERROR);
        }
        //保存SpuDetail
        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId());
        int i1 = spuDetailMapper.insert(spuDetail);
        if(i1 != 1){
            throw new LyException(ExceptionEnums.GOODS_SAVE_ERROR);
        }
        //保存Sku和Stock
        saveSkuAndStock(spu.getSkus(),spu.getId());
        //发送mq消息
        template.convertAndSend("item.insert",spu.getId());

    }

    private void saveSkuAndStock(List<Sku> skus, Long spuid) {
        List<Stock> list = new ArrayList();
        //保存sku
        for (Sku sku : skus) {
            sku.setSpuId(spuid);
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            skuMapper.insert(sku);

            //保存库存信息
            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            list.add(stock);
        }

        int i = stockMapper.insertList(list);
        if(i != list.size()){
            throw new LyException(ExceptionEnums.GOODS_SAVE_ERROR);
        }

    }

    public SpuDetail findSpuDetailByID(Long id) {
        SpuDetail spuDetail = spuDetailMapper.selectByPrimaryKey(id);
        if (spuDetail ==null){
            throw new LyException(ExceptionEnums.GOODS_NOT_FOUND);
        }
        return spuDetail;
    }

    public List<Sku> querySkuBySpuId(Long spuId) {
        Sku sku = new Sku();
        sku.setSpuId(spuId);
        List<Sku> skus = skuMapper.select(sku);
        if(CollectionUtils.isEmpty(skus)){
            throw new LyException(ExceptionEnums.GOODS_NOT_FOUND);
        }
        return skus;
    }

    public Spu querySpuById(Long id) {
        Spu spu = spuMapper.selectByPrimaryKey(id);
        if (spu == null){
            throw new LyException(ExceptionEnums.SPU_NOT_FOUND);
        }
        //查询sku
        List<Sku> skus = querySkuBySpuId(id);
        spu.setSkus(skus);
        //查询detail
        SpuDetail detail = findSpuDetailByID(id);
        spu.setSpuDetail(detail);
        return spu;
    }

    @Transactional
    public void updateGoodsInfo(Spu spu) {
        if (spu.getId() == 0) {
            throw new LyException(ExceptionEnums.SPU_NOT_FOUND);
        }
        //首先查询sku
        Sku sku = new Sku();
        sku.setSpuId(spu.getId());
        List<Sku> skuList = skuMapper.select(sku);
        if (!CollectionUtils.isEmpty(skuList)) {
            //删除所有sku
            skuMapper.delete(sku);
            //删除库存
            List<Long> ids = skuList.stream()
                    .map(Sku::getId)
                    .collect(Collectors.toList());
            stockMapper.deleteByIdList(ids);
        }
        //更新数据库  spu  spuDetail
        spu.setLastUpdateTime(new Date());
        //更新spu spuDetail
        int count = spuMapper.updateByPrimaryKeySelective(spu);
        if (count == 0) {
            throw new LyException(ExceptionEnums.GOODS_UPDATE_ERROR);
        }


        SpuDetail spuDetail = spu.getSpuDetail();
        spuDetail.setSpuId(spu.getId());
        count = spuDetailMapper.updateByPrimaryKeySelective(spuDetail);
        if (count == 0) {
            throw new LyException(ExceptionEnums.GOODS_UPDATE_ERROR);
        }

        //更新sku和stock
        saveSkuAndStock(spu);

        //发送消息
        template.convertAndSend("item.update",spu.getId());
    }


    /**
     * 保存sku和库存
     *
     * @param spu
     */
    @Transactional
    public  void saveSkuAndStock(Spu spu) {
        List<Sku> skuList = spu.getSkus();
        List<Stock> stocks = new ArrayList<>();

        for (Sku sku : skuList) {
            sku.setSpuId(spu.getId());
            sku.setCreateTime(new Date());
            sku.setLastUpdateTime(sku.getCreateTime());
            int count = skuMapper.insert(sku);
            if (count != 1) {
                throw new LyException(ExceptionEnums.GOODS_SAVE_ERROR);
            }

            Stock stock = new Stock();
            stock.setSkuId(sku.getId());
            stock.setStock(sku.getStock());
            stocks.add(stock);
        }
        //批量插入库存数据
        int count = stockMapper.insertList(stocks);
        if (count == 0) {
            throw new LyException(ExceptionEnums.GOODS_SAVE_ERROR);
        }
    }

}
