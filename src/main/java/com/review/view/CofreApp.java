package com.review.view;

import com.review.User;
import com.review.view.HeaderComponent;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class CofreApp extends  Application{

    private Stage primaryStage;
    private User user = new User();

    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Cria a cena inicial com um user aleatorio
        user.fetchDefault();
        showHomePage(user);

        primaryStage.setTitle("Cofre digital");
        primaryStage.show();
    }

    private void showHomePage(User user) {
        Label label = new Label("Menu principal");
        VBox header = new HeaderComponent(user);
        Button toCadastro = new Button("Cadastrar novo usuário");
        Button toConsulta = new Button("Consultar arquivos");
        Button toLogout = new Button("Logout");

        toCadastro.setOnAction(e -> showCadastroPage());

        //modificar para rodar uma função de reset antes
        toLogout.setOnAction(e -> primaryStage.close());

        VBox layout = new VBox(10, label, header, toCadastro, toConsulta,toLogout);
        layout.setAlignment(javafx.geometry.Pos.CENTER);

        Scene scene = new Scene(layout, 300, 200);
        primaryStage.setScene(scene);
    }

    private void showCadastroPage() {
        Label titleLabel = new Label("Cadastro");
        // Add more components as needed

        Button backButton = new Button("Voltar");
        backButton.setOnAction(e -> showHomePage(user));

        VBox layout = new VBox(20, titleLabel, backButton);
        layout.setAlignment(javafx.geometry.Pos.CENTER);

        Scene scene = new Scene(layout, 400, 300);
        primaryStage.setScene(scene);
    }
}
