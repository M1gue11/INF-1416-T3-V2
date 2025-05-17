package com.review;

public class User {
    public String login;
    public String password;
    public String nome;
    public String grupo;
    public boolean bloqueado;

    public void fetchData() {

    }

    /*
    * This function exist just for tests purpose*/
    public void fetchDefault(){
        login = "admin";
        password = "PASSWORD";
        nome = "Administrador";
        grupo = "Administrador";
        bloqueado = false;
    }

}
