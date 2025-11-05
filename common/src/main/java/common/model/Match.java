package common.model;

public class Match {
    private String id;
    private String winner;

    public Match() {}

    public Match(String id, String winner) {
        this.id = id;
        this.winner = winner;
    }


    public String getId() {
        return id;
    }

    public String getWinner() {
        return winner;
    }

    // Setters
    public void setId(String id) {
        this.id = id;
    }

    public void setWinner(String winner) {
        this.winner = winner;
    }


}
