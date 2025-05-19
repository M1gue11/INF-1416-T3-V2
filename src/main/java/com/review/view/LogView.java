package com.review.view;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.List;

import com.review.DatabaseManager;
import com.review.view.CofreApp;

public class LogView extends Application {
    private TextArea logTextArea;

    @Override
    public void start(Stage primaryStage) {
        logTextArea = new TextArea();
        logTextArea.setEditable(false);
        logTextArea.setWrapText(true);
        logTextArea.setStyle("-fx-font-family: monospace;");

        // Configuração para expandir o TextArea
        VBox.setVgrow(logTextArea, Priority.ALWAYS);
        HBox.setHgrow(logTextArea, Priority.ALWAYS);

        Button clearButton = new Button("Limpar");
        clearButton.setOnAction(e -> logTextArea.clear());

        Button closeButton = new Button("close");
        closeButton.setOnAction(e -> {
            primaryStage.close();
        });

        Button updateButton = new Button("Atualizar");
        updateButton.setOnAction(e -> {
            List<String> logs = DatabaseManager.getMessagesAndTimeByActualTime(System.currentTimeMillis());
            String logsConcatenados = String.join("\n", logs);

            // Adiciona apenas se houver logs
            if (!logs.isEmpty()) {
                String separator = logTextArea.getText().isEmpty() ? "" : "\n";
                logTextArea.setText(separator + logsConcatenados);
            }

            System.out.println(System.currentTimeMillis());
        });

        HBox buttonsContainer = new HBox(70, clearButton, updateButton,closeButton);
        VBox layout = new VBox(10, new Label("Logs do Sistema"), logTextArea, buttonsContainer);

        Scene scene = new Scene(layout, 600, 400);
        primaryStage.setTitle("Visualizador de Logs");
        primaryStage.setScene(scene);
        primaryStage.show();
    }
}