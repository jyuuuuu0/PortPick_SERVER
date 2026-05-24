package com.example.PortPick_SERVER.model;

public enum CareerType {
    NEWCOMER("신입"),
    EXPERIENCED("경력");

    private final String label;

    CareerType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static CareerType from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("경력 구분을 선택해 주세요.");
        }

        String normalized = value.trim();
        for (CareerType careerType : values()) {
            if (careerType.name().equalsIgnoreCase(normalized) || careerType.label.equals(normalized)) {
                return careerType;
            }
        }

        throw new IllegalArgumentException("유효하지 않은 경력 구분입니다.");
    }
}
