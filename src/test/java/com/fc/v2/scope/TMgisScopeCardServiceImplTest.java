package com.fc.v2.scope;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fc.v2.common.scope.ScopeField;
import com.fc.v2.common.scope.ScopeHandleAction;
import com.fc.v2.common.scope.ScopeLedger;
import com.fc.v2.common.scope.ScopeRegisterException;
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
import com.fc.v2.service.impl.TMgisScopeCardServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 立卡工序单测：编号对档案（同名不作数）、注销拒收、同段范围第二张顶回、
 * 抄送/自报并一笔、创建人不挪与经手各记各的、新增类别数、核对定档冻结、
 * 省局/属地两视角、两头对账、旧写入门封死。
 */
class TMgisScopeCardServiceImplTest {

    private TMgisScopeCardMapper cardMapper;
    private TMgisUnitDocMapper unitMapper;
    private TMgisProfCategoryMapper catalogMapper;
    private TMgisScopeCardCategoryMapper catMapper;
    private TMgisScopeSourceMapper sourceMapper;
    private TMgisScopeHandlerMapper handlerMapper;
    private TMgisScopeCardServiceImpl service;

    private List<TMgisUnitDoc> units;
    private List<TMgisScopeCard> cards;
    private List<TMgisScopeCardCategory> cardCats;
    private List<TMgisScopeSource> sources;
    private List<TMgisScopeHandler> handlers;
    private List<TMgisProfCategory> catalog;
    private long idGen = 9000L;

