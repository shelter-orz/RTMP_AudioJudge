package org.example;

/**
 * @author: Zhou Yujie
 * @date: 2023/6/12
 **/
public enum PcmAnalyzeResultEnum {
    MUTE_OR_LIGHT_WHITE_NOISE(0, "静音或轻微白噪声"),
    LITTLE_HUMAN_VOICE(1, "少量人声"),
    SPEAK_VOICE(2, "人声"),
    MUSIC(3, "音乐"),
    STRONG_WHITE_NOISE(4, "强烈白噪声")
    ;

    private int value;
    private String name;

    PcmAnalyzeResultEnum(int value, String name) {
        this.value = value;
        this.name = name;
    }

    public int getValue() {
        return value;
    }

    public void setValue(int value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
