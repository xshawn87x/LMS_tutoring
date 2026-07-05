package com.lms.platform;

import com.lms.auth.Tenant;
import com.lms.auth.TenantRepository;
import com.lms.billing.Invoice;
import com.lms.billing.InvoiceRepository;
import com.lms.billing.PlanPrice;
import com.lms.billing.PlanPriceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 플랫폼(SaaS 제공자) 애널리틱스 — 전 학원(테넌트) 대상 SaaS 지표.
 * 매출 추이·MRR·플랜 분포·이탈(상태)·학원별 KPI. 모두 RLS-free 전역 테이블(tenant·invoice·plan_price)에서 집계.
 */
@Service
@Transactional(readOnly = true)
public class PlatformAnalyticsService {

    private final TenantRepository tenantRepository;
    private final InvoiceRepository invoiceRepository;
    private final PlanPriceRepository planPriceRepository;

    public PlatformAnalyticsService(TenantRepository tenantRepository, InvoiceRepository invoiceRepository,
                                    PlanPriceRepository planPriceRepository) {
        this.tenantRepository = tenantRepository;
        this.invoiceRepository = invoiceRepository;
        this.planPriceRepository = planPriceRepository;
    }

    public record RevenuePoint(String period, int issued, int paid) {}

    public record TenantKpi(String orgCode, String name, String plan, String status,
                            int monthlyPrice, String latestInvoice) {}

    public record PlatformAnalytics(
            int tenantCount, int activeCount, int pastDueCount, int suspendedCount, int churnRate,
            long mrr, Map<String, Integer> planDistribution,
            List<RevenuePoint> revenueTrend, List<TenantKpi> tenants) {}

    public PlatformAnalytics analytics() {
        List<Tenant> tenants = tenantRepository.findAll();
        Map<Plan, Integer> priceByPlan = new LinkedHashMap<>();
        for (PlanPrice p : planPriceRepository.findAll()) {
            priceByPlan.put(p.getPlan(), p.getMonthlyPrice());
        }

        int active = 0, pastDue = 0, suspended = 0;
        long mrr = 0;
        Map<String, Integer> planDist = new LinkedHashMap<>();
        for (Plan pl : Plan.values()) planDist.put(pl.name(), 0);

        List<TenantKpi> kpis = new ArrayList<>();
        for (Tenant t : tenants) {
            switch (t.getStatus()) {
                case ACTIVE -> active++;
                case PAST_DUE -> pastDue++;
                case SUSPENDED -> suspended++;
            }
            planDist.merge(t.getPlan().name(), 1, Integer::sum);
            int price = priceByPlan.getOrDefault(t.getPlan(), 0);
            if (t.getStatus() == TenantStatus.ACTIVE) mrr += price;

            String latest = invoiceRepository.findByTenantIdOrderByIssuedAtDesc(t.getId())
                    .stream().findFirst().map(Invoice::getStatus).orElse("—");
            kpis.add(new TenantKpi(t.getOrgCode(), t.getName(), t.getPlan().name(),
                    t.getStatus().name(), price, latest));
        }
        kpis.sort((a, b) -> Integer.compare(b.monthlyPrice(), a.monthlyPrice())); // 매출 기여 높은 순

        int total = tenants.size();
        int churnRate = total == 0 ? 0 : (int) Math.round((suspended + pastDue) * 100.0 / total);

        return new PlatformAnalytics(total, active, pastDue, suspended, churnRate,
                mrr, planDist, revenueTrend(), kpis);
    }

    /** 인보이스를 월(period)별로 집계 — 발행액/결제액. 최근 12개월. */
    private List<RevenuePoint> revenueTrend() {
        Map<String, int[]> byPeriod = new TreeMap<>();
        for (Invoice inv : invoiceRepository.findAll()) {
            int[] a = byPeriod.computeIfAbsent(inv.getPeriod(), k -> new int[2]);
            a[0] += inv.getTotal();
            if ("PAID".equals(inv.getStatus())) a[1] += inv.getTotal();
        }
        List<RevenuePoint> trend = new ArrayList<>();
        byPeriod.forEach((period, v) -> trend.add(new RevenuePoint(period, v[0], v[1])));
        return trend.size() > 12 ? trend.subList(trend.size() - 12, trend.size()) : trend;
    }
}
