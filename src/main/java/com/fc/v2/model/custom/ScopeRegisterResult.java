package com.fc.v2.model.custom;

import com.fc.v2.model.auto.TMgisScopeCard;
import com.fc.v2.model.auto.TMgisScopeCardCategory;
import com.fc.v2.model.auto.TMgisScopeHandler;
import com.fc.v2.model.auto.TMgisScopeSource;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * 立卡回话：接口怎么回、账就怎么记。
 *
 * <p>同一单位同一段范围第二回递来（或抄送、自报并发同递一件），系统不重立，
 * 把头一笔原样回、{@code duplicated=true}——账上不许留两笔。</p>
 *
 * @author fuce
 * @date 2026-09-27
 */
public class ScopeRegisterResult implements Serializable {
    private static final long serialVersionUID = 1L;

    /** 落定的卡 */
    private TMgisScopeCard card;

    /** 卡上末级类别 */
    private List<TMgisScopeCardCategory> categories;

    /** 卡上出处（抄送/自报并一笔） */
    private List<TMgisScopeSource> sources;

    /** 经手手续链（头一条即立卡那一笔） */
    private List<TMgisScopeHandler> handlers;

    /** 是否为重卡：true 表示这一回没有新立，回的是头一笔 */
    private boolean duplicated;

    public ScopeRegisterResult() {
    }

    public ScopeRegisterResult(TMgisScopeCard card, List<TMgisScopeCardCategory> categories,
                               List<TMgisScopeSource> sources, List<TMgisScopeHandler> handlers,
                               boolean duplicated) {
        this.card = card;
        this.categories = categories == null ? Collections.<TMgisScopeCardCategory>emptyList() : categories;
        this.sources = sources == null ? Collections.<TMgisScopeSource>emptyList() : sources;
        this.handlers = handlers == null ? Collections.<TMgisScopeHandler>emptyList() : handlers;
        this.duplicated = duplicated;
    }

    public TMgisScopeCard getCard() {
        return card;
    }

    public void setCard(TMgisScopeCard card) {
        this.card = card;
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

    public boolean isDuplicated() {
        return duplicated;
    }

    public void setDuplicated(boolean duplicated) {
        this.duplicated = duplicated;
    }
}
