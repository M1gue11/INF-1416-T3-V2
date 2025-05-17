package com.review;

public class User {
    public String login;
    public String password;
    public String nome;
    public String grupo;
    public boolean bloqueado;
    public Integer acessosTotais;
    public String token;
    public String caminhoCertificado;

    public void fetchData() {

    }

    public void fetchDefault(){
        login = "none";
        password = "none";
        nome = "none";
        grupo = "none";
        bloqueado = false;
        acessosTotais = 0;
        token = "none";
        caminhoCertificado = "none/none";
    }

}
