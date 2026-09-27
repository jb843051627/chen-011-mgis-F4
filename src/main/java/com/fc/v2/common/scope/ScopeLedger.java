package com.fc.v2.common.scope;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 看账一屏的定局：省局、属地两头数出来的同一份卡数与两种排行。
 *
 * <p>省局一头从新增类别最多的单位往下排；属地一头把单位按最末一级
 * （区县）归堆再数卡。{@code total} 是行目总数，两头小计都须与它相等，
 * 差一笔即由 {@link ScopeCardJudge#ledger} 抛账，这一屏不算过。</p>
 *
 * @author fuce
 * @date 2026-09-27
 */
public final class ScopeLedger {

    /** 行目卡总数（两头小计恒等于此数） */
    private final int total;

    /** 省局一头：按单位排行，新增类别多的在前 */
    private final List<UnitRank> unitRanking;

    /** 属地一头：按最末一级归堆 */
    private final List<RegionBucket> regionBuckets;

    public ScopeLedger(int total, List<UnitRank> unitRanking, List<RegionBucket> regionBuckets) {
        this.total = total;
        this.unitRanking = unitRanking;
        this.regionBuckets = regionBuckets;
    }

    public int getTotal() {
        return total;
    }

    public List<UnitRank> getUnitRanking() {
        return unitRanking;
    }

    public List<RegionBucket> getRegionBuckets() {
        return regionBuckets;
    }

    /** 翻账行目（省局全省、属地带本市过滤后进来） */
    public static final class Row {
        private final String unitNo;
        private final String unitName;
        private final String regionCounty;
        private final int newCategoryCount;

        public Row(String unitNo, String unitName, String regionCounty, int newCategoryCount) {
            this.unitNo = unitNo;
            this.unitName = unitName;
            this.regionCounty = regionCounty;
            this.newCategoryCount = newCategoryCount;
        }

        public String getUnitNo() {
            return unitNo;
        }

        public String getUnitName() {
            return unitName;
        }

        public String getRegionCounty() {
            return regionCounty;
        }

        public int getNewCategoryCount() {
            return newCategoryCount;
        }
    }

    /** 省局一头一行一单位：卡数、新增类别合计 */
    public static final class UnitRank {
        private final String unitNo;
        private final String unitName;
        private int cardCount;
        private int newCategoryCount;

        public UnitRank(String unitNo, String unitName) {
            this.unitNo = unitNo;
            this.unitName = unitName;
        }

        void add(int newCount) {
            this.cardCount++;
            this.newCategoryCount += newCount;
        }

        public String getUnitNo() {
            return unitNo;
        }

        public String getUnitName() {
            return unitName;
        }

        public int getCardCount() {
            return cardCount;
        }

        public int getNewCategoryCount() {
            return newCategoryCount;
        }
    }

    /** 属地一头一堆：最末一级（区县）内单位数、卡数 */
    public static final class RegionBucket {
        private final String regionCounty;
        private final Set<String> unitNos = new LinkedHashSet<String>();
        private int cardCount;

        public RegionBucket(String regionCounty) {
            this.regionCounty = regionCounty;
        }

        void add(String unitNo) {
            this.cardCount++;
            if (unitNo != null) {
                this.unitNos.add(unitNo);
            }
        }

        public String getRegionCounty() {
            return regionCounty;
        }

        public int getUnitCount() {
            return unitNos.size();
        }

        public int getCardCount() {
            return cardCount;
        }

        public List<String> getUnitNos() {
            return new ArrayList<String>(unitNos);
        }
    }
}
