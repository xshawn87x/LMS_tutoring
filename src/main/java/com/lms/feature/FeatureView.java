package com.lms.feature;

/**
 * 기능 플래그의 현재 상태(테넌트 기준).
 *
 * <ul>
 *   <li>{@code entitled} — 계층①: 요금제로 이 기능을 쓸 자격이 있는가(플랫폼이 부여).
 *   <li>{@code enabled}  — 유효 활성 = 자격 있음 AND 기관이 켬. 실제로 동작하는지.
 * </ul>
 * 기관 ADMIN 화면은 자격 없는 기능(🔒)은 켤 수 없고, 자격 있는 기능만 on/off(활성화)할 수 있다.
 */
public record FeatureView(
        String name,
        String displayName,
        boolean enabled,
        boolean entitled,
        boolean implemented
) {
    /** entitled: 요금제 자격 여부, activated: 기관 ADMIN의 on/off 선택. 유효 enabled = 둘 다 참. */
    public static FeatureView of(Feature feature, boolean entitled, boolean activated) {
        return new FeatureView(
                feature.name(), feature.getDisplayName(),
                entitled && activated, entitled, feature.isImplemented());
    }
}
