package com.bank;

import com.bank.model.Account;
import com.bank.model.AuditLog;
import com.bank.model.Transaction;
import com.bank.model.User;
import com.bank.repository.AuditLogRepository;
import com.bank.service.AccountService;
import com.bank.service.ApiService;
import com.bank.service.AuthService;
import com.bank.service.OtpService;
import com.bank.service.TransactionService;
import com.bank.util.AlertHelper;
import com.bank.util.InputValidator;
import com.bank.util.SessionManager;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.util.List;

public class App extends Application {
    private Stage primaryStage;
    private final AuthService authService = new AuthService();
    private final AccountService accountService = new AccountService();
    private final TransactionService transactionService = new TransactionService();
    private final OtpService otpService = new OtpService();
    private final ApiService apiService = new ApiService();
    private final AuditLogRepository auditRepo = new AuditLogRepository();

    // Testing Banner Label for easy OTP viewing
    private static Label otpBannerLabel;
    private Label apiStatusLabel;

    public static void launchApp(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        stage.setTitle("TECHN GLOBAL BANK - Enterprise Banking Application");
        stage.setMinWidth(760);
        stage.setMinHeight(540);

        // Setup initial view
        showLogin();
    }

    private void applyStyle(Scene scene) {
        try {
            String cssPath = App.class.getResource("/css/styles.css").toExternalForm();
            scene.getStylesheets().add(cssPath);
        } catch (Exception e) {
            System.err.println("Could not load styles.css: " + e.getMessage());
        }
    }

    private void applyResponsiveSidebar(Scene scene, BorderPane layout, VBox sidebar) {
        Runnable updateLayout = () -> {
            if (scene.getWidth() < 900) {
                if (layout.getLeft() != null) {
                    layout.setLeft(null);
                    layout.setTop(sidebar);
                    sidebar.setPrefWidth(Double.MAX_VALUE);
                }
            } else {
                if (layout.getTop() != null) {
                    layout.setTop(null);
                    layout.setLeft(sidebar);
                    sidebar.setPrefWidth(220);
                }
            }
        };
        scene.widthProperty().addListener((obs, oldValue, newValue) -> updateLayout.run());
        updateLayout.run();
    }

    private FlowPane createResponsiveFlowPane(double hgap, double vgap) {
        FlowPane flow = new FlowPane(hgap, vgap);
        flow.setPrefWrapLength(900);
        flow.setMaxWidth(Double.MAX_VALUE);
        flow.setAlignment(Pos.TOP_LEFT);
        return flow;
    }

    private HBox createApiStatusBanner() {
        HBox banner = new HBox();
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(10, 0, 10, 0));
        banner.getStyleClass().add("api-status-banner");

