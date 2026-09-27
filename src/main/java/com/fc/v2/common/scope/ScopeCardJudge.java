package com.fc.v2.common.scope;

import com.fc.v2.model.auto.TMgisProfCategory;
import com.fc.v2.model.auto.TMgisScopeCard;
import com.fc.v2.model.auto.TMgisUnitDoc;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * 作业范围标签建档卡引擎：资质监督处立卡规矩在系统里的唯一出处。
 *
 * <p>立卡、翻账、对账都从这里过，别处不许另写一套：</p>
 * <ul>
 *   <li><b>挂用类别数照原值核</b>：负数、零、不是数、越过档案三档顶线一大截，
 *       当场拦下并指到「挂用类别数」栏，浑数带填报原值，不只甩一句「不合法」；</li>
 *   <li><b>层级档系统折算</b>：拿挂用类别数去撞档案上的三道顶线，等号算高一档，
 *       系数 = 挂用类别数 ÷ 所在档顶线，留两位小数；只在立卡那天折一次远远不够，
 *       翻账时按最新档案当场再折；</li>
 *   <li><b>类别只许挂末级</b>：大类与末级混挂、挂早已并进别处的旧类别，都不给落；</li>
 *   <li><b>两头对账</b>：省局从新增类别最多的单位往下排，属地按最末一级归堆再数，
 *       两条路数出来的卡数必须一致，差一笔这一屏就没过。</li>
 * </ul>
 *
 * @author fuce
 * @date 2026-09-27
 */
public final class ScopeCardJudge {

    private ScopeCardJudge() {
    }

    /**
     * 挂用类别数照填报原值核：只认正整数。
     * 空白、写字、带小数都不是数；零、负数当场拦——全部指到「挂用类别数」栏，
     * 异常带填报原值，追得到原笔。
     */
    public static int parseScopeCount(String raw) {
        String t = raw == null ? null : raw.trim();
        if (t == null || t.isEmpty()) {
            throw new ScopeRegisterException("挂用类别数未填：这一栏没有数，卡立不成",
                    ScopeField.SCOPE_COUNT, raw);
        }
        long value;
        try {
            if (!t.matches("[+-]?\\d+")) {
                throw new NumberFormatException(t);
            }
            value = Long.parseLong(t);
        } catch (NumberFormatException e) {
            throw new ScopeRegisterException("挂用类别数不是数（填报原值：" + t + "）：只认整数",
                    ScopeField.SCOPE_COUNT, raw);
        }
        if (value < 0L) {
            throw new ScopeRegisterException("挂用类别数不得为负（填报原值：" + t
                    + "）：挂用项数没有倒着数的", ScopeField.SCOPE_COUNT, raw);
        }
        if (value == 0L) {
            throw new ScopeRegisterException("挂用类别数不得为零：一张卡总得挂一段范围，零项不给落（填报原值："
                    + t + "）", ScopeField.SCOPE_COUNT, raw);
        }
        if (value > Integer.MAX_VALUE) {
            throw new ScopeRegisterException("挂用类别数越界（填报原值：" + t + "）",
                    ScopeField.SCOPE_COUNT, raw);
        }
        return (int) value;
    }

    /**
     * 挂用类别数撞档案三道顶线，折算层级档。
     * 等号算高一档（恰为一档顶线按二档说话、恰为二档顶线按三档说话）；
     * 越过三档顶线在 {@link #checkWithinCeiling} 已先拦下，此处不设四档。
     */
    public static ScopeGrade grade(int scopeCount, TMgisUnitDoc arch) {
        requireCeilings(arch);
        BigDecimal n = BigDecimal.valueOf(scopeCount);
        BigDecimal th1 = arch.getTh1Max();
        BigDecimal th2 = arch.getTh2Max();
        BigDecimal th3 = arch.getTh3Max();
        int level;
        BigDecimal ceiling;
        if (n.compareTo(th1) < 0) {
            level = 1;
            ceiling = th1;
        } else if (n.compareTo(th2) < 0) {
            // 恰为一档顶线也落这一档：等号按高一档
            level = 2;
            ceiling = th2;
        } else {
            // 恰为二档顶线按三档；恰为三档顶线无四档可升，仍归三档
            level = 3;
            ceiling = th3;
        }
        BigDecimal score = n.divide(ceiling, 2, RoundingMode.HALF_UP);
        return new ScopeGrade(level, score, ceiling);
    }

    /**
     * 顶线校验：档案上的三道顶线缺一道、不为正、次序颠倒，是档案本身的问题；
     * 挂用类别数越过三档顶线一大截，当场拦在「挂用类别数」栏。
     */
    public static void checkWithinCeiling(int scopeCount, TMgisUnitDoc arch) {
        requireCeilings(arch);
        if (BigDecimal.valueOf(scopeCount).compareTo(arch.getTh3Max()) > 0) {
            throw new ScopeRegisterException("挂用类别数 " + scopeCount + " 超出该档案允许承接的三档顶线 "
                    + arch.getTh3Max().stripTrailingZeros().toPlainString()
                    + " 项一大截：顶线之外的类别卡上不给挂", ScopeField.SCOPE_COUNT);
        }
    }

