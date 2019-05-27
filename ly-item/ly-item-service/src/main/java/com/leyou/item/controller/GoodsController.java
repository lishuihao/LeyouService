package com.leyou.item.controller;

import com.leyou.common.vo.PageResult;
import com.leyou.item.mapper.SkuMapper;
import com.leyou.item.pojo.Sku;
import com.leyou.item.pojo.Spu;
import com.leyou.item.pojo.SpuDetail;
import com.leyou.item.service.GoodsService;
import com.sun.xml.internal.bind.v2.model.core.ID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GoodsController {

    @Autowired
    GoodsService goodsService;

    /**
     * 分页查询Spu
     * @param page
     * @param rows
     * @param saleable
     * @param desc
     * @param key
     * @return
     */
    @RequestMapping("/spu/page")
    public ResponseEntity<PageResult<Spu>> querySpuByPage(
            @RequestParam(value = "page",defaultValue = "1") Integer page,
            @RequestParam(value = "rows", defaultValue = "5") Integer rows,
            @RequestParam(value = "saleable", defaultValue = "true") Boolean saleable,
            @RequestParam(value = "desc", defaultValue = "false") Boolean desc,
            @RequestParam(value = "key", required = false) String key
    ){
        System.out.println(key);
        return ResponseEntity.ok(goodsService.querySpuByPage(page,rows,saleable,desc,key));
    }

    /**
     * 保存商品信息
     * @param spu
     * @return
     */
    @PostMapping("/goods")
    public ResponseEntity<Void> saveGoodsInformation(@RequestBody Spu spu){
        goodsService.saveGoodsInfo(spu);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * 修改商品信息
     */
    @PutMapping("/goods")
    public ResponseEntity<Void> updateGoodsInformation(@RequestBody Spu spu){
        goodsService.updateGoodsInfo(spu);
        return ResponseEntity.ok(null);
    }

    /**
     * 根据spuId查询商品详情
     * @param id
     * @return
     */
    @GetMapping("/goods/spu/detail/{spuId}")
    public ResponseEntity<SpuDetail> findSpuDetailById(@PathVariable("spuId") Long id){
        return ResponseEntity.ok( goodsService.findSpuDetailByID(id));
    }
    /**
     * 根据spuId查询商品详情
     * @param id
     * @return
     */
    @GetMapping("/spu/detail/{spuId}")
    public ResponseEntity<SpuDetail> findSpuDetailById2(@PathVariable("spuId") Long id){
        return ResponseEntity.ok( goodsService.findSpuDetailByID(id));
    }

    /**
     * 根据spuId查询sku列表
     * @param spuId
     * @return
     */
    @GetMapping("/sku/list")
    public ResponseEntity<List<Sku>> querySkuBySpuId(@RequestParam("id") Long spuId){
        return ResponseEntity.ok(goodsService.querySkuBySpuId(spuId));
    }

    /**
     * 根据Id查询spu
     * @param id
     * @return
     */
    @GetMapping("spu/{id}")
    public ResponseEntity<Spu> querySpuById(@PathVariable("id") Long id){
        //查询spu
        Spu spu = this.goodsService.querySpuById(id);
        spu.getSpuDetail().getSpecialSpec();
        return ResponseEntity.ok(spu);
    }

}
