package com.offbynull.peernetic.examples.chord;

import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.core.shuttle.Address;
import com.offbynull.peernetic.core.shuttle.Message;
import com.offbynull.peernetic.examples.chord.internalmessages.Kill;
import com.offbynull.peernetic.examples.chord.internalmessages.Start;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import java.util.Collections;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.apache.commons.lang3.Validate;

final class ControllerStage extends Stage {

    public ControllerStage(ActorThread actorThread, int bits) {
        Validate.notNull(actorThread);

        VBox vbox = new VBox();
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);

        
        Button addButton = new Button("Create");
        TextField addStartTextField = new TextField();
        TextField addEndTextField = new TextField();
        TextField addConnTextField = new TextField();
        addStartTextField.setPromptText("Start id");
        addEndTextField.setPromptText("End id");
        addConnTextField.setPromptText("Conn id (optional)");
        vbox.getChildren().add(addStartTextField);
        vbox.getChildren().add(addEndTextField);
        vbox.getChildren().add(addConnTextField);
        vbox.getChildren().add(addButton);
        addButton.setOnAction((x) -> {
                int startId = Integer.parseInt(addStartTextField.getText());
                int endId = Integer.parseInt(addEndTextField.getText());
                
                String connectToId = addConnTextField.getText();
                
                for (int id = startId; id <= endId; id++) {
                    actorThread.addCoroutineActor("" + id, new ChordClientCoroutine(),
                            new Start(
                                    connectToId.isEmpty() ? null : Address.of("actor", connectToId),
                                    new NodeId(id, bits),
                                    id,
                                    Address.fromString("timer"),
                                    Address.fromString("graph"),
                                    Address.fromString("log")
                            )
                    );
                }
        });
        
        
        Button removeButton = new Button("Kill");
        TextField removeStartTextField = new TextField();
        TextField removeEndTextField = new TextField();
        removeStartTextField.setPromptText("Start id");
        removeEndTextField.setPromptText("End id");
        vbox.getChildren().add(removeStartTextField);
        vbox.getChildren().add(removeEndTextField);
        vbox.getChildren().add(removeButton);
        removeButton.setOnAction((x) -> {
            int startId = Integer.parseInt(removeStartTextField.getText());
            int endId = Integer.parseInt(removeEndTextField.getText());

            for (int id = startId; id <= endId; id++) {
                actorThread.getIncomingShuttle().send(
                        Collections.singleton(
                                new Message(
                                        Address.of("actor", "" + id),
                                        Address.of("actor", "" + id),
                                        new Kill()
                                )
                        )
                );
            }
        });
        
        vbox.requestFocus();

        setTitle("Controller");
        setWidth(300);
        setHeight(400);

        setOnCloseRequest(x -> hide());


        Scene scene = new Scene(vbox, 1.0, 1.0, true, SceneAntialiasing.DISABLED);
        scene.setFill(Color.WHITESMOKE);
        setScene(scene);
    }
}
