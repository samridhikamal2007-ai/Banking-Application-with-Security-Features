package com.bank.util;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class AlertHelper {

    public enum AlertType {
        SUCCESS,
        ERROR,
        INFO,
        WARNING
    }

    public static void showNotification(Stage owner, String title, String message, AlertType type) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initOwner(owner);
        dialog.initStyle(StageStyle.TRANSPARENT);

        VBox container = new VBox(15);
        container.setPadding(new Insets(25));
        container.setAlignment(Pos.CENTER);
        
        // Base Styling Class based on AlertType
        container.getStyleClass().add("custom-alert-box");
        switch (type) {
            case SUCCESS -> container.getStyleClass().add("alert-success");
            case ERROR -> container.getStyleClass().add("alert-error");
            case WARNING -> container.getStyleClass().add("alert-warning");
            default -> container.getStyleClass().add("alert-info");
        }

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("alert-title");

        Label messageLabel = new Label(message);
        messageLabel.getStyleClass().add("alert-message");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(350);
        messageLabel.setAlignment(Pos.CENTER);

        Button closeBtn = new Button("Dismiss");
        closeBtn.getStyleClass().add("alert-button");
        closeBtn.setOnAction(e -> dialog.close());

        container.getChildren().addAll(titleLabel, messageLabel, closeBtn);

        Scene scene = new Scene(container);
        scene.setFill(Color.TRANSPARENT);
        
        // Add styling from stylesheet
        if (owner.getScene() != null && !owner.getScene().getStylesheets().isEmpty()) {
            scene.getStylesheets().addAll(owner.getScene().getStylesheets());
        }

        dialog.setScene(scene);
        dialog.showAndWait();
    }
}
