package com.leyou.item.pojo;

import lombok.Data;
import tk.mybatis.mapper.annotation.KeySql;

import javax.persistence.Id;
import javax.persistence.Table;

@Table(name = "tb_specification")
@Data
public class Specification {

    @Id
    @KeySql(useGeneratedKeys = true)
    private Long categoryId;
    private String specifications;

}
