package io.pocat.platform.util.id;

public class PlatformIdGenerator {
    private static final String MESSAGE_ID_PREFIX = "M";
    private static final String PROCESS_ID_PREFIX = "P";
    private static final String TX_ID_PREFIX = "T";

    public static String generateMessageId() {
        return MESSAGE_ID_PREFIX + DistributedIdGenerator.getInstance().generate();
    }

    public static String generatePid() {
        return PROCESS_ID_PREFIX + DistributedIdGenerator.getInstance().generate();
    }

    public static String generateTxId() {
        return TX_ID_PREFIX + DistributedIdGenerator.getInstance().generate();
    }
}
