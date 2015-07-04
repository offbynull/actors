package com.offbynull.peernetic.examples.chord;

// Created this to avoid mixing commands to run the example with log output to the console
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

final class ConsoleStage extends Stage {

    private static final ArrayBlockingQueue<ConsoleStage> SINGLETON_QUEUE = new ArrayBlockingQueue<>(1);

    private final Lock lock;
    private final TextArea outputTextArea;
    private final TextField inputTextField;
    private CommandProcessor commandProcessor;

    public ConsoleStage() {
        lock = new ReentrantLock();

        BorderPane borderPane = new BorderPane();
//        borderPane.setPadding(new Insets(10));

        outputTextArea = new TextArea();
        inputTextField = new TextField();
//        ScrollPane outputScrollPane = new ScrollPane(outputTextArea);

        inputTextField.setOnKeyPressed(x -> {
            if (x.getCode() != KeyCode.ENTER) {
                return;
            }
            
            String input = inputTextField.getText();
            inputTextField.setText("");

            lock.lock();
            try {
                if (commandProcessor != null) {
                    String output = commandProcessor.commandEntered(input);
                    outputTextArea.appendText(output);
                    outputTextArea.appendText("\n");
                } else {
                    outputTextArea.appendText("No command processor\n");
                }
            } catch (Exception e) {
                outputTextArea.appendText(e.toString());
                outputTextArea.appendText("\n");
            } finally {
                lock.unlock();
            }
        });

        outputTextArea.setEditable(false);
        outputTextArea.setWrapText(false);

        borderPane.setCenter(outputTextArea);
        borderPane.setBottom(inputTextField);

        setTitle("Controller");
        setWidth(300);
        setHeight(400);

        setOnCloseRequest(x -> hide());

        Scene scene = new Scene(borderPane, 1.0, 1.0, true, SceneAntialiasing.DISABLED);
        scene.setFill(Color.WHITESMOKE);
        setScene(scene);

        SINGLETON_QUEUE.add(this);
    }

    public void outputLine(String text) {
        Platform.runLater(() -> {
            outputTextArea.appendText(text);
            outputTextArea.appendText("\n");
        });
    }

    public void setCommandProcessor(CommandProcessor commandProcessor) {
        lock.lock();
        try {
            this.commandProcessor = commandProcessor;
        } finally {
            lock.unlock();
        }
    }

    public static ConsoleStage getInstance() throws InterruptedException {
        return SINGLETON_QUEUE.take();
    }

    interface CommandProcessor {

        String commandEntered(String input);
    }
}