    @BeforeEach
    void setUp() {
        cardMapper = mock(TMgisScopeCardMapper.class);
        unitMapper = mock(TMgisUnitDocMapper.class);
        catalogMapper = mock(TMgisProfCategoryMapper.class);
        catMapper = mock(TMgisScopeCardCategoryMapper.class);
        sourceMapper = mock(TMgisScopeSourceMapper.class);
        handlerMapper = mock(TMgisScopeHandlerMapper.class);
        service = new TMgisScopeCardServiceImpl();
        ReflectionTestUtils.setField(service, "mgisScopeCardMapper", cardMapper);
        ReflectionTestUtils.setField(service, "mgisUnitDocMapper", unitMapper);
        ReflectionTestUtils.setField(service, "mgisProfCategoryMapper", catalogMapper);
        ReflectionTestUtils.setField(service, "mgisScopeCardCategoryMapper", catMapper);
        ReflectionTestUtils.setField(service, "mgisScopeSourceMapper", sourceMapper);
        ReflectionTestUtils.setField(service, "mgisScopeHandlerMapper", handlerMapper);

        units = new ArrayList<TMgisUnitDoc>(Arrays.asList(unit("ZZ00", "江南省测绘院第一院", 0, "临川市", "城东区"),
                unit("ZZ01", "旧临川市城乡测绘服务中心", 1, "临川市", "城西区"),
                unit("QX10", "清溪市测绘队", 0, "清溪市", "清溪县"),
                unit("ZZ99", "江南省测绘院第一院", 0, "临川市", "城南区")));
        cards = new ArrayList<TMgisScopeCard>();
        cardCats = new ArrayList<TMgisScopeCardCategory>();
        sources = new ArrayList<TMgisScopeSource>();
        handlers = new ArrayList<TMgisScopeHandler>();
        catalog = new ArrayList<TMgisProfCategory>(Arrays.asList(
                prof("G04", "工程测量", null, 1, 0, "CURRENT", null),
                prof("G0401", "控制测量", "G04", 2, 1, "CURRENT", null),
                prof("G0402", "地形测量", "G04", 2, 1, "CURRENT", null),
                prof("G06", "界线与不动产测绘", null, 1, 0, "CURRENT", null),
                prof("G0601", "地籍测绘", "G06", 2, 1, "CURRENT", null),
                prof("G0603", "界线测绘", "G06", 2, 1, "CURRENT", null),
                prof("OLD_BOUNDARY", "行政区域界线测绘（旧目录）", null, 2, 1, "REVOKED", "G0603")));

        when(unitMapper.selectOne(any())).thenAnswer(inv -> {
            Map<String, Object> eq = eqPairs(inv.getArgument(0));
            String no = (String) eq.get("site_no");
            if (no == null) {
                return null;
            }
            for (TMgisUnitDoc u : units) {
                if (u.getSiteNo().equals(no)) {
                    return u;
                }
            }
            return null;
        });
        when(unitMapper.selectList(any())).thenAnswer(inv -> {
            Map<String, Object> eq = eqPairs(inv.getArgument(0));
            List<Object> in = inValues(inv.getArgument(0), "site_no");
            List<TMgisUnitDoc> found = new ArrayList<TMgisUnitDoc>();
            for (TMgisUnitDoc u : units) {
                if (eq.containsKey("region_city") && !eq.get("region_city").equals(u.getRegionCity())) {
                    continue;
                }
                if (eq.containsKey("site_no") && !eq.get("site_no").equals(u.getSiteNo())) {
                    continue;
                }
                if (!in.isEmpty() && !in.contains(u.getSiteNo())) {
                    continue;
                }
                found.add(u);
            }
            return found;
        });
        when(catalogMapper.selectList(any())).thenAnswer(inv -> new ArrayList<TMgisProfCategory>(catalog));
        when(cardMapper.selectById(any())).thenAnswer(inv -> {
            Long id = ((Number) inv.getArgument(0)).longValue();
            for (TMgisScopeCard c : cards) {
                if (c.getId().equals(id)) {
                    return c;
                }
            }
            return null;
        });
        when(cardMapper.selectOne(any())).thenAnswer(inv -> {
            Map<String, Object> eq = eqPairs(inv.getArgument(0));
            for (TMgisScopeCard c : cards) {
                boolean hit = true;
                for (Map.Entry<String, Object> e : eq.entrySet()) {
                    if ("unit_no".equals(e.getKey()) && !e.getValue().equals(c.getUnitNo())) {
                        hit = false;
                    }
                    if ("scope_hash".equals(e.getKey()) && !e.getValue().equals(c.getScopeHash())) {
                        hit = false;
                    }
                }
                if (hit) {
                    return c;
                }
            }
            return null;
        });
        when(cardMapper.selectList(any())).thenAnswer(inv -> {
            List<Object> unitNos = inValues(inv.getArgument(0), "unit_no");
            List<TMgisScopeCard> found = new ArrayList<TMgisScopeCard>();
            for (TMgisScopeCard c : cards) {
                if (unitNos.isEmpty() || unitNos.contains(c.getUnitNo())) {
                    found.add(c);
                }
            }
            found.sort((a, b) -> Long.compare(a.getId(), b.getId()));
            return found;
        });
        when(cardMapper.insert(any())).thenAnswer(inv -> {
            // 卡主键由服务层 IdWorker 先给号，明细都挂在这个号上，桩不得改号
            TMgisScopeCard c = inv.getArgument(0);
            cards.add(c);
            return 1;
        });
        when(cardMapper.update(isNull(), any())).thenReturn(1);

        when(catMapper.insert(any())).thenAnswer(inv -> {
            TMgisScopeCardCategory c = inv.getArgument(0);
            c.setId(++idGen);
            cardCats.add(c);
            return 1;
        });
        when(catMapper.selectList(any())).thenAnswer(inv -> {
            Map<String, Object> eq = eqPairs(inv.getArgument(0));
            List<TMgisScopeCardCategory> found = new ArrayList<TMgisScopeCardCategory>();
            for (TMgisScopeCardCategory c : cardCats) {
                if (eq.containsKey("card_id") && !eq.get("card_id").equals(((Number) c.getCardId()).longValue())
                        && !eq.get("card_id").equals(c.getCardId())) {
                    continue;
                }
                if (eq.containsKey("unit_no") && !eq.get("unit_no").equals(c.getUnitNo())) {
                    continue;
                }
                found.add(c);
            }
            found.sort((a, b) -> a.getCatCode().compareTo(b.getCatCode()));
            return found;
        });
        when(sourceMapper.insert(any())).thenAnswer(inv -> {
            TMgisScopeSource s = inv.getArgument(0);
            s.setId(++idGen);
            sources.add(s);
            return 1;
        });
        when(sourceMapper.selectList(any())).thenAnswer(inv -> {
            Map<String, Object> eq = eqPairs(inv.getArgument(0));
            List<TMgisScopeSource> found = new ArrayList<TMgisScopeSource>();
            for (TMgisScopeSource s : sources) {
                Object cardId = eq.get("card_id");
                if (cardId != null && !cardId.equals(s.getCardId())) {
                    continue;
                }
                found.add(s);
            }
            found.sort((a, b) -> a.getSourceType().compareTo(b.getSourceType()));
            return found;
        });
        when(handlerMapper.insert(any())).thenAnswer(inv -> {
            TMgisScopeHandler h = inv.getArgument(0);
            h.setId(++idGen);
            handlers.add(h);
            return 1;
        });
        when(handlerMapper.selectList(any())).thenAnswer(inv -> {
            Map<String, Object> eq = eqPairs(inv.getArgument(0));
            List<TMgisScopeHandler> found = new ArrayList<TMgisScopeHandler>();
            for (TMgisScopeHandler h : handlers) {
                Object cardId = eq.get("card_id");
                if (cardId != null && !cardId.equals(h.getCardId())) {
                    continue;
                }
                found.add(h);
            }
            found.sort((a, b) -> Integer.compare(a.getSeq(), b.getSeq()));
            return found;
        });
    }

