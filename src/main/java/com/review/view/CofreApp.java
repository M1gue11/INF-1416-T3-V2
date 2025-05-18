package com.review.view;

import com.review.*;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.control.TextField;
import javafx.scene.control.Alert;

import java.util.List;

public class CofreApp extends Application {

    private Stage primaryStage;
    private User user = new User();
    private ExecutionPipeline pipeline = ExecutionPipeline.getInstance();
    private boolean isFirstAccess = pipeline.isFirstAccess();

    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        // Cria a cena inicial com um user aleatorio
        user.fetchDefault();
        if (isFirstAccess) {
            showCadastroPage();
        } else {
            showPassphrasePage();
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

        Label userAcssesCount = new Label("Total de acessos do usuário: " + Integer.toString(user.numero_acessos));
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
            int uid = pipeline.cadastro(caminhoCertificado, caminhoChavePrivada, fraseSecreta, grupo, senha,
                    confirmacaoSenha);
            String alertMessage;
            if (uid == -1) {
                // TODO: log
                alertMessage = "Erro ao cadastrar usuário";
            } else {
                // TODO: log
                alertMessage = "Usuário cadastrado com sucesso";
            }

            Alert a = new Alert(Alert.AlertType.INFORMATION);
            a.setTitle("Cadastro");
            a.setHeaderText(alertMessage);
            if (uid == -1) {
                a.setOnCloseRequest(exit -> exit.consume());
            } else {
                a.setOnCloseRequest(exit -> System.exit(0));
            }
            a.showAndWait();
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

    private void showPassphrasePage() {
        Label titleLabel = new Label("Frase secreta do administrador");

        HBox campoFraseSecreta = new HBox(10, new Label("Frase secreta: "), new TextField());

        Button continuarButton = new Button("Validar");
        continuarButton.setOnAction(e -> {
            String fraseSecreta = ((TextField) campoFraseSecreta.getChildren().get(1)).getText();
            boolean isOk = pipeline.admPassphraseValidation(fraseSecreta);
            if (isOk) {
                //pipeline.setPassphrase(fraseSecreta);
                showLoginPage();
            } else {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Cadastro");
                a.setHeaderText("Nao foi possivel validar a frase secreta.");
                a.setOnCloseRequest(event -> System.exit(0));
                a.showAndWait();
            }
        });

        Button fecharButton = new Button("Fechar");
        fecharButton.setOnAction(e -> {
            System.exit(0);
        });

        HBox botoes = new HBox(10, fecharButton, continuarButton);
        botoes.setAlignment(javafx.geometry.Pos.CENTER);

        VBox layout = new VBox(20, titleLabel, campoFraseSecreta, botoes);
        layout.setAlignment(javafx.geometry.Pos.CENTER);

        Scene scene = new Scene(layout, 350, 350);
        primaryStage.setScene(scene);
    }

    private void showLoginPage() {
        List<Integer> botoesPressionados = new java.util.ArrayList<>();
        List<Integer> valores = new java.util.ArrayList<>();
        ArvoreSenha arvore = new ArvoreSenha();

        Label titleLabel = new Label("Login");

        HBox campoLogin = new HBox(10, new Label("Login: "), new TextField());
        HBox campoFraseSecreta = new HBox(10, new Label("Frase secreta: "), new TextField());

        Label senhaLabel = new Label("Senha (selecione usando os botões): ");

        TextField senhaDisplay = new TextField();
        senhaDisplay.setEditable(false);


        for (int i = 0; i < 10; i++) {
            valores.add(i);
        }
        java.util.Collections.shuffle(valores);

        java.util.Map<Integer, int[]> valoresPorBotao = new java.util.HashMap<>();

        HBox botoesContainer = new HBox(10);
        List<Button> botoesSenha = new java.util.ArrayList<>();

        for (int i = 0; i < 5; i++) {
            final int indiceButton = i;
            Button botao = new Button();

            atualizarTextoButao(botao, valores, i);

            botao.setOnAction(e -> {
                botoesPressionados.add(indiceButton);
                final int valor1 = valores.get(indiceButton * 2);
                final int valor2 = valores.get(indiceButton * 2 + 1);

                arvore.inserirOpcao(indiceButton, valor1, valor2);

                senhaDisplay.setText(senhaDisplay.getText() + "*");

                // Agora, após pressionar o botão, embaralhamos novamente os valores
                java.util.Collections.shuffle(valores);

                // Atualizamos o texto de todos os botões com os novos valores
                for (int j = 0; j < botoesSenha.size(); j++) {
                    atualizarTextoButao(botoesSenha.get(j), valores, j);
                }
            });

            botoesSenha.add(botao);
            botoesContainer.getChildren().add(botao);
        }

        Button limparSenha = new Button("Limpar");
        limparSenha.setOnAction(e -> {
            botoesPressionados.clear();
            senhaDisplay.setText("");
            arvore.resetarArvore();

            java.util.Collections.shuffle(valores);
            for (int j = 0; j < botoesSenha.size(); j++) {
                atualizarTextoButao(botoesSenha.get(j), valores, j);
            }
        });
        botoesContainer.getChildren().add(limparSenha);

        Button loginButton = new Button("Entrar");
        loginButton.setOnAction(e -> {
            String login = ((TextField) campoLogin.getChildren().get(1)).getText();
            String fraseSecreta = ((TextField) campoFraseSecreta.getChildren().get(1)).getText();

            List<String> bago = arvore.gerarSequenciasNumericas();
            for (String b : bago) {
                if(KeyManager.validarSenha(b, DatabaseManager.getPasswordByLogin(login))){
                    // Todo definir funcionalidade correta
                    System.out.println(b);
                    System.out.println("Resultado encontrado!");
                    continue;
                }
            }

        });
        Button cadastroButton = new Button("Cadastrar");
        cadastroButton.setOnAction(e -> {
            showCadastroPage();
        });
        Button encerrarButton = new Button("Encerrar");
        encerrarButton.setOnAction(e -> {
            System.exit(0);
        });

        HBox campoSenha = new HBox(10, senhaLabel, senhaDisplay);
        HBox buttonContainer = new HBox(10, encerrarButton, cadastroButton, loginButton);
        buttonContainer.setAlignment(javafx.geometry.Pos.CENTER);

        VBox layout = new VBox(20, titleLabel, campoLogin, campoFraseSecreta, campoSenha,
                botoesContainer, buttonContainer);
        layout.setAlignment(javafx.geometry.Pos.CENTER);

        Scene scene = new Scene(layout, 350, 350);
        primaryStage.setScene(scene);
    }

    // Método auxiliar para atualizar o texto dos botões
    private void atualizarTextoButao(Button botao, List<Integer> valores, int indice) {
        final int valor1 = valores.get(indice * 2);
        final int valor2 = valores.get(indice * 2 + 1);
        botao.setText(valor1 + "-" + valor2);
    }
}
