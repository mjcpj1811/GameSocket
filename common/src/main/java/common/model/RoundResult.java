package common.model;

public class RoundResult {
    private String id;
    private int roundNo;
    private String matchId;
    private String player1Id;
    private String player2Id;
    private String sequence;
    private String answerPlayer1;
    private String answerPlayer2;
    private int scorePlayer1;
    private int scorePlayer2;
    private double timePlayer1;
    private double timePlayer2;

    public RoundResult() {}

    public RoundResult(String id, int roundNo, String matchId, String player1Id, String player2Id,
                       String sequence, String answerPlayer1, String answerPlayer2,
                       int scorePlayer1, int scorePlayer2, double timePlayer1, double timePlayer2) {
        this.id = id;
        this.roundNo = roundNo;
        this.matchId = matchId;
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.sequence = sequence;
        this.answerPlayer1 = answerPlayer1;
        this.answerPlayer2 = answerPlayer2;
        this.scorePlayer1 = scorePlayer1;
        this.scorePlayer2 = scorePlayer2;
        this.timePlayer1 = timePlayer1;
        this.timePlayer2 = timePlayer2;
    }

    // Getters
    public String getId() { return id; }
    public int getRoundNo() { return roundNo; }
    public String getMatchId() { return matchId; }
    public String getPlayer1Id() { return player1Id; }
    public String getPlayer2Id() { return player2Id; }
    public String getSequence() { return sequence; }
    public String getAnswerPlayer1() { return answerPlayer1; }
    public String getAnswerPlayer2() { return answerPlayer2; }
    public int getScorePlayer1() { return scorePlayer1; }
    public int getScorePlayer2() { return scorePlayer2; }
    public double getTimePlayer1() { return timePlayer1; }
    public double getTimePlayer2() { return timePlayer2; }
}
