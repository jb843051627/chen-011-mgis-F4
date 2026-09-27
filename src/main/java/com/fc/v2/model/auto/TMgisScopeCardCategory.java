package com.fc.v2.model.auto;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.Date;

/**
 * 范围卡挂用类别明细对象 t_mgis_scope_card_category。
 *
 * <p>一条一项，只许挂现行目录的末级；大类与末级混挂、挂已废止并入别处的
 * 旧类别，在立卡推演那一步即顶回，一条不落。只增不改：无 update/del_flag。</p>
 *
 * @author fuce
 * @date 2026-09-27
 */
@TableName("t_mgis_scope_card_category")
@ApiModel(value = "TMgisScopeCardCategory", description = "范围卡挂用类别明细")
public class TMgisScopeCardCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 范围卡主键 */
    @TableField("card_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "范围卡主键")
    private Long cardId;

    /** 资质单位编号（冗余；按单位比对新旧类别算新增数用） */
    @TableField("unit_no")
    @ApiModelProperty(value = "资质单位编号")
    private String unitNo;

    /** 末级专业类别代号（现行目录；废止/大类不给落） */
    @TableField("cat_code")
    @ApiModelProperty(value = "末级专业类别代号")
    private String catCode;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCardId() {
        return cardId;
    }

    public void setCardId(Long cardId) {
        this.cardId = cardId;
    }

    public String getUnitNo() {
        return unitNo;
    }

    public void setUnitNo(String unitNo) {
        this.unitNo = unitNo;
    }

    public String getCatCode() {
        return catCode;
    }

    public void setCatCode(String catCode) {
        this.catCode = catCode;
    }

    public String getCreateBy() {
        return createBy;
    }

    public void setCreateBy(String createBy) {
        this.createBy = createBy;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }
}
