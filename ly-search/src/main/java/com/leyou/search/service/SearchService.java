package com.leyou.search.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leyou.common.enums.ExceptionEnums;
import com.leyou.common.exception.LyException;
import com.leyou.common.utils.JsonUtils;
import com.leyou.common.vo.PageResult;
import com.leyou.item.pojo.*;
import com.leyou.search.client.BrandClient;
import com.leyou.search.client.CategoryClient;
import com.leyou.search.client.GoodsClient;
import com.leyou.search.client.SpecificationClient;
import com.leyou.search.pojo.Goods;
import com.leyou.search.pojo.SearchRequest;
import com.leyou.search.pojo.SearchResult;
import com.leyou.search.repository.GoodsRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.math.NumberUtils;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.Aggregations;
import org.elasticsearch.search.aggregations.bucket.terms.LongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.StringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.data.elasticsearch.core.aggregation.AggregatedPage;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilter;
import org.springframework.data.elasticsearch.core.query.NativeSearchQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SearchService {

    @Autowired
    private CategoryClient categoryClient;

    @Autowired
    private GoodsClient goodsClient;

    private ObjectMapper mapper= new ObjectMapper();
    @Autowired
    private BrandClient brandClient;

    @Autowired
    private SpecificationClient specificationClient;

    @Autowired
    private GoodsRepository repository;

    @Autowired
    ElasticsearchTemplate template;

    public Goods buildGoods(Spu spu){
        //查询分类
        List<Category> categories = categoryClient.queryCategoryByIds(Arrays
                .asList(spu.getCid1(), spu.getCid2(), spu.getCid3()));
        if(CollectionUtils.isEmpty(categories)){
            throw new LyException(ExceptionEnums.CATEGORY_NOT_FOUND);
        }
        List<String> names = categories.stream().map(Category::getName)
                .collect(Collectors.toList());
        //查询品牌
        Brand brand = brandClient.queryBrandById(spu.getBrandId());
        if(brand == null){
            throw new LyException(ExceptionEnums.BRAND_NOT_FOUND);
        }
        //搜索字段
        String all = spu.getTitle()+ StringUtils.join(names," ")+brand.getName();

        //查询SKU
        List<Sku> skuList = goodsClient.querySkuBySpuId(spu.getId());
        if(CollectionUtils.isEmpty(skuList)){
            throw new LyException(ExceptionEnums.GOODS_NOT_FOUND);
        }
        //价格集合
        Set<Long> priceList = new HashSet<>();
        //对SKU进行处理
        List<Map<String,Object>> skus = new ArrayList<>();
        for (Sku sku : skuList) {
            Map<String,Object> map = new HashMap<>();
            map.put("id",sku.getId());
            map.put("title",sku.getTitle());
            map.put("price",sku.getPrice());
            map.put("images",StringUtils.substringBefore(sku.getImages(),","));
            skus.add(map);
            //处理价格
            priceList.add(sku.getPrice());
        }

        //查询规格参数
        List<SpecParam> params= specificationClient.querySpecParams(null, spu.getCid3(), true,null);
        if(CollectionUtils.isEmpty(params)){
            throw new LyException(ExceptionEnums.SPEC_NOTFOUND);
        }
        //查询商品详情
        SpuDetail spuDetail = goodsClient.findSpuDetailById(spu.getId());
        //获取通用规格参数
        Map<Long, String> gennericSpec = JsonUtils.
        toMap(spuDetail.getGenericSpec(),
                Long.class, String.class);
        //获取特有规格参数
        Map<Long, List<String>> specialSpec = JsonUtils.
                nativeRead(spuDetail.getSpecialSpec(),
                        new TypeReference<Map<Long, List<String>>>() {});
        //规格参数,key是规格参数的名字，值时规格参数的值
        Map<String,Object> specs= new HashMap<>();
        for (SpecParam param : params) {
            //规格名称
            String key = param.getName();
            Object value="";
            //判断是否是通用规格属性
            if(param.getGeneric()){
                value=gennericSpec.get(param.getId());
                //判断是否为数值类型
                if(param.getNumeric()){
                    //处理成段,覆盖之前的value值
                  value = chooseSegment(value.toString(),param);
                }
            }else{
                value=specialSpec.get(param.getId());
            }
            value=(value == null ? "其他":value);
            //存入map
            specs.put(key,value);
        }

        //构建goods对象
        Goods goods = new Goods();
        goods.setBrandId(spu.getBrandId());
        goods.setCid1(spu.getCid1());
        goods.setCid2(spu.getCid2());
        goods.setCid3(spu.getCid3());
        goods.setCreateTime(spu.getCreateTime());
        goods.setId(spu.getId());
        goods.setAll(all);//  搜索字段:包含标题，分类，品牌，规格等信息
        goods.setPrice(priceList);// 所有sku价格集合
        goods.setSkus(JsonUtils.toString(skus));//  所有Sku的集合的json格式
        goods.setSpecs(specs); //所有可搜索的规格参数
        goods.setSubTitle(spu.getSubTitle());
        return goods;
    }


    private String chooseSegment(String value, SpecParam p) {
        double val = NumberUtils.toDouble(value);
        String result = "其它";
        // 保存数值段
        for (String segment : p.getSegments().split(",")) {
            String[] segs = segment.split("-");
            // 获取数值范围
            double begin = NumberUtils.toDouble(segs[0]);
            double end = Double.MAX_VALUE;
            if(segs.length == 2){
                end = NumberUtils.toDouble(segs[1]);
            }
            // 判断是否在范围内
            if(val >= begin && val < end){
                if(segs.length == 1){
                    result = segs[0] + p.getUnit() + "以上";
                }else if(begin == 0){
                    result = segs[1] + p.getUnit() + "以下";
                }else{
                    result = segment + p.getUnit();
                }
                break;
            }
        }
        return result;
    }


    public PageResult<Goods> search(SearchRequest searchRequest) {
        Integer page = searchRequest.getPage() -1;
        Integer size = searchRequest.getSize();
        //1.创建查询构造器
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        //2.结果过滤
        queryBuilder.withSourceFilter(new FetchSourceFilter(new String[]{"id","subTitle","skus"},null));
        //2.1分页
        queryBuilder.withPageable(PageRequest.of(page,size));
        //2.2搜索条件
        QueryBuilder basicQuery = buildBasicQuery(searchRequest);
        queryBuilder.withQuery(basicQuery);
        //3.聚合分类和品牌
        //3.1聚合分类
        String categoryAggName="category";
        queryBuilder.addAggregation(AggregationBuilders.terms(categoryAggName).field("cid3"));
        //3.2聚合品牌
        String brandAggName="brand";
        queryBuilder.addAggregation(AggregationBuilders.terms(brandAggName).field("brandId"));
        //4.查询
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);
        //5解析结果
        //5.1解析分页结果
        long total = result.getTotalElements();
        int totalPage = result.getTotalPages();
        List<Goods> goodsList = result.getContent();
        //5.2解析聚合结果
        Aggregations aggregations = result.getAggregations();
        List<Category> categories = parseCategoryAgg(aggregations.get(categoryAggName));
        List<Brand> brands = parBrandAgg(aggregations.get(brandAggName));
        //6.完成规格参数聚合
        List<Map<String,Object>> specs = null;
        if(categories != null && categories.size() == 1){
            //商品存在并且数量为1，可以聚合规格参数
            specs = buildSpecificationAgg(categories.get(0).getId(),basicQuery);
        }

        return new SearchResult(total,totalPage,goodsList,categories,brands,specs);
    }

    private QueryBuilder buildBasicQuery(SearchRequest searchRequest) {
        //创建布尔查询
        BoolQueryBuilder queryBuilder = QueryBuilders.boolQuery();
        //查询条件
        queryBuilder.must(QueryBuilders.matchQuery("all",searchRequest.getKey()));
        //过滤条件
        Map<String, String> filter = searchRequest.getFilter();
        for (Map.Entry<String, String> entry : filter.entrySet()) {
            String key = entry.getKey();
            //处理key
            if(!"cid3".equals(key) && !"brandId".equals(key)){
                key="spec."+key+".keyword";
            }
            queryBuilder.filter(QueryBuilders.termQuery(key,entry.getValue()));
        }
        return queryBuilder;
    }

    private List<Map<String, Object>> buildSpecificationAgg(Long cid, QueryBuilder basicQuery) {

        List<Map<String,Object>> specs = new ArrayList<>();
        //查询可过滤的规格参数
        List<SpecParam> params = specificationClient.querySpecParams(null, cid, true, null);

        //基本查询条件
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(basicQuery);
        queryBuilder.withPageable(PageRequest.of(0, 1));

        for (SpecParam param : params) {
            //聚合
            String name = param.getName();
            queryBuilder.addAggregation(AggregationBuilders.terms(name).field("specs." + name + ".keyword"));
        }
        //查询
        AggregatedPage<Goods> result = template.queryForPage(queryBuilder.build(), Goods.class);

        //对聚合结果进行解析
        Aggregations aggs = result.getAggregations();
        for (SpecParam param : params) {
            String name = param.getName();
            Terms terms = aggs.get(name);
            //创建聚合结果
            HashMap<String, Object> map = new HashMap<>();
            map.put("k", name);
            map.put("options", terms.getBuckets()
                    .stream()
                    .map(b -> b.getKey())
                    .collect(Collectors.toList()));
            specs.add(map);
        }
        return specs;
    }

    private List<Brand> parBrandAgg(LongTerms terms) {
        try {
            List<Long> brandIds = terms.getBuckets().stream()
                    .map(bucket -> bucket.getKeyAsNumber().longValue()).
                            collect(Collectors.toList());
            List<Brand> brands = brandClient.queryByBrandIds(brandIds);
            return brands;
        }catch (Exception e){
            log.error("[搜索服务]查询品牌异常",e);
            return null;
        }
    }

    private List<Category> parseCategoryAgg(LongTerms terms) {
        try {
        List<Long> categoryIds = terms.getBuckets().stream()
                .map(bucket -> bucket.getKeyAsNumber().longValue()).
                        collect(Collectors.toList());
        List<Category> categories = categoryClient.queryCategoryByIds(categoryIds);
            for (Category category : categories) {
                category.setParentId(null);
                category.setIsParent(null);
                category.setSort(null);
            }
        return categories;
        }catch (Exception e){
            log.error("[搜索服务]查询分类异常",e);
            return null;
        }
    }

    public void createOrUpdateIndex(Long spuId) {
        //查询spu
        Spu spu = goodsClient.querySpuById(spuId);
        //构建goods对象
        Goods goods = buildGoods(spu);
        //存入索引库
        repository.save(goods);
    }
}
