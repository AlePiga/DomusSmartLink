package org.domotica;

public class Sensore {
    String type;
    String  nome;
    int status;
    Integer id;
    public Sensore(String nome,int  status,int Integer,String type) {
        this.nome = nome;
        this.status = status;
        this.id = Integer;
        this.type = type;
    }
}
