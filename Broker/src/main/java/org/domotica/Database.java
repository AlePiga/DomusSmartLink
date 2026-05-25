package org.domotica;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class Database {
    /// TEST
    static Connection conn;

    public static void init() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:sensori.db");

            Statement stmt = conn.createStatement();

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS sensori (
                    id INTEGER PRIMARY KEY,
                    nome TEXT,
                    status INTEGER,
                    type TEXT
                )
            """);

            System.out.println("[DB] Database pronto!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save(String nome, int status, int id, String type) {
        try {
            var ps = conn.prepareStatement("""
            INSERT INTO sensori (id, nome, status, type)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                nome = excluded.nome,
                status = excluded.status
        """);

            ps.setInt(1, id);
            ps.setString(2, nome);
            ps.setInt(3, status);
            ps.setString(4, type);

            ps.executeUpdate();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void update(int id, int status) {
        try {
            var ps = conn.prepareStatement("""
            UPDATE sensori
            SET status = ?
            WHERE id = ?
        """);

            ps.setInt(1, status);
            ps.setInt(2, id);

            int rows = ps.executeUpdate();

            System.out.println("Righe aggiornate: " + rows);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static Sensore select(String nome) {
        try {
            var ps = conn.prepareStatement("""
            SELECT *
            FROM sensori
            WHERE id = ?
        """);

            ps.setString(1, nome);

            var rs = ps.executeQuery();

            if (rs.next()) {

                Integer id = rs.getObject("id") != null ? rs.getInt("id") : null;

                return new Sensore(rs.getString("nome"), rs.getInt("status"),rs.getInt("id"),rs.getString("type"));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
