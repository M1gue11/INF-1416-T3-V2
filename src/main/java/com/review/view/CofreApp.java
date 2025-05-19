package com.review.view;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.review.ArvoreSenha;
import com.review.DatabaseManager;
import com.review.ExecutionPipeline;
import com.review.KeyManager;
import com.review.RetornoCadastro;
import com.review.TOTP;
import com.review.User;
import com.review.Files.Arquivo;
import com.review.Files.ArquivoModel;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.image.WritableImage;

import java.awt.image.BufferedImage;
import javafx.scene.image.ImageView;
import javafx.embed.swing.SwingFXUtils;

import java.util.List;
import java.util.Optional;

import static com.review.DatabaseManager.insereLog;

public class CofreApp extends Application {

    private Stage primaryStage;
    private ExecutionPipeline pipeline = ExecutionPipeline.getInstance();
    private boolean isFirstAccess = pipeline.isFirstAccess();
    private static final boolean bypassLogin = false;
    private int toptCount = 0;

    public void start(Stage primaryStage) {
        this.primaryStage = primaryStage;

        if (isFirstAccess) {
            insereLog(1005, Optional.empty(), Optional.empty());
            showCadastroPage();
        } else {
            insereLog(1006, Optional.empty(), Optional.empty());
            if (bypassLogin) {
                pipeline.bypassLogin();
                showHomePage();
            } else {
                showPassphrasePage();
            }
        }

        primaryStage.setTitle("Cofre digital");
        primaryStage.show();
    }

    private void showTOTPPage() {
        insereLog(3002, null, Optional.of(pipeline.user));
        insereLog(4001, null, Optional.of(pipeline.user));
        Label titleLabel = new Label("Verificação de dois fatores");
        HBox campoCodigoTotp = new HBox(10, new Label("Codigo de autenticação: "), new TextField());

        Button confirmarButton = new Button("Confirmar código");
        confirmarButton.setOnAction(e -> {
            String codigoTotp = ((TextField) campoCodigoTotp.getChildren().get(1)).getText();
            boolean isOk = pipeline.isValidTOTP(codigoTotp);
            pipeline.isLogado = true;
            if (isOk) {
                insereLog(4003, null, Optional.of(pipeline.user));
                insereLog(4002, null, Optional.of(pipeline.user));
                this.toptCount = 0;
                showHomePage();
            } else {
                // TODO: falta fazer a logica de bloqueio
                this.toptCount++;
                insereLog(4003 + this.toptCount, null, Optional.of(pipeline.user));
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Erro");
                a.setHeaderText("Código inválido.");
                a.showAndWait();
            }

        });

        Button voltarButton = new Button("Voltar");
        voltarButton.setOnAction(e -> {
            showLoginPage();
        });

        Button sairButton = new Button("Sair");
        sairButton.setOnAction(e -> {
            System.exit(0);
        });

        HBox buttonContainer = new HBox(10, voltarButton, sairButton);
        buttonContainer.setAlignment(javafx.geometry.Pos.CENTER);

        VBox layout = new VBox(20, titleLabel, campoCodigoTotp, confirmarButton, buttonContainer);
        layout.setAlignment(javafx.geometry.Pos.CENTER);

        Scene scene = new Scene(layout, 400, 300);
        primaryStage.setScene(scene);
    }

    private void showHomePage() {
        insereLog(5001, null, Optional.of(pipeline.user));
        Label label = new Label("Menu principal");
        VBox header = new HeaderComponent(pipeline.user);
        Button toCadastro = new Button("Cadastrar novo usuário");
        // desativa se nao for admin
        toCadastro.setDisable(!pipeline.user.isAdmin());
        Button toConsulta = new Button("Consultar arquivos");
        Button toLogout = new Button("Logout");
        Label userAcssesCount = new Label(
                "Total de acessos do usuário: " + Integer.toString(pipeline.user.numero_acessos));
        Button openLogs = new Button("Visualizar Logs");

        toCadastro.setOnAction(e -> {
            insereLog(5002, Optional.empty(), Optional.of(pipeline.user));
            showCadastroPage();
        });

        toLogout.setOnAction(e -> {
            insereLog(5004, Optional.empty(), Optional.of(pipeline.user));
            insereLog(8003, Optional.empty(), Optional.of(pipeline.user));
            pipeline.logout();
            showPassphrasePage();
        });
        toConsulta.setOnAction(e -> {
            insereLog(5003, Optional.empty(), Optional.of(pipeline.user));
            showFilesPage();
        });
        openLogs.setOnAction(e -> {
            LogView logView = new LogView();
            logView.start(new Stage());
        });

        VBox layout = new VBox(10, header, userAcssesCount, label, toCadastro, toConsulta, openLogs, toLogout);
        layout.setAlignment(javafx.geometry.Pos.CENTER);

        Scene scene = new Scene(layout, 400, 300);
        primaryStage.setScene(scene);
    }

