package com.example.PortPick_SERVER.model;

import java.util.Arrays;

public enum JobRole {
    FRONT_END("Front-end"),
    BACK_END("Back-end"),
    IOS("iOS"),
    ANDROID("Android"),
    FLUTTER("Flutter"),
    REACT_NATIVE("React Native"),
    DEVOPS("DevOps"),
    AI("AI"),
    DATA_ENGINEER("Data Engineer"),
    UI_UX("UI/UX"),
    GRAPHIC_3D("3D Graphic"),
    PM_PO("PM/PO"),
    IOT("IoT"),
    GAME("Game"),
    SECURITY_ENGINEER("Security Engineer"),
    IT_ENGINEER("IT Engineer"),
    CLOUD_ENGINEER("Cloud Engineer"),
    OTHER("그 외");

    private final String label;

    JobRole(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static JobRole from(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("직군을 선택해 주세요.");
        }

        return Arrays.stream(values())
                .filter(role -> role.name().equalsIgnoreCase(normalizeEnumValue(value))
                        || role.label.equalsIgnoreCase(value.trim()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 직군입니다."));
    }

    private static String normalizeEnumValue(String value) {
        return value.trim()
                .replace("-", "_")
                .replace("/", "_")
                .replace(" ", "_")
                .toUpperCase();
    }
}
