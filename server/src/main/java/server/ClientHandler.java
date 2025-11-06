package server;

import common.Message;
import common.Protocol;
import common.model.User;
import server.dao.LeaderboardDAO;
import server.dao.MatchDAO;
import server.dao.UserDAO;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Quản lý 1 kết nối client – mỗi người chơi có 1 luồng riêng
 * Giao tiếp qua socket và gửi nhận Message (JSON)
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final Matchmaker hub;
    private final UserDAO userDAO = new UserDAO();
    private final MatchDAO matchDAO = new MatchDAO();
    private final LeaderboardDAO lbDAO = new LeaderboardDAO();

    private volatile boolean running = true;
    private PrintWriter out;
    private BufferedReader in;

    private String userId;
    private String username;
    private boolean inGame = false;
    private GameRoom currentRoom;

    public ClientHandler(Socket socket, Matchmaker hub) {
        this.socket = socket;
        this.hub = hub;
    }

    public String userId() { return userId; }
    public String username() { return username; }

    public void setInGame(boolean ig, GameRoom room) {
        this.inGame = ig;
        this.currentRoom = room;
    }

    /** Gửi message tới client */
    public void send(Message m) {
        try {
            out.println(Message.toJson(m));
            out.flush();
        } catch (Exception e) {
            System.err.println("[Send failed] " + username + ": " + e.getMessage());
        }
    }

    /** Gửi tín hiệu bắt đầu trận */
    public void notifyMatchStart(String matchId, String opponent) {
        send(new Message(Protocol.MATCH_START)
                .put(Protocol.MATCH_ID, matchId)
                .put(Protocol.OPP, opponent));
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));

            String line;
            while (running && (line = in.readLine()) != null) {
                Message msg = Message.fromJson(line);
                switch (msg.getType()) {
                    case Protocol.LOGIN -> onLogin(msg);
                    case Protocol.LOGOUT -> onLogout();
                    case Protocol.LIST_ONLINE -> onListOnline();
                    case Protocol.CHALLENGE -> onChallenge(msg);
                    case Protocol.CHALLENGE_RESP -> onChallengeResp(msg);
                    case Protocol.SUBMIT_ANSWER -> onSubmitAnswer(msg);
                    case Protocol.GET_LEADERBOARD -> onLeaderboard();
                    case Protocol.GET_HISTORY -> onHistory();
                    case Protocol.QUIT_MATCH -> onQuitMatch();
                }
            }

        } catch (IOException e) {
            System.out.println("[Disconnect] " + username + " (" + socket.getRemoteSocketAddress() + ")");
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            cleanup();
        }
    }

    /** Xử lý đăng nhập */
    private void onLogin(Message m) {
        String uname = (String) m.getPayload().get(Protocol.USERNAME);
        String pass  = (String) m.getPayload().get(Protocol.PASSWORD);


        userDAO.login(uname, pass).ifPresentOrElse(u -> {
            this.userId = u.getId();
            this.username = u.getUsername();

            userDAO.setStatus(userId, Protocol.STATUS_ONLINE);

            // 🟢 Bước 1: thêm client mới vào hub
            hub.add(userId, this);

            // 🟢 Bước 2: chờ một chút để hub ổn định (tránh race-condition)
            try {
                Thread.sleep(50);
            } catch (InterruptedException ignored) {}

            // 🟢 Bước 3: gửi phản hồi đăng nhập thành công
            Message okMsg = new Message(Protocol.LOGIN_OK)
                    .put(Protocol.TOKEN, userId)
                    .put(Protocol.YOU, username);
            send(okMsg);

            // 🟢 Bước 4: broadcast danh sách online tới tất cả
            broadcastOnline();

        }, () -> {
            System.out.println("[LOGIN] Login failed for " + uname);
            send(new Message(Protocol.LOGIN_FAIL));
        });
    }

    /** Đăng xuất */
    private void onLogout() {
        running = false;
        try {
            userDAO.setStatus(userId, Protocol.STATUS_OFFLINE);
            hub.remove(userId);
            broadcastOnline();
            socket.close();
        } catch (IOException ignored) {}
    }

    /** Gửi danh sách người chơi online (trừ chính mình) */
    public void onListOnline() {
        var arr = hub.all().values().stream()
                .filter(h -> !Objects.equals(h.userId, this.userId))
                .map(h -> Map.of(
                        "id", h.userId,
                        "username", h.username,
                        "status", h.inGame ? Protocol.STATUS_INGAME : Protocol.STATUS_ONLINE
                ))
                .collect(Collectors.toList());

        send(new Message(Protocol.ONLINE_UPDATE).put(Protocol.USERS, arr));
    }

    /** Broadcast danh sách online cho tất cả client */
    public void broadcastOnline() {
        List<ClientHandler> snapshot = new ArrayList<>(hub.all().values());
        for (ClientHandler h : hub.all().values()) {
            try {
                h.onListOnline();
            } catch (Exception e) {
                System.err.println("[SERVER] Failed to send ONLINE_UPDATE to " + h.username() + ": " + e.getMessage());
            }
        }
    }

    /** Xử lý thách đấu */
    private void onChallenge(Message m) {
        String oppId = (String) m.getPayload().get(Protocol.OPP);
        ClientHandler opp = hub.findByUserId(oppId);
        if (opp == null) return;
        if (opp.inGame) {
            send(new Message(Protocol.CHALLENGE_RESP)
                    .put(Protocol.ACCEPT, false)
                    .put("opponent", opp.username + " đang trong trận đấu khác."));
            return;
        }
        opp.send(new Message(Protocol.CHALLENGE_REQ)
                .put(Protocol.YOU, this.userId)
                .put("name", this.username));
    }

    /** Xử lý phản hồi thách đấu */
    private void onChallengeResp(Message m) {
        boolean accept = Boolean.TRUE.equals(m.getPayload().get(Protocol.ACCEPT));
        String challengerId = (String) m.getPayload().get(Protocol.YOU);
        ClientHandler challenger = hub.findByUserId(challengerId);
        if (challenger == null) return;

        if (!accept) {
            challenger.send(new Message(Protocol.CHALLENGE_RESP)
                    .put(Protocol.ACCEPT, false)
                    .put("opponent", this.username));
            return;
        }
        if (this.inGame || challenger.inGame) {
            challenger.send(new Message(Protocol.CHALLENGE_RESP)
                    .put(Protocol.ACCEPT, false)
                    .put("opponent", this.username + " đang trong trận"));
            return;
        }

        this.inGame = challenger.inGame = true;
        userDAO.setStatus(this.userId, Protocol.STATUS_INGAME);
        userDAO.setStatus(challenger.userId, Protocol.STATUS_INGAME);
        GameRoom room = new GameRoom(challenger, this);
        new Thread(room::start, "GameRoom-" + System.currentTimeMillis()).start();
        broadcastOnline();
    }

    /** Xử lý nộp đáp án */
    private void onSubmitAnswer(Message m) {
        if (currentRoom == null) return;
        int round = ((Number) m.getPayload().get(Protocol.ROUND_NO)).intValue();
        String answer = (String) m.getPayload().get(Protocol.ANSWER);
        double elapsed = ((Number) m.getPayload().get(Protocol.ELAPSED)).doubleValue();
        currentRoom.submit(userId, round, answer, elapsed);
    }

    /** Lấy bảng xếp hạng */
    private void onLeaderboard() {
        var entries = lbDAO.getLeaderboard(50);
        send(new Message(Protocol.LEADERBOARD_DATA).put(Protocol.ENTRIES, entries));
    }

    /** Lấy lịch sử đấu */
    private void onHistory() {
        var his = matchDAO.getHistoryByUser(userId);
        send(new Message(Protocol.HISTORY_DATA).put(Protocol.HISTORY, his));
    }

    /** Xử lý thoát trận */
    private void onQuitMatch() {
        if (inGame && currentRoom != null) {
            currentRoom.playerQuit(userId);
            userDAO.setStatus(userId, Protocol.STATUS_ONLINE);
            inGame = false;
            currentRoom = null;
        }
        broadcastOnline();
    }
    /** Dọn dẹp khi client mất kết nối */
    private void cleanup() {
        try {
            if (userId != null) {
                userDAO.setStatus(userId, Protocol.STATUS_OFFLINE);
                hub.remove(userId);
                new Thread(() -> {
                    try {
                        Thread.sleep(100);
                        broadcastOnline();
                    } catch (Exception e) {
                        System.err.println("[Cleanup] broadcast failed: " + e.getMessage());
                    }
                }).start();
            }
        } catch (Exception e) {
            System.err.println("Cleanup error for " + username + ": " + e.getMessage());
        } finally {
            try { socket.close(); } catch (IOException ignored) {}
        }
    }
}
