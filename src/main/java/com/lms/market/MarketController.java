package com.lms.market;

import com.lms.market.MarketService.Settlement;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 콘텐츠 마켓 API.
 * 플랫폼(본사, PLATFORM_ADMIN): 카탈로그 등록/수정/삭제 + 정산.
 * 학원(ADMIN): 공개 콘텐츠 둘러보기 + 구매 + 내 구매내역.
 */
@RestController
public class MarketController {

    private final MarketService service;

    public MarketController(MarketService service) {
        this.service = service;
    }

    // --- 플랫폼(본사) ---

    @GetMapping("/api/platform/market/content")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<ContentView> platformList() {
        return service.listAll().stream().map(ContentView::from).toList();
    }

    @PostMapping("/api/platform/market/content")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ContentView create(@RequestBody ContentRequest req) {
        return ContentView.from(service.create(req.title(), req.description(), req.category(), req.price(), req.provider()));
    }

    @PutMapping("/api/platform/market/content/{id}")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public ContentView update(@PathVariable UUID id, @RequestBody ContentRequest req) {
        return ContentView.from(service.update(id, req.title(), req.description(), req.category(),
                req.price(), req.provider(), req.published() == null || req.published()));
    }

    @DeleteMapping("/api/platform/market/content/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public void delete(@PathVariable UUID id) {
        service.delete(id);
    }

    @GetMapping("/api/platform/market/settlements")
    @PreAuthorize("hasRole('PLATFORM_ADMIN')")
    public List<Settlement> settlements() {
        return service.settlements();
    }

    // --- 학원(ADMIN) ---

    @GetMapping("/api/market/content")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ContentView> browse() {
        return service.listPublished().stream().map(ContentView::from).toList();
    }

    @PostMapping("/api/market/content/{id}/purchase")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public PurchaseView purchase(@PathVariable UUID id, @AuthenticationPrincipal Jwt jwt) {
        return PurchaseView.from(service.purchase(id, jwt.getSubject()));
    }

    @GetMapping("/api/market/purchases")
    @PreAuthorize("hasRole('ADMIN')")
    public List<PurchaseView> myPurchases() {
        return service.myPurchases().stream().map(PurchaseView::from).toList();
    }

    // --- DTO ---

    public record ContentRequest(@NotBlank String title, String description, String category,
                                 int price, String provider, Boolean published) {
    }

    public record ContentView(UUID id, String title, String description, String category,
                              int price, String provider, boolean published, OffsetDateTime createdAt) {
        static ContentView from(MarketContent c) {
            return new ContentView(c.getId(), c.getTitle(), c.getDescription(), c.getCategory(),
                    c.getPrice(), c.getProvider(), c.isPublished(), c.getCreatedAt());
        }
    }

    public record PurchaseView(UUID id, UUID contentId, String purchasedBy, int amount, OffsetDateTime createdAt) {
        static PurchaseView from(ContentPurchase p) {
            return new PurchaseView(p.getId(), p.getContentId(), p.getPurchasedBy(), p.getAmount(), p.getCreatedAt());
        }
    }
}