    // ---------- 假库帮手 ----------

    private static TMgisUnitDoc unit(String no, String name, int status, String city, String county) {
        TMgisUnitDoc u = new TMgisUnitDoc();
        u.setId((long) (no.hashCode() & 0x7fffffff));
        u.setSiteNo(no);
        u.setSiteName(name);
        u.setSiteType("事业单位");
        u.setRegionProvince("江南省");
        u.setRegionCity(city);
        u.setRegionCounty(county);
        u.setTh1Max(new BigDecimal("30"));
        u.setTh2Max(new BigDecimal("120"));
        u.setTh3Max(new BigDecimal("360"));
        u.setStatus(status);
        u.setDelFlag(0);
        return u;
    }

    private static TMgisProfCategory prof(String code, String name, String parent,
                                          int level, int leaf, String status, String merged) {
        TMgisProfCategory c = new TMgisProfCategory();
        c.setCatCode(code);
        c.setCatName(name);
        c.setParentCode(parent);
        c.setCatLevel(level);
        c.setLeafFlag(leaf);
        c.setCatStatus(status);
        c.setMergedInto(merged);
        c.setDelFlag(0);
        return c;
    }

    private static ScopeRegisterRequest req(String unitNo, String unitName, String count,
                                            List<String> cats, ScopeRegisterRequest.SourceReq... src) {
        ScopeRegisterRequest r = new ScopeRegisterRequest();
        r.setUnitNo(unitNo);
        r.setUnitName(unitName);
        r.setScopeCount(count);
        r.setCategories(cats);
        r.setSources(new ArrayList<ScopeRegisterRequest.SourceReq>(Arrays.asList(src)));
        return r;
    }

    private static ScopeRegisterRequest.SourceReq copy(String no) {
        return new ScopeRegisterRequest.SourceReq("CITY_COPY", no);
    }

    private static ScopeRegisterRequest.SourceReq self(String no) {
        return new ScopeRegisterRequest.SourceReq("SELF_REPORT", no);
    }

    /** 取 QueryWrapper 里按列名 -> 等值参数（按 SQL 片段中 MPGENVAL 次序） */
    private static Map<String, Object> eqPairs(Wrapper<?> w) {
        QueryWrapper<?> qw = (QueryWrapper<?>) w;
        qw.getSqlSegment();
        Map<String, Object> params = qw.getParamNameValuePairs();
        Map<String, Object> out = new LinkedHashMap<String, Object>();
        Matcher m = Pattern.compile("(\\w+)\\s*=\\s*#\\{[^}]*MPGENVAL(\\d+)}").matcher(qw.getSqlSegment());
        while (m.find()) {
            Object val = null;
            for (Map.Entry<String, Object> e : params.entrySet()) {
                if (e.getKey().endsWith("MPGENVAL" + m.group(2))) {
                    val = e.getValue();
                    break;
                }
            }
            out.put(m.group(1), val);
        }
        return out;
    }

    /** 取某列 IN (...) 里的参数值 */
    private static List<Object> inValues(Wrapper<?> w, String column) {
        QueryWrapper<?> qw = (QueryWrapper<?>) w;
        String seg = qw.getSqlSegment();
        Map<String, Object> params = qw.getParamNameValuePairs();
        List<Object> out = new ArrayList<Object>();
        Matcher m = Pattern.compile(Pattern.quote(column) + "\\s+IN\\s*\\(([^)]*)\\)",
                Pattern.CASE_INSENSITIVE).matcher(seg);
        while (m.find()) {
            Matcher pm = Pattern.compile("MPGENVAL(\\d+)").matcher(m.group(1));
            while (pm.find()) {
                for (Map.Entry<String, Object> e : params.entrySet()) {
                    if (e.getKey().endsWith("MPGENVAL" + pm.group(1))) {
                        out.add(e.getValue());
                    }
                }
            }
        }
        return out;
    }

