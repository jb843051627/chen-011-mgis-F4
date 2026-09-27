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
 * 范围卡出处对象 t_mgis_scope_source。
 *
 * <p>市局抄送（CITY_COPY）与单位自报（SELF_REPORT）常常是同一件事，并在
 * 同一张卡上，账上一张卡每类出处至多一条，不许留两笔。来文/自报单号一径
 * 由人工填写，系统不代拟。只增不改。</p>
 *
 * @author fuce
 * @date 2026-09-27
 */
@TableName("t_mgis_scope_source")
@ApiModel(value = "TMgisScopeSource", description = "范围卡出处")
public class TMgisScopeSource implements Serializable {
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

    /** 出处 CITY_COPY市局抄送 SELF_REPORT单位自报 */
    @TableField("source_type")
    @ApiModelProperty(value = "出处 CITY_COPY市局抄送 SELF_REPORT单位自报")
    private String sourceType;

    /** 来文/自报单号（一径由人工填写，系统不代拟） */
    @TableField("source_doc_no")
    @ApiModelProperty(value = "来文/自报单号(人工填写)")
    private String sourceDocNo;

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

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceDocNo() {
        return sourceDocNo;
    }

    public void setSourceDocNo(String sourceDocNo) {
        this.sourceDocNo = sourceDocNo;
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
