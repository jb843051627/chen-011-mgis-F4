package com.fc.v2.model.custom;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

import java.io.Serializable;
import java.util.List;

/**
 * 立卡（登记）请求：进门只认「登记」一处。
 *
 * <p>页面不许预填硬数：卡号、层级档、创建人、新增类别数都不由请求带；
 * 单号（出处文号）一径由人工填写；校验的活儿不落进页面，全在服务层。</p>
 *
 * @author fuce
 * @date 2026-09-27
 */
@ApiModel(value = "ScopeRegisterRequest", description = "作业范围卡立卡请求")
public class ScopeRegisterRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 资质单位编号（立卡按此对档案；只按名称匹配不作数） */
    @ApiModelProperty(value = "资质单位编号(必填;按编号对档案)")
    private String unitNo;

    /** 单位名称（只作核对，重名不认） */
    @ApiModelProperty(value = "单位名称(只核对,同名不作数)")
    private String unitName;

    /** 挂用类别数填报原值（正整数；负数/零/非数/越顶线照原值拦） */
    @ApiModelProperty(value = "挂用类别数(填报原值)")
    private String scopeCount;

    /** 本卡所挂专业类别代号（只许现行目录末级） */
    @ApiModelProperty(value = "挂用专业类别代号列表(只许末级)")
    private List<String> categories;

    /** 出处（市局抄送、单位自报可同卡并一笔；文号人工填） */
    @ApiModelProperty(value = "出处列表(抄送/自报同一事件并一卡)")
    private List<SourceReq> sources;

    /** 附言（可空，随立卡那一条经手记下） */
    @ApiModelProperty(value = "附言(可空)")
    private String note;

    public static class SourceReq implements Serializable {
        private static final long serialVersionUID = 1L;

        /** 出处类型 CITY_COPY市局抄送 SELF_REPORT单位自报 */
        @ApiModelProperty(value = "出处类型 CITY_COPY/SELF_REPORT")
        private String sourceType;

        /** 来文/自报单号（人工填写，系统不代拟） */
        @ApiModelProperty(value = "来文/自报单号(人工填写)")
        private String docNo;

        public SourceReq() {
        }

        public SourceReq(String sourceType, String docNo) {
            this.sourceType = sourceType;
            this.docNo = docNo;
        }

        public String getSourceType() {
            return sourceType;
        }

        public void setSourceType(String sourceType) {
            this.sourceType = sourceType;
        }

        public String getDocNo() {
            return docNo;
        }

        public void setDocNo(String docNo) {
            this.docNo = docNo;
        }
    }

    public String getUnitNo() {
        return unitNo;
    }

    public void setUnitNo(String unitNo) {
        this.unitNo = unitNo;
    }

    public String getUnitName() {
        return unitName;
    }

    public void setUnitName(String unitName) {
        this.unitName = unitName;
    }

    public String getScopeCount() {
        return scopeCount;
    }

    public void setScopeCount(String scopeCount) {
        this.scopeCount = scopeCount;
    }

    public List<String> getCategories() {
        return categories;
    }

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public List<SourceReq> getSources() {
        return sources;
    }

    public void setSources(List<SourceReq> sources) {
        this.sources = sources;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
