package com.leyou.page.service;

import ch.qos.logback.classic.Logger;
import com.leyou.item.pojo.*;
import com.leyou.page.client.BrandClient;
import com.leyou.page.client.CategoryClient;
import com.leyou.page.client.GoodsClient;
import com.leyou.page.client.SpecificationClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class PageService {

    @Autowired
    BrandClient brandClient;
    @Autowired
    CategoryClient categoryClient;
    @Autowired
    GoodsClient goodsClient;
    @Autowired
    SpecificationClient specificationClient;
    @Autowired
    private TemplateEngine engine;


    public Map<String, Object> loadModel(Long spuId) {
        Map<String,Object> model = new HashMap<>();
        //查询spu
        Spu spu = goodsClient.querySpuById(spuId);
        //查询skus
        List<Sku> skus = spu.getSkus();
        //查询detail
        SpuDetail spuDetail = spu.getSpuDetail();
        //查询Brand
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        //查询商品分类
        List<Category> categories = categoryClient.queryCategoryByIds(Arrays.asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        //查询规格参数
        List<SpecGroup> specs = specificationClient.queryGroupByCid(spu.getCid3());
        model.put("title",spu.getTitle());
        model.put("subTitle",spu.getSubTitle());
        model.put("skus",skus);
        model.put("detail",spuDetail);
        model.put("brand",brand);
        model.put("categories",categories);
        model.put("specs",specs);
        return model;
    }

    public void createHtml(Long spuId){
        //准备上下文
        Context context = new Context();
        context.setVariables(loadModel(spuId));

        //输出流
        File file = new File("G:\\upload", spuId + ".html");

        if(file.exists()){
            file.delete();
        }

        PrintWriter writer = null;
        try {
            writer = new PrintWriter(file,"UTF-8");
            //生成Html
            engine.process("item",context,writer);
        } catch (FileNotFoundException e) {
            log.error("[静态页服务] 生成静态页异常"+e);
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            log.error("[静态页服务] 生成静态页异常"+e);
            e.printStackTrace();
        }
    }


}