    private static void requireCeilings(TMgisUnitDoc arch) {
        if (arch == null) {
            throw new ScopeRegisterException("单位档案未指：层级档无处可折");
        }
        BigDecimal th1 = arch.getTh1Max();
        BigDecimal th2 = arch.getTh2Max();
        BigDecimal th3 = arch.getTh3Max();
        if (th1 == null || th2 == null || th3 == null) {
            throw new ScopeRegisterException("档案三道顶线不齐（单位编号 " + arch.getSiteNo()
                    + "）：先补齐一档、二档、三档顶线再立卡");
        }
        if (th1.signum() <= 0 || th2.signum() <= 0 || th3.signum() <= 0
                || th1.compareTo(th2) > 0 || th2.compareTo(th3) > 0) {
            throw new ScopeRegisterException("档案三道顶线不正（单位编号 " + arch.getSiteNo()
                    + "）：须为正且一档≤二档≤三档");
        }
    }

    /**
     * 核挂用类别：只许现行目录的末级；一个大类都不许直挂；废止旧类别指它并入
     * 哪里；册内重复、代号不识，指到具体代号栏顶回。返回去重升序的末级代号，
     * 范围签名与挂用类别数都以它为准。
     */
    public static List<String> normalizeCategories(List<String> rawCodes,
                                                   Map<String, TMgisProfCategory> catalog) {
        if (rawCodes == null || rawCodes.isEmpty()) {
            throw new ScopeRegisterException("挂用类别一栏空白：卡上没有类别，这张卡讲的是哪段范围？",
                    ScopeField.CATEGORIES);
        }
        TreeSet<String> dedup = new TreeSet<String>();
        for (String raw : rawCodes) {
            String code = raw == null ? null : raw.trim();
            if (code == null || code.isEmpty()) {
                throw new ScopeRegisterException("挂用类别里有空白代号：空白挂不上账",
                        ScopeField.CAT_CODE);
            }
            if (!dedup.add(code)) {
                throw new ScopeRegisterException("同一张卡上类别「" + code + "」挂了两回：一段范围只挂一笔",
                        ScopeField.CAT_CODE, code);
            }
            TMgisProfCategory cat = catalog == null ? null : catalog.get(code);
            if (cat == null || (cat.getDelFlag() != null && cat.getDelFlag() == 1)) {
                throw new ScopeRegisterException("类别代号不识：「" + code
                        + "」不在现行目录里，不擅自归类", ScopeField.CAT_CODE, code);
            }
            if ((cat.getCatLevel() != null && cat.getCatLevel() == 1)
                    || (cat.getLeafFlag() != null && cat.getLeafFlag() == 0)) {
                throw new ScopeRegisterException("「" + code + " " + safeName(cat)
                        + "」是大类：卡上只许挂到末级具体作业类别，不许拿大类凑数",
                        ScopeField.CAT_CODE, code);
            }
            if ("REVOKED".equals(cat.getCatStatus())) {
                String into = cat.getMergedInto();
                throw new ScopeRegisterException("类别「" + code + " " + safeName(cat)
                        + "」早已并进别处，不给落；要挂请挂现行的「"
                        + (into == null ? "对应末级类别" : into) + "」", ScopeField.CAT_CODE, code);
            }
        }
        return new ArrayList<String>(dedup);
    }

    /** 挂用类别数须与所挂末级类别条数一致：两张嘴说的须是同一段范围 */
    public static void checkCountMatches(int declared, List<String> categories) {
        if (declared != categories.size()) {
            throw new ScopeRegisterException("挂用类别数填的是 " + declared + "，卡上实挂末级类别 "
                    + categories.size() + " 项：两数对不上，先把这一栏核齐", ScopeField.SCOPE_COUNT);
        }
    }

    /** 范围签名：末级代号升序逗号拼接。同单位签名相同即同一段范围，第二张顶回 */
    public static String scopeSign(List<String> sortedLeafCodes) {
        StringBuilder sb = new StringBuilder();
        for (String code : sortedLeafCodes) {
            if (sb.length() > 0) {
                sb.append(',');
            }
            sb.append(code);
        }
        return sb.toString();
    }

    /** 范围签名定长指纹：唯一约束押它，免得长签名（三档可挂 360 项）越过索引长度 */
    public static String scopeHash(String sign) {
        return sha256Hex(sign);
    }

