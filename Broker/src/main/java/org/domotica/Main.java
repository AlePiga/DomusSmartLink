package org.domotica;

public class Main {
    public static void main(String[] args) {
        Server server = new Server();
        Database.init();
        Database.save("LivelloAcqua",0,53,"NUMBER");
    }
}
