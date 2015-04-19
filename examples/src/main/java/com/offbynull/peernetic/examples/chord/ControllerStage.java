package com.offbynull.peernetic.examples.chord;

import com.offbynull.peernetic.core.actor.ActorThread;
import com.offbynull.peernetic.examples.chord.internalmessages.Start;
import com.offbynull.peernetic.examples.common.nodeid.NodeId;
import java.util.Random;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
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

        
        Button addButton = new Button("Add");
        TextField addTextField = new TextField();
        HBox addHbox = new HBox(2.0, addTextField, addButton);
        VBox.setMargin(addHbox, new Insets(0, 0, 0, 8));
        vbox.getChildren().add(new Label("<id> <connect_to_id>"));
        vbox.getChildren().add(addHbox);
        addButton.setOnAction((x) -> {
            String text = addTextField.getText();
            String[] splitText = text.split("\\s+");
            int id = Integer.parseInt(splitText[0]);
            int connId = Integer.parseInt(splitText[1]);
            actorThread.addCoroutineActor("" + id, new ChordClientCoroutine(),
                    new Start("actor:" + connId, new NodeId(id, bits), new Random(id), "timer", "graph"));
//            addTextField.setText("");
        });
        
        
        Button removeButton = new Button("Remove");
        TextField removeTextField = new TextField();
        HBox removeHbox = new HBox(2.0, removeTextField, removeButton);
        VBox.setMargin(removeHbox, new Insets(0, 0, 0, 8));
        vbox.getChildren().add(new Label("<id>"));
        vbox.getChildren().add(removeHbox);
        removeButton.setOnAction((x) -> {
            String text = removeTextField.getText();
            actorThread.removeActor(text);
//            removeTextField.setText("");
        });

        setTitle("Controller");
        setWidth(300);
        setHeight(200);

        setOnCloseRequest(x -> hide());


        Scene scene = new Scene(vbox, 1.0, 1.0, true, SceneAntialiasing.DISABLED);
        scene.setFill(Color.WHITESMOKE);
        setScene(scene);
    }
}