    // ---------- 立卡全程 ----------

    @Test
    void 立卡卡号系统给层级档系统折创建人记按下那一下() {
        ScopeRegisterResult r = service.register(
                req("ZZ00", "江南省测绘院第一院", "2", Arrays.asList("G0401", "G0601"),
                        copy("临市测抄〔2026〕3号"), self("院自报〔2026〕7号")), "alice");
        assertFalse(r.isDuplicated());
        TMgisScopeCard c = r.getCard();
        assertTrue(c.getCardNo().startsWith("FW"));
        assertEquals("ZZ00", c.getUnitNo());
        assertEquals("G0401,G0601", c.getScopeSign());
        assertEquals(Integer.valueOf(2), c.getScopeCount());
        assertEquals(Integer.valueOf(2), c.getNewCategoryCount());
        assertEquals(Integer.valueOf(2026), c.getRegisteredYear());
        // 2 项在一档（<30）：系数 2÷30=0.07（四舍五入两位）
        assertEquals(Integer.valueOf(1), c.getGradeLevel());
        assertEquals(new BigDecimal("0.07"), c.getGradeScore());
        assertEquals(Integer.valueOf(0), c.getStatus());
        assertEquals("alice", c.getCreateBy());
        assertEquals(2, r.getCategories().size());
        assertEquals(2, r.getSources().size());
        assertEquals(1, r.getHandlers().size());
        assertEquals(ScopeHandleAction.REGISTER.name(), r.getHandlers().get(0).getAction());
        assertEquals("alice", r.getHandlers().get(0).getHandler());
    }

    @Test
    void 等号按高一档三档顶线归三档() {
        ScopeRegisterResult on1 = service.register(
                req("ZZ00", null, "30", cats(30), self("S-30")), "alice");
        assertEquals(Integer.valueOf(2), on1.getCard().getGradeLevel());
        ScopeRegisterResult on3 = service.register(
                req("QX10", null, "360", catsForUnit(360, "QX10"), self("S-360")), "bob");
        assertEquals(Integer.valueOf(3), on3.getCard().getGradeLevel());
        assertEquals(new BigDecimal("1.00"), on3.getCard().getGradeScore());
    }

    // ---------- 按编号对档案、注销 ----------

    @Test
    void 只按编号对档案名称再像不作数() {
        // ZZ99 与 ZZ00 同名，但编号写错：名称对上也不作数
        ScopeRegisterException e = assertThrows(ScopeRegisterException.class,
                () -> service.register(
                        req("ZZ98", "江南省测绘院第一院", "1", Arrays.asList("G0401"), self("S1")), "alice"));
        assertEquals(ScopeField.UNIT_NO, e.getField());
        assertTrue(e.getMessage().contains("对不上"));
        assertEquals(0, cards.size());
    }

    @Test
    void 注销档案不再收新卡() {
        ScopeRegisterException e = assertThrows(ScopeRegisterException.class,
                () -> service.register(
                        req("ZZ01", "旧临川市城乡测绘服务中心", "1", Arrays.asList("G0401"), self("S1")), "alice"));
        assertEquals(ScopeField.UNIT_NO, e.getField());
        assertTrue(e.getMessage().contains("注销"));
        assertEquals(0, cards.size());
    }

    @Test
    void 编号缺操作人缺也顶回() {
        assertThrows(ScopeRegisterException.class,
                () -> service.register(req(" ", null, "1", Arrays.asList("G0401"), self("S1")), "alice"));
        assertThrows(ScopeRegisterException.class,
                () -> service.register(req("ZZ00", null, "1", Arrays.asList("G0401"), self("S1")), " "));
    }

    // ---------- 同段范围第二张顶回、出处并一笔 ----------

