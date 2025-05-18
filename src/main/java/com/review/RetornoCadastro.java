package com.review;

public class RetornoCadastro {
    public int uid;
    public String chaveB32;
    public String email;

    public RetornoCadastro(int uid, String chaveB32, String email) {
        this.uid = uid;
        this.chaveB32 = chaveB32;
        this.email = email;
    }
}