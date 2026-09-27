package com.fc.v2.model.custom;

import com.fc.v2.common.scope.ScopeGrade;
import com.fc.v2.model.auto.TMgisScopeCard;
import com.fc.v2.model.auto.TMgisScopeCardCategory;
import com.fc.v2.model.auto.TMgisScopeHandler;
import com.fc.v2.model.auto.TMgisScopeSource;
import com.fc.v2.model.auto.TMgisUnitDoc;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 一张卡的翻账页面：抬头、末级类别、出处、经手链当场拉齐。
 *
 * <p>层级档两份并列：立卡时折的一份存在卡上（{@code card.gradeLevel}）；
 * 当前一份拿最新档案当场重折（{@code currentGrade}）——类别有增减、单位改了
 * 属地，页面认的是重折的这一份。同一条卡省局、属地两头点开是同一笔账。</p>
 *
 * @author fuce
 * @date 2026-09-27
 */
public class ScopeCardView implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 卡（卡号、单位编号、范围签名、立卡折的层级档、创建人） */
    private TMgisScopeCard card;

    /** 单位档案抬头（名称、性质、注册地省—市—区县、三道顶线；注销标记照显） */
    private TMgisUnitDoc unit;

    /** 卡上末级类别 */
    private List<TMgisScopeCardCategory> categories;

    /** 出处（抄送/自报） */
    private List<TMgisScopeSource> sources;

    /** 经手手续链（立卡/核对/定档，只增不改） */
    private List<TMgisScopeHandler> handlers;

    /** 按最新档案当场重折的层级档（档案注销照显老卡，但无当前档可折） */
    private ScopeGrade currentGrade;

    public ScopeCardView() {
    }

    public ScopeCardView(TMgisScopeCard card, TMgisUnitDoc unit,
                         List<TMgisScopeCardCategory> categories, List<TMgisScopeSource> sources,
                         List<TMgisScopeHandler> handlers, ScopeGrade currentGrade) {
        this.card = card;
        this.unit = unit;
        this.categories = categories == null ? Collections.<TMgisScopeCardCategory>emptyList() : categories;
        this.sources = sources == null ? Collections.<TMgisScopeSource>emptyList() : sources;
        this.handlers = handlers == null ? Collections.<TMgisScopeHandler>emptyList() : handlers;
        this.currentGrade = currentGrade;
    }

    public TMgisScopeCard getCard() {
        return card;
    }

    public void setCard(TMgisScopeCard card) {
        this.card = card;
    }

    public TMgisUnitDoc getUnit() {
        return unit;
    }

    public void setUnit(TMgisUnitDoc unit) {
        this.unit = unit;
    }

    public List<TMgisScopeCardCategory> getCategories() {
        return categories;
    }

    public void setCategories(List<TMgisScopeCardCategory> categories) {
        this.categories = categories;
    }

    public List<TMgisScopeSource> getSources() {
        return sources;
    }

    public void setSources(List<TMgisScopeSource> sources) {
        this.sources = sources;
    }

    public List<TMgisScopeHandler> getHandlers() {
        return handlers;
    }

    public void setHandlers(List<TMgisScopeHandler> handlers) {
        this.handlers = handlers;
    }

    public ScopeGrade getCurrentGrade() {
        return currentGrade;
    }

    public void setCurrentGrade(ScopeGrade currentGrade) {
        this.currentGrade = currentGrade;
    }
}
