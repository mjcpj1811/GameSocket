package common;
/**
 * Các hằng số tên message type & field dùng chung giữa server-client.
 * Giao thức: 1 dòng JSON/1 message qua TCP (sockets).
 */
public interface Protocol {
    String LOGIN            = "LOGIN";
    String LOGIN_OK         = "LOGIN_OK";
    String LOGIN_FAIL       = "LOGIN_FAIL";
    String LOGOUT           = "LOGOUT";

    String LIST_ONLINE      = "LIST_ONLINE";
    String ONLINE_UPDATE    = "ONLINE_UPDATE";

    String CHALLENGE        = "CHALLENGE";          // mời thách đấu
    String CHALLENGE_REQ    = "CHALLENGE_REQ";      // server đẩy sang người bị mời
    String CHALLENGE_RESP   = "CHALLENGE_RESP";     // accept/decline
    String MATCH_START      = "MATCH_START";
    String ROUND_START      = "ROUND_START";
    String SUBMIT_ANSWER    = "SUBMIT_ANSWER";
    String QUIT_MATCH       = "QUIT_MATCH";
    String ROUND_RESULT     = "ROUND_RESULT";
    String MATCH_RESULT     = "MATCH_RESULT";

    String GET_LEADERBOARD  = "GET_LEADERBOARD";
    String LEADERBOARD_DATA = "LEADERBOARD_DATA";

    String GET_HISTORY      = "GET_HISTORY";
    String HISTORY_DATA     = "HISTORY_DATA";

    String USERNAME = "username";
    String PASSWORD = "password";
    String TOKEN    = "token";

    String YOU      = "you";
    String OPP      = "opponent";
    String USERS    = "users";

    String ACCEPT   = "accept";
    String MATCH_ID = "matchId";
    String ROUND_NO = "round";
    String SEQ      = "sequence";
    String TIME_SHOW= "timeShow";
    String TIME_INPUT = "timeInput";
    String ANSWER   = "answer";
    String ELAPSED  = "elapsed";

    String YOUR_SCORE   = "yourScore";
    String OPP_SCORE    = "oppScore";
    String YOUR_TIME    = "yourTime";
    String OPP_TIME     = "oppTime";
    String TOTAL_SCORE  = "totalScore";
    String TOTAL_TIME   = "totalTime";

    String ENTRIES  = "entries";
    String HISTORY  = "history";


    String STATUS_ONLINE   = "ONLINE";
    String STATUS_OFFLINE  = "OFFLINE";
    String STATUS_INGAME   = "INGAME";
}
