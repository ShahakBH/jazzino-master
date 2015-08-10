package com.yazino.novomatic.cgs;

public enum NovomaticCommand {
    Join("Init"),
    Spin("btn_start"),
    IncreaseLines("btn_inc_lines"),
    DecreaseLines("btn_dec_lines"),
    IncreaseBetPlacement("btn_inc_betpl"),
    DecreaseBetPlacement("btn_dec_betpl"),
    GambleOnRed("btn_red"),
    GambleOnBlack("btn_black"),
    CollectGamble("btn_collect");

    private final String novomaticButtonCode;

    NovomaticCommand(String novomaticButtonCode) {

        this.novomaticButtonCode = novomaticButtonCode;
    }

    public String getNovomaticButtonCode() {
        return novomaticButtonCode;
    }
}
