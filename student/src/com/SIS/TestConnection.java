package com.SIS;

import java.sql.Connection;

public class TestConnection {
    public static void main(String[] args) {
        Connection conn = DBConnection.getConnection();
        if (conn != null) {
            System.out.println("Connection to database successful!");
        } else {
            System.out.println("Failed to connect to database.");
        }
    }
} 
