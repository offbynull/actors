/*
 * Copyright (c) 2015, Kasra Faghihi, All rights reserved.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.
 */
package com.offbynull.peernetic.visualizer.gateways.graph;

import com.offbynull.peernetic.core.shuttle.Address;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.ListView;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.apache.commons.collections4.MultiMap;
import org.apache.commons.collections4.map.MultiValueMap;
import org.apache.commons.lang3.Validate;

/**
 * JavaFX application to display a 2D graph.
 * @author Kasra Faghihi
 */
public final class GraphApplication extends Application {

    private static volatile GraphApplication instance;
    private static final Lock LOCK = new ReentrantLock();
    private static final Condition STARTED_CONDITION = LOCK.newCondition();
    private static final Condition STOPPED_CONDITION = LOCK.newCondition();

    private ListView<String> listView;
    private ObservableList<String> listItems;
    private Map<String, GraphStage> graphStages;

    @Override
    public void init() throws Exception {
        Platform.setImplicitExit(true);
        graphStages = new HashMap<>();
    }

    @Override
    public void start(Stage stage) {
        listItems = FXCollections.observableArrayList();
        listView = new ListView<>(listItems);

        listView.setOnMouseClicked((MouseEvent click) -> {
            if (click.getClickCount() == 2) {
                String name = listView.getSelectionModel().getSelectedItem();
                graphStages.get(name).show();
            }
        });

        stage.setTitle("Graph Selection");
        stage.setWidth(150);
        stage.setHeight(400);

        // We have to do this because in some cases JavaFX threads won't shut down when the last stage closes, even if implicitExit is set
        // http://stackoverflow.com/questions/15808063/how-to-stop-javafx-application-thread/22997736#22997736
        stage.setOnCloseRequest(x -> Platform.exit());

        Scene scene = new Scene(listView, 1.0, 1.0, true, SceneAntialiasing.DISABLED);
        scene.setFill(Color.WHITESMOKE);
        stage.setScene(scene);

        stage.show();

        LOCK.lock();
        try {
            instance = this;
            STARTED_CONDITION.signalAll();
        } finally {
            LOCK.unlock();
        }
    }

    @Override
    public void stop() throws Exception {
        LOCK.lock();
        try {
            instance = null;
            STOPPED_CONDITION.signalAll();
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Get the instance of {@link GraphApplication} that was created when this JavaFX application was started.
     * @return {@link GraphApplication} instance
     */
    static GraphApplication getInstance() {
        LOCK.lock();
        try {
            return instance;
        } finally {
            LOCK.unlock();
        }
    }

    static GraphApplication awaitStarted() throws InterruptedException {
        LOCK.lock();
        try {
            if (instance == null) {
                STARTED_CONDITION.await();
            }
            return instance;
        } finally {
            LOCK.unlock();
        }
    }

    static void awaitStopped() throws InterruptedException {
        LOCK.lock();
        try {
            if (instance != null) {
                STOPPED_CONDITION.await();
            }
        } finally {
            LOCK.unlock();
        }
    }

    void execute(MultiMap<Address, Object> commands) {
        Validate.notNull(commands);

        MultiMap<Address, Object> commandsCopy = new MultiValueMap<>();
        commandsCopy.putAll(commands);
        Platform.runLater(() -> {
            for (Entry<Address, Object> entry : commandsCopy.entrySet()) {
                String fromAddress = entry.getKey().toString();
                GraphStage graphStage = graphStages.get(fromAddress);
                if (graphStage == null) {
                    graphStage = new GraphStage();
                    graphStages.put(fromAddress, graphStage);
                    listItems.add(fromAddress);

                    if (listItems.size() == 1) { // if this is the first graph, show it
                        graphStage.show();
                    }
                }

                @SuppressWarnings("unchecked")
                Collection<Object> stageCommands = (Collection<Object>) entry.getValue();

                List<Runnable> stageRunnables = graphStage.execute(stageCommands);
                for (Runnable runnable : stageRunnables) {
                    try {
                        runnable.run();
                    } catch (RuntimeException re) {
                        // TODO log here
                    }
                }
            }
        });
    }

    void execute(CreateStage createStage) {
        Validate.notNull(createStage);

        Platform.runLater(() -> {
            Stage stage = createStage.getFactory().get();
            stage.setOnCloseRequest(x -> stage.close()); // override close behaviour
            stage.show();
        });
    }
}
