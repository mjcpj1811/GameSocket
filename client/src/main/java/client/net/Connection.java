package client.net;

import common.Message;
import common.Protocol;

import java.io.*;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * Connection trung tâm:
 *  - mainListener: listener chính (Lobby / Game UI)
 *  - once(type, cb): nhận đúng 1 gói cho các màn phụ (history/leaderboard)
 *  - cache ONLINE_UPDATE và luôn phát cho mainListener nếu có
 *  - messageQueue riêng cho GameService
 */
public class Connection implements AutoCloseable {
    private final Socket socket;
    private final PrintWriter out;
    private final BufferedReader in;

    private volatile Consumer<Message> mainListener; // listener chính
    private final Map<String, Consumer<Message>> oneTime = new ConcurrentHashMap<>();
    private final BlockingQueue<Message> gameQueue = new LinkedBlockingQueue<>();

    private volatile Message lastOnlineUpdate;

    public Connection(String host, int port, Consumer<Message> initialListener) throws IOException {
        this.socket = new Socket(host, port);
        this.out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
        this.in  = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
        this.mainListener = initialListener;

        Thread reader = new Thread(this::readLoop, "Client-Reader");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop() {
        String line;
        try {
            while ((line = in.readLine()) != null) {
                Message m = Message.fromJson(line);

                // 1) Ưu tiên gói online update: cache + phát live cho mainListener nếu có
                if (Protocol.ONLINE_UPDATE.equals(m.getType())) {
                    lastOnlineUpdate = m;
                    Consumer<Message> ml = mainListener;
                    if (ml != null) {
                        // Phát trực tiếp cho Lobby
                        ml.accept(m);
                    }
                    continue;
                }

                // 2) Nếu có one-time listener đăng ký cho type này thì trả cho nó
                Consumer<Message> cb = oneTime.remove(m.getType());
                if (cb != null) {
                    cb.accept(m);
                    continue;
                }

                // 3) Đưa các gói game vào hàng đợi
                String t = m.getType();
                if (Protocol.ROUND_START.equals(t) ||
                        Protocol.ROUND_RESULT.equals(t) ||
                        Protocol.MATCH_RESULT.equals(t)) {
                    gameQueue.offer(m);
                    continue;
                }

                // 4) Còn lại chuyển về mainListener (nếu có)
                Consumer<Message> ml = mainListener;
                if (ml != null) ml.accept(m);
            }
        } catch (Exception e) {
            System.err.println("[Connection] Reader stopped: " + e.getMessage());
        }
    }

    /** Gửi message tới server */
    public synchronized void send(Message m) {
        out.println(Message.toJson(m));
        out.flush();
    }

    /** Đặt listener chính (Lobby hoặc Game) */
    public synchronized void setMainListener(Consumer<Message> listener) {
        this.mainListener = listener;
    }

    /** Đăng ký callback nhận 1 lần duy nhất theo type (không đụng mainListener) */
    public void once(String type, Consumer<Message> callback) {
        if (type == null || callback == null) return;
        oneTime.put(type, callback);
    }

    /** Lấy gói game từ hàng đợi (GameService dùng) */
    public Message takeGameMessage() {
        try { return gameQueue.take(); }
        catch (InterruptedException e) { return null; }
    }

    /** Lấy gói ONLINE_UPDATE mới nhất (để lobby hiển thị ngay khi mở) */
    public Message getLastOnlineUpdate() { return lastOnlineUpdate; }
    public void setLastOnlineUpdate(Message m) { this.lastOnlineUpdate = m; }

    @Override
    public void close() throws IOException {
        socket.close();
    }
}
