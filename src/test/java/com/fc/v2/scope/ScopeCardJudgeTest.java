package com.fc.v2.scope;

import com.fc.v2.common.scope.ScopeCardJudge;
import com.fc.v2.common.scope.ScopeField;
import com.fc.v2.common.scope.ScopeGrade;
import com.fc.v2.common.scope.ScopeLedger;
import com.fc.v2.common.scope.ScopeRegisterException;
import com.fc.v2.model.auto.TMgisProfCategory;
import com.fc.v2.model.auto.TMgisScopeCard;
import com.fc.v2.model.auto.TMgisUnitDoc;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 立卡章程单测：浑数零负顶线指栏、撞三档折算（等号高档、两位小数）、
 * 只许末级、混挂与废止类别不给落、范围签名、新增类别数、两头对账。
 */
class ScopeCardJudgeTest {

    private static TMgisUnitDoc arch() {
        TMgisUnitDoc a = new TMgisUnitDoc();
        a.setId(0L);
        a.setSiteNo("ZZ00");
        a.setSiteName("江南省测绘院第一地理信息工程院");
        a.setRegionProvince("江南省");
        a.setRegionCity("临川市");
        a.setRegionCounty("城东区");
        a.setTh1Max(new BigDecimal("30"));
        a.setTh2Max(new BigDecimal("120"));
        a.setTh3Max(new BigDecimal("360"));
        a.setStatus(0);
        a.setDelFlag(0);
        return a;
    }

    private static TMgisProfCategory leaf(String code, String name, String parent) {
        TMgisProfCategory c = new TMgisProfCategory();
        c.setCatCode(code);
        c.setCatName(name);
        c.setParentCode(parent);
        c.setCatLevel(2);
        c.setLeafFlag(1);
        c.setCatStatus("CURRENT");
        c.setDelFlag(0);
        return c;
    }

    private static Map<String, TMgisProfCategory> catalog(TMgisProfCategory... cats) {
        Map<String, TMgisProfCategory> m = new HashMap<String, TMgisProfCategory>();
        for (TMgisProfCategory c : cats) {
            m.put(c.getCatCode(), c);
        }
        return m;
    }

    // ---------- 挂用类别数：浑数/零/负 指栏 ----------

    @Test
    void 挂用类别数负数零非数都指本栏顶回带原值() {
        ScopeRegisterException neg = assertThrows(ScopeRegisterException.class,
                () -> ScopeCardJudge.parseScopeCount("-5"));
        assertEquals(ScopeField.SCOPE_COUNT, neg.getField());
        assertTrue(neg.getMessage().contains("-5"));

        ScopeRegisterException zero = assertThrows(ScopeRegisterException.class,
                () -> ScopeCardJudge.parseScopeCount("0"));
        assertEquals(ScopeField.SCOPE_COUNT, zero.getField());
        assertTrue(zero.getMessage().contains("零"));

        for (String bad : Arrays.asList("", "  ", "abc", "12.5", "3项", "1e3")) {
            ScopeRegisterException e = assertThrows(ScopeRegisterException.class,
                    () -> ScopeCardJudge.parseScopeCount(bad), "原值：" + bad);
            assertEquals(ScopeField.SCOPE_COUNT, e.getField());
        }
        assertEquals(7, ScopeCardJudge.parseScopeCount(" +7 "));
    }

    @Test
    void 超出三档顶线一大截拦在挂用类别数栏() {
        ScopeRegisterException e = assertThrows(ScopeRegisterException.class,
                () -> ScopeCardJudge.checkWithinCeiling(361, arch()));
        assertEquals(ScopeField.SCOPE_COUNT, e.getField());
        assertTrue(e.getMessage().contains("360"));
        // 恰为三档顶线不拦（无四档可升，仍归三档）
        ScopeCardJudge.checkWithinCeiling(360, arch());
    }

    // ---------- 层级档折算 ----------

