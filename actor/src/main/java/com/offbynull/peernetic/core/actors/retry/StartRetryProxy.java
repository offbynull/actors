package com.offbynull.peernetic.core.actors.retry;

import org.apache.commons.lang3.Validate;

public class StartRetryProxy {
    private final String timerPrefix;
    private final String actorPrefix;
    private final IdExtractor idExtractor;
    private final SendGuidelineGenerator sendGuidelineGenerator;
    private final ReceiveGuidelineGenerator receiveGuidelineGenerator;

    public StartRetryProxy(String timerPrefix, String actorPrefix, IdExtractor idExtractor,
            SendGuidelineGenerator sendGuidelineGenerator, ReceiveGuidelineGenerator receiveGuidelineGenerator) {
        Validate.notNull(timerPrefix);
        Validate.notNull(actorPrefix);
        Validate.notNull(idExtractor);
        Validate.notNull(sendGuidelineGenerator);
        Validate.notNull(receiveGuidelineGenerator);
        this.timerPrefix = timerPrefix;
        this.actorPrefix = actorPrefix;
        this.idExtractor = idExtractor;
        this.sendGuidelineGenerator = sendGuidelineGenerator;
        this.receiveGuidelineGenerator = receiveGuidelineGenerator;
    }

    public String getTimerPrefix() {
        return timerPrefix;
    }
    
    public String getActorPrefix() {
        return actorPrefix;
    }

    public IdExtractor getIdExtractor() {
        return idExtractor;
    }

    public SendGuidelineGenerator getSendGuidelineGenerator() {
        return sendGuidelineGenerator;
    }

    public ReceiveGuidelineGenerator getReceiveGuidelineGenerator() {
        return receiveGuidelineGenerator;
    }
}
