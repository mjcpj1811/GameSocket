package server;

import common.Message;
import common.Protocol;
import server.dao.MatchDAO;
import server.dao.RoundDAO;
import server.dao.UserDAO;

import java.security.SecureRandom;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/** Quản lý 1 trận đấu 2 người trong 15 vòng */
public class GameRoom {
    private final ClientHandler p1;
    private final ClientHandler p2;
    private final String matchId;
    private final MatchDAO matchDAO = new MatchDAO();
    private final RoundDAO roundDAO = new RoundDAO();
    private final UserDAO userDAO = new UserDAO();
    private final SecureRandom rnd = new SecureRandom();
    private volatile boolean aborted = false;

    private final Map<Integer, RoundState> rounds = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicBoolean> processed = new ConcurrentHashMap<>();

    private int totalScore1 = 0, totalScore2 = 0;
    private double totalTime1 = 0, totalTime2 = 0;

    static class RoundState {
        String sequence;
        String ans1, ans2;
        double t1, t2;
        int s1, s2;
        boolean done1, done2;
    }

    public GameRoom(ClientHandler p1, ClientHandler p2) {
        this.p1 = p1;
        this.p2 = p2;
        this.matchId = matchDAO.createMatch(); // ✅ tạo ID và insert vào Matches
    }

    private String genSeq(int len) {
        String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < len; i++)
            sb.append(alphabet.charAt(rnd.nextInt(alphabet.length())));
        return sb.toString();
    }

    public void start() {
        // ✅ Đánh dấu đang trong trận
        p1.setInGame(true, this);
        p2.setInGame(true, this);
        userDAO.setStatus(p1.userId(), Protocol.STATUS_INGAME);
        userDAO.setStatus(p2.userId(), Protocol.STATUS_INGAME);
        p1.broadcastOnline();
        p2.broadcastOnline();

        p1.notifyMatchStart(matchId, p2.username());
        p2.notifyMatchStart(matchId, p1.username());

        for (int round = 1; round <= 5; round++) {
            if (aborted) return;

            String seq = genSeq(round);
            int timeShow = Math.min(round+1, 10);
            int timeInput = 5 + (round - 1);

            RoundState st = new RoundState();
            st.sequence = seq;
            rounds.put(round, st);
            processed.put(round, new AtomicBoolean(false));

            sendRoundStart(p1, round, seq, timeShow, timeInput);
            sendRoundStart(p2, round, seq, timeShow, timeInput);

            waitForRound(round, timeShow + timeInput + 3);
            if (aborted) return;

            if (!processed.get(round).compareAndSet(false, true)) continue;

            RoundState rs = rounds.get(round);
            computeScore(rs);

            double rt1 = round4(rs.t1);
            double rt2 = round4(rs.t2);

            roundDAO.saveRound(matchId, round, rs.sequence,
                    p1.userId(), p2.userId(),
                    rs.ans1, rs.ans2,
                    rs.s1, rs.s2, rt1, rt2);

            sendRoundResult(p1, p2, round, rs);
            sendRoundResult(p2, p1, round, rs);
        }

        if (!aborted) endMatch();
    }

    private void sendRoundStart(ClientHandler player, int round, String seq, int timeShow, int timeInput) {
        player.send(new Message(Protocol.ROUND_START)
                .put(Protocol.MATCH_ID, matchId)
                .put(Protocol.ROUND_NO, round)
                .put(Protocol.SEQ, seq)
                .put(Protocol.TIME_SHOW, timeShow)
                .put(Protocol.TIME_INPUT, timeInput));
    }

    private void sendRoundResult(ClientHandler player, ClientHandler opponent, int round, RoundState rs) {
        player.send(new Message(Protocol.ROUND_RESULT)
                .put(Protocol.ROUND_NO, round)
                .put("seq", rs.sequence)
                .put("yourAnswer", player == p1 ? rs.ans1 : rs.ans2)
                .put("oppAnswer", player == p1 ? rs.ans2 : rs.ans1)
                .put(Protocol.YOUR_SCORE, player == p1 ? rs.s1 : rs.s2)
                .put(Protocol.OPP_SCORE, player == p1 ? rs.s2 : rs.s1)
                .put(Protocol.YOUR_TIME, String.format("%.4f", player == p1 ? rs.t1 : rs.t2))
                .put(Protocol.OPP_TIME, String.format("%.4f", player == p1 ? rs.t2 : rs.t1)));
    }

    private void computeScore(RoundState rs) {
        rs.s1 = scoreFor(rs.sequence, rs.ans1);
        rs.s2 = scoreFor(rs.sequence, rs.ans2);
        totalScore1 += rs.s1;
        totalScore2 += rs.s2;

        totalTime1 = round4(totalTime1 + rs.t1);
        totalTime2 = round4(totalTime2 + rs.t2);
    }

    private int scoreFor(String seq, String ans) {
        if (ans == null) ans = "";
        seq = seq.toUpperCase();
        ans = ans.toUpperCase();
        if (ans.equals(seq)) return seq.length() + 3; // perfect +3
        int correct = 0;
        for (int i = 0; i < Math.min(seq.length(), ans.length()); i++) {
            if (seq.charAt(i) == ans.charAt(i)) correct++;
        }
        return correct;
    }

    private void waitForRound(int round, int maxSeconds) {
        long deadline = System.currentTimeMillis() + maxSeconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            RoundState st = rounds.get(round);
            if (st.done1 && st.done2) return;
            try { Thread.sleep(100); } catch (InterruptedException ignored) {}
        }
    }

    public synchronized void submit(String userId, int round, String answer, double elapsed) {
        RoundState st = rounds.get(round);
        if (st == null || aborted) return;

        double t = round4(elapsed);
        if (userId.equals(p1.userId())) { st.ans1 = answer; st.t1 = t; st.done1 = true; }
        else if (userId.equals(p2.userId())) { st.ans2 = answer; st.t2 = t; st.done2 = true; }

        // Nếu cả 2 đã nộp và chưa xử lý → xử lý ngay
        AtomicBoolean flag = processed.get(round);
        if (flag != null && !flag.get() && st.done1 && st.done2) {
            if (flag.compareAndSet(false, true)) {
                computeScore(st);
                sendRoundResult(p1, p2, round, st);
                sendRoundResult(p2, p1, round, st);
            }
        }
    }

    private void endMatch() {
        String winner = null;
        if (totalScore1 > totalScore2) winner = p1.userId();
        else if (totalScore2 > totalScore1) winner = p2.userId();
        else if (totalTime1 < totalTime2) winner = p1.userId();
        else if (totalTime2 < totalTime1) winner = p2.userId();

        if (winner != null)
            matchDAO.setWinner(matchId, winner);

        matchDAO.saveMatchDetail(p1.userId(), matchId, totalScore1, round4(totalTime1));
        matchDAO.saveMatchDetail(p2.userId(), matchId, totalScore2, round4(totalTime2));

        sendMatchResult(p1, totalScore1, totalTime1, winner);
        sendMatchResult(p2, totalScore2, totalTime2, winner);

        p1.setInGame(false, null);
        p2.setInGame(false, null);
        userDAO.setStatus(p1.userId(), Protocol.STATUS_ONLINE);
        userDAO.setStatus(p2.userId(), Protocol.STATUS_ONLINE);

        p1.broadcastOnline();
        p2.broadcastOnline();
    }

    private void sendMatchResult(ClientHandler player, int score, double time, String winner) {
        player.send(new Message(Protocol.MATCH_RESULT)
                .put(Protocol.MATCH_ID, matchId)
                .put(Protocol.TOTAL_SCORE, score)
                .put(Protocol.TOTAL_TIME, String.format("%.4f", round4(time)))
                .put("winner", winner));
    }

    private double round4(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    public void playerQuit(String quitterId) {
        try {
            aborted = true;
            boolean quitterIsP1 = p1.userId().equals(quitterId);
            ClientHandler quitter = quitterIsP1 ? p1 : p2;
            ClientHandler winner  = quitterIsP1 ? p2 : p1;

            int winnerScore  = quitterIsP1 ? totalScore2 : totalScore1;
            double winnerTime = quitterIsP1 ? totalTime2 : totalTime1;

            int quitterScore  = quitterIsP1 ? totalScore1 : totalScore2;
            double quitterTime = quitterIsP1 ? totalTime1 : totalTime2;

            String winnerId = winner.userId();

            matchDAO.setWinner(matchId, winnerId);
            matchDAO.saveMatchDetail(winnerId, matchId, winnerScore, round4(winnerTime));
            matchDAO.saveMatchDetail(quitter.userId(), matchId, quitterScore, round4(quitterTime));

            winner.send(new Message(Protocol.MATCH_RESULT)
                    .put(Protocol.MATCH_ID, matchId)
                    .put(Protocol.TOTAL_SCORE, winnerScore)
                    .put(Protocol.TOTAL_TIME, String.format("%.4f", round4(winnerTime)))
                    .put("winner", winnerId)
                    .put("info", "Đối thủ đã thoát, bạn thắng!"));

            quitter.send(new Message(Protocol.MATCH_RESULT)
                    .put(Protocol.MATCH_ID, matchId)
                    .put(Protocol.TOTAL_SCORE, quitterScore)
                    .put(Protocol.TOTAL_TIME, String.format("%.4f", round4(quitterTime)))
                    .put("winner", winnerId)
                    .put("info", "Bạn đã bị xử thua vì thoát giữa trận."));

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            p1.setInGame(false, null);
            p2.setInGame(false, null);
            userDAO.setStatus(p1.userId(), Protocol.STATUS_ONLINE);
            userDAO.setStatus(p2.userId(), Protocol.STATUS_ONLINE);
            p1.broadcastOnline();
            p2.broadcastOnline();
        }
    }
}
