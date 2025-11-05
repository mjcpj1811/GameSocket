package common.model;

public class MatchDetail {
    private String userId;
    private String matchId;
    private int totalScore;
    private double playTime;

    public MatchDetail() {}

    public MatchDetail(String userId, String matchId, int totalScore, double playTime) {
        this.userId = userId;
        this.matchId = matchId;
        this.totalScore = totalScore;
        this.playTime = playTime;
    }

    // Getters
    public String getUserId() {
        return userId;
    }

    public String getMatchId() {
        return matchId;
    }

    public int getTotalScore() {
        return totalScore;
    }

    public double getPlayTime() {
        return playTime;
    }

    // Setters
    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setMatchId(String matchId) {
        this.matchId = matchId;
    }

    public void setTotalScore(int totalScore) {
        this.totalScore = totalScore;
    }

    public void setPlayTime(double playTime) {
        this.playTime = playTime;
    }

}
