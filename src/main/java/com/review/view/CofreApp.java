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

        Label senhaLabel = new Label("Senha (selecione usando os botões): ");

        TextField senhaDisplay = new TextField();
        senhaDisplay.setEditable(false);

        List<Integer> botoesPressionados = new java.util.ArrayList<>();

        List<Integer> valores = new java.util.ArrayList<>();
        for (int i = 0; i < 10; i++) {
            valores.add(i);
        }
        java.util.Collections.shuffle(valores);

        java.util.Map<Integer, int[]> valoresPorBotao = new java.util.HashMap<>();

        HBox botoesContainer = new HBox(10);
        List<Button> botoesSenha = new java.util.ArrayList<>();

        for (int i = 0; i < 5; i++) {
            final int indiceButton = i;
            final int valor1 = valores.get(i * 2);
            final int valor2 = valores.get(i * 2 + 1);

            valoresPorBotao.put(indiceButton, new int[]{valor1, valor2});

            Button botao = new Button(valor1 + "-" + valor2);

            botao.setOnAction(e -> {
                botoesPressionados.add(indiceButton);

                senhaDisplay.setText(senhaDisplay.getText() + "*");
            });

            botoesSenha.add(botao);
            botoesContainer.getChildren().add(botao);
        }

        Button limparSenha = new Button("Limpar");
        limparSenha.setOnAction(e -> {
            botoesPressionados.clear();
            senhaDisplay.setText("");
        });
        botoesContainer.getChildren().add(limparSenha);

        Button loginButton = new Button("Entrar");
        loginButton.setOnAction(e -> {
            String login = ((TextField) campoLogin.getChildren().get(1)).getText();
            String fraseSecreta = ((TextField) campoFraseSecreta.getChildren().get(1)).getText();

            // TODO Verificar se a senha é válida - teste atual
            String senhaReal = "1234";

            if (verificarSenha(senhaReal, botoesPressionados, valoresPorBotao)) {
                System.out.println("Login bem-sucedido!");
            } else {
                System.out.println("Senha inválida!");
            }

        });

        HBox campoSenha = new HBox(10, senhaLabel, senhaDisplay);

        VBox layout = new VBox(20, titleLabel, campoLogin, campoFraseSecreta, campoSenha, botoesContainer, loginButton);
        layout.setAlignment(javafx.geometry.Pos.CENTER);

        Scene scene = new Scene(layout, 500, 400);
        primaryStage.setScene(scene);
    }


    private boolean verificarRecursivo(String senhaReal, int posicao, List<Integer> botoesPressionados,
                                       java.util.Map<Integer, int[]> valoresPorBotao, String senhaAtual) {
        if (posicao == botoesPressionados.size()) {
            return senhaAtual.equals(senhaReal);
        }

        int botao = botoesPressionados.get(posicao);

        int[] valores = valoresPorBotao.get(botao);

        // Tentar com o primeiro valor
        if (verificarRecursivo(senhaReal, posicao + 1, botoesPressionados, valoresPorBotao,
                senhaAtual + valores[0])) {
            return true;
        }

        // Tentar com o segundo valor
        return verificarRecursivo(senhaReal, posicao + 1, botoesPressionados, valoresPorBotao,
                senhaAtual + valores[1]);
    }

    private boolean verificarSenha(String senhaReal, List<Integer> botoesPressionados, java.util.Map<Integer, int[]> valoresPorBotao) {
        // Verificar se a quantidade de botões pressionados corresponde ao tamanho da senha
        if (botoesPressionados.size() != senhaReal.length()) {
            return false;
        }

        // Lista de todas as possíveis combinações de senha
        return verificarRecursivo(senhaReal, 0, botoesPressionados, valoresPorBotao, "");
    }
}