        apiStatusLabel = new Label();
        apiStatusLabel.getStyleClass().add("api-status-text");
        banner.getChildren().add(apiStatusLabel);
        updateApiStatusBanner();
        return banner;
    }

    private void updateApiStatusBanner() {
        if (apiStatusLabel == null) {
            return;
        }
        if (!apiService.isRemoteEnabled()) {
            apiStatusLabel
                    .setText("Remote API disabled. Set BANK_API_BASE_URL or bank.api.base.url to enable integration.");
            apiStatusLabel.getStyleClass().removeAll("api-status-live", "api-status-warning", "api-status-error");
            apiStatusLabel.getStyleClass().add("api-status-warning");
            return;
        }

        apiStatusLabel.setText("Remote API: checking connectivity...");
        apiStatusLabel.getStyleClass().removeAll("api-status-live", "api-status-warning", "api-status-error");
        apiStatusLabel.getStyleClass().add("api-status-warning");

        new Thread(() -> {
            boolean reachable = apiService.isApiReachable();
            Platform.runLater(() -> {
                apiStatusLabel.setText(reachable ? "Remote API: connected" : "Remote API: unreachable");
                apiStatusLabel.getStyleClass().removeAll("api-status-live", "api-status-warning", "api-status-error");
                apiStatusLabel.getStyleClass().add(reachable ? "api-status-live" : "api-status-error");
            });
        }, "ApiStatusChecker").start();
    }

    // ==========================================
    // VIEW: LOGIN
    // ==========================================
    public void showLogin() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));

        VBox card = new VBox(15);
        card.getStyleClass().add("glass-pane");
        card.setMaxWidth(400);
        card.setPadding(new Insets(35));
        card.setAlignment(Pos.CENTER_LEFT);

        Label brandLabel = new Label("TECHN GLOBAL BANK");
        brandLabel.getStyleClass().add("heading");
        brandLabel.setStyle("-fx-font-size: 24px; -fx-padding: 0 0 5 0;");

        Label subtitle = new Label("Secure Portal Access");
        subtitle.getStyleClass().add("subheading");

        Label usernameLabel = new Label("Username");
        usernameLabel.getStyleClass().add("text-label");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Enter your username");

        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("text-label");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Enter your password");

        Button loginBtn = new Button("Sign In");
        loginBtn.getStyleClass().add("btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);

        Hyperlink registerLink = new Hyperlink("Create an account");
        registerLink.getStyleClass().add("btn-link");

        HBox footer = new HBox(5, new Label("Don't have an account?"), registerLink);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(10, 0, 0, 0));

        card.getChildren().addAll(
                brandLabel, subtitle,
                usernameLabel, usernameField,
                passwordLabel, passwordField,
                loginBtn, footer);

        root.getChildren().add(card);
        Scene scene = new Scene(root, 960, 640);
        applyStyle(scene);
        primaryStage.setScene(scene);
        primaryStage.show();

        // Actions
        loginBtn.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String pass = passwordField.getText();

            if (user.isEmpty() || pass.isEmpty()) {
                AlertHelper.showNotification(primaryStage, "Required Fields",
                        "Please enter both username and password.", AlertHelper.AlertType.WARNING);
                return;
            }

            AuthService.AuthResponse response = authService.authenticate(user, pass);
            switch (response.getResult()) {
                case SUCCESS -> {
                    SessionManager.startSession(response.getUser());
                    showDashboard();
                }
                case USER_NOT_FOUND -> AlertHelper.showNotification(primaryStage, "Access Denied",
                        "Invalid username or password.", AlertHelper.AlertType.ERROR);
                case WRONG_PASSWORD -> AlertHelper.showNotification(primaryStage, "Access Denied",
                        "Invalid username or password.", AlertHelper.AlertType.ERROR);
                case LOCKED_OUT -> AlertHelper.showNotification(primaryStage, "Account Locked",
                        "Your account has been locked due to 3 consecutive failed attempts. Please wait 15 minutes.",
                        AlertHelper.AlertType.WARNING);
                case BLOCKED -> AlertHelper.showNotification(primaryStage, "Account Blocked",
                        "Your account is disabled by the administrator. Contact support.", AlertHelper.AlertType.ERROR);
            }
        });

        registerLink.setOnAction(e -> showRegister());
    }

    // ==========================================
    // VIEW: REGISTER
    // ==========================================
    public void showRegister() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(40));

        VBox card = new VBox(12);
        card.getStyleClass().add("glass-pane");
        card.setMaxWidth(420);
        card.setPadding(new Insets(30));
        card.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Register Customer Account");
        title.getStyleClass().add("heading");
        title.setStyle("-fx-font-size: 20px; -fx-padding: 0 0 5 0;");

        Label usernameLabel = new Label("Username");
        usernameLabel.getStyleClass().add("text-label");
        TextField usernameField = new TextField();
        usernameField.setPromptText("Letters and numbers, 3-20 chars");

        Label emailLabel = new Label("Email Address");
        emailLabel.getStyleClass().add("text-label");
        TextField emailField = new TextField();
        emailField.setPromptText("e.g. name@domain.com");

        Label phoneLabel = new Label("Phone Number");
        phoneLabel.getStyleClass().add("text-label");
        TextField phoneField = new TextField();
        phoneField.setPromptText("e.g. 9876543210");

        Label passwordLabel = new Label("Password");
        passwordLabel.getStyleClass().add("text-label");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Min 8 chars, Upper, Lower, Digit, Symbol");

        Button registerBtn = new Button("Register Account");
        registerBtn.getStyleClass().add("btn-primary");
        registerBtn.setMaxWidth(Double.MAX_VALUE);

        Hyperlink loginLink = new Hyperlink("Login here");
        loginLink.getStyleClass().add("btn-link");

        HBox footer = new HBox(5, new Label("Already registered?"), loginLink);
        footer.setAlignment(Pos.CENTER);
        footer.setPadding(new Insets(10, 0, 0, 0));

        card.getChildren().addAll(
                title,
                usernameLabel, usernameField,
                emailLabel, emailField,
                phoneLabel, phoneField,
                passwordLabel, passwordField,
                registerBtn, footer);

        root.getChildren().add(card);
        Scene scene = new Scene(root, 960, 640);
        applyStyle(scene);
        primaryStage.setScene(scene);

        // Actions
        registerBtn.setOnAction(e -> {
            String user = usernameField.getText().trim();
            String email = emailField.getText().trim();
            String phone = phoneField.getText().trim();
            String pass = passwordField.getText();

            if (!InputValidator.isValidUsername(user)) {
                AlertHelper.showNotification(primaryStage, "Invalid Username",
                        "Username must be alphanumeric, 3 to 20 characters.", AlertHelper.AlertType.WARNING);
                return;
            }
            if (!InputValidator.isValidEmail(email)) {
                AlertHelper.showNotification(primaryStage, "Invalid Email", "Please enter a valid email format.",
                        AlertHelper.AlertType.WARNING);
                return;
            }
            if (!InputValidator.isValidPhone(phone)) {
                AlertHelper.showNotification(primaryStage, "Invalid Phone",
                        "Please enter a valid 10-15 digit phone number.", AlertHelper.AlertType.WARNING);
                return;
            }
            if (!InputValidator.isStrongPassword(pass)) {
                AlertHelper.showNotification(primaryStage, "Weak Password",
                        "Password must have at least 8 characters, an uppercase, a lowercase, a digit, and a special character.",
                        AlertHelper.AlertType.WARNING);
                return;
            }

            boolean ok = authService.register(user, pass, email, phone);
            if (ok) {
                AlertHelper.showNotification(primaryStage, "Registration Success",
                        "Account created successfully! You can now log in.", AlertHelper.AlertType.SUCCESS);
                showLogin();
            } else {
                AlertHelper.showNotification(primaryStage, "Registration Failed",
                        "Username or Email already registered in our system.", AlertHelper.AlertType.ERROR);
            }
        });

        loginLink.setOnAction(e -> showLogin());
    }

    // ==========================================
    // SIDEBAR COMPONENT
    // ==========================================
    private VBox createSidebar(String activeTab) {
        VBox sidebar = new VBox(10);
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(220);

        Label brand = new Label("TG BANK");
        brand.getStyleClass().add("sidebar-brand");
        brand.setAlignment(Pos.CENTER);
        brand.setMaxWidth(Double.MAX_VALUE);

        Button dashBtn = new Button("Dashboard");
        dashBtn.getStyleClass().addAll("sidebar-btn", activeTab.equals("dash") ? "sidebar-btn-active" : "normal");
        dashBtn.setMaxWidth(Double.MAX_VALUE);
        dashBtn.setOnAction(e -> showDashboard());

        Button transferBtn = new Button("Fund Transfer");
        transferBtn.getStyleClass().addAll("sidebar-btn",
                activeTab.equals("transfer") ? "sidebar-btn-active" : "normal");
        transferBtn.setMaxWidth(Double.MAX_VALUE);
        transferBtn.setOnAction(e -> showTransfer());

        Button historyBtn = new Button("Statements");
        historyBtn.getStyleClass().addAll("sidebar-btn", activeTab.equals("history") ? "sidebar-btn-active" : "normal");
        historyBtn.setMaxWidth(Double.MAX_VALUE);
        historyBtn.setOnAction(e -> showHistory());

        sidebar.getChildren().addAll(brand, dashBtn, transferBtn, historyBtn);

        User currentUser = SessionManager.getCurrentUser();
        if (currentUser != null && "ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            Button adminBtn = new Button("Admin Logs");
            adminBtn.getStyleClass().addAll("sidebar-btn", activeTab.equals("admin") ? "sidebar-btn-active" : "normal");
            adminBtn.setMaxWidth(Double.MAX_VALUE);
            adminBtn.setOnAction(e -> showAdmin());
            sidebar.getChildren().add(adminBtn);
        }

        // Spacer to push logout to bottom
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        Button logoutBtn = new Button("Log Out");
        logoutBtn.getStyleClass().add("sidebar-btn");
        logoutBtn.setStyle("-fx-text-fill: #ef4444;");
        logoutBtn.setMaxWidth(Double.MAX_VALUE);
        logoutBtn.setOnAction(e -> {
            SessionManager.logout();
            showLogin();
        });

        sidebar.getChildren().addAll(spacer, logoutBtn);
        return sidebar;
    }

    // ==========================================
    // COMPONENT: TESTING OTP BANNER
    // ==========================================
    private VBox createSimulationBanner() {
        VBox banner = new VBox(5);
        banner.getStyleClass().add("simulation-banner");

        Label title = new Label("OTP SIMULATION CONTROL BANNER");
        title.getStyleClass().add("simulation-banner-text");
        title.setStyle("-fx-font-size: 11px; -fx-opacity: 0.8;");

        otpBannerLabel = new Label("Status: No OTP requested. (Trigger a transfer or login to request)");
        otpBannerLabel.getStyleClass().add("simulation-banner-text");
        otpBannerLabel.setStyle("-fx-font-size: 13px;");

        banner.getChildren().addAll(title, otpBannerLabel);
        return banner;
    }

    private void updateOtpBanner(String code) {
        if (otpBannerLabel != null) {
            otpBannerLabel.setText("SIMULATED OTP RECEIVED: " + code + " (Use this to approve transaction)");
        }
    }

    // ==========================================
    // VIEW: DASHBOARD
    // ==========================================
    public void showDashboard() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showLogin();
            return;
        }

        BorderPane mainLayout = new BorderPane();
        VBox sidebar = createSidebar("dash");

        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setMaxWidth(Double.MAX_VALUE);
        BorderPane.setAlignment(content, Pos.TOP_CENTER);

        // Header
        HBox header = new HBox();
        header.setAlignment(Pos.CENTER_LEFT);
        VBox titleArea = new VBox(5);
        Label mainTitle = new Label("Welcome Back, " + currentUser.getUsername());
        mainTitle.getStyleClass().add("heading");
        Label subtitle = new Label("Role: " + currentUser.getRole() + " | Email: " + currentUser.getEmail());
        subtitle.getStyleClass().add("subheading");
        titleArea.getChildren().addAll(mainTitle, subtitle);
        header.getChildren().add(titleArea);

        // Simulated OTP display
        VBox banner = createSimulationBanner();

        // Cards Row (Savings / Checking summaries)
        FlowPane cardsRow = createResponsiveFlowPane(20, 20);
        cardsRow.setPrefHeight(150);

        List<Account> accounts = accountService.getAccountsByUserId(currentUser.getId());
        Account savings = accounts.stream().filter(a -> "SAVINGS".equals(a.getAccountType())).findFirst().orElse(null);
        Account checking = accounts.stream().filter(a -> "CHECKING".equals(a.getAccountType())).findFirst()
                .orElse(null);

        VBox savingsCard = createAccountCard("SAVINGS ACCOUNT", savings, currentUser.getId());
        VBox checkingCard = createAccountCard("CHECKING ACCOUNT", checking, currentUser.getId());
        HBox.setHgrow(savingsCard, Priority.ALWAYS);
        HBox.setHgrow(checkingCard, Priority.ALWAYS);
        cardsRow.getChildren().addAll(savingsCard, checkingCard);

        // Operations Section & Quick Charts
        FlowPane opsAndCharts = createResponsiveFlowPane(20, 20);
        VBox.setVgrow(opsAndCharts, Priority.ALWAYS);

        // Quick Deposit/Withdraw
        VBox quickOpsCard = new VBox(15);
        quickOpsCard.getStyleClass().add("glass-pane");
        quickOpsCard.setPadding(new Insets(20));
        quickOpsCard.setPrefWidth(350);

        Label opsTitle = new Label("Quick Deposit / Withdrawal");
        opsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        Label selectAccLabel = new Label("Select Target Account");
        selectAccLabel.getStyleClass().add("text-label");
        ComboBox<String> accountCombo = new ComboBox<>();
        accountCombo.setMaxWidth(Double.MAX_VALUE);
        for (Account a : accounts) {
            accountCombo.getItems().add(a.getAccountNumber() + " (" + a.getAccountType() + ")");
        }
        if (!accountCombo.getItems().isEmpty()) {
            accountCombo.getSelectionModel().selectFirst();
        }

        Label amountLabel = new Label("Amount ($)");
        amountLabel.getStyleClass().add("text-label");
        TextField amountField = new TextField();
        amountField.setPromptText("0.00");

        HBox btnRow = new HBox(10);
        Button depBtn = new Button("Deposit");
        depBtn.getStyleClass().add("btn-primary");
        depBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(depBtn, Priority.ALWAYS);

        Button wdrBtn = new Button("Withdraw");
        wdrBtn.getStyleClass().add("btn-secondary");
        wdrBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(wdrBtn, Priority.ALWAYS);
        btnRow.getChildren().addAll(depBtn, wdrBtn);

        quickOpsCard.getChildren().addAll(opsTitle, selectAccLabel, accountCombo, amountLabel, amountField, btnRow);

        // Recent Analytics Chart
        VBox chartCard = new VBox(10);
        chartCard.getStyleClass().add("glass-pane");
        chartCard.setPadding(new Insets(20));
        HBox.setHgrow(chartCard, Priority.ALWAYS);

        Label chartTitle = new Label("Account Balance Analytics");
        chartTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #f8fafc;");

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Account");
        yAxis.setLabel("Balance ($)");

        BarChart<String, Number> barChart = new BarChart<>(xAxis, yAxis);
        barChart.setLegendVisible(false);
        barChart.setAnimated(false);

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        if (savings != null) {
            series.getData().add(new XYChart.Data<>("Savings (" + savings.getAccountNumber().substring(7) + ")",
                    savings.getBalance()));
        }
        if (checking != null) {
            series.getData().add(new XYChart.Data<>("Checking (" + checking.getAccountNumber().substring(7) + ")",
                    checking.getBalance()));
        }
        barChart.getData().add(series);
        chartCard.getChildren().addAll(chartTitle, barChart);

        opsAndCharts.getChildren().addAll(quickOpsCard, chartCard);

        HBox apiStatus = createApiStatusBanner();
        content.getChildren().addAll(header, apiStatus, banner, cardsRow, opsAndCharts);
        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(content);

        Scene scene = new Scene(mainLayout, 1000, 680);
        applyStyle(scene);
        applyResponsiveSidebar(scene, mainLayout, sidebar);
        primaryStage.setScene(scene);

        // Deposit Action
        depBtn.setOnAction(e -> {
            String selected = accountCombo.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertHelper.showNotification(primaryStage, "No Account",
                        "Please create or select an active account first.", AlertHelper.AlertType.WARNING);
                return;
            }
            String accNum = selected.split(" ")[0];
            BigDecimal amount = InputValidator.validateAndParseAmount(amountField.getText());
            if (amount == null) {
                AlertHelper.showNotification(primaryStage, "Invalid Amount", "Please input a positive numeric amount.",
                        AlertHelper.AlertType.WARNING);
                return;
            }

            boolean ok = accountService.deposit(accNum, amount, currentUser.getId());
            if (ok) {
                apiService.notifyRemoteEvent("DEPOSIT",
                        "Deposited $" + amount + " to account " + accNum + " by " + currentUser.getUsername());
                AlertHelper.showNotification(primaryStage, "Deposit Confirmed",
                        "Successfully deposited $" + amount + " to account " + accNum, AlertHelper.AlertType.SUCCESS);
                showDashboard();
            } else {
                AlertHelper.showNotification(primaryStage, "Deposit Failed",
                        "Could not complete deposit. Account status may be inactive.", AlertHelper.AlertType.ERROR);
            }
        });

        // Withdraw Action
        wdrBtn.setOnAction(e -> {
            String selected = accountCombo.getSelectionModel().getSelectedItem();
            if (selected == null) {
                AlertHelper.showNotification(primaryStage, "No Account", "Please select an active account first.",
                        AlertHelper.AlertType.WARNING);
                return;
            }
            String accNum = selected.split(" ")[0];
            BigDecimal amount = InputValidator.validateAndParseAmount(amountField.getText());
            if (amount == null) {
                AlertHelper.showNotification(primaryStage, "Invalid Amount", "Please input a positive numeric amount.",
                        AlertHelper.AlertType.WARNING);
                return;
            }

            boolean ok = accountService.withdraw(accNum, amount, currentUser.getId());
            if (ok) {
                apiService.notifyRemoteEvent("WITHDRAWAL",
                        "Withdrew $" + amount + " from account " + accNum + " by " + currentUser.getUsername());
                AlertHelper.showNotification(primaryStage, "Withdrawal Confirmed",
                        "Successfully withdrew $" + amount + " from account " + accNum, AlertHelper.AlertType.SUCCESS);
                showDashboard();
            } else {
                AlertHelper.showNotification(primaryStage, "Withdrawal Failed",
                        "Insufficient funds or account inactive.", AlertHelper.AlertType.ERROR);
            }
        });
    }

    private VBox createAccountCard(String titleText, Account account, int userId) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");

        Label title = new Label(titleText);
        title.getStyleClass().add("card-title");

        if (account == null) {
            Label noAccount = new Label("No account active");
            noAccount.setStyle("-fx-text-fill: #475569; -fx-font-size: 15px; -fx-font-weight: 600;");

            Button createBtn = new Button("Initialize Account");
            createBtn.getStyleClass().add("btn-primary");
            createBtn.setStyle("-fx-padding: 6px 12px; -fx-font-size: 12px;");
            createBtn.setOnAction(e -> {
                String type = titleText.contains("SAVINGS") ? "SAVINGS" : "CHECKING";
                accountService.createAccount(userId, type);
                showDashboard();
            });

            card.getChildren().addAll(title, noAccount, createBtn);
        } else {
            Label accNum = new Label("Acc No: " + account.getAccountNumber());
            accNum.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");

            Label balance = new Label(
                    "$" + account.getBalance().setScale(2, java.math.RoundingMode.HALF_UP).toString());
            balance.getStyleClass().add("card-value");

            Label status = new Label("Status: " + account.getStatus());
            status.setStyle("-fx-text-fill: " + ("ACTIVE".equals(account.getStatus()) ? "#10b981" : "#ef4444")
                    + "; -fx-font-size: 12px; -fx-font-weight: bold;");

            card.getChildren().addAll(title, accNum, balance, status);
        }
        return card;
    }

    // ==========================================
    // VIEW: FUND TRANSFER
    // ==========================================
    public void showTransfer() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showLogin();
            return;
        }

        BorderPane mainLayout = new BorderPane();
        VBox sidebar = createSidebar("transfer");

        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setMaxWidth(Double.MAX_VALUE);
        BorderPane.setAlignment(content, Pos.TOP_CENTER);

        // Header
        Label headerTitle = new Label("Secure Fund Transfer");
        headerTitle.getStyleClass().add("heading");
        Label subtitle = new Label("Transfer money instantly to any verified account");
        subtitle.getStyleClass().add("subheading");

        VBox banner = createSimulationBanner();

        // Main form pane
        VBox form = new VBox(15);
        form.getStyleClass().add("glass-pane");
        form.setPadding(new Insets(30));
        form.setMaxWidth(600);

        List<Account> userAccounts = accountService.getAccountsByUserId(currentUser.getId());

        Label sourceLabel = new Label("Source Account");
        sourceLabel.getStyleClass().add("text-label");
        ComboBox<String> sourceCombo = new ComboBox<>();
        sourceCombo.setMaxWidth(Double.MAX_VALUE);
        for (Account a : userAccounts) {
            if ("ACTIVE".equals(a.getStatus())) {
                sourceCombo.getItems()
                        .add(a.getAccountNumber() + " (" + a.getAccountType() + ") - Balance: $" + a.getBalance());
            }
        }
        if (!sourceCombo.getItems().isEmpty()) {
            sourceCombo.getSelectionModel().selectFirst();
        }

        Label destLabel = new Label("Destination Account Number (10 Digits)");
        destLabel.getStyleClass().add("text-label");
        TextField destField = new TextField();
        destField.setPromptText("Enter 10-digit account number");

        Label amountLabel = new Label("Amount to Transfer ($)");
        amountLabel.getStyleClass().add("text-label");
        TextField amountField = new TextField();
        amountField.setPromptText("0.00");

        Label descLabel = new Label("Memo / Reference Description");
        descLabel.getStyleClass().add("text-label");
        TextField descField = new TextField();
        descField.setPromptText("Optional description");

        Button submitBtn = new Button("Initiate Transfer");
        submitBtn.getStyleClass().add("btn-primary");
        submitBtn.setMaxWidth(Double.MAX_VALUE);

        form.getChildren().addAll(
                sourceLabel, sourceCombo,
                destLabel, destField,
                amountLabel, amountField,
                descLabel, descField,
                submitBtn);

        HBox apiStatus = createApiStatusBanner();
        content.getChildren().addAll(headerTitle, subtitle, apiStatus, banner, form);
        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(content);

        Scene scene = new Scene(mainLayout, 1000, 680);
        applyStyle(scene);
        applyResponsiveSidebar(scene, mainLayout, sidebar);
        primaryStage.setScene(scene);

        // Actions
        submitBtn.setOnAction(e -> {
            String selectedSource = sourceCombo.getSelectionModel().getSelectedItem();
            if (selectedSource == null) {
                AlertHelper.showNotification(primaryStage, "No Source Account",
                        "You must have an active source account with funds to initiate transfers.",
                        AlertHelper.AlertType.WARNING);
                return;
            }
            String fromAcc = selectedSource.split(" ")[0];
            String toAcc = destField.getText().trim();
            BigDecimal amount = InputValidator.validateAndParseAmount(amountField.getText());
            String desc = descField.getText().trim();

            if (toAcc.isEmpty()) {
                AlertHelper.showNotification(primaryStage, "Destination Required",
                        "Please input a destination account number.", AlertHelper.AlertType.WARNING);
                return;
            }
            if (fromAcc.equals(toAcc)) {
                AlertHelper.showNotification(primaryStage, "Invalid Transfer",
                        "Source and destination accounts must be different.", AlertHelper.AlertType.WARNING);
                return;
            }
            if (amount == null) {
                AlertHelper.showNotification(primaryStage, "Invalid Amount",
                        "Please input a positive numeric transfer amount.", AlertHelper.AlertType.WARNING);
                return;
            }

            // Verify destination account exists
            Account destAccount = accountService.getAccountByNumber(toAcc);
            if (destAccount == null) {
                AlertHelper.showNotification(primaryStage, "Account Not Found",
                        "The destination account number does not exist.", AlertHelper.AlertType.ERROR);
                return;
            }
            if (!"ACTIVE".equals(destAccount.getStatus())) {
                AlertHelper.showNotification(primaryStage, "Account Restricted",
                        "The destination account is currently suspended/inactive.", AlertHelper.AlertType.WARNING);
                return;
            }

            // Check source account balance
            Account srcAccount = accountService.getAccountByNumber(fromAcc);
            if (srcAccount.getBalance().compareTo(amount) < 0) {
                AlertHelper.showNotification(primaryStage, "Insufficient Funds",
                        "Your selected source account balance is less than the transfer amount.",
                        AlertHelper.AlertType.WARNING);
                return;
            }

            // Trigger MFA OTP
            String otpCode = otpService.generateOtp(currentUser.getId(), "TRANSFER");
            updateOtpBanner(otpCode);

            // Open OTP Verification Dialog Modal
            showOtpVerificationDialog(currentUser, currentUser.getId(), fromAcc, toAcc, amount, desc);
        });
    }

    private void showOtpVerificationDialog(User currentUser, int userId, String fromAcc, String toAcc,
            BigDecimal amount, String desc) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initOwner(primaryStage);
        modal.initStyle(StageStyle.UTILITY);
        modal.setTitle("MFA Secure Verification");

        VBox layout = new VBox(15);
        layout.setPadding(new Insets(25));
        layout.setAlignment(Pos.CENTER);

        Label infoLabel = new Label(
                "A simulated 6-digit OTP code has been dispatched. Enter it below to approve the transfer of $" + amount
                        + " to account " + toAcc);
        infoLabel.setStyle("-fx-text-fill: #cbd5e1; -fx-font-size: 13px;");
        infoLabel.setWrapText(true);
        infoLabel.setMaxWidth(350);

        TextField otpInput = new TextField();
        otpInput.setPromptText("Enter 6-digit code");
        otpInput.setAlignment(Pos.CENTER);
        otpInput.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        Button confirmBtn = new Button("Authorize Transfer");
        confirmBtn.getStyleClass().add("btn-primary");
        confirmBtn.setMaxWidth(Double.MAX_VALUE);

        layout.getChildren().addAll(infoLabel, otpInput, confirmBtn);
        Scene scene = new Scene(layout, 400, 220);
        applyStyle(scene);
        modal.setScene(scene);

        confirmBtn.setOnAction(e -> {
            String enteredCode = otpInput.getText().trim();
            boolean verified = otpService.verifyOtp(userId, enteredCode, "TRANSFER");
            if (verified) {
                modal.close();
                boolean success = transactionService.transfer(fromAcc, toAcc, amount, desc, userId);
                if (success) {
                    apiService.notifyRemoteEvent("TRANSFER", "Transferred $" + amount + " from " + fromAcc + " to "
                            + toAcc + " by " + currentUser.getUsername());
                    AlertHelper
                            .showNotification(primaryStage, "Transfer Complete",
                                    "Successfully transferred $" + amount + " to " + toAcc
                                            + ". The transaction was completed securely.",
                                    AlertHelper.AlertType.SUCCESS);
                    showDashboard();
                } else {
                    AlertHelper.showNotification(primaryStage, "Transfer Failed",
                            "An error occurred during database commit. Transaction rolled back.",
                            AlertHelper.AlertType.ERROR);
                }
            } else {
                AlertHelper.showNotification(modal, "Verification Error", "The code entered is invalid or expired.",
                        AlertHelper.AlertType.ERROR);
            }
        });

        modal.showAndWait();
    }

    // ==========================================
    // VIEW: STATEMENTS / HISTORY
    // ==========================================
    public void showHistory() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            showLogin();
            return;
        }

        BorderPane mainLayout = new BorderPane();
        VBox sidebar = createSidebar("history");

        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setMaxWidth(Double.MAX_VALUE);
        BorderPane.setAlignment(content, Pos.TOP_CENTER);

        // Header
        Label titleLabel = new Label("Account Statements & History");
        titleLabel.getStyleClass().add("heading");
        Label subtitle = new Label("Review, filter, and download your transaction statements");
        subtitle.getStyleClass().add("subheading");

        // Filter Controls Card
        FlowPane filtersCard = createResponsiveFlowPane(15, 15);
        filtersCard.getStyleClass().add("glass-pane");
        filtersCard.setPadding(new Insets(15));
        filtersCard.setAlignment(Pos.CENTER_LEFT);

        List<Account> userAccounts = accountService.getAccountsByUserId(currentUser.getId());

        ComboBox<String> accountCombo = new ComboBox<>();
        for (Account a : userAccounts) {
            accountCombo.getItems().add(a.getAccountNumber() + " (" + a.getAccountType() + ")");
        }
        if (!accountCombo.getItems().isEmpty()) {
            accountCombo.getSelectionModel().selectFirst();
        }

        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("ALL", "DEPOSIT", "WITHDRAWAL", "TRANSFER");
        typeCombo.getSelectionModel().selectFirst();

        DatePicker startDatePicker = new DatePicker();
        startDatePicker.setPromptText("Start Date");

        DatePicker endDatePicker = new DatePicker();
        endDatePicker.setPromptText("End Date");

        Button filterBtn = new Button("Apply Filter");
        filterBtn.getStyleClass().add("btn-primary");
        filterBtn.setStyle("-fx-padding: 8px 16px;");

        Button exportBtn = new Button("Export CSV");
        exportBtn.getStyleClass().add("btn-secondary");
        exportBtn.setStyle("-fx-padding: 8px 16px;");

        filtersCard.getChildren().addAll(
                new Label("Account:"), accountCombo,
                new Label("Type:"), typeCombo,
                startDatePicker, endDatePicker,
                filterBtn, exportBtn);

        // Table
        TableView<Transaction> table = new TableView<>();
        table.setPlaceholder(new Label("No transactions match the selected filters."));
        VBox.setVgrow(table, Priority.ALWAYS);

        TableColumn<Transaction, String> txIdCol = new TableColumn<>("Transaction ID");
        txIdCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTransactionId()));
        txIdCol.setMinWidth(150);

        TableColumn<Transaction, String> dateCol = new TableColumn<>("Timestamp");
        dateCol.setCellValueFactory(data -> {
            Timestamp ts = data.getValue().getTimestamp();
            String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(ts);
            return new SimpleStringProperty(formatted);
        });
        dateCol.setMinWidth(140);

        TableColumn<Transaction, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTransactionType()));

        TableColumn<Transaction, String> fromCol = new TableColumn<>("From Account");
        fromCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getFromAccount() == null ? "-" : data.getValue().getFromAccount()));

        TableColumn<Transaction, String> toCol = new TableColumn<>("To Account");
        toCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getToAccount() == null ? "-" : data.getValue().getToAccount()));

        TableColumn<Transaction, String> amountCol = new TableColumn<>("Amount ($)");
        amountCol.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getAmount().setScale(2, java.math.RoundingMode.DOWN).toString()));

        TableColumn<Transaction, String> descCol = new TableColumn<>("Memo");
        descCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        descCol.setMinWidth(150);

        table.getColumns().add(txIdCol);
        table.getColumns().add(dateCol);
        table.getColumns().add(typeCol);
        table.getColumns().add(fromCol);
        table.getColumns().add(toCol);
        table.getColumns().add(amountCol);
        table.getColumns().add(descCol);

        // Load Initial Data
        Runnable loadData = () -> {
            String selected = accountCombo.getSelectionModel().getSelectedItem();
            if (selected == null)
                return;
            String accNum = selected.split(" ")[0];
            String type = typeCombo.getSelectionModel().getSelectedItem();

            LocalDate start = startDatePicker.getValue();
            LocalDate end = endDatePicker.getValue();
            String startStr = start != null ? start.toString() : null;
            String endStr = end != null ? end.toString() : null;

            List<Transaction> txs = transactionService.getHistoryFiltered(accNum, type, startStr, endStr);
            table.setItems(FXCollections.observableArrayList(txs));
        };

        filterBtn.setOnAction(e -> loadData.run());

        // Export Action
        exportBtn.setOnAction(e -> {
            String selected = accountCombo.getSelectionModel().getSelectedItem();
            if (selected == null)
                return;
            String accNum = selected.split(" ")[0];

            List<Transaction> records = table.getItems();
            if (records.isEmpty()) {
                AlertHelper.showNotification(primaryStage, "No Data", "No transaction records available to export.",
                        AlertHelper.AlertType.WARNING);
                return;
            }

            try {
                // Ensure output directory exists inside workspace
                File exportDir = new File("c:/Users/HP/OneDrive/Desktop/Intern-Java-Major/exports");
                if (!exportDir.exists()) {
                    exportDir.mkdirs();
                }

                File exportFile = new File(exportDir,
                        "statement_" + accNum + "_" + System.currentTimeMillis() + ".csv");
                try (PrintWriter pw = new PrintWriter(exportFile)) {
                    pw.println("Transaction ID,Timestamp,Type,From Account,To Account,Amount,Memo");
                    for (Transaction tx : records) {
                        pw.println(String.format("%s,%s,%s,%s,%s,%s,\"%s\"",
                                tx.getTransactionId(),
                                tx.getTimestamp().toString(),
                                tx.getTransactionType(),
                                tx.getFromAccount() == null ? "" : tx.getFromAccount(),
                                tx.getToAccount() == null ? "" : tx.getToAccount(),
                                tx.getAmount().toString(),
                                tx.getDescription() == null ? "" : tx.getDescription().replace("\"", "\"\"")));
                    }
                }
                AlertHelper.showNotification(primaryStage, "Export Completed",
                        "Statement exported successfully to: " + exportFile.getName() + " in /exports",
                        AlertHelper.AlertType.SUCCESS);
            } catch (Exception ex) {
                ex.printStackTrace();
                AlertHelper.showNotification(primaryStage, "Export Failed",
                        "Error exporting statement: " + ex.getMessage(), AlertHelper.AlertType.ERROR);
            }
        });

        // Trigger load
        loadData.run();

        HBox apiStatus = createApiStatusBanner();
        content.getChildren().addAll(titleLabel, subtitle, apiStatus, filtersCard, table);
        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(content);

        Scene scene = new Scene(mainLayout, 1000, 680);
        applyStyle(scene);
        applyResponsiveSidebar(scene, mainLayout, sidebar);
        primaryStage.setScene(scene);
    }

    // ==========================================
    // VIEW: ADMIN USER & AUDIT LOGS
    // ==========================================
    public void showAdmin() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null || !"ADMIN".equalsIgnoreCase(currentUser.getRole())) {
            showDashboard();
            return;
        }

        BorderPane mainLayout = new BorderPane();
        VBox sidebar = createSidebar("admin");

        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setMaxWidth(Double.MAX_VALUE);
        BorderPane.setAlignment(content, Pos.TOP_CENTER);

        // Header
        Label titleLabel = new Label("Security Audits & Account Moderation");
        titleLabel.getStyleClass().add("heading");
        Label subtitle = new Label("Monitor system events, locking/unlocking users, and general compliance logs");
        subtitle.getStyleClass().add("subheading");

        TabPane tabs = new TabPane();
        VBox.setVgrow(tabs, Priority.ALWAYS);

        // TAB 1: User Management
        Tab userTab = new Tab("User Accounts");
        userTab.setClosable(false);
        VBox userLayout = new VBox(15);
        userLayout.setPadding(new Insets(15));

        TableView<User> userTable = new TableView<>();
        VBox.setVgrow(userTable, Priority.ALWAYS);

        TableColumn<User, String> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));

        TableColumn<User, String> nameCol = new TableColumn<>("Username");
        nameCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));

        TableColumn<User, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getEmail()));

        TableColumn<User, String> roleCol = new TableColumn<>("Role");
        roleCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getRole()));

        TableColumn<User, String> statusCol = new TableColumn<>("Status");
        statusCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getStatus()));

        TableColumn<User, String> failedCol = new TableColumn<>("Failed Strikes");
        failedCol.setCellValueFactory(
                data -> new SimpleStringProperty(String.valueOf(data.getValue().getFailedAttempts())));

        userTable.getColumns().add(idCol);
        userTable.getColumns().add(nameCol);
        userTable.getColumns().add(emailCol);
        userTable.getColumns().add(roleCol);
        userTable.getColumns().add(statusCol);
        userTable.getColumns().add(failedCol);

        HBox actionRow = new HBox(10);
        Button blockBtn = new Button("Block Selected");
        blockBtn.getStyleClass().add("btn-primary");
        blockBtn.setStyle("-fx-background-color: #ef4444; -fx-effect: none;");

        Button unblockBtn = new Button("Unlock / Activate");
        unblockBtn.getStyleClass().add("btn-secondary");

        actionRow.getChildren().addAll(blockBtn, unblockBtn);
        userLayout.getChildren().addAll(userTable, actionRow);
        userTab.setContent(userLayout);

        // TAB 2: Audit Logs
        Tab auditTab = new Tab("System Audit Log");
        auditTab.setClosable(false);

        TableView<AuditLog> auditTable = new TableView<>();
        VBox.setVgrow(auditTable, Priority.ALWAYS);

        TableColumn<AuditLog, String> logIdCol = new TableColumn<>("Log ID");
        logIdCol.setCellValueFactory(data -> new SimpleStringProperty(String.valueOf(data.getValue().getId())));

        TableColumn<AuditLog, String> logUserCol = new TableColumn<>("Target User");
        logUserCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));

        TableColumn<AuditLog, String> logActionCol = new TableColumn<>("Security Action");
        logActionCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getAction()));

        TableColumn<AuditLog, String> logDescCol = new TableColumn<>("Details");
        logDescCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDescription()));
        logDescCol.setMinWidth(250);

        TableColumn<AuditLog, String> logTimeCol = new TableColumn<>("Timestamp");
        logTimeCol.setCellValueFactory(data -> {
            Timestamp ts = data.getValue().getTimestamp();
            String formatted = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(ts);
            return new SimpleStringProperty(formatted);
        });
        logTimeCol.setMinWidth(140);

        auditTable.getColumns().add(logIdCol);
        auditTable.getColumns().add(logUserCol);
        auditTable.getColumns().add(logActionCol);
        auditTable.getColumns().add(logDescCol);
        auditTable.getColumns().add(logTimeCol);
        auditTab.setContent(auditTable);

        tabs.getTabs().addAll(userTab, auditTab);

        // Loading Data Helper
        Runnable loadData = () -> {
            List<User> users = authService.getAllUsers();
            userTable.setItems(FXCollections.observableArrayList(users));

            List<AuditLog> logs = auditRepo.findAllWithUsernames();
            auditTable.setItems(FXCollections.observableArrayList(logs));
        };

        // Actions
        blockBtn.setOnAction(e -> {
            User selected = userTable.getSelectionModel().getSelectedItem();
            if (selected == null)
                return;
            if ("ADMIN".equalsIgnoreCase(selected.getRole())) {
                AlertHelper.showNotification(primaryStage, "Unauthorized", "You cannot block an administrator account.",
                        AlertHelper.AlertType.WARNING);
                return;
            }
            authService.updateUserStatus(selected.getId(), "BLOCKED", currentUser.getId());
            loadData.run();
            AlertHelper.showNotification(primaryStage, "Action Complete", "User account status set to: BLOCKED",
                    AlertHelper.AlertType.SUCCESS);
        });

        unblockBtn.setOnAction(e -> {
            User selected = userTable.getSelectionModel().getSelectedItem();
            if (selected == null)
                return;
            authService.updateUserStatus(selected.getId(), "ACTIVE", currentUser.getId());
            loadData.run();
            AlertHelper.showNotification(primaryStage, "Action Complete", "User account status unlocked: ACTIVE",
                    AlertHelper.AlertType.SUCCESS);
        });

        // Trigger load
        loadData.run();

        HBox apiStatus = createApiStatusBanner();
        content.getChildren().addAll(titleLabel, subtitle, apiStatus, tabs);
        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(content);

        Scene scene = new Scene(mainLayout, 1000, 680);
        applyStyle(scene);
        applyResponsiveSidebar(scene, mainLayout, sidebar);
        primaryStage.setScene(scene);
    }
}
