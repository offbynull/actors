package com.offbynull.peernetic.visualization.graph;

import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage stage) {
        
        final Group root = new Group();
        stage.setTitle("Example");
        stage.setWidth(500);
        stage.setHeight(500);

        Scene scene = new Scene(root, 500, 500, true, SceneAntialiasing.BALANCED);
        scene.setFill(Color.WHITESMOKE);

        Graph graph = new Graph();
        graph.addNode("111111", 200, 200);
        graph.addNode("222222", 50, 50);
        graph.addNode("333333", 100, 50);
        graph.addNode("444444", 50, 100);
        
        graph.addEdge("111111", "222222");
        graph.addEdge("222222", "333333");
        graph.addEdge("333333", "444444");
        graph.addEdge("444444", "111111");
        
        graph.removeNode("222222");
        
        graph.moveNode("111111", 300, 300);
        graph.styleNode("111111", "-fx-background-color: #000000; -fx-text-fill: #FFFFFF;");
        
        root.getChildren().add(graph);


        stage.setScene(scene);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
