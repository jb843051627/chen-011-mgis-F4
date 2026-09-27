package com.fc.v2.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.fc.v2.common.scope.ScopeLedger;
import com.fc.v2.model.auto.TMgisProfCategory;
import com.fc.v2.model.auto.TMgisScopeCard;
import com.fc.v2.model.auto.TMgisUnitDoc;
import com.fc.v2.model.custom.ScopeCardView;
import com.fc.v2.model.custom.ScopeRegisterRequest;
import com.fc.v2.model.custom.ScopeRegisterResult;

import java.util.List;

/**
 * 作业范围标签建档卡 Service接口。
 *
 * <p>2026-09-12 自动生成的六个方法签名一仍其旧（旧门脸 {@code MgisScopeCardController}
 * 仍编得过）；立卡新规矩另起 {@link #register} 等新方法，扩功能走新重载，
 * 不在旧签名上动刀。旧写入门在实现里已封死：卡只立不改，添卡只走登记门。</p>
 *
 * @author fuce
 * @date 2026-09-12
 */
public interface ITMgisScopeCardService {

    /** 按主键查询（旧签名，老卡照旧查得到） */
    TMgisScopeCard selectTMgisScopeCardById(Long id);

    /** 按条件查询列表（旧签名；分页由调用方统一处理） */
    List<TMgisScopeCard> selectTMgisScopeCardList(Wrapper<TMgisScopeCard> queryWrapper);

    /**
     * 新增（旧签名，已封）：新卡一律走 {@link #register} 登记门，
     * 此门不再落账——卡必须有出处、层级档由系统折算，旧 CRUD 绕不过章程。
     */
    int insertTMgisScopeCard(TMgisScopeCard record);

    /** 修改（旧签名，已封）：卡只立不改 */
    int updateTMgisScopeCard(TMgisScopeCard record);

    /** 批量删除（旧签名，已封）：卡只立不改 */
    int deleteTMgisScopeCardByIds(String ids);

    /** 按主键删除（旧签名，已封）：卡只立不改 */
    int deleteTMgisScopeCardById(Long id);

    // ---------- 新规矩（2026-09-27 起，新门脸 /mgisScope） ----------

    /**
     * 登记立卡（唯一写卡门）。按单位编号对档案、同名不作数；注销档案不收新卡；
     * 同单位同段范围第二张顶回（抄送/自报同一件的新出处并到原卡，不留两笔）；
     * 挂用类别数与末级类别逐项核、层级档系统折算；创建人取按下立卡那一下的人。
     */
    ScopeRegisterResult register(ScopeRegisterRequest req, String operator);

    /** 一张卡翻账：抬头、末级类别、出处、经手链拉齐，层级档按最新档案当场重折 */
    ScopeCardView view(Long id);

    /**
     * 列表翻账。regionCity 非空只出该市单位的卡（属地一头），空着出全省（省局一头）；
     * unitNo 非空再按单位编号精确收窄。
     */
    List<ScopeCardView> listViews(String regionCity, String unitNo);

    /**
     * 过一道内部手续（核对/定档），每道记一条经手，只顺着走、只认头一笔；
     * 档案注销的老卡冻结，不能从老卡上接着添东西。
     */
    ScopeCardView handle(Long cardId, String action, String operator, String note);

    /**
     * 看账一屏：省局按单位（新增类别最多的往下排）、属地按最末一级归堆，
     * 两头卡数当场对，差一笔即抛账。regionCity 非空为属地一市，空着为全省。
     */
    ScopeLedger ledger(String regionCity);

    /** 现行专业类别目录（卡上挂类别的页面下拉与校验同此一处） */
    List<TMgisProfCategory> currentCatalog();

    /** 按资质单位编号调档案（只按名称匹配不作数）；不在账返 null */
    TMgisUnitDoc findUnitDoc(String unitNo);
}
