package com.fc.v2.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.fc.v2.common.scope.ScopeCardJudge;
import com.fc.v2.common.scope.ScopeField;
import com.fc.v2.common.scope.ScopeGrade;
import com.fc.v2.common.scope.ScopeHandleAction;
import com.fc.v2.common.scope.ScopeLedger;
import com.fc.v2.common.scope.ScopeRegisterException;
import com.fc.v2.common.scope.ScopeSourceType;
import com.fc.v2.mapper.auto.TMgisProfCategoryMapper;
import com.fc.v2.mapper.auto.TMgisScopeCardCategoryMapper;
import com.fc.v2.mapper.auto.TMgisScopeCardMapper;
import com.fc.v2.mapper.auto.TMgisScopeHandlerMapper;
import com.fc.v2.mapper.auto.TMgisScopeSourceMapper;
import com.fc.v2.mapper.auto.TMgisUnitDocMapper;
import com.fc.v2.model.auto.TMgisProfCategory;
import com.fc.v2.model.auto.TMgisScopeCard;
import com.fc.v2.model.auto.TMgisScopeCardCategory;
import com.fc.v2.model.auto.TMgisScopeHandler;
import com.fc.v2.model.auto.TMgisScopeSource;
import com.fc.v2.model.auto.TMgisUnitDoc;
import com.fc.v2.model.custom.ScopeCardView;
import com.fc.v2.model.custom.ScopeRegisterRequest;
import com.fc.v2.model.custom.ScopeRegisterResult;
import com.fc.v2.service.ITMgisScopeCardService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 作业范围标签建档卡 Service业务层处理。
 *
 * <p>立卡章程的唯一出处在 {@link ScopeCardJudge}，本层只管对档案、落账、翻账：</p>
 * <ul>
 *   <li>进门只认 {@code register} 一处：卡号系统给号，页面不预填硬数，
 *       校验全压在本层，灵不灵以接口回传为凭，出处单号一径人工填写；</li>
 *   <li>立卡按资质单位编号对档案，同名不作数；注销档案不收新卡，老卡照查
 *       但冻结，不能从老卡上接着添东西；</li>
 *   <li>同单位同段范围第二张顶回，抄送/自报同一件的新出处并进原卡，账上
 *       不留两笔；创建人按下立卡那一下写死，经手每道手续另记一条，两个位置
 *       不许互相顶；</li>
 *   <li>层级档落账时折一份，翻账按最新档案当场重折，单位改了属地也照重折。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-09-27
 */
@Service
public class TMgisScopeCardServiceImpl implements ITMgisScopeCardService {

    /** 档案情形：已注销 */
    private static final int ARCH_REVOKED = 1;

    @javax.annotation.Resource
    private TMgisScopeCardMapper mgisScopeCardMapper;

    @javax.annotation.Resource
    private TMgisUnitDocMapper mgisUnitDocMapper;

    @javax.annotation.Resource
    private TMgisProfCategoryMapper mgisProfCategoryMapper;

    @javax.annotation.Resource
    private TMgisScopeCardCategoryMapper mgisScopeCardCategoryMapper;

    @javax.annotation.Resource
    private TMgisScopeSourceMapper mgisScopeSourceMapper;

    @javax.annotation.Resource
    private TMgisScopeHandlerMapper mgisScopeHandlerMapper;

    // ---------- 旧签名（2026-09-12 门脸留档；写入门已封，读门照旧） ----------

    @Override
    public TMgisScopeCard selectTMgisScopeCardById(Long id) {
        return this.mgisScopeCardMapper.selectById(id);
    }

    @Override
    public List<TMgisScopeCard> selectTMgisScopeCardList(Wrapper<TMgisScopeCard> queryWrapper) {
        return this.mgisScopeCardMapper.selectList(queryWrapper);
    }

    @Override
    public int insertTMgisScopeCard(TMgisScopeCard record) {
        throw new IllegalStateException("旧写卡门已封：添卡只走登记门 /mgisScope/register");
    }

    @Override
    public int updateTMgisScopeCard(TMgisScopeCard record) {
        throw new IllegalStateException("旧改卡门已封：卡只立不改，手续进展只走 /mgisScope/handle");
    }

    @Override
    public int deleteTMgisScopeCardByIds(String ids) {
        throw new IllegalStateException("旧删卡门已封：卡只立不改，账上不删卡");
    }

    @Override
    public int deleteTMgisScopeCardById(Long id) {
        throw new IllegalStateException("旧删卡门已封：卡只立不改，账上不删卡");
    }

