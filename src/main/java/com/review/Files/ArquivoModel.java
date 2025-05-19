package com.review.Files;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class ArquivoModel {
    public StringProperty nomeCodigoArquivo;
    public StringProperty nomeSecretoArquivo;
    public StringProperty donoArquivo;
    public StringProperty grupoArquivo;

    public ArquivoModel(String nomeCodigoArquivo, String nomeSecretoArquivo, String grupoArquivo, String donoArquivo) {
        this.nomeCodigoArquivo = new SimpleStringProperty(nomeCodigoArquivo);
        this.nomeSecretoArquivo = new SimpleStringProperty(nomeSecretoArquivo);
        this.grupoArquivo = new SimpleStringProperty(grupoArquivo);
        this.donoArquivo = new SimpleStringProperty(donoArquivo);
    }

    public static ArquivoModel from(Arquivo arquivo) {
        return new ArquivoModel(arquivo.nomeCodigoArquivo, arquivo.nomeSecretoArquivo, arquivo.grupoArquivo,
                arquivo.donoArquivo);
    }

}
