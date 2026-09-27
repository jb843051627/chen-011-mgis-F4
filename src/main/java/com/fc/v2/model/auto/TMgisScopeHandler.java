package com.fc.v2.model.auto;

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
 * 范围卡经手手续链对象 t_mgis_scope_handler。
 *
 * <p>每过一道手续记一条：立卡（REGISTER）、核对（CHECK）、定档（CONFIRM），
 * 只增不改。经手人与卡上创建人是两个位置，不许互相顶——后来人把整张卡
 * 重录一遍，创建人仍是按下立卡那一下的人，经手链上只添新的一条。
 * 落笔时辰由服务层写死，精确到秒。</p>
 *
 * @author fuce
 * @date 2026-09-27
 */
@TableName("t_mgis_scope_handler")
@ApiModel(value = "TMgisScopeHandler", description = "范围卡经手手续链")
public class TMgisScopeHandler implements Serializable {
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

    /** 卡内手续序号（立卡为 1，逐道加一） */
    @TableField("seq")
    @ApiModelProperty(value = "卡内手续序号")
    private Integer seq;

    /** 手续 REGISTER立卡 CHECK核对 CONFIRM定档 */
    @TableField("action")
    @ApiModelProperty(value = "手续 REGISTER立卡 CHECK核对 CONFIRM定档")
    private String action;

    /** 经手人（落笔那一下自登录态取，请求代报不算） */
    @TableField("handler")
    @ApiModelProperty(value = "经手人")
    private String handler;

    /** 落笔时辰（服务层写死，精确到秒） */
    @TableField("handled_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @ApiModelProperty(value = "落笔时辰")
    private Date handledAt;

    /** 附言（可空） */
    @TableField("note")
    @ApiModelProperty(value = "附言")
    private String note;

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

    public Integer getSeq() {
        return seq;
    }

    public void setSeq(Integer seq) {
        this.seq = seq;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getHandler() {
        return handler;
    }

    public void setHandler(String handler) {
        this.handler = handler;
    }

    public Date getHandledAt() {
        return handledAt;
    }

    public void setHandledAt(Date handledAt) {
        this.handledAt = handledAt;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }
}
