package com.wordiga.service;

import com.wordiga.dto.tourismContent.SigunguResponse;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class ChungnamSigungu {
    private ChungnamSigungu() { }

    public static final List<SigunguResponse> ALL = List.of(
            new SigunguResponse("110", "천안시 동남구"), new SigunguResponse("120", "천안시 서북구"),
            new SigunguResponse("150", "공주시"), new SigunguResponse("180", "보령시"),
            new SigunguResponse("200", "아산시"), new SigunguResponse("210", "서산시"),
            new SigunguResponse("230", "논산시"), new SigunguResponse("250", "계룡시"),
            new SigunguResponse("270", "당진시"), new SigunguResponse("310", "금산군"),
            new SigunguResponse("330", "부여군"), new SigunguResponse("340", "서천군"),
            new SigunguResponse("350", "청양군"), new SigunguResponse("360", "홍성군"),
            new SigunguResponse("370", "예산군"), new SigunguResponse("380", "태안군")
    );
    public static final Map<String, String> NAMES = ALL.stream()
            .collect(Collectors.toUnmodifiableMap(SigunguResponse::code, SigunguResponse::name));

    public static final Map<String, String> DATA_LAB_CODES = Map.ofEntries(
            Map.entry("110", "44131"), Map.entry("120", "44133"),
            Map.entry("150", "44150"), Map.entry("180", "44180"),
            Map.entry("200", "44200"), Map.entry("210", "44210"),
            Map.entry("230", "44230"), Map.entry("250", "44250"),
            Map.entry("270", "44270"), Map.entry("310", "44710"),
            Map.entry("330", "44760"), Map.entry("340", "44770"),
            Map.entry("350", "44790"), Map.entry("360", "44800"),
            Map.entry("370", "44810"), Map.entry("380", "44825"));
}
