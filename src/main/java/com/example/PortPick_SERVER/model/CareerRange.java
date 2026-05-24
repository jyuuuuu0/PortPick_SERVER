package com.example.PortPick_SERVER.model;

public enum CareerRange {
    YEAR_1_TO_3("1년 ~ 3년"),
    YEAR_4_TO_7("4년 ~ 7년"),
    YEAR_8_PLUS("8년 ~");

    private final String label;

    CareerRange(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static CareerRange from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("경력 구간을 선택해 주세요.");
        }

        String normalized = value.trim();
        for (CareerRange careerRange : values()) {
            if (careerRange.name().equalsIgnoreCase(normalized) || careerRange.label.equals(normalized)) {
                return careerRange;
            }
        }

        throw new IllegalArgumentException("유효하지 않은 경력 구간입니다.");
    }
}