    @Test
    void 撞三道顶线折算等号算高一档系数两位小数() {
        ScopeGrade under = ScopeCardJudge.grade(29, arch());
        assertEquals(1, under.getLevel());
        assertEquals(new BigDecimal("0.97"), under.getScore());

        // 恰为一档顶线：等号按高一档（二档）
        ScopeGrade on1 = ScopeCardJudge.grade(30, arch());
        assertEquals(2, on1.getLevel());
        assertEquals(new BigDecimal("0.25"), on1.getScore());

        // 恰为二档顶线：按三档
        ScopeGrade on2 = ScopeCardJudge.grade(120, arch());
        assertEquals(3, on2.getLevel());
        assertEquals(new BigDecimal("0.33"), on2.getScore());

        // 恰为三档顶线：无四档，归三档；系数 1.00
        ScopeGrade on3 = ScopeCardJudge.grade(360, arch());
        assertEquals(3, on3.getLevel());
        assertEquals(new BigDecimal("1.00"), on3.getScore());
    }

    @Test
    void 档案顶线缺失或不正顶回() {
        TMgisUnitDoc bad = arch();
        bad.setTh2Max(null);
        assertThrows(ScopeRegisterException.class, () -> ScopeCardJudge.grade(10, bad));
        TMgisUnitDoc order = arch();
        order.setTh2Max(new BigDecimal("20"));
        assertThrows(ScopeRegisterException.class, () -> ScopeCardJudge.grade(10, order));
    }

    // ---------- 类别：只许末级 ----------

    @Test
    void 挂大类或混挂不给落() {
        TMgisProfCategory big = leaf("G04", "工程测量", null);
        big.setCatLevel(1);
        big.setLeafFlag(0);
        Map<String, TMgisProfCategory> cat = catalog(big, leaf("G0401", "控制测量", "G04"));
        // 单挂大类
        ScopeRegisterException e = assertThrows(ScopeRegisterException.class,
                () -> ScopeCardJudge.normalizeCategories(Arrays.asList("G04"), cat));
        assertEquals(ScopeField.CAT_CODE, e.getField());
        assertTrue(e.getMessage().contains("大类"));
        // 大类与末级混挂同样不给落
        assertThrows(ScopeRegisterException.class,
                () -> ScopeCardJudge.normalizeCategories(Arrays.asList("G04", "G0401"), cat));
    }

    @Test
    void 挂早已并进别处的旧类别不给落并指去向() {
        TMgisProfCategory old = leaf("OLD_BOUNDARY", "行政区域界线测绘（旧目录）", null);
        old.setCatStatus("REVOKED");
        old.setMergedInto("G0603");
        Map<String, TMgisProfCategory> cat = catalog(old);
        ScopeRegisterException e = assertThrows(ScopeRegisterException.class,
                () -> ScopeCardJudge.normalizeCategories(Arrays.asList("OLD_BOUNDARY"), cat));
        assertEquals(ScopeField.CAT_CODE, e.getField());
        assertTrue(e.getMessage().contains("G0603"));
    }

    @Test
    void 类别不识空白册内重复都顶回去重后升序出签名() {
        Map<String, TMgisProfCategory> cat = catalog(
                leaf("G0401", "控制测量", "G04"), leaf("G0601", "地籍测绘", "G06"));
        assertThrows(ScopeRegisterException.class,
                () -> ScopeCardJudge.normalizeCategories(Arrays.asList("G9999"), cat));
        assertThrows(ScopeRegisterException.class,
                () -> ScopeCardJudge.normalizeCategories(Arrays.asList("G0401", " "), cat));
        ScopeRegisterException dup = assertThrows(ScopeRegisterException.class,
                () -> ScopeCardJudge.normalizeCategories(Arrays.asList("G0601", "G0401", "G0601"), cat));
        assertTrue(dup.getMessage().contains("两回"));

        List<String> norm = ScopeCardJudge.normalizeCategories(Arrays.asList("G0601", " G0401 "), cat);
        assertEquals(Arrays.asList("G0401", "G0601"), norm);
        String sign = ScopeCardJudge.scopeSign(norm);
        assertEquals("G0401,G0601", sign);
        // 范围指纹定长 64：同签名同指纹，不同签名不同指纹（唯一约束押它）
        assertEquals(64, ScopeCardJudge.scopeHash(sign).length());
        assertEquals(ScopeCardJudge.scopeHash(sign), ScopeCardJudge.scopeHash("G0401,G0601"));
        assertTrue(!ScopeCardJudge.scopeHash(sign).equals(ScopeCardJudge.scopeHash("G0401,G0603")));
        assertThrows(ScopeRegisterException.class,
                () -> ScopeCardJudge.normalizeCategories(new ArrayList<String>(), cat));
    }