    @Test
    void 同单位同段范围第二张顶回创建人不挪经手不添() {
        ScopeRegisterResult first = service.register(
                req("ZZ00", null, "2", Arrays.asList("G0401", "G0601"), self("SR-1")), "alice");
        // 后来人把整张卡重录一遍：第二张顶回去，回的是头一笔，创建人还是 alice
        ScopeRegisterResult second = service.register(
                req("ZZ00", null, "2", Arrays.asList("G0601", "G0401"), self("SR-1")), "bob");
        assertTrue(second.isDuplicated());
        assertEquals(first.getCard().getId(), second.getCard().getId());
        assertEquals(1, cards.size());
        assertEquals("alice", cards.get(0).getCreateBy());
        // 同一件事（同类型出处）账上不许留两笔
        assertEquals(1, second.getSources().size());
        // 重卡不是新手续，经手链不添
        assertEquals(1, handlers.size());
    }

    @Test
    void 第二回哪怕挂用数填坏或越线仍认头一笔不改账() {
        ScopeRegisterResult first = service.register(
                req("ZZ00", null, "2", Arrays.asList("G0401", "G0601"), self("SR-1")), "alice");
        int catsAfterFirst = cardCats.size();
        // 第二回把挂用数填成 0、又一回越过顶线：同段范围在账，头一笔原样回
        for (String badCount : Arrays.asList("0", "99999", "abc")) {
            ScopeRegisterResult second = service.register(
                    req("ZZ00", null, badCount, Arrays.asList("G0401", "G0601"), self("SR-1")), "bob");
            assertTrue(second.isDuplicated());
            assertEquals(first.getCard().getId(), second.getCard().getId());
            assertEquals(1, cards.size());
            assertEquals(catsAfterFirst, cardCats.size());
            assertEquals("alice", cards.get(0).getCreateBy());
        }
    }

    @Test
    void 抄送与自报是同一件事新出处并进原卡() {        ScopeRegisterResult first = service.register(
                req("ZZ00", null, "1", Arrays.asList("G0401"), self("SR-9")), "alice");
        // 市局抄送后到，同一段范围：不立新卡，把抄送这条出处并到原卡
        ScopeRegisterResult second = service.register(
                req("ZZ00", null, "1", Arrays.asList("G0401"), copy("CS-9")), "clerk");
        assertTrue(second.isDuplicated());
        assertEquals(1, cards.size());
        assertEquals(2, second.getSources().size());
        assertEquals(1, cardCats.size());
        assertEquals(1, handlers.size());
    }

    // ---------- 校验指栏 ----------

    @Test
    void 负数零非数超顶线都拦在挂用类别数栏() {
        for (String bad : Arrays.asList("-1", "0", "abc", "361", "12.0")) {
            ScopeRegisterException e = assertThrows(ScopeRegisterException.class,
                    () -> service.register(req("ZZ00", null, bad, Arrays.asList("G0401"), self("S")), "alice"),
                    "原值：" + bad);
            assertEquals(ScopeField.SCOPE_COUNT, e.getField());
            assertEquals(0, cards.size(), "顶回一笔不落：" + bad);
        }
    }

    @Test
    void 大类混挂废止类别不给落() {
        ScopeRegisterException big = assertThrows(ScopeRegisterException.class,
                () -> service.register(req("ZZ00", null, "1", Arrays.asList("G04"), self("S")), "alice"));
        assertEquals(ScopeField.CAT_CODE, big.getField());
        ScopeRegisterException revoked = assertThrows(ScopeRegisterException.class,
                () -> service.register(req("ZZ00", null, "1", Arrays.asList("OLD_BOUNDARY"), self("S")), "alice"));
        assertTrue(revoked.getMessage().contains("G0603"));
        assertEquals(0, cards.size());
    }

    @Test
    void 数与实挂条数不符以及出处毛病都指栏顶回() {
        ScopeRegisterException mismatch = assertThrows(ScopeRegisterException.class,
                () -> service.register(req("ZZ00", null, "3", Arrays.asList("G0401", "G0601"), self("S")), "alice"));
        assertEquals(ScopeField.SCOPE_COUNT, mismatch.getField());

        ScopeRegisterException noDoc = assertThrows(ScopeRegisterException.class,
                () -> service.register(req("ZZ00", null, "1", Arrays.asList("G0401"),
                        new ScopeRegisterRequest.SourceReq("CITY_COPY", "  ")), "alice"));
        assertEquals(ScopeField.SOURCE_DOC_NO, noDoc.getField());

        ScopeRegisterException badSrc = assertThrows(ScopeRegisterException.class,
                () -> service.register(req("ZZ00", null, "1", Arrays.asList("G0401"),
                        new ScopeRegisterRequest.SourceReq("省里转来", "X1")), "alice"));
        assertEquals(ScopeField.SOURCE_TYPE, badSrc.getField());

        ScopeRegisterRequest noSource = req("ZZ00", null, "1", Arrays.asList("G0401"));
        noSource.setSources(new ArrayList<ScopeRegisterRequest.SourceReq>());
        assertThrows(ScopeRegisterException.class, () -> service.register(noSource, "alice"));
        assertEquals(0, cards.size());
    }

