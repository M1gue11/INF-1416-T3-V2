package com.review.view;

import com.review.User;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

public class HeaderComponent extends VBox {
    private Label loginLabel;
    private Label groupLabel;
    private Label nameLabel;

    public HeaderComponent(User user) {

        // Criando os labels
        loginLabel = new Label("Login: " + user.login);
        groupLabel = new Label("Grupo: " + user.grupo);
        nameLabel = new Label("Nome: " + user.nome);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        this.getChildren().addAll(loginLabel, groupLabel, spacer, nameLabel);
    }

    // Método para atualizar os dados do usuário
    public void updateUser(User user) {
        loginLabel.setText("Login: " + user.login);
        groupLabel.setText("Grupo: " + user.grupo);
        nameLabel.setText("Nome: " + user.nome);
    }
}