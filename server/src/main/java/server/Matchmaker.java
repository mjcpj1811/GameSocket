package server;

import common.Message;
import common.Protocol;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Hub lưu danh sách ClientHandler đang online
 * Hỗ trợ broadcast danh sách online tới tất cả client
 */
public class Matchmaker {
    private final Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    public void add(String userId, ClientHandler handler) {
        clients.put(userId, handler);
    }

    public void remove(String userId) {
        clients.remove(userId);
    }

    public Map<String, ClientHandler> all() {
        return clients;
    }

    public ClientHandler findByUserId(String id) {
        return clients.get(id);
    }

}