    // ---------- 新增类别数 ----------

    @Test
    void 本年度新增类别数与该单位老卡比对() {
        // 老卡已挂 G0401
        service.register(req("ZZ00", null, "1", Arrays.asList("G0401"), self("OLD")), "alice");
        // 新卡挂 G0401、G0601：新增 1 项；G0401 是老范围
        ScopeRegisterResult r = service.register(
                req("ZZ00", null, "2", Arrays.asList("G0401", "G0601"), self("NEW")), "alice");
        assertEquals(Integer.valueOf(1), r.getCard().getNewCategoryCount());
        // 别的单位同挂 G0601 不受 ZZ00 老卡影响
        ScopeRegisterResult other = service.register(
                req("QX10", null, "1", Arrays.asList("G0601"), self("Q1")), "bob");
        assertEquals(Integer.valueOf(1), other.getCard().getNewCategoryCount());
    }

    // ---------- 经手手续、冻结 ----------

    @Test
    void 核对定档一道接一道创建人与经手两个位置() {
        ScopeRegisterResult r = service.register(
                req("ZZ00", null, "1", Arrays.asList("G0401"), self("S")), "alice");
        Long id = r.getCard().getId();

        // 隔着站先定档：落不下去
        ScopeRegisterException skip = assertThrows(ScopeRegisterException.class,
                () -> service.handle(id, "CONFIRM", "bob", null));
        assertEquals(ScopeField.HANDLE_ACTION, skip.getField());

        ScopeCardView checked = service.handle(id, "CHECK", "bob", "核过");
        assertEquals(Integer.valueOf(1), checked.getCard().getStatus());
        ScopeCardView confirmed = service.handle(id, "定档", "carol", null);
        assertEquals(Integer.valueOf(2), confirmed.getCard().getStatus());

        List<TMgisScopeHandler> chain = service.view(id).getHandlers();
        assertEquals(3, chain.size());
        assertEquals("alice", chain.get(0).getHandler());
        assertEquals("bob", chain.get(1).getHandler());
        assertEquals("carol", chain.get(2).getHandler());
        // 创建人仍是立卡的 alice，没被经手人顶掉
        assertEquals("alice", service.view(id).getCard().getCreateBy());

        // 同一道手续第二回来：只认头一笔，不重落
        ScopeCardView again = service.handle(id, "CHECK", "dave", null);
        assertEquals(Integer.valueOf(2), again.getCard().getStatus());
        assertEquals(3, handlers.size());
    }

    @Test
    void 档案注销后老卡照查但冻结() {
        ScopeRegisterResult r = service.register(
                req("ZZ00", null, "1", Arrays.asList("G0401"), self("S")), "alice");
        // 老卡查得到
        assertNotNull(service.view(r.getCard().getId()));
        // 档案注销
        for (TMgisUnitDoc u : units) {
            if ("ZZ00".equals(u.getSiteNo())) {
                u.setStatus(1);
            }
        }
        ScopeRegisterException e = assertThrows(ScopeRegisterException.class,
                () -> service.handle(r.getCard().getId(), "CHECK", "bob", null));
        assertEquals(ScopeField.HANDLE_ACTION, e.getField());
        assertTrue(e.getMessage().contains("冻结"));
        // 新卡不收
        assertThrows(ScopeRegisterException.class,
                () -> service.register(req("ZZ00", null, "1", Arrays.asList("G0601"), self("S2")), "bob"));
    }

    // ---------- 翻账重折 ----------

    @Test
    void 翻账层级档按最新档案当场重折() {
        ScopeRegisterResult r = service.register(
                req("ZZ00", null, "30", cats(30), self("S")), "alice");
        assertEquals(Integer.valueOf(2), r.getCard().getGradeLevel());
        // 单位改了属地，同时档案一档顶线调到 50：翻账按新档案重折，30 < 50 归一档
        TMgisUnitDoc arch = null;
        for (TMgisUnitDoc u : units) {
            if ("ZZ00".equals(u.getSiteNo())) {
                arch = u;
            }
        }
        arch.setRegionCounty("城北区");
        arch.setTh1Max(new BigDecimal("50"));
        ScopeCardView v = service.view(r.getCard().getId());
        assertEquals("城北区", v.getUnit().getRegionCounty());
        assertEquals(1, v.getCurrentGrade().getLevel());
        assertEquals(new BigDecimal("0.60"), v.getCurrentGrade().getScore());
        // 立卡那日折的一份仍在卡上，两份并列
        assertEquals(Integer.valueOf(2), v.getCard().getGradeLevel());
    }

