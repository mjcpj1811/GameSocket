package common;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import java.util.HashMap;
import java.util.Map;

public class Message {
    @SerializedName("type")
    private String type;             // e.g., LOGIN, LIST_ONLINE, CHALLENGE, ...
    @SerializedName("payload")
    private Map<String, Object> payload = new HashMap<>();

    public Message() {}
    public Message(String type) { this.type = type; }

    public String getType() { return type; }
    public Map<String, Object> getPayload() { return payload; }

    public Message put(String key, Object val) { payload.put(key, val); return this; }

    public static String toJson(Message m) { return new Gson().toJson(m); }
    public static Message fromJson(String json) { return new Gson().fromJson(json, Message.class); }
}