    // ---------- 新规矩 ----------

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScopeRegisterResult register(ScopeRegisterRequest req, String operator) {
        if (req == null) {
            throw new ScopeRegisterException("立卡请求为空：进门只认「登记」一处");
        }
        if (operator == null || operator.trim().isEmpty()) {
            throw new ScopeRegisterException("创建人未认：按下立卡那一下是谁，账上就记谁");
        }
        String who = operator.trim();

        // 先按资质单位编号对档案——只按名称匹配不作数，同名两家我市真有过
        String unitNo = trim(req.getUnitNo());
        if (unitNo == null) {
            throw new ScopeRegisterException("资质单位编号未填：立卡先按编号对档案，只按名称匹配不作数",
                    ScopeField.UNIT_NO);
        }
        TMgisUnitDoc arch = findUnitDoc(unitNo);
        if (arch == null) {
            throw new ScopeRegisterException("资质单位编号 " + unitNo + " 对不上在册基础档案：名称再像也不作数",
                    ScopeField.UNIT_NO);
        }
        if (arch.getStatus() != null && arch.getStatus() == ARCH_REVOKED) {
            throw new ScopeRegisterException("该档案已注销：往后不再收它的新卡；老卡照旧查得到",
                    ScopeField.UNIT_NO);
        }

        // 类别先归置（重递认头一笔要靠它算范围签名）：只许现行末级，混挂/废止不给落
        List<String> leafCodes = ScopeCardJudge.normalizeCategories(req.getCategories(), catalogMap());
        String sign = ScopeCardJudge.scopeSign(leafCodes);
        String hash = ScopeCardJudge.scopeHash(sign);

        // 出处：抄送/自报同一件事并在一张卡上，文号人工填，一类出处至多一条
        List<TMgisScopeSource> claimedSources = packSources(req);

        // 同单位同一段范围想再立一张：第二张顶回去；带新出处来的，并到原卡，不留两笔。
        // 重递认头一笔，先查在账——第二回哪怕数填坏了，也顶不掉、挡不住头一笔的账。
        TMgisScopeCard existing = findByScope(unitNo, hash);
        if (existing != null) {
            List<TMgisScopeSource> merged = mergeSources(existing.getId(), claimedSources, who);
            return new ScopeRegisterResult(existing, listCategories(existing.getId()),
                    merged, listHandlers(existing.getId()), true);
        }

        // 头一张才核挂用类别数（浑数、零、负数、超顶线都指这一栏），须与实挂条数对得上
        int scopeCount = ScopeCardJudge.parseScopeCount(req.getScopeCount());
        ScopeCardJudge.checkWithinCeiling(scopeCount, arch);
        ScopeCardJudge.checkCountMatches(scopeCount, leafCodes);

        // 本年度新增类别数：与该单位老卡上挂过的类别并集当场比对，系统算出
        int year = Calendar.getInstance().get(Calendar.YEAR);
        Set<String> prior = priorCategories(unitNo);
        int newCount = ScopeCardJudge.newCategoryCount(leafCodes, prior);

        // 层级档不许人手填：系统拿挂用类别数撞档案三道顶线折算，留两位小数
        ScopeGrade grade = ScopeCardJudge.grade(scopeCount, arch);

        long cardId = IdWorker.getId();
        TMgisScopeCard card = new TMgisScopeCard();
        card.setId(cardId);
        card.setCardNo("FW" + cardId);
        card.setUnitId(arch.getId());
        card.setUnitNo(unitNo);
        card.setScopeSign(sign);
        card.setScopeHash(hash);
        card.setScopeCount(scopeCount);
        card.setNewCategoryCount(newCount);
        card.setRegisteredYear(year);
        card.setGradeLevel(grade.getLevel());
        card.setGradeScore(grade.getScore());
        card.setStatus(ScopeHandleAction.REGISTER.getStatusAfter());
        card.setCreateBy(who);
        try {
            this.mgisScopeCardMapper.insert(card);
        } catch (DuplicateKeyException e) {
            // 抄送、自报并发同递一件：唯一键 (unit_no, scope_hash) 兜底，认头一笔
            TMgisScopeCard first = requireByScope(unitNo, hash);
            List<TMgisScopeSource> merged = mergeSources(first.getId(), claimedSources, who);
            return new ScopeRegisterResult(first, listCategories(first.getId()),
                    merged, listHandlers(first.getId()), true);
        }

        List<TMgisScopeCardCategory> cats = new ArrayList<TMgisScopeCardCategory>(leafCodes.size());
        for (String code : leafCodes) {
            TMgisScopeCardCategory c = new TMgisScopeCardCategory();
            c.setCardId(cardId);
            c.setUnitNo(unitNo);
            c.setCatCode(code);
            c.setCreateBy(who);
            this.mgisScopeCardCategoryMapper.insert(c);
            cats.add(c);
        }
        List<TMgisScopeSource> sources = new ArrayList<TMgisScopeSource>(claimedSources.size());
        for (TMgisScopeSource s : claimedSources) {
            s.setCardId(cardId);
            s.setCreateBy(who);
            this.mgisScopeSourceMapper.insert(s);
            sources.add(s);
        }
        // 经手链头一条：立卡；创建人是创建人、经手是经手，两个位置不互相顶
        TMgisScopeHandler registerPen = newHandler(cardId, 1, ScopeHandleAction.REGISTER, who, req.getNote());
        this.mgisScopeHandlerMapper.insert(registerPen);

        List<TMgisScopeHandler> handlers = new ArrayList<TMgisScopeHandler>(1);
        handlers.add(registerPen);
        return new ScopeRegisterResult(card, cats, sources, handlers, false);
    }