    private void showFilesPage() {
        Label label = new Label("Arquivos Secretos");
        VBox header = new HeaderComponent(pipeline.user);

        Label totalConsultas = new Label("Total de consultas do usuário: 0");

        HBox campoCaminhoPasta = new HBox(10, new Label("Caminho da pasta: "), new TextField());
        HBox campoFraseSecreta = new HBox(10, new Label("Frase secreta: "), new TextField());

        Button botaoListar = new Button("Listar");
        Button botaoVoltar = new Button("Voltar");
        botaoVoltar.setOnAction(e -> showHomePage());

        TableView<ArquivoModel> table = new TableView<>();
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY); // Faz as colunas preencherem a largura

        // Colunas da tabela
        TableColumn<ArquivoModel, String> nomeColumn = new TableColumn<>("NOME_SECRETO_DO_ARQUIVO");
        nomeColumn.setCellValueFactory(cellData -> cellData.getValue().nomeSecretoArquivo);

        TableColumn<ArquivoModel, String> donoColumn = new TableColumn<>("DONO_ARQUIVO");
        donoColumn.setCellValueFactory(cellData -> cellData.getValue().donoArquivo);

        TableColumn<ArquivoModel, String> grupoColumn = new TableColumn<>("GRUPO_ARQUIVO");
        grupoColumn.setCellValueFactory(cellData -> cellData.getValue().grupoArquivo);

        table.getColumns().addAll(nomeColumn, donoColumn, grupoColumn);

        ObservableList<ArquivoModel> files = FXCollections.observableArrayList();
        botaoListar.setOnAction(e -> {
            String caminhoPasta = ((TextField) campoCaminhoPasta.getChildren().get(1)).getText();
            String fraseSecreta = ((TextField) campoFraseSecreta.getChildren().get(1)).getText();
            List<Arquivo> arquivos = pipeline.listUserGroupFiles(caminhoPasta, fraseSecreta);
            if (arquivos == null) {
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Erro");
                a.setHeaderText("Erro ao listar arquivos.");
                a.showAndWait();
                return;
            }

            files.clear();
            files.addAll(arquivos.stream()
                    .map(arquivo -> ArquivoModel.from(arquivo)).toList());
            table.setItems(files);
        });

