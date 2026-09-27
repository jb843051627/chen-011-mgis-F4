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
 * 测绘专业类别目录树对象 t_mgis_prof_category。
 *
 * <p>一级是大类，二级才是具体作业类别（末级），卡上只许挂到末级。废止类别
 * 保留在目录里并指着并到哪个现行类别（{@code mergedInto}），挂旧类别的卡
 * 不给落。目录由资质监督处维护，立卡只读取。</p>
 *
 * @author fuce
 * @date 2026-09-27
 */
@TableName("t_mgis_prof_category")
@ApiModel(value = "TMgisProfCategory", description = "测绘专业类别目录树")
public class TMgisProfCategory implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 类别代号（大类 G01..G10，末级 G0101..） */
    @TableField("cat_code")
    @ApiModelProperty(value = "类别代号")
    private String catCode;

    /** 类别名称 */
    @TableField("cat_name")
    @ApiModelProperty(value = "类别名称")
    private String catName;

    /** 父级代号（一级大类为空；末级指大类） */
    @TableField("parent_code")
    @ApiModelProperty(value = "父级代号")
    private String parentCode;

    /** 层级 1大类 2具体作业类别（末级） */
    @TableField("cat_level")
    @ApiModelProperty(value = "层级 1大类 2末级")
    private Integer catLevel;

    /** 是否末级 0否 1是（卡上只许挂末级） */
    @TableField("leaf_flag")
    @ApiModelProperty(value = "是否末级 0否 1是")
    private Integer leafFlag;

    /** 情形 CURRENT现行 REVOKED已废止（早已并进别处） */
    @TableField("cat_status")
    @ApiModelProperty(value = "情形 CURRENT现行 REVOKED废止")
    private String catStatus;

    /** 废止后并入的现行末级代号（挂旧类别不给落，指它去哪） */
    @TableField("merged_into")
    @ApiModelProperty(value = "废止后并入的末级代号")
    private String mergedInto;

    @TableField("del_flag")
    private Integer delFlag;

    @TableField(value = "create_by", fill = FieldFill.INSERT)
    private String createBy;

    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date createTime;

    @TableField(value = "update_by", fill = FieldFill.UPDATE)
    private String updateBy;

    @TableField(value = "update_time", fill = FieldFill.UPDATE)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date updateTime;

    @TableField("remark")
    private String remark;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCatCode() {
        return catCode;
    }

    public void setCatCode(String catCode) {
        this.catCode = catCode;
    }

    public String getCatName() {
        return catName;
    }

    public void setCatName(String catName) {
        this.catName = catName;
    }

    public String getParentCode() {
        return parentCode;
    }

    public void setParentCode(String parentCode) {
        this.parentCode = parentCode;
    }

    public Integer getCatLevel() {
        return catLevel;
    }

    public void setCatLevel(Integer catLevel) {
        this.catLevel = catLevel;
    }

    public Integer getLeafFlag() {
        return leafFlag;
    }

    public void setLeafFlag(Integer leafFlag) {
        this.leafFlag = leafFlag;
    }

    public String getCatStatus() {
        return catStatus;
    }

    public void setCatStatus(String catStatus) {
        this.catStatus = catStatus;
    }

    public String getMergedInto() {
        return mergedInto;
    }

    public void setMergedInto(String mergedInto) {
        this.mergedInto = mergedInto;
    }

    public Integer getDelFlag() {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag) {
        this.delFlag = delFlag;
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

    public String getUpdateBy() {
        return updateBy;
    }

    public void setUpdateBy(String updateBy) {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime) {
        this.updateTime = updateTime;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
