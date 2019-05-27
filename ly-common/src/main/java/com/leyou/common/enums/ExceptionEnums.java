package com.leyou.common.enums;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

public enum  ExceptionEnums {

    PRICE_CANNOT_BE_NULL(400,"价格不能为空!"),
    CATEGORY_NOT_FOUND(404,"商品分类没有找到"),
    BRAND_NOT_FOUND(404,"品牌没有找到"),
    SPU_NOT_FOUND(404,"SPU没有找到"),
    BRAND_SAVE_ERROR(500,"新增品牌失败"),
    GOODS_SAVE_ERROR(500,"新增商品失败"),
    FILE_UPLOAD_ERROR(500,"文件上传失败"),
    GOODS_NOT_FOUND(404,"商品未找到"),
    FILE_TYPE_NOT_MATCH_ERROR(400,"无效的文件类型"),
    SPEC_NOTFOUND(400,"商品规格不存在"),
    GOODS_UPDATE_ERROR(500,"商品更新失败"),
    CREATE_TOKEN_ERROR(500,"用户凭证生成失败"),
    USER_DATA_TYPE_ERROR(400,"用户的数据类型有误!"),
    INVALID_VERYFY_CODE(400,"无效的验证码"),
    INVALID_USERANDPASSWORD(400,"用户名或密码错误"),
    UNAUTHORIZED(403,"用户未授权"),
    ;
    private int code;
    private String msg;

    ExceptionEnums(int code, String msg) {
        this.code = code;
        this.msg = msg;
    }

    ExceptionEnums() {
    }

    public int getCode() {
        return code;
    }

    public String getMsg() {
        return msg;
    }

}