        // Evento de seleção
        table.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                System.out.println("Arquivo selecionado: " + newSelection.nomeSecretoArquivo);
                // Adicione aqui a lógica para quando um arquivo for selecionado
            }
        });

        VBox layout = new VBox(10, header, totalConsultas, label, campoCaminhoPasta,
                campoFraseSecreta, botaoListar, table, botaoVoltar);
        layout.setAlignment(Pos.CENTER);
        VBox.setVgrow(table, Priority.ALWAYS); // Faz a tabela expandir verticalmente

        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
    }

    private BufferedImage generateQRCodeImage(String text) throws Exception {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, 200, 200);

        BufferedImage qrCodeImage = new BufferedImage(200, 200, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 200; x++) {
            for (int y = 0; y < 200; y++) {
                qrCodeImage.setRGB(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF); // Preto e branco
            }
        }
        return qrCodeImage;
    }

    private void showCadastroPage() {
        Label titleLabel = new Label("Cadastro");
        // Add more components as needed
        if(!isFirstAccess && pipeline.isLogado) {
            insereLog(6001, Optional.empty(), Optional.of(pipeline.user));
        }
        VBox header = new HeaderComponent(pipeline.user);
        HBox campoCaminhoCertificado = new HBox(10, new Label("Caminho do certificado: "), new TextField());
        HBox campoCaminhoChavePrivada = new HBox(10, new Label("Caminho da chave privada: "), new TextField());
        HBox campoFraseSecreta = new HBox(10, new Label("Frase secreta: "), new TextField());
        HBox campoGrupo = new HBox(10, new Label("Grupo: "), new TextField());
        HBox campoSenha = new HBox(10, new Label("Senha: "), new TextField());
        HBox campoConfirmacaoSenha = new HBox(10, new Label("Confirmação Senha: "), new TextField());

        Button cadastrarButton = new Button("Cadastrar");
        cadastrarButton.setOnAction(e -> {
            if(!isFirstAccess && pipeline.isLogado){
                insereLog(6002, Optional.empty(), Optional.of(pipeline.user));
            }
            String caminhoCertificado = ((TextField) campoCaminhoCertificado.getChildren().get(1)).getText();
            String caminhoChavePrivada = ((TextField) campoCaminhoChavePrivada.getChildren().get(1)).getText();
            String fraseSecreta = ((TextField) campoFraseSecreta.getChildren().get(1)).getText();
            String grupo = ((TextField) campoGrupo.getChildren().get(1)).getText();
            String senha = ((TextField) campoSenha.getChildren().get(1)).getText();
            String confirmacaoSenha = ((TextField) campoConfirmacaoSenha.getChildren().get(1)).getText();
            RetornoCadastro retCadastro = pipeline.cadastro(caminhoCertificado, caminhoChavePrivada,
                    fraseSecreta, grupo, senha,
                    confirmacaoSenha);

            Alert a;
            if (retCadastro.uid == -1) {
                a = new Alert(Alert.AlertType.ERROR);
            } else {
                a = new Alert(Alert.AlertType.CONFIRMATION);
            }
            a.setTitle("Cadastro");
            a.setHeaderText(retCadastro.uid == -1 ? "Erro ao cadastrar usuário" : "Usuário cadastrado com sucesso");
            a.setOnCloseRequest(exit -> {
                if (isFirstAccess) {
                    System.exit(0);
                }
            });
            a.showAndWait();
            if (retCadastro.uid == -1) {
                // TODO: interrompe o fluxo
                return;
            }

            String urlChave = TOTP.getGoogleAuthUrl(retCadastro.email, retCadastro.chaveB32);
            Alert qrCode = new Alert(Alert.AlertType.INFORMATION);
            try {
                BufferedImage qrCodeImage = generateQRCodeImage(urlChave);
                WritableImage qrCodeWritableImage = SwingFXUtils.toFXImage(qrCodeImage, null);
                ImageView qrCodeView = new ImageView(qrCodeWritableImage);

                StackPane layout = new StackPane(qrCodeView);
                Scene scene = new Scene(layout, 200, 200);

                Stage qrCodeStage = new Stage();
                qrCodeStage.setTitle("Chave B32 QR Code");
                qrCodeStage.setScene(scene);
                qrCodeStage.show();

                qrCode.setOnCloseRequest(exit -> {
                    if (isFirstAccess) {
                        System.exit(0);
                    }
                });
            } catch (Exception ex) {
                // TODO: log e rever esse comportamento
                ex.printStackTrace();
                qrCode.setContentText("Erro ao gerar QR Code.");
                qrCode.setOnCloseRequest(exit -> System.exit(0));
            }
            showHomePage();
        });
        HBox bottonButtons = new HBox(10, cadastrarButton);

        if (!isFirstAccess && pipeline.isLogado) {
            Button backButton = new Button("Voltar");
            backButton.setOnAction(e -> showHomePage());
            bottonButtons.getChildren().add(backButton);
        }

        Label userAcssesCount = new Label(
                "Total de acessos do usuário: " + Integer.toString(pipeline.user.numero_acessos));
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
                // pipeline.setPassphrase(fraseSecreta);
                insereLog(1006, Optional.empty(), Optional.empty());
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

    private int passwordLoginCount = 0; // Moved count to an instance variable

    private void showLoginPage() {
        List<Integer> botoesPressionados = new java.util.ArrayList<>();
        List<Integer> valores = new java.util.ArrayList<>();
        ArvoreSenha arvore = new ArvoreSenha();

        Label titleLabel = new Label("Login");

        HBox campoLogin = new HBox(10, new Label("Login: "), new TextField());
        Label senhaLabel = new Label("Senha (selecione usando os botões): ");

        TextField senhaDisplay = new TextField();
        senhaDisplay.setEditable(false);

        for (int i = 0; i < 10; i++) {
            valores.add(i);
        }
        java.util.Collections.shuffle(valores);

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
            insereLog(2001, null, null);
            loginButton.setDisable(true);
            String login = ((TextField) campoLogin.getChildren().get(1)).getText();
            List<String> senhasPossiveis = arvore.gerarSequenciasNumericas();
            User user = DatabaseManager.getUserByEmail(login);
            if (user == null) {
                user = new User();
                user.email = login;
                insereLog(2005, null, Optional.of(user));
                user = null;
                Alert a = new Alert(Alert.AlertType.ERROR);
                a.setTitle("Erro");
                a.setHeaderText("Login ou senha inválidos.");
                a.showAndWait();
                loginButton.setDisable(false);
                return;
                //TODO: como logar o bloqueado
            }
            insereLog(2003, null, Optional.of(user));
            insereLog(2002, null, null);
            insereLog(3001, null, Optional.of(user));
            for (String senha : senhasPossiveis) {
                if (KeyManager.validarSenha(senha, user.senha_pessoal_hash)) {
                    pipeline.setPassword(senha);
                    pipeline.user = user;
                    this.passwordLoginCount = 0;
                    showTOTPPage();
                    break;
                }
            }
            this.passwordLoginCount++;
            insereLog(3003 +this.passwordLoginCount, null, Optional.of(user));
            if(this.passwordLoginCount >=3){
                //ToDo: bloquear o usuario
                insereLog(3007, null, Optional.of(user));
            }
            insereLog(3002, null, Optional.of(user));
            loginButton.setDisable(false);
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

        VBox layout = new VBox(20, titleLabel, campoLogin, campoSenha,
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
