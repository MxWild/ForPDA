package forpdateam.ru.forpda.api.events.models;

/**
 * Created by radiationx on 29.07.17.
 */

public class NotificationEvent {
    public final static int SRC_EVENT_NEW = 1;
    public final static int SRC_EVENT_READ = 2;
    public final static int SRC_EVENT_MENTION = 3;
    public final static int SRC_EVENT_HAT_EDITED = 4;
    public final static String SRC_TYPE_SITE = "s";
    public final static String SRC_TYPE_THEME = "t";
    public final static String SRC_TYPE_QMS = "q";


    public enum Type {
        NEW(2),
        READ(4),
        MENTION(8),
        HAT_EDITED(16);

        private final int value;

        Type(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    public enum Source {
        THEME(32),
        SITE(64),
        QMS(128);

        private final int value;

        Source(int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }

    private Type type;
    private Source source;

    private int messageId = 0;

    private int sourceId = 0;
    private int userId = 0;

    private int timeStamp = 0;
    private int lastTimeStamp = 0;

    private int msgCount = 0;
    private boolean important = false;

    private String sourceTitle = "";
    private String userNick = "";

    private String sourceEventText;

    public int getMessageId() {
        return messageId;
    }

    public void setMessageId(int messageId) {
        this.messageId = messageId;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public Source getSource() {
        return source;
    }

    public void setSource(Source source) {
        this.source = source;
    }

    public int getSourceId() {
        return sourceId;
    }

    public void setSourceId(int sourceId) {
        this.sourceId = sourceId;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public int getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(int timeStamp) {
        this.timeStamp = timeStamp;
    }

    public int getLastTimeStamp() {
        return lastTimeStamp;
    }

    public void setLastTimeStamp(int lastTimeStamp) {
        this.lastTimeStamp = lastTimeStamp;
    }

    public int getMsgCount() {
        return msgCount;
    }

    public void setMsgCount(int msgCount) {
        this.msgCount = msgCount;
    }

    public boolean isImportant() {
        return important;
    }

    public void setImportant(boolean important) {
        this.important = important;
    }

    public String getSourceTitle() {
        return sourceTitle;
    }

    public void setSourceTitle(String sourceTitle) {
        this.sourceTitle = sourceTitle;
    }

    public String getUserNick() {
        return userNick;
    }

    public void setUserNick(String userNick) {
        this.userNick = userNick;
    }

    public String getSourceEventText() {
        return sourceEventText;
    }

    public void setSourceEventText(String sourceEventText) {
        this.sourceEventText = sourceEventText;
    }

    /*
    * short
    * */

    public boolean isNew() {
        return NotificationEvent.isNew(type);
    }

    public boolean isRead() {
        return NotificationEvent.isRead(type);
    }

    public boolean isMention() {
        return NotificationEvent.isMention(type);
    }

    public boolean fromTheme() {
        return NotificationEvent.fromTheme(source);
    }

    public boolean fromSite() {
        return NotificationEvent.fromSite(source);
    }

    public boolean fromQms() {
        return NotificationEvent.fromQms(source);
    }


    public static boolean isNew(Type type) {
        return type != null && type == Type.NEW;
    }

    public static boolean isRead(Type type) {
        return type != null && type == Type.READ;
    }

    public static boolean isMention(Type type) {
        return type != null && type == Type.MENTION;
    }

    public static boolean fromTheme(Source source) {
        return source != null && source == Source.THEME;
    }

    public static boolean fromSite(Source source) {
        return source != null && source == Source.SITE;
    }

    public static boolean fromQms(Source source) {
        return source != null && source == Source.QMS;
    }

    /*
    * for notifications
    *
    * */
    public int notifyId() {
        return notifyId(type);
    }

    public int notifyId(Type type) {
        return (sourceId / 4) + type.getValue() + type.getValue();
    }
}