    @Test
    void 挂用类别数与实挂条数对不上指栏() {
        ScopeRegisterException e = assertThrows(ScopeRegisterException.class,
                () -> ScopeCardJudge.checkCountMatches(3, Arrays.asList("G0401", "G0601")));
        assertEquals(ScopeField.SCOPE_COUNT, e.getField());
    }

    // ---------- 新增类别数 ----------

    @Test
    void 本年度新增类别数按老卡并集比对() {
        List<String> now = Arrays.asList("G0401", "G0402", "G0601");
        assertEquals(3, ScopeCardJudge.newCategoryCount(now, new LinkedHashSet<String>()));
        assertEquals(2, ScopeCardJudge.newCategoryCount(now,
                new LinkedHashSet<String>(Arrays.asList("G0401"))));
        assertEquals(0, ScopeCardJudge.newCategoryCount(now,
                new LinkedHashSet<String>(Arrays.asList("G0401", "G0402", "G0601", "G0901"))));
    }

    // ---------- 翻账重折 ----------

    @Test
    void 单位档案顶线变了翻账当场重折() {
        TMgisScopeCard card = new TMgisScopeCard();
        card.setScopeCount(30);
        card.setGradeLevel(1);
        // 档案三档顶线调小，30 仍在一档内，按新档折（同样的 30 当年折的档不再算数）
        TMgisUnitDoc moved = arch();
        moved.setTh1Max(new BigDecimal("50"));
        moved.setTh2Max(new BigDecimal("150"));
        moved.setTh3Max(new BigDecimal("400"));
        ScopeGrade g = ScopeCardJudge.regrade(card, moved);
        assertEquals(1, g.getLevel());
        assertEquals(new BigDecimal("0.60"), g.getScore());
    }

    // ---------- 两头对账 ----------

    @Test
    void 省局按新增排属地按区县堆两途卡数一致() {
        List<ScopeLedger.Row> rows = Arrays.asList(
                new ScopeLedger.Row("U1", "甲院", "城东区", 5),
                new ScopeLedger.Row("U1", "甲院", "城东区", 2),
                new ScopeLedger.Row("U2", "乙司", "城西区", 8),
                new ScopeLedger.Row("U3", "丙队", "城东区", 0));
        ScopeLedger ledger = ScopeCardJudge.ledger(rows);
        assertEquals(4, ledger.getTotal());
        // 新增类别最多的单位往下排：乙司(8) → 甲院(7) → 丙队(0)
        assertEquals(Arrays.asList("U2", "U1", "U3"),
                Arrays.asList(ledger.getUnitRanking().get(0).getUnitNo(),
                        ledger.getUnitRanking().get(1).getUnitNo(),
                        ledger.getUnitRanking().get(2).getUnitNo()));
        // 甲院两张卡
        assertEquals(2, ledger.getUnitRanking().get(1).getCardCount());
        // 属地按最末一级归堆：城东 3 张、城西 1 张
        assertEquals(2, ledger.getRegionBuckets().size());
        assertEquals(3, ledger.getRegionBuckets().get(0).getCardCount());
        assertEquals(1, ledger.getRegionBuckets().get(1).getCardCount());
        assertEquals(2, ledger.getRegionBuckets().get(0).getUnitCount());
    }

    @Test
    void 空屏两途都数零也过() {
        ScopeLedger ledger = ScopeCardJudge.ledger(Collections.<ScopeLedger.Row>emptyList());
        assertEquals(0, ledger.getTotal());
        assertTrue(ledger.getUnitRanking().isEmpty());
        assertTrue(ledger.getRegionBuckets().isEmpty());
    }
}