    // ---------- 省局/属地两视角、两头对账 ----------

    @Test
    void 属地只见本市省局见全省两头卡数对得上() {
        service.register(req("ZZ00", null, "1", Arrays.asList("G0401"), self("A")), "alice");
        service.register(req("ZZ99", null, "2", Arrays.asList("G0401", "G0601"), self("B")), "alice");
        service.register(req("QX10", null, "1", Arrays.asList("G0603"), self("C")), "bob");

        List<ScopeCardView> city = service.listViews("临川市", null);
        assertEquals(2, city.size());
        List<ScopeCardView> province = service.listViews(null, null);
        assertEquals(3, province.size());
        // 同一条卡两边点开是同一笔
        assertEquals(city.get(0).getCard().getId(), province.get(0).getCard().getId());

        ScopeLedger cityBook = service.ledger("临川市");
        assertEquals(2, cityBook.getTotal());
        // 城东 1、城南 1
        assertEquals(2, cityBook.getRegionBuckets().size());
        ScopeLedger provinceBook = service.ledger(null);
        assertEquals(3, provinceBook.getTotal());
        // 新增类别最多的单位往下排：ZZ99 新增 2 居首
        assertEquals("ZZ99", provinceBook.getUnitRanking().get(0).getUnitNo());
        assertEquals(Integer.valueOf(2), provinceBook.getUnitRanking().get(0).getNewCategoryCount());
    }

    @Test
    void 按单位编号精确收窄列表() {
        service.register(req("ZZ00", null, "1", Arrays.asList("G0401"), self("A")), "alice");
        service.register(req("ZZ99", null, "1", Arrays.asList("G0402"), self("B")), "alice");
        List<ScopeCardView> one = service.listViews("临川市", "ZZ99");
        assertEquals(1, one.size());
        assertEquals("ZZ99", one.get(0).getCard().getUnitNo());
        // 不在账上的卡：翻账顶回，不回空壳
        assertThrows(ScopeRegisterException.class, () -> service.view(-1L));
    }

    // ---------- 旧签名照封 ----------

    @Test
    void 旧写入门封死读门照旧() {
        service.register(req("ZZ00", null, "1", Arrays.asList("G0401"), self("A")), "alice");
        assertEquals(1, service.selectTMgisScopeCardList(new QueryWrapper<TMgisScopeCard>()).size());
        assertNotNull(service.selectTMgisScopeCardById(cards.get(0).getId()));
        assertThrows(IllegalStateException.class,
                () -> service.insertTMgisScopeCard(new TMgisScopeCard()));
        assertThrows(IllegalStateException.class,
                () -> service.updateTMgisScopeCard(new TMgisScopeCard()));
        assertThrows(IllegalStateException.class, () -> service.deleteTMgisScopeCardById(1L));
        assertThrows(IllegalStateException.class, () -> service.deleteTMgisScopeCardByIds("1"));
    }

    // 造 n 个现行末级类别代号（G0401..），并把目录补齐
    private List<String> cats(int n) {
        return catsForUnit(n, "ZZ00");
    }

    private List<String> catsForUnit(int n, String unitNo) {
        List<String> codes = new ArrayList<String>();
        for (int i = 1; i <= n; i++) {
            // 复用既有末级目录；超出种子目录的条目临时补进目录，避免与本用例无关的噪声
            String code;
            if (i == 1) {
                code = "G0401";
            } else if (i == 2) {
                code = "G0402";
            } else if (i == 3) {
                code = "G0601";
            } else {
                code = String.format("T%04d", i);
                boolean exists = false;
                for (TMgisProfCategory c : catalog) {
                    if (c.getCatCode().equals(code)) {
                        exists = true;
                    }
                }
                if (!exists) {
                    catalog.add(prof(code, "临时类别" + i, "G04", 2, 1, "CURRENT", null));
                }
            }
            codes.add(code);
        }
        return codes;
    }
}