    @Override
    public ScopeCardView view(Long id) {
        TMgisScopeCard card = requireCard(id);
        TMgisUnitDoc arch = findUnitDoc(card.getUnitNo());
        return toView(card, arch);
    }

    @Override
    public List<ScopeCardView> listViews(String regionCity, String unitNo) {
        List<TMgisUnitDoc> units;
        QueryWrapper<TMgisUnitDoc> uw = new QueryWrapper<TMgisUnitDoc>().eq("del_flag", 0);
        // 属地的人打开列表只看得见本市的单位与卡；省局（市名空着）看得见全省
        if (regionCity != null && !regionCity.trim().isEmpty()) {
            uw.eq("region_city", regionCity.trim());
        }
        if (unitNo != null && !unitNo.trim().isEmpty()) {
            uw.eq("site_no", unitNo.trim());
        }
        units = this.mgisUnitDocMapper.selectList(uw);

        Map<String, TMgisUnitDoc> byNo = new LinkedHashMap<String, TMgisUnitDoc>();
        for (TMgisUnitDoc u : units) {
            byNo.put(u.getSiteNo(), u);
        }
        QueryWrapper<TMgisScopeCard> cw = new QueryWrapper<TMgisScopeCard>().orderByAsc("id");
        if (!byNo.isEmpty()) {
            cw.in("unit_no", new ArrayList<String>(byNo.keySet()));
        } else {
            // 该市一家在册单位也没有，列表即空——不能把全省的卡拉下来凑数
            return new ArrayList<ScopeCardView>();
        }
        List<TMgisScopeCard> cards = this.mgisScopeCardMapper.selectList(cw);
        List<ScopeCardView> views = new ArrayList<ScopeCardView>(cards.size());
        for (TMgisScopeCard card : cards) {
            views.add(toView(card, byNo.get(card.getUnitNo())));
        }
        return views;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScopeCardView handle(Long cardId, String actionRaw, String operator, String note) {
        if (operator == null || operator.trim().isEmpty()) {
            throw new ScopeRegisterException("经手人未认：每过一道手续，落笔那一下是谁就记谁");
        }
        TMgisScopeCard card = requireCard(cardId);
        ScopeHandleAction action = ScopeHandleAction.require(actionRaw);
        TMgisUnitDoc arch = findUnitDoc(card.getUnitNo());
        if (arch != null && arch.getStatus() != null && arch.getStatus() == ARCH_REVOKED) {
            // 档案注销后老卡照旧查得到，但不能从老卡上接着添东西
            throw new ScopeRegisterException("该单位档案已注销：老卡冻结，不能从老卡上接着添东西",
                    ScopeField.HANDLE_ACTION);
        }

        List<TMgisScopeHandler> chain = listHandlers(cardId);
        // 同一道手续第二回来的只认头一笔（哪怕卡已办到后一道），不重落、不添条
        if (findAction(chain, action) != null) {
            return toView(card, arch);
        }

        int current = card.getStatus() == null ? 0 : card.getStatus();
        if (current != action.getStatusAfter() - 1) {
            throw new ScopeRegisterException("卡停在「" + statusName(current) + "」，这一道「"
                    + action.getLabel() + "」落不下去：手续只许一道接一道顺走",
                    ScopeField.HANDLE_ACTION);
        }

        TMgisScopeHandler pen = newHandler(cardId, chain.size() + 1, action, operator.trim(), note);
        try {
            this.mgisScopeHandlerMapper.insert(pen);
        } catch (DuplicateKeyException e) {
            // 并发同办一道：唯一键 (card_id, action) 兜底，只认头一笔
            throw new ScopeRegisterException("这道手续已有人办过，只认头一笔；请刷新后再看",
                    ScopeField.HANDLE_ACTION);
        }
        // 卡上只许挪手续状态这一栏；其余内容一律不更新
        UpdateWrapper<TMgisScopeCard> upd = new UpdateWrapper<TMgisScopeCard>()
                .eq("id", cardId).eq("status", current)
                .set("status", action.getStatusAfter());
        int rows = this.mgisScopeCardMapper.update(null, upd);
        if (rows != 1) {
            throw new ScopeRegisterException("卡已被人办过手续，请刷新后重来", ScopeField.HANDLE_ACTION);
        }
        card.setStatus(action.getStatusAfter());
        return toView(card, arch);
    }

    @Override
    public ScopeLedger ledger(String regionCity) {
        List<ScopeCardView> views = listViews(regionCity, null);
        List<ScopeLedger.Row> rows = new ArrayList<ScopeLedger.Row>(views.size());
        for (ScopeCardView v : views) {
            TMgisScopeCard c = v.getCard();
            String county = v.getUnit() == null ? null : v.getUnit().getRegionCounty();
            String unitName = v.getUnit() == null ? null : v.getUnit().getSiteName();
            rows.add(new ScopeLedger.Row(c.getUnitNo(), unitName, county,
                    c.getNewCategoryCount() == null ? 0 : c.getNewCategoryCount()));
        }
        // 省局按单位排、属地按最末一级归堆：两途当场对卡数，差一笔即抛账
        return ScopeCardJudge.ledger(rows);
    }

    @Override
    public List<TMgisProfCategory> currentCatalog() {
        return this.mgisProfCategoryMapper.selectList(new QueryWrapper<TMgisProfCategory>()
                .eq("del_flag", 0).eq("cat_status", "CURRENT").orderByAsc("cat_code"));
    }

    @Override
    public TMgisUnitDoc findUnitDoc(String unitNo) {
        if (unitNo == null || unitNo.trim().isEmpty()) {
            return null;
        }
        return this.mgisUnitDocMapper.selectOne(new QueryWrapper<TMgisUnitDoc>()
                .eq("site_no", unitNo.trim()).eq("del_flag", 0));
    }

    // ---------- 翻账 ----------

    /** 卡抬头、明细、出处、经手链当场拉齐；层级档按最新档案重折，折不动显 null */
    private ScopeCardView toView(TMgisScopeCard card, TMgisUnitDoc arch) {
        ScopeGrade current = null;
        if (arch != null && (arch.getStatus() == null || arch.getStatus() != ARCH_REVOKED)) {
            try {
                current = ScopeCardJudge.regrade(card, arch);
            } catch (ScopeRegisterException ignore) {
                // 档案顶线调到卡上挂用数之下：老卡照查，当前档折不动留空，不遮盖
                current = null;
            }
        }
        return new ScopeCardView(card, arch, listCategories(card.getId()),
                listSources(card.getId()), listHandlers(card.getId()), current);
    }

    private Map<String, TMgisProfCategory> catalogMap() {
        List<TMgisProfCategory> all = this.mgisProfCategoryMapper.selectList(
                new QueryWrapper<TMgisProfCategory>().eq("del_flag", 0));
        Map<String, TMgisProfCategory> map = new LinkedHashMap<String, TMgisProfCategory>();
        for (TMgisProfCategory c : all) {
            map.put(c.getCatCode(), c);
        }
        return map;
    }

    /** 出处核成实体：类型要识、文号必填（一径人工填）、同一请求一类只许一条 */
    private List<TMgisScopeSource> packSources(ScopeRegisterRequest req) {
        if (req.getSources() == null || req.getSources().isEmpty()) {
            throw new ScopeRegisterException("出处未填：卡必须有出处，市局抄送或单位自报至少挂一条",
                    ScopeField.SOURCE_TYPE);
        }
        List<TMgisScopeSource> out = new ArrayList<TMgisScopeSource>(req.getSources().size());
        Set<String> seen = new HashSet<String>();
        for (ScopeRegisterRequest.SourceReq r : req.getSources()) {
            if (r == null) {
                throw new ScopeRegisterException("出处里夹了一条空的", ScopeField.SOURCE_TYPE);
            }
            ScopeSourceType type = ScopeSourceType.tryOf(r.getSourceType());
            if (type == null) {
                throw new ScopeRegisterException("出处不识：「" + r.getSourceType()
                        + "」；只认 市局抄送 / 单位自报", ScopeField.SOURCE_TYPE, r.getSourceType());
            }
            String docNo = trim(r.getDocNo());
            if (docNo == null) {
                throw new ScopeRegisterException(type.getLabel() + "的来文/自报单号未填：单号一径由人工填写，系统不代拟",
                        ScopeField.SOURCE_DOC_NO);
            }
            if (!seen.add(type.name())) {
                throw new ScopeRegisterException(type.getLabel() + "出处一张卡上挂了两条：同一件事并成一条，账上不许留两笔",
                        ScopeField.SOURCE_TYPE);
            }
            TMgisScopeSource s = new TMgisScopeSource();
            s.setSourceType(type.name());
            s.setSourceDocNo(docNo);
            out.add(s);
        }
        return out;
    }

    /**
     * 重卡新出处并进原卡：已有的出处类型不重落，并完回原卡全量出处。
     * 并出处只是把同一件事的两笔账并一笔，不是新手续，不经手链上添条。
     */
    private List<TMgisScopeSource> mergeSources(Long cardId, List<TMgisScopeSource> claimed, String operator) {
        List<TMgisScopeSource> existing = listSources(cardId);
        Set<String> have = new HashSet<String>();
        for (TMgisScopeSource s : existing) {
            have.add(s.getSourceType());
        }
        for (TMgisScopeSource s : claimed) {
            if (have.add(s.getSourceType())) {
                s.setId(null);
                s.setCardId(cardId);
                s.setCreateBy(operator);
                this.mgisScopeSourceMapper.insert(s);
                existing.add(s);
            }
        }
        return existing;
    }

    private TMgisScopeCard findByScope(String unitNo, String scopeHash) {
        return this.mgisScopeCardMapper.selectOne(new QueryWrapper<TMgisScopeCard>()
                .eq("unit_no", unitNo).eq("scope_hash", scopeHash));
    }

    private TMgisScopeCard requireByScope(String unitNo, String scopeHash) {
        TMgisScopeCard card = findByScope(unitNo, scopeHash);
        if (card == null) {
            throw new ScopeRegisterException("并发立卡兜底落空：单位 " + unitNo + " 同段范围的卡不见了");
        }
        return card;
    }

    private TMgisScopeCard requireCard(Long id) {
        if (id == null) {
            throw new ScopeRegisterException("卡号未指");
        }
        TMgisScopeCard card = this.mgisScopeCardMapper.selectById(id);
        if (card == null) {
            throw new ScopeRegisterException("卡不在账上：" + id);
        }
        return card;
    }

    private List<TMgisScopeCardCategory> listCategories(Long cardId) {
        return this.mgisScopeCardCategoryMapper.selectList(new QueryWrapper<TMgisScopeCardCategory>()
                .eq("card_id", cardId).orderByAsc("cat_code"));
    }

    private List<TMgisScopeSource> listSources(Long cardId) {
        return this.mgisScopeSourceMapper.selectList(new QueryWrapper<TMgisScopeSource>()
                .eq("card_id", cardId).orderByAsc("source_type"));
    }

    private List<TMgisScopeHandler> listHandlers(Long cardId) {
        return this.mgisScopeHandlerMapper.selectList(new QueryWrapper<TMgisScopeHandler>()
                .eq("card_id", cardId).orderByAsc("seq"));
    }

    /** 该单位老卡上挂过的全部末级类别（算本年度新增数比对用） */
    private Set<String> priorCategories(String unitNo) {
        List<TMgisScopeCardCategory> prior = this.mgisScopeCardCategoryMapper.selectList(
                new QueryWrapper<TMgisScopeCardCategory>().eq("unit_no", unitNo));
        Set<String> codes = new HashSet<String>();
        for (TMgisScopeCardCategory c : prior) {
            codes.add(c.getCatCode());
        }
        return codes;
    }

    private TMgisScopeHandler findAction(List<TMgisScopeHandler> chain, ScopeHandleAction action) {
        for (TMgisScopeHandler h : chain) {
            if (action.name().equals(h.getAction())) {
                return h;
            }
        }
        return null;
    }

    private static TMgisScopeHandler newHandler(Long cardId, int seq, ScopeHandleAction action,
                                                String handler, String note) {
        TMgisScopeHandler h = new TMgisScopeHandler();
        h.setCardId(cardId);
        h.setSeq(seq);
        h.setAction(action.name());
        h.setHandler(handler);
        h.setHandledAt(new java.util.Date(System.currentTimeMillis() / 1000L * 1000L));
        h.setNote(note == null ? null : note.trim());
        return h;
    }

    private static String statusName(int status) {
        switch (status) {
            case 0: return "待核对";
            case 1: return "已核对";
            case 2: return "定档";
            default: return "状态不明(" + status + ")";
        }
    }

    private static String trim(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
