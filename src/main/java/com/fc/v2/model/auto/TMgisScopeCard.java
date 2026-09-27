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
import java.math.BigDecimal;
import java.util.Date;

/**
 * 作业范围标签建档卡对象 t_mgis_scope_card。
 *
 * <p>一张卡只讲一段范围：卡号系统给号，同一单位同一段范围（范围签名相同）
 * 只许一张；挂用类别数撞档案三道顶线折算层级档，层级档不许人手填。
 * 卡只立不改：不设 update_by/update_time/del_flag，服务层没有改卡、删卡入口；
 * 老卡查得到，但档案注销后不能从老卡上接着添东西（手续门也焊死）。</p>
 *
 * @author fuce
 * @date 2026-09-27
 */
@TableName("t_mgis_scope_card")
@ApiModel(value = "TMgisScopeCard", description = "作业范围标签建档卡")
public class TMgisScopeCard implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 主键 */
    @TableId(type = IdType.ASSIGN_ID)
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "主键")
    private Long id;

    /** 标签卡号（系统给号 FW+雪花；同一单位同段范围第二张顶回） */
    @TableField("card_no")
    @ApiModelProperty(value = "标签卡号(系统给号)")
    private String cardNo;

    /** 资质单位基础档案主键（备查；对档案只认单位编号） */
    @TableField("unit_id")
    @JsonSerialize(using = ToStringSerializer.class)
    @ApiModelProperty(value = "所属资质单位基础档案主键")
    private Long unitId;

    /** 资质单位编号（立卡按此对档案，只按名称匹配不作数） */
    @TableField("unit_no")
    @ApiModelProperty(value = "资质单位编号")
    private String unitNo;

    /** 范围签名：本卡末级类别代号升序逗号拼接（同单位同签名即同一段范围） */
    @TableField("scope_sign")
    @ApiModelProperty(value = "范围签名(末级类别代号升序拼接)")
    private String scopeSign;

    /** 范围签名 SHA-256（定长；唯一约束押它，免得长签名越过索引长度） */
    @TableField("scope_hash")
    @ApiModelProperty(value = "范围签名SHA-256")
    private String scopeHash;

    /** 挂用类别数（项，正整数；即末级类别条数） */
    @TableField("scope_count")
    @ApiModelProperty(value = "挂用类别数(项)")
    private Integer scopeCount;

    /** 本年度新增挂用类别数（立卡时与该单位老卡比对，系统算出） */
    @TableField("new_category_count")
    @ApiModelProperty(value = "本年度新增挂用类别数(项)")
    private Integer newCategoryCount;

    /** 立卡年度（新增类别按本年度口径比对） */
    @TableField("registered_year")
    @ApiModelProperty(value = "立卡年度")
    private Integer registeredYear;

    /** 层级档 1/2/3（系统撞档案三道顶线折算；等号按高一档） */
    @TableField("grade_level")
    @ApiModelProperty(value = "层级档 1一档 2二档 3三档")
    private Integer gradeLevel;

    /** 层级档系数 = 挂用类别数 ÷ 所在档顶线，两位小数（系统折算） */
    @TableField("grade_score")
    @ApiModelProperty(value = "层级档系数(挂用类别数÷所在档顶线,两位小数)")
    private BigDecimal gradeScore;

    /** 手续进展 0待核对 1已核对 2已定档（只由经手手续推进） */
    @TableField("status")
    @ApiModelProperty(value = "手续进展 0待核对 1已核对 2已定档")
    private Integer status;

    /** 创建人：按下立卡那一下是谁就是谁，整张卡重录也不许挪 */
    @TableField(value = "create_by", fill = FieldFill.INSERT)
    @ApiModelProperty(value = "创建人")
    private String createBy;

    /** 创建时间 */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "创建时间")
    private Date createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCardNo() {
        return cardNo;
    }

    public void setCardNo(String cardNo) {
        this.cardNo = cardNo;
    }

    public Long getUnitId() {
        return unitId;
    }

    public void setUnitId(Long unitId) {
        this.unitId = unitId;
    }

    public String getUnitNo() {
        return unitNo;
    }

    public void setUnitNo(String unitNo) {
        this.unitNo = unitNo;
    }

    public String getScopeSign() {
        return scopeSign;
    }

    public void setScopeSign(String scopeSign) {
        this.scopeSign = scopeSign;
    }

    public String getScopeHash() {
        return scopeHash;
    }

    public void setScopeHash(String scopeHash) {
        this.scopeHash = scopeHash;
    }

    public Integer getScopeCount() {
        return scopeCount;
    }

    public void setScopeCount(Integer scopeCount) {
        this.scopeCount = scopeCount;
    }

    public Integer getNewCategoryCount() {
        return newCategoryCount;
    }

    public void setNewCategoryCount(Integer newCategoryCount) {
        this.newCategoryCount = newCategoryCount;
    }

    public Integer getRegisteredYear() {
        return registeredYear;
    }

    public void setRegisteredYear(Integer registeredYear) {
        this.registeredYear = registeredYear;
    }

    public Integer getGradeLevel() {
        return gradeLevel;
    }

    public void setGradeLevel(Integer gradeLevel) {
        this.gradeLevel = gradeLevel;
    }

    public BigDecimal getGradeScore() {
        return gradeScore;
    }

    public void setGradeScore(BigDecimal gradeScore) {
        this.gradeScore = gradeScore;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
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
