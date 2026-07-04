package com.lms.market;

import com.lms.error.ConflictException;
import com.lms.error.NotFoundException;
import com.lms.tenant.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 콘텐츠 마켓: 플랫폼 카탈로그 관리 + 학원 구매 + 정산.
 * market_content / content_purchase 모두 RLS 없는 전역이므로 구매/조회는 tenant_id를 명시 스코프한다.
 */
@Service
@Transactional
public class MarketService {

    private final MarketContentRepository contentRepository;
    private final ContentPurchaseRepository purchaseRepository;

    public MarketService(MarketContentRepository contentRepository, ContentPurchaseRepository purchaseRepository) {
        this.contentRepository = contentRepository;
        this.purchaseRepository = purchaseRepository;
    }

    // --- 플랫폼(본사) 카탈로그 ---

    public MarketContent create(String title, String description, String category, int price, String provider) {
        return contentRepository.save(new MarketContent(title, description, category, price, provider));
    }

    public MarketContent update(UUID id, String title, String description, String category,
                                int price, String provider, boolean published) {
        MarketContent c = require(id);
        c.update(title, description, category, price, provider, published);
        return c;
    }

    public void delete(UUID id) {
        contentRepository.delete(require(id));
    }

    @Transactional(readOnly = true)
    public List<MarketContent> listAll() {
        return contentRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public List<MarketContent> listPublished() {
        return contentRepository.findByPublishedTrueOrderByCreatedAtDesc();
    }

    // --- 학원(테넌트) 구매 ---

    /** 현재 테넌트가 콘텐츠를 구매(중복 구매 방지). */
    public ContentPurchase purchase(UUID contentId, String purchasedBy) {
        UUID tenant = TenantContext.get()
                .orElseThrow(() -> new NotFoundException("테넌트 컨텍스트가 없습니다"));
        MarketContent content = require(contentId);
        if (!content.isPublished()) {
            throw new NotFoundException("판매 중이 아닌 콘텐츠입니다");
        }
        if (purchaseRepository.existsByTenantIdAndContentId(tenant, contentId)) {
            throw new ConflictException("이미 구매한 콘텐츠입니다");
        }
        return purchaseRepository.save(new ContentPurchase(tenant, contentId, purchasedBy, content.getPrice()));
    }

    @Transactional(readOnly = true)
    public List<ContentPurchase> myPurchases() {
        UUID tenant = TenantContext.get().orElseThrow(() -> new NotFoundException("테넌트 컨텍스트가 없습니다"));
        return purchaseRepository.findByTenantId(tenant);
    }

    // --- 플랫폼 정산 ---

    /** 콘텐츠별 판매 건수·매출 집계(전 테넌트). */
    @Transactional(readOnly = true)
    public List<Settlement> settlements() {
        Map<UUID, MarketContent> byId = contentRepository.findAll().stream()
                .collect(Collectors.toMap(MarketContent::getId, Function.identity()));
        Map<UUID, List<ContentPurchase>> grouped = purchaseRepository.findAll().stream()
                .collect(Collectors.groupingBy(ContentPurchase::getContentId));
        return grouped.entrySet().stream().map(e -> {
            MarketContent c = byId.get(e.getKey());
            int revenue = e.getValue().stream().mapToInt(ContentPurchase::getAmount).sum();
            return new Settlement(e.getKey(), c == null ? "(삭제됨)" : c.getTitle(),
                    c == null ? null : c.getProvider(), e.getValue().size(), revenue);
        }).toList();
    }

    private MarketContent require(UUID id) {
        return contentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("콘텐츠를 찾을 수 없습니다: " + id));
    }

    public record Settlement(UUID contentId, String title, String provider, int purchaseCount, int revenue) {
    }
}
