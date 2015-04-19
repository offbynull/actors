package com.offbynull.peernetic.gateways.visualizer;

import java.util.function.Supplier;
import javafx.stage.Stage;
import org.apache.commons.lang3.Validate;

final class CreateStage {
    private final Supplier<? extends Stage> factory;

    public CreateStage(Supplier<? extends Stage> factory) {
        Validate.notNull(factory);
        this.factory = factory;
    }

    public Supplier<? extends Stage> getFactory() {
        return factory;
    }
    
}
