package server.dao;

import common.model.Match;
import common.model.MatchDetail;
import java.sql.*;
import java.util.*;

public class MatchDAO {

    // Lấy lịch sử đấu của người chơi
    public List<Map<String, Object>> getHistoryByUser(String userId) {
        List<Map<String, Object>> list = new ArrayList<>();
        String sql = """
        SELECT m.id AS match_id, u.username AS opponent,
               md.total_score, md.play_time, m.winner
        FROM MatchDetails md
        JOIN Matches m ON md.MatchId = m.id
        JOIN MatchDetails opp_md ON opp_md.MatchId = m.id AND opp_md.UserId <> md.UserId
        JOIN Users u ON u.id = opp_md.UserId
        WHERE md.UserId = ?
        ORDER BY m.id DESC
    """;

        try (java.sql.Connection conn = Connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                String winnerId = rs.getString("winner");
                String opponentName = rs.getString("opponent");

                String winnerDisplay;
                if (winnerId == null) {
                    winnerDisplay = "Hòa";
                } else if (winnerId.equals(userId)) {
                    winnerDisplay = "Bạn";
                } else {
                    winnerDisplay = opponentName;
                }

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("matchId", rs.getString("match_id"));
                row.put("opponent", opponentName);
                row.put("score", rs.getInt("total_score"));
                row.put("time", rs.getDouble("play_time"));
                row.put("winner", winnerDisplay);
                list.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // Tạo 1 trận mới
    public String createMatch() {
        String id = "M" + System.currentTimeMillis();
        String sql = "INSERT INTO Matches (id, winner) VALUES (?, NULL)";
        try (java.sql.Connection conn = Connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return id;
    }


    // Lưu tổng kết trận
    public void saveMatchDetail(String userId, String matchId, int totalScore, double totalTime) {
        String sql = "INSERT INTO MatchDetails (UserId, MatchId, total_score, play_time) VALUES (?, ?, ?, ?)";
        try (java.sql.Connection conn = Connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.setString(2, matchId);
            ps.setInt(3, totalScore);
            ps.setDouble(4, totalTime);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // Cập nhật người thắng
    public void setWinner(String matchId, String winnerId) {
        String sql = "UPDATE Matches SET winner = ? WHERE id = ?";
        try (java.sql.Connection conn = Connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, winnerId);
            ps.setString(2, matchId);
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
