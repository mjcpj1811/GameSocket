package server.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RoundDAO {
    public void saveRound(String matchId, int roundNo, String seq,
                          String p1Id, String p2Id,
                          String a1, String a2,
                          int s1, int s2,
                          double t1, double t2) {
        String id = "R" + matchId + "_" + roundNo;
        String sql = """
            INSERT INTO Rounds (id, number_round, sequence_shown,
                                player1_id, player2_id,
                                answer_player1, answer_player2,
                                score_player1, score_player2,
                                time_spent_player1, time_spent_player2,
                                MatchId)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (java.sql.Connection conn = Connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setInt(2, roundNo);
            ps.setString(3, seq);
            ps.setString(4, p1Id);
            ps.setString(5, p2Id);
            ps.setString(6, a1);
            ps.setString(7, a2);
            ps.setInt(8, s1);
            ps.setInt(9, s2);
            ps.setDouble(10, t1);
            ps.setDouble(11, t2);
            ps.setString(12, matchId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
