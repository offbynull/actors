package com.offbynull.peernetic.debug.visualizer;

import javafx.application.Application;
import javafx.geometry.Bounds;
import javafx.geometry.VPos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.Stage;

public final class JavaFxVisualizer<A> implements Visualizer<A> {

    private InternalApplication internalApplication;

    public JavaFxVisualizer() {
        Application.launch(InternalApplication.class);
        internalApplication = new InternalApplication();
        internalApplication.getParameters();
    }

    @Override
    public void step(String output, Command... commands) {
    }

    @Override
    public void visualize() {
    }

    @Override
    public void visualize(Recorder recorder, VisualizerEventListener listener) {
    }

    public static final class InternalApplication extends Application {

        private Group rootGroup;
        private Scene scene;

        public InternalApplication() {
        }

        @Override
        public void start(Stage stage) throws Exception {

            rootGroup = new Group();
            scene = new Scene(rootGroup, 500, 400, Color.WHITE);

            for (int i = 0; i < 1000; i++) {
                Node node = createNode("test");
                node.setLayoutX(i * 2);
                node.setLayoutY(i * 2);
                rootGroup.getChildren().add(node);
            }

            stage.setScene(scene);
            stage.show();
        }
        
        private Group createNode(String nodeText) {
            Group nodeGroup = new Group();
            
            Text text = new Text(2, 2, nodeText);
            text.setTextOrigin(VPos.TOP);
            text.setFont(Font.font("Serif", 30));
            text.setFill(Color.RED);
            Bounds textBounds = text.getLayoutBounds();
            
            nodeGroup.getChildren().add(new Rectangle(textBounds.getWidth() + 4, textBounds.getHeight() + 4, Color.BLACK));
            nodeGroup.getChildren().add(text);
            
            return nodeGroup;
        }

    }
}
