package com.lms.platform;

/** 기능 자격이 어디서 왔는지. 요금제 변경 시 PLAN 행만 교체하고 ADDON 행은 보존한다. */
public enum EntitlementSource {
    /** 요금제로 부여됨 — 요금제가 바뀌면 통째로 교체된다. */
    PLAN,
    /** 요금제 밖에서 개별 부여됨(애드온) — 요금제 변경과 무관하게 유지된다. */
    ADDON
}
