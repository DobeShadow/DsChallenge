package io.bukkitcode.dschallenge.data;

import io.bukkitcode.dschallenge.challenge.Challenge;

import java.util.UUID;

/**
 * 单个玩家的挑战数据。
 * 每个玩家每天拥有自己独立抽取的主挑战与支线挑战。
 */
public class PlayerData {

    /** 挑战状态 */
    public enum Status {
        /** 进行中 */
        IN_PROGRESS,
        /** 已完成 */
        COMPLETED,
        /** 已失败（主挑战死亡） */
        FAILED
    }

    private final UUID uuid;
    /** 数据所属日期，格式 yyyy-MM-dd */
    private String date;

    /** 该玩家今日的主挑战，null 表示尚未抽取 */
    private Challenge mainChallenge;
    /** 该玩家今日的支线挑战 */
    private Challenge sideChallenge;

    private int mainProgress;
    private Status mainStatus = Status.IN_PROGRESS;

    private int sideProgress;
    private Status sideStatus = Status.IN_PROGRESS;

    public PlayerData(UUID uuid, String date) {
        this.uuid = uuid;
        this.date = date;
    }

    public UUID getUuid() {
        return uuid;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public Challenge getMainChallenge() {
        return mainChallenge;
    }

    public void setMainChallenge(Challenge mainChallenge) {
        this.mainChallenge = mainChallenge;
    }

    public Challenge getSideChallenge() {
        return sideChallenge;
    }

    public void setSideChallenge(Challenge sideChallenge) {
        this.sideChallenge = sideChallenge;
    }

    /**
     * 是否已经抽取过今日挑战
     */
    public boolean hasDrawn() {
        return mainChallenge != null && sideChallenge != null;
    }

    public int getMainProgress() {
        return mainProgress;
    }

    public void setMainProgress(int mainProgress) {
        this.mainProgress = mainProgress;
    }

    public void addMainProgress(int delta) {
        this.mainProgress += delta;
    }

    public Status getMainStatus() {
        return mainStatus;
    }

    public void setMainStatus(Status mainStatus) {
        this.mainStatus = mainStatus;
    }

    public int getSideProgress() {
        return sideProgress;
    }

    public void setSideProgress(int sideProgress) {
        this.sideProgress = sideProgress;
    }

    public void addSideProgress(int delta) {
        this.sideProgress += delta;
    }

    public Status getSideStatus() {
        return sideStatus;
    }

    public void setSideStatus(Status sideStatus) {
        this.sideStatus = sideStatus;
    }

    /**
     * 重置为全新一天的进度（清空挑战与进度）
     */
    public void reset(String newDate) {
        this.date = newDate;
        this.mainChallenge = null;
        this.sideChallenge = null;
        this.mainProgress = 0;
        this.mainStatus = Status.IN_PROGRESS;
        this.sideProgress = 0;
        this.sideStatus = Status.IN_PROGRESS;
    }
}