    private static String sha256Hex(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                hex.append(Character.forDigit((b >> 4) & 0xF, 16));
                hex.append(Character.forDigit(b & 0xF, 16));
            }
            return hex.toString();
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    /**
     * 本年度新增挂用类别数：本卡末级类别里，该单位老卡上从没挂过的条数。
     * 老卡由调用处取齐（同单位、早于本卡），引擎只认传入的并集。
     */
    public static int newCategoryCount(List<String> leafCodes, Set<String> priorCategories) {
        Set<String> prior = priorCategories == null ? Collections.<String>emptySet() : priorCategories;
        int n = 0;
        for (String code : leafCodes) {
            if (!prior.contains(code)) {
                n++;
            }
        }
        return n;
    }

    /**
     * 两头对账：省局按单位（新增类别最多的往下排）与属地按最末一级归堆，
     * 两条路数出来的卡数必须一致；差一笔抛账，这一屏不算过。
     *
     * @param rows 一屏里的卡（省局传全省，属地带本市，行内须已按视角过滤）
     */
    public static ScopeLedger ledger(List<ScopeLedger.Row> rows) {
        List<ScopeLedger.Row> safe = rows == null ? Collections.<ScopeLedger.Row>emptyList() : rows;

        // 省局一头：按单位归堆，新增类别数相加，从多的往下排
        Map<String, ScopeLedger.UnitRank> rankMap = new LinkedHashMap<String, ScopeLedger.UnitRank>();
        // 属地一头：按最末一级（区县）归堆数卡
        Map<String, ScopeLedger.RegionBucket> regionMap = new LinkedHashMap<String, ScopeLedger.RegionBucket>();
        int total = 0;
        for (ScopeLedger.Row r : safe) {
            if (r == null) {
                continue;
            }
            total++;
            ScopeLedger.UnitRank rank = rankMap.get(r.getUnitNo());
            if (rank == null) {
                rank = new ScopeLedger.UnitRank(r.getUnitNo(), r.getUnitName());
                rankMap.put(r.getUnitNo(), rank);
            }
            rank.add(r.getNewCategoryCount());

            String county = r.getRegionCounty();
            String key = county == null || county.trim().isEmpty() ? "属地未归堆" : county.trim();
            ScopeLedger.RegionBucket bucket = regionMap.get(key);
            if (bucket == null) {
                bucket = new ScopeLedger.RegionBucket(key);
                regionMap.put(key, bucket);
            }
            bucket.add(r.getUnitNo());
        }

        List<ScopeLedger.UnitRank> ranking = new ArrayList<ScopeLedger.UnitRank>(rankMap.values());
        Collections.sort(ranking, new Comparator<ScopeLedger.UnitRank>() {
            @Override
            public int compare(ScopeLedger.UnitRank a, ScopeLedger.UnitRank b) {
                int byNew = Integer.compare(b.getNewCategoryCount(), a.getNewCategoryCount());
                if (byNew != 0) {
                    return byNew;
                }
                return a.getUnitNo().compareTo(b.getUnitNo());
            }
        });
        List<ScopeLedger.RegionBucket> buckets = new ArrayList<ScopeLedger.RegionBucket>(regionMap.values());
        Collections.sort(buckets, new Comparator<ScopeLedger.RegionBucket>() {
            @Override
            public int compare(ScopeLedger.RegionBucket a, ScopeLedger.RegionBucket b) {
                return a.getRegionCounty().compareTo(b.getRegionCounty());
            }
        });

        int byUnits = 0;
        for (ScopeLedger.UnitRank u : ranking) {
            byUnits += u.getCardCount();
        }
        int byRegions = 0;
        for (ScopeLedger.RegionBucket b : buckets) {
            byRegions += b.getCardCount();
        }
        if (byUnits != total || byRegions != total) {
            // 同一份行目两头数出两张卡数，必是哪一处归堆漏了行——不过屏
            throw new ScopeRegisterException("两头账对不上：按单位数得 " + byUnits
                    + " 张，按最末一级归堆数得 " + byRegions + " 张，行目共 " + total
                    + " 张，差一笔这一屏都不算过");
        }
        return new ScopeLedger(total, ranking, buckets);
    }

    /**
     * 翻账当场重折层级档：类别有增减、单位改了属地，都拿挂用类别数按最新档案
     * 再撞一遍三道顶线。卡上立卡那日折的一份只留作快照，不作当前数。
     */
    public static ScopeGrade regrade(TMgisScopeCard card, TMgisUnitDoc currentArch) {
        if (card == null || card.getScopeCount() == null) {
            throw new ScopeRegisterException("卡上挂用类别数缺失，无法重折层级档");
        }
        checkWithinCeiling(card.getScopeCount(), currentArch);
        return grade(card.getScopeCount(), currentArch);
    }

    private static String safeName(TMgisProfCategory cat) {
        return cat == null || cat.getCatName() == null ? "" : cat.getCatName();
    }
}
