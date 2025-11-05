package server.util;
import java.util.UUID;
public final class IdGen {
    private IdGen() {}
    public static String id() { return UUID.randomUUID().toString(); }
}
