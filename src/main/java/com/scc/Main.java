package com.scc;

import com.scc.dao.DatabaseInitializer;
import com.scc.dao.ScholarshipDao;
import com.scc.dao.UserDao;
import com.scc.ui.Authentication;

import java.sql.SQLException;
import java.util.Scanner;

public final class Main {

    private Main() {}

    public static void main(String[] args) {
        try {
            // 1. Initialize database
            DatabaseInitializer.initialize();
            System.out.println(
                    "[Database] SQLite tables initialized successfully."
            );

            // 2. Create shared DAO and Scanner instances
            Scanner scanner = new Scanner(System.in);
            UserDao userDao = new UserDao();
            ScholarshipDao scholarshipDao = new ScholarshipDao();

            // 3. Delegate to Authentication
            Authentication authentication =
                    new Authentication(userDao, scholarshipDao, scanner);

            authentication.start();

        } catch (SQLException e) {
            System.err.println(
                    "[Database] Startup failed: " + e.getMessage()
            );
        }
    }
}