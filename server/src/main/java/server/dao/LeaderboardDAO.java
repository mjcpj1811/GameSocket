package server.dao;

import java.sql.*;
import java.util.*;

public class LeaderboardDAO {

    public List<Map<String, Object>> getLeaderboard(int limit) {
        List<Map<String, Object>> list = new ArrayList<>();

        String sql = """
        SELECT u.username,
               t.total_score AS best_score,
               t.play_time  AS best_time
        FROM (
            SELECT md.UserId,
                   md.total_score,
                   md.play_time,
                   ROW_NUMBER() OVER (
                       PARTITION BY md.UserId
                       ORDER BY md.total_score DESC, md.play_time ASC
                   ) AS rn
            FROM MatchDetails md
        ) t
        JOIN Users u ON u.id = t.UserId
        WHERE t.rn = 1
        ORDER BY t.total_score DESC, t.play_time ASC
        LIMIT ?
    """;

        try (java.sql.Connection conn = Connection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("username", rs.getString("username"));
                row.put("bestScore", rs.getInt("best_score"));
                row.put("bestTime", rs.getDouble("best_time"));
                list.add(row);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

}
