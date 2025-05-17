package com.review.view;

import com.review.ExecutionPipeline;
import com.review.User;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.TextField;

public class CofreApp extends Application {

    private Stage primaryStage;
    private User user = new User();
    private ExecutionPipeline pipeline = ExecutionPipeline.getInstance();
    private boolean isFirstAccess = pipeline.isFirstAccess();

    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Cria a cena inicial com um user aleatorio
        user.fetchDefault();
        if (!isFirstAccess) {
            showCadastroPage();
        } else {
            showLoginPage();
        }

        primaryStage.setTitle("Cofre digital");
        primaryStage.show();
    }

    private void showHomePage(User user) {
        Label label = new Label("Menu principal");
        VBox header = new HeaderComponent(user);
        Button toCadastro = new Button("Cadastrar novo usuário");
        Button toConsulta = new Button("Consultar arquivos");
        Button toLogout = new Button("Logout");

        Label userAcssesCount = new Label("Total de acessos do usuário: " + Integer.toString(user.acessosTotais));
        toCadastro.setOnAction(e -> showCadastroPage());

        // modificar para rodar uma função de reset antes
        toLogout.setOnAction(e -> primaryStage.close());

        VBox layout = new VBox(10, label, header, userAcssesCount, toCadastro, toConsulta, toLogout);
        layout.setAlignment(javafx.geometry.Pos.CENTER);

        Scene scene = new Scene(layout, 400, 300);
        primaryStage.setScene(scene);
    }

    private void showCadastroPage() {
        Label titleLabel = new Label("Cadastro");
        // Add more components as needed
        VBox header = new HeaderComponent(user);
        HBox campoCaminhoCertificado = new HBox(10, new Label("Caminho do certificado: "), new TextField());
        HBox campoCaminhoChavePrivada = new HBox(10, new Label("Caminho da chave privada: "), new TextField());
        HBox campoFraseSecreta = new HBox(10, new Label("Frase secreta: "), new TextField());
        HBox campoGrupo = new HBox(10, new Label("Grupo: "), new TextField());
        HBox campoSenha = new HBox(10, new Label("Senha: "), new TextField());
        HBox campoConfirmacaoSenha = new HBox(10, new Label("Confirmação Senha: "), new TextField());

        Button cadastrarButton = new Button("Cadastrar");
        cadastrarButton.setOnAction(e -> {
            String caminhoCertificado = ((TextField) campoCaminhoCertificado.getChildren().get(1)).getText();
            String caminhoChavePrivada = ((TextField) campoCaminhoChavePrivada.getChildren().get(1)).getText();
            String fraseSecreta = ((TextField) campoFraseSecreta.getChildren().get(1)).getText();
            String grupo = ((TextField) campoGrupo.getChildren().get(1)).getText();
            String senha = ((TextField) campoSenha.getChildren().get(1)).getText();
            String confirmacaoSenha = ((TextField) campoConfirmacaoSenha.getChildren().get(1)).getText();

        });
        HBox bottonButtons = new HBox(10, cadastrarButton);

        if (!isFirstAccess) {
            Button backButton = new Button("Voltar");
            backButton.setOnAction(e -> showHomePage(user));
            bottonButtons.getChildren().add(backButton);
        }

        /* Cria o objeto user */

        Label userAcssesCount = new Label("Total de acessos do usuário: " + Integer.toString(user.acessosTotais));
        Label formulario = new Label("Formulário de cadastro:");

        VBox layout = new VBox(20, titleLabel, header, userAcssesCount, formulario, campoCaminhoCertificado,
                campoCaminhoChavePrivada, campoFraseSecreta, campoGrupo, campoSenha, campoConfirmacaoSenha,
                bottonButtons);
        layout.setAlignment(javafx.geometry.Pos.CENTER);

        Scene scene = new Scene(layout, 500, 500);
        primaryStage.setScene(scene);
    }

    private void showLoginPage() {
        Label titleLabel = new Label("Login");

        HBox campoLogin = new HBox(10, new Label("Login: "), new TextField());
        HBox campoFraseSecreta = new HBox(10, new Label("Frase secreta: "), new TextField());
        HBox campoSenha = new HBox(10, new Label("Senha: "), new TextField());
        HBox campoConfirmacaoSenha = new HBox(10, new Label("Confirmação Senha: "), new TextField());

        VBox layout = new VBox(20,titleLabel, campoLogin, campoFraseSecreta, campoSenha, campoConfirmacaoSenha);
        layout.setAlignment(javafx.geometry.Pos.CENTER);

        Scene scene = new Scene(layout, 350, 350);
        primaryStage.setScene(scene);
    }
}
