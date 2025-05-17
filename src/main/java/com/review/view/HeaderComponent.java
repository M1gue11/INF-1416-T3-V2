package com.review.view;

import com.review.User;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class HeaderComponent extends VBox {
    private Label loginLabel;
    private Label groupLabel;
    private Label nameLabel;

    public HeaderComponent(User user) {
        // Configurar o alinhamento do VBox principal
        this.setAlignment(Pos.CENTER);
        this.setSpacing(1); // Espaçamento entre os labels

        // Criando os labels
        loginLabel = new Label("Login: " + user.login);
        groupLabel = new Label("Grupo: " + user.grupo);
        nameLabel = new Label("Nome: " + user.nome);

        // Adicionar os labels verticalmente ao VBox
        this.getChildren().addAll(loginLabel, groupLabel, nameLabel);
    }

    // Método para atualizar os dados do usuário
    public void updateUser(User user) {
        loginLabel.setText("Login: " + user.login);
        groupLabel.setText("Grupo: " + user.grupo);
        nameLabel.setText("Nome: " + user.nome);
    }
}