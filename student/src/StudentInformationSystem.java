import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StudentInformationSystem {
    // Session handling
    private static String currentUserId = null;
    private static String currentUserRole = null;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(RoleSelection::new);
    }

    public static void applyStyle(JComponent component, Color bgColor) {
        Font font = new Font("Arial", Font.PLAIN, 18);
        component.setFont(font);
        component.setBackground(bgColor);
        if (component instanceof JButton) {
            component.setForeground(Color.BLACK);
            ((JButton) component).setOpaque(true);
            ((JButton) component).setBorderPainted(false);
            JButton btn = (JButton) component;
            Color originalColor = bgColor;
            btn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    btn.setBackground(originalColor.brighter());
                }
                @Override
                public void mouseExited(MouseEvent e) {
                    btn.setBackground(originalColor);
                }
            });
        } else if (component instanceof JLabel) {
            component.setForeground(Color.BLACK);
        } else if (component instanceof JTextField || component instanceof JPasswordField) {
            component.setForeground(Color.BLACK);
            component.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(Color.GRAY, 1),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)));
        } else if (component instanceof JComboBox) {
            component.setForeground(Color.BLACK);
            ((JComboBox<?>) component).setOpaque(true);
        }
    }

    // Password hashing utility
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error hashing password", e);
        }
    }

    // Session management methods
    public static void setCurrentUser(String userId, String role) {
        currentUserId = userId;
        currentUserRole = role;
    }

    public static String getCurrentUserId() {
        return currentUserId;
    }

    public static String getCurrentUserRole() {
        return currentUserRole;
    }

    public static void clearSession() {
        currentUserId = null;
        currentUserRole = null;
    }
}

class DatabaseHelper {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/student_info?useSSL=false&serverTimezone=UTC";
    private static final String USER = "root";
    private static final String PASS = "root";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }

    public static void initializeDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Create users table
            String createUsersTable = """
                CREATE TABLE IF NOT EXISTS users (
                    user_id VARCHAR(36) PRIMARY KEY,
                    email VARCHAR(255) UNIQUE NOT NULL,
                    password_hash VARCHAR(64) NOT NULL,
                    name VARCHAR(100) NOT NULL,
                    role VARCHAR(20) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )
            """;
            stmt.executeUpdate(createUsersTable);

            // Create students table
            String createStudentsTable = """
                CREATE TABLE IF NOT EXISTS students (
                    roll_no VARCHAR(20) PRIMARY KEY,
                    name VARCHAR(100) NOT NULL,
                    dob DATE,
                    email VARCHAR(255) UNIQUE NOT NULL,
                    address VARCHAR(255),
                    gender VARCHAR(10),
                    phone VARCHAR(15),
                    dept VARCHAR(50)
                )
            """;
            stmt.executeUpdate(createStudentsTable);

            // Create attendance table
            String createAttendanceTable = """
                CREATE TABLE IF NOT EXISTS attendance (
                    student_id VARCHAR(20),
                    month VARCHAR(20),
                    year INT,
                    working_days INT,
                    attended_days INT,
                    percentage DOUBLE,
                    PRIMARY KEY (student_id, month, year),
                    FOREIGN KEY (student_id) REFERENCES students(roll_no)
                )
            """;
            stmt.executeUpdate(createAttendanceTable);

            // Create marks table
            String createMarksTable = """
                CREATE TABLE IF NOT EXISTS marks (
                    student_id VARCHAR(20),
                    semester INT,
                    subject_code VARCHAR(20),
                    subject_name VARCHAR(100),
                    grade VARCHAR(2),
                    grade_point DOUBLE,
                    credit DOUBLE,
                    PRIMARY KEY (student_id, semester, subject_code),
                    FOREIGN KEY (student_id) REFERENCES students(roll_no)
                )
            """;
            stmt.executeUpdate(createMarksTable);

            // Create fees table
            String createFeesTable = """
                CREATE TABLE IF NOT EXISTS fees (
                    roll_no VARCHAR(20) PRIMARY KEY,
                    amount_due DOUBLE,
                    amount_paid DOUBLE,
                    balance DOUBLE,
                    FOREIGN KEY (roll_no) REFERENCES students(roll_no)
                )
            """;
            stmt.executeUpdate(createFeesTable);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

class RoleSelection extends JFrame {
    RoleSelection() {
        setTitle("Student Information System");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Color bgColor = new Color(240, 248, 255);
        getContentPane().setBackground(bgColor);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(bgColor);
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JLabel heading = new JLabel("STUDENT INFORMATION SYSTEM");
        heading.setFont(new Font("Arial", Font.BOLD, 24));
        heading.setForeground(new Color(25, 25, 112));
        heading.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel label = new JLabel("Choose Your Role:");
        String[] roles = {"Admin", "Staff", "Student"};
        JComboBox<String> roleBox = new JComboBox<>(roles);
        JButton proceed = new JButton("Proceed");

        StudentInformationSystem.applyStyle(label, bgColor);
        StudentInformationSystem.applyStyle(roleBox, Color.WHITE);
        StudentInformationSystem.applyStyle(proceed, new Color(135, 206, 250));

        proceed.setAlignmentX(Component.CENTER_ALIGNMENT);
        roleBox.setMaximumSize(new Dimension(200, 40));
        label.setAlignmentX(Component.CENTER_ALIGNMENT);

        proceed.addActionListener(e -> {
            String selected = (String) roleBox.getSelectedItem();
            new Login(selected);
            dispose();
        });

        mainPanel.add(heading);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(label);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        mainPanel.add(roleBox);
        mainPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        mainPanel.add(proceed);

        add(mainPanel);
        DatabaseHelper.initializeDatabase();
        setVisible(true);
    }
}

class Register extends JFrame {
    Register(String role) {
        setTitle(role + " Registration");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Color bgColor = new Color(245, 245, 220);
        getContentPane().setBackground(bgColor);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(bgColor);
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        JTextField email = new JTextField(15);
        JPasswordField pass = new JPasswordField(15);
        JPasswordField confirmPass = new JPasswordField(15);
        JTextField name = new JTextField(15);
        JButton register = new JButton("Register");
        JButton back = new JButton("Back");
        JLabel nameLabel = new JLabel("Name:");
        JLabel emailLabel = new JLabel("Email:");
        JLabel passLabel = new JLabel("Password:");
        JLabel confirmPassLabel = new JLabel("Confirm Password:");

        StudentInformationSystem.applyStyle(email, Color.WHITE);
        StudentInformationSystem.applyStyle(pass, Color.WHITE);
        StudentInformationSystem.applyStyle(confirmPass, Color.WHITE);
        StudentInformationSystem.applyStyle(name, Color.WHITE);
        StudentInformationSystem.applyStyle(register, new Color(144, 238, 144));
        StudentInformationSystem.applyStyle(back, new Color(216, 191, 216));
        StudentInformationSystem.applyStyle(nameLabel, bgColor);
        StudentInformationSystem.applyStyle(emailLabel, bgColor);
        StudentInformationSystem.applyStyle(passLabel, bgColor);
        StudentInformationSystem.applyStyle(confirmPassLabel, bgColor);

        register.addActionListener(e -> {
            String emailText = email.getText().trim();
            String nameText = name.getText().trim();
            String password = new String(pass.getPassword());
            String confirmPassword = new String(confirmPass.getPassword());

            // Validation
            String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.(com|edu|org)$";
            if (!Pattern.matches(emailRegex, emailText)) {
                JOptionPane.showMessageDialog(this, "Invalid Email: Must be a valid format (e.g., user@domain.com)");
                return;
            }
            if (nameText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Name cannot be empty");
                return;
            }
            if (password.length() < 8) {
                JOptionPane.showMessageDialog(this, "Password must be at least 8 characters long");
                return;
            }
            if (!password.equals(confirmPassword)) {
                JOptionPane.showMessageDialog(this, "Passwords do not match");
                return;
            }

            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "INSERT INTO users (user_id, email, password_hash, name, role) VALUES (?, ?, ?, ?, ?)")) {
                String userId = UUID.randomUUID().toString();
                String hashedPassword = StudentInformationSystem.hashPassword(password);
                
                pstmt.setString(1, userId);
                pstmt.setString(2, emailText);
                pstmt.setString(3, hashedPassword);
                pstmt.setString(4, nameText);
                pstmt.setString(5, role);
                
                pstmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Registration successful! Please login.");
                new Login(role);
                dispose();
            } catch (SQLException ex) {
                if (ex.getErrorCode() == 1062) { // Duplicate entry
                    JOptionPane.showMessageDialog(this, "Email already registered");
                } else {
                    JOptionPane.showMessageDialog(this, "Error during registration: " + ex.getMessage());
                }
            }
        });

        back.addActionListener(e -> {
            new RoleSelection();
            dispose();
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; mainPanel.add(nameLabel, gbc);
        gbc.gridx = 1; mainPanel.add(name, gbc);
        gbc.gridx = 0; gbc.gridy = 1; mainPanel.add(emailLabel, gbc);
        gbc.gridx = 1; mainPanel.add(email, gbc);
        gbc.gridx = 0; gbc.gridy = 2; mainPanel.add(passLabel, gbc);
        gbc.gridx = 1; mainPanel.add(pass, gbc);
        gbc.gridx = 0; gbc.gridy = 3; mainPanel.add(confirmPassLabel, gbc);
        gbc.gridx = 1; mainPanel.add(confirmPass, gbc);
        gbc.gridx = 0; gbc.gridy = 4; mainPanel.add(register, gbc);
        gbc.gridx = 1; mainPanel.add(back, gbc);

        add(mainPanel);
        setVisible(true);
    }
}

class Login extends JFrame {
    Login(String role) {
        setTitle(role + " Login");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Color bgColor = new Color(240, 255, 240);
        getContentPane().setBackground(bgColor);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(bgColor);
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(20, 20, 20, 20)));

        JTextField email = new JTextField(15);
        JPasswordField pass = new JPasswordField(15);
        JButton login = new JButton("Login");
        JButton register = new JButton("Register");
        JButton back = new JButton("Back");
        JLabel emailLabel = new JLabel("Email:");
        JLabel passLabel = new JLabel("Password:");

        StudentInformationSystem.applyStyle(email, Color.WHITE);
        StudentInformationSystem.applyStyle(pass, Color.WHITE);
        StudentInformationSystem.applyStyle(login, new Color(173, 216, 230));
        StudentInformationSystem.applyStyle(register, new Color(144, 238, 144));
        StudentInformationSystem.applyStyle(back, new Color(216, 191, 216));
        StudentInformationSystem.applyStyle(emailLabel, bgColor);
        StudentInformationSystem.applyStyle(passLabel, bgColor);

        login.addActionListener(e -> {
            String emailText = email.getText().trim();
            String password = new String(pass.getPassword());

            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement(
                         "SELECT user_id, password_hash, role FROM users WHERE email = ?")) {
                pstmt.setString(1, emailText);
                ResultSet rs = pstmt.executeQuery();
                
                if (rs.next()) {
                    String storedHash = rs.getString("password_hash");
                    String inputHash = StudentInformationSystem.hashPassword(password);
                    String storedRole = rs.getString("role");
                    
                    if (storedHash.equals(inputHash) && storedRole.equals(role)) {
                        StudentInformationSystem.setCurrentUser(rs.getString("user_id"), role);
                        new Dashboard(role);
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(this, "Invalid credentials or role mismatch");
                    }
                } else {
                    JOptionPane.showMessageDialog(this, "User not found");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error during login: " + ex.getMessage());
            }
        });

        register.addActionListener(e -> {
            new Register(role);
            dispose();
        });

        back.addActionListener(e -> {
            new RoleSelection();
            dispose();
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; mainPanel.add(emailLabel, gbc);
        gbc.gridx = 1; mainPanel.add(email, gbc);
        gbc.gridx = 0; gbc.gridy = 1; mainPanel.add(passLabel, gbc);
        gbc.gridx = 1; mainPanel.add(pass, gbc);
        gbc.gridx = 0; gbc.gridy = 2; mainPanel.add(login, gbc);
        gbc.gridx = 1; mainPanel.add(register, gbc);
        gbc.gridx = 2; mainPanel.add(back, gbc);

        add(mainPanel);
        setVisible(true);
    }
}

class Dashboard extends JFrame {
    Dashboard(String role) {
        if (StudentInformationSystem.getCurrentUserId() == null) {
            JOptionPane.showMessageDialog(null, "Please login first");
            new RoleSelection();
            dispose();
            return;
        }

        setTitle(role + " Dashboard - Welcome " + getUserName());
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Color bgColor = new Color(255, 245, 238);
        getContentPane().setBackground(bgColor);

        JPanel mainPanel = new JPanel();
        mainPanel.setBackground(bgColor);
        mainPanel.setLayout(new GridBagLayout());
        mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        JButton studentBtn = new JButton("Student Management");
        JButton attendanceBtn = new JButton("Attendance Module");
        JButton marksBtn = new JButton("Marks Module");
        JButton feesBtn = new JButton("Fees Module");
        JButton logoutBtn = new JButton("Logout");

        StudentInformationSystem.applyStyle(studentBtn, new Color(144, 238, 144));
        StudentInformationSystem.applyStyle(attendanceBtn, new Color(173, 216, 230));
        StudentInformationSystem.applyStyle(marksBtn, new Color(240, 230, 140));
        StudentInformationSystem.applyStyle(feesBtn, new Color(216, 191, 216));
        StudentInformationSystem.applyStyle(logoutBtn, new Color(255, 182, 193));

        studentBtn.addActionListener(e -> {
            try {
                new StudentManagement(role);
                dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error opening Student Management: " + ex.getMessage());
                ex.printStackTrace();
            }
        });

        attendanceBtn.addActionListener(e -> {
            new AttendanceModule(role);
            dispose();
        });

        marksBtn.addActionListener(e -> {
            new MarksModule(role);
            dispose();
        });

        feesBtn.addActionListener(e -> {
            new FeesModule(role);
            dispose();
        });

        logoutBtn.addActionListener(e -> {
            StudentInformationSystem.clearSession();
            new RoleSelection();
            dispose();
        });

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;

        gbc.gridy = 0; mainPanel.add(studentBtn, gbc);
        gbc.gridy = 1; mainPanel.add(attendanceBtn, gbc);
        gbc.gridy = 2; mainPanel.add(marksBtn, gbc);
        gbc.gridy = 3; mainPanel.add(feesBtn, gbc);
        gbc.gridy = 4; mainPanel.add(logoutBtn, gbc);

        add(mainPanel);
        setVisible(true);
    }

    private String getUserName() {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT name FROM users WHERE user_id = ?")) {
            pstmt.setString(1, StudentInformationSystem.getCurrentUserId());
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getString("name");
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error retrieving user name: " + ex.getMessage());
        }
        return "";
    }
}

class StudentManagement extends JFrame {
    DefaultTableModel model;
    JTable table;
    JTextField[] fields;
    JComboBox<String> gender;

    StudentManagement(String role) {
        setTitle("Student Management");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Color bgColor = new Color(245, 245, 220);
        getContentPane().setBackground(bgColor);

        String[] columns = {"Roll No", "Name", "DOB", "Email", "Address", "Gender", "Phone", "Dept"};
        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 18));
        table.setForeground(Color.BLACK);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(173, 216, 250));
        table.setRowHeight(30);

        fields = new JTextField[7];
        for (int i = 0; i < fields.length; i++) fields[i] = new JTextField(10);
        gender = new JComboBox<>(new String[]{"Male", "Female"});

        JButton add = new JButton("Add");
        JButton edit = new JButton("Edit");
        JButton delete = new JButton("Delete");
        JButton search = new JButton("Search");
        JButton back = new JButton("Back to Dashboard");

        JLabel[] labels = new JLabel[]{
            new JLabel("Roll No"), new JLabel("Name"), new JLabel("DOB"),
            new JLabel("Email"), new JLabel("Address"), new JLabel("Gender"),
            new JLabel("Phone"), new JLabel("Dept")
        };

        for (JTextField field : fields) StudentInformationSystem.applyStyle(field, Color.WHITE);
        StudentInformationSystem.applyStyle(gender, Color.WHITE);
        StudentInformationSystem.applyStyle(add, new Color(144, 238, 144));
        StudentInformationSystem.applyStyle(edit, new Color(173, 216, 230));
        StudentInformationSystem.applyStyle(delete, new Color(255, 182, 193));
        StudentInformationSystem.applyStyle(search, new Color(240, 230, 140));
        StudentInformationSystem.applyStyle(back, new Color(216, 191, 216));
        for (JLabel label : labels) StudentInformationSystem.applyStyle(label, bgColor);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) {
                        fields[0].setText((String) model.getValueAt(row, 0));
                        fields[1].setText((String) model.getValueAt(row, 1));
                        fields[2].setText((String) model.getValueAt(row, 2));
                        fields[3].setText((String) model.getValueAt(row, 3));
                        fields[4].setText((String) model.getValueAt(row, 4));
                        gender.setSelectedItem(model.getValueAt(row, 5));
                        fields[5].setText((String) model.getValueAt(row, 6));
                        fields[6].setText((String) model.getValueAt(row, 7));
                    }
                }
            }
        });

        loadStudentData();

        add.addActionListener(e -> {
            if (!role.equals("Student")) {
                String phone = fields[5].getText();
                String email = fields[3].getText();
                if (!phone.matches("\\d{10}")) {
                    JOptionPane.showMessageDialog(this, "Invalid Phone Number: Must be exactly 10 digits");
                    return;
                }
                String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.(com|edu|org)$";
                if (!Pattern.matches(emailRegex, email)) {
                    JOptionPane.showMessageDialog(this, "Invalid Email: Must be a valid format (e.g., user@domain.com)");
                    return;
                }
                try (Connection conn = DatabaseHelper.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(
                             "INSERT INTO students (roll_no, name, dob, email, address, gender, phone, dept) VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    pstmt.setString(1, fields[0].getText());
                    pstmt.setString(2, fields[1].getText());
                    pstmt.setString(3, fields[2].getText());
                    pstmt.setString(4, fields[3].getText());
                    pstmt.setString(5, fields[4].getText());
                    pstmt.setString(6, (String) gender.getSelectedItem());
                    pstmt.setString(7, fields[5].getText());
                    pstmt.setString(8, fields[6].getText());
                    pstmt.executeUpdate();
                    model.addRow(new Object[]{
                            fields[0].getText(), fields[1].getText(), fields[2].getText(), fields[3].getText(),
                            fields[4].getText(), gender.getSelectedItem(), fields[5].getText(), fields[6].getText()
                    });
                    JOptionPane.showMessageDialog(this, "Student added successfully");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error adding student: " + ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(this, "Access Denied");
            }
        });

        edit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1 && !role.equals("Student")) {
                String phone = fields[5].getText();
                String email = fields[3].getText();
                if (!phone.matches("\\d{10}")) {
                    JOptionPane.showMessageDialog(this, "Invalid Phone Number: Must be exactly 10 digits");
                    return;
                }
                String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.(com|edu|org)$";
                if (!Pattern.matches(emailRegex, email)) {
                    JOptionPane.showMessageDialog(this, "Invalid Email: Must be a valid format (e.g., user@domain.com)");
                    return;
                }
                boolean hasChanges = !fields[0].getText().equals(model.getValueAt(row, 0)) ||
                                    !fields[1].getText().equals(model.getValueAt(row, 1)) ||
                                    !fields[2].getText().equals(model.getValueAt(row, 2)) ||
                                    !fields[3].getText().equals(model.getValueAt(row, 3)) ||
                                    !fields[4].getText().equals(model.getValueAt(row, 4)) ||
                                    !gender.getSelectedItem().equals(model.getValueAt(row, 5)) ||
                                    !fields[5].getText().equals(model.getValueAt(row, 6)) ||
                                    !fields[6].getText().equals(model.getValueAt(row, 7));
                if (!hasChanges) {
                    JOptionPane.showMessageDialog(this, "No changes detected");
                    return;
                }
                try (Connection conn = DatabaseHelper.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(
                             "UPDATE students SET name = ?, dob = ?, email = ?, address = ?, gender = ?, phone = ?, dept = ? WHERE roll_no = ?")) {
                    pstmt.setString(1, fields[1].getText());
                    pstmt.setString(2, fields[2].getText());
                    pstmt.setString(3, fields[3].getText());
                    pstmt.setString(4, fields[4].getText());
                    pstmt.setString(5, (String) gender.getSelectedItem());
                    pstmt.setString(6, fields[5].getText());
                    pstmt.setString(7, fields[6].getText());
                    pstmt.setString(8, fields[0].getText());
                    int updatedRows = pstmt.executeUpdate();
                    if (updatedRows > 0) {
                        model.setValueAt(fields[0].getText(), row, 0);
                        model.setValueAt(fields[1].getText(), row, 1);
                        model.setValueAt(fields[2].getText(), row, 2);
                        model.setValueAt(fields[3].getText(), row, 3);
                        model.setValueAt(fields[4].getText(), row, 4);
                        model.setValueAt(gender.getSelectedItem(), row, 5);
                        model.setValueAt(fields[5].getText(), row, 6);
                        model.setValueAt(fields[6].getText(), row, 7);
                        JOptionPane.showMessageDialog(this, "Student updated successfully");
                        table.clearSelection();
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error updating student: " + ex.getMessage());
                }
            } else {
                JOptionPane.showMessageDialog(this, "Please select a row to edit or check access rights");
            }
        });

        delete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1 && !role.equals("Student")) {
                String rollNo = (String) model.getValueAt(row, 0);
                try (Connection conn = DatabaseHelper.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM students WHERE roll_no = ?")) {
                    pstmt.setString(1, rollNo);
                    pstmt.executeUpdate();
                    model.removeRow(row);
                    JOptionPane.showMessageDialog(this, "Student deleted successfully");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error deleting student: " + ex.getMessage());
                }
            }
        });

        search.addActionListener(e -> {
            String roll = fields[0].getText();
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM students WHERE roll_no = ?")) {
                pstmt.setString(1, roll);
                ResultSet rs = pstmt.executeQuery();
                if (rs.next()) {
                    JOptionPane.showMessageDialog(this, "Student Found: " + rs.getString("name"));
                } else {
                    JOptionPane.showMessageDialog(this, "Student Not Found");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error searching student: " + ex.getMessage());
            }
        });

        back.addActionListener(e -> {
            new Dashboard(role);
            dispose();
        });

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; panel.add(labels[0], gbc);
        gbc.gridx = 1; panel.add(fields[0], gbc);
        gbc.gridx = 2; panel.add(labels[1], gbc);
        gbc.gridx = 3; panel.add(fields[1], gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(labels[2], gbc);
        gbc.gridx = 1; panel.add(fields[2], gbc);
        gbc.gridx = 2; panel.add(labels[3], gbc);
        gbc.gridx = 3; panel.add(fields[3], gbc);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(labels[4], gbc);
        gbc.gridx = 1; panel.add(fields[4], gbc);
        gbc.gridx = 2; panel.add(labels[5], gbc);
        gbc.gridx = 3; panel.add(gender, gbc);
        gbc.gridx = 0; gbc.gridy = 3; panel.add(labels[6], gbc);
        gbc.gridx = 1; panel.add(fields[5], gbc);
        gbc.gridx = 2; panel.add(labels[7], gbc);
        gbc.gridx = 3; panel.add(fields[6], gbc);
        gbc.gridx = 0; gbc.gridy = 4; panel.add(add, gbc);
        gbc.gridx = 1; panel.add(edit, gbc);
        gbc.gridx = 2; panel.add(delete, gbc);
        gbc.gridx = 3; panel.add(search, gbc);
        gbc.gridx = 4; panel.add(back, gbc);

        add(panel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        setVisible(true);
    }

    private void loadStudentData() {
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM students")) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("roll_no"),
                        rs.getString("name"),
                        rs.getString("dob"),
                        rs.getString("email"),
                        rs.getString("address"),
                        rs.getString("gender"),
                        rs.getString("phone"),
                        rs.getString("dept")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading students: " + ex.getMessage());
        }
    }
}

class AttendanceModule extends JFrame {
    DefaultTableModel model;
    JTable table;
    JTextField studentId, workingDays, attendedDays, searchId;
    JComboBox<String> monthCombo, yearCombo, searchMonth;
    JLabel percentageLabel;

    AttendanceModule(String role) {
        setTitle("Attendance Management");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Color bgColor = new Color(240, 255, 240);
        getContentPane().setBackground(bgColor);

        String[] columns = {"Student ID", "Month", "Year", "Working Days", "Attended Days", "Percentage"};
        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 16));
        table.setForeground(Color.BLACK);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(173, 216, 250));
        table.setRowHeight(30);

        studentId = new JTextField(10);
        String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
        monthCombo = new JComboBox<>(months);
        String[] years = {"2023", "2024", "2025", "2026"};
        yearCombo = new JComboBox<>(years);
        workingDays = new JTextField(5);
        attendedDays = new JTextField(5);
        percentageLabel = new JLabel("0.00%");
        searchId = new JTextField(10);
        searchMonth = new JComboBox<>(months);

        JButton add = new JButton("Add");
        JButton edit = new JButton("Edit");
        JButton delete = new JButton("Delete");
        JButton search = new JButton("Search");
        JButton back = new JButton("Back to Dashboard");

        JLabel[] labels = {
            new JLabel("Student ID:"), new JLabel("Month:"), new JLabel("Year:"),
            new JLabel("Working Days:"), new JLabel("Attended Days:"), new JLabel("Percentage:"),
            new JLabel("Search ID:"), new JLabel("Search Month:")
        };

        for (JTextField field : new JTextField[]{studentId, workingDays, attendedDays, searchId}) {
            StudentInformationSystem.applyStyle(field, Color.WHITE);
        }
        StudentInformationSystem.applyStyle(monthCombo, Color.WHITE);
        StudentInformationSystem.applyStyle(yearCombo, Color.WHITE);
        StudentInformationSystem.applyStyle(searchMonth, Color.WHITE);
        StudentInformationSystem.applyStyle(add, new Color(144, 238, 144));
        StudentInformationSystem.applyStyle(edit, new Color(135, 206, 250));
        StudentInformationSystem.applyStyle(delete, new Color(255, 182, 193));
        StudentInformationSystem.applyStyle(search, new Color(240, 230, 140));
        StudentInformationSystem.applyStyle(back, new Color(216, 191, 216));
        StudentInformationSystem.applyStyle(percentageLabel, bgColor);
        for (JLabel label : labels) StudentInformationSystem.applyStyle(label, bgColor);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) {
                        studentId.setText((String) model.getValueAt(row, 0));
                        monthCombo.setSelectedItem(model.getValueAt(row, 1));
                        yearCombo.setSelectedItem(model.getValueAt(row, 2));
                        workingDays.setText(model.getValueAt(row, 3).toString());
                        attendedDays.setText(model.getValueAt(row, 4).toString());
                        percentageLabel.setText(model.getValueAt(row, 5).toString() + "%");
                    }
                }
            }
        });

        attendedDays.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                try {
                    int wd = Integer.parseInt(workingDays.getText().trim());
                    int ad = Integer.parseInt(attendedDays.getText().trim());
                    if (wd > 0) {
                        double percentage = (ad * 100.0) / wd;
                        percentageLabel.setText(String.format("%.2f%%", percentage));
                    } else {
                        percentageLabel.setText("0.00%");
                    }
                } catch (NumberFormatException ex) {
                    percentageLabel.setText("0.00%");
                }
            }
        });

        loadAttendanceData();

        add.addActionListener(e -> {
            if (!role.equals("Student")) {
                try {
                    int wd = Integer.parseInt(workingDays.getText());
                    int ad = Integer.parseInt(attendedDays.getText());
                    if (ad > wd) {
                        JOptionPane.showMessageDialog(this, "Attended days cannot exceed working days");
                        return;
                    }
                    double percentage = (ad * 100.0) / wd;
                    try (Connection conn = DatabaseHelper.getConnection();
                         PreparedStatement pstmt = conn.prepareStatement(
                                 "INSERT INTO attendance (student_id, month, year, working_days, attended_days, percentage) VALUES (?, ?, ?, ?, ?, ?)")) {
                        pstmt.setString(1, studentId.getText());
                        pstmt.setString(2, (String) monthCombo.getSelectedItem());
                        pstmt.setInt(3, Integer.parseInt((String) yearCombo.getSelectedItem()));
                        pstmt.setInt(4, wd);
                        pstmt.setInt(5, ad);
                        pstmt.setDouble(6, percentage);
                        pstmt.executeUpdate();
                        model.addRow(new Object[]{studentId.getText(), monthCombo.getSelectedItem(), yearCombo.getSelectedItem(), wd, ad, String.format("%.2f", percentage)});
                        JOptionPane.showMessageDialog(this, "Attendance added successfully");
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error adding attendance: " + ex.getMessage());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid number format");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Access Denied");
            }
        });

        edit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1 && !role.equals("Student")) {
                try {
                    int wd = Integer.parseInt(workingDays.getText());
                    int ad = Integer.parseInt(attendedDays.getText());
                    if (ad > wd) {
                        JOptionPane.showMessageDialog(this, "Attended days cannot exceed working days");
                        return;
                    }
                    double percentage = (ad * 100.0) / wd;
                    boolean hasChanges = !studentId.getText().equals(model.getValueAt(row, 0)) ||
                                        !monthCombo.getSelectedItem().equals(model.getValueAt(row, 1)) ||
                                        !yearCombo.getSelectedItem().equals(model.getValueAt(row, 2)) ||
                                        wd != Integer.parseInt(model.getValueAt(row, 3).toString()) ||
                                        ad != Integer.parseInt(model.getValueAt(row, 4).toString());
                    if (!hasChanges) {
                        JOptionPane.showMessageDialog(this, "No changes detected");
                        return;
                    }
                    try (Connection conn = DatabaseHelper.getConnection();
                         PreparedStatement pstmt = conn.prepareStatement(
                                 "UPDATE attendance SET working_days = ?, attended_days = ?, percentage = ? WHERE student_id = ? AND month = ? AND year = ?")) {
                        pstmt.setInt(1, wd);
                        pstmt.setInt(2, ad);
                        pstmt.setDouble(3, percentage);
                        pstmt.setString(4, studentId.getText());
                        pstmt.setString(5, (String) monthCombo.getSelectedItem());
                        pstmt.setInt(6, Integer.parseInt((String) yearCombo.getSelectedItem()));
                        int updatedRows = pstmt.executeUpdate();
                        if (updatedRows > 0) {
                            model.setValueAt(studentId.getText(), row, 0);
                            model.setValueAt(monthCombo.getSelectedItem(), row, 1);
                            model.setValueAt(yearCombo.getSelectedItem(), row, 2);
                            model.setValueAt(wd, row, 3);
                            model.setValueAt(ad, row, 4);
                            model.setValueAt(String.format("%.2f", percentage), row, 5);
                            JOptionPane.showMessageDialog(this, "Attendance updated successfully");
                            table.clearSelection();
                        }
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error updating attendance: " + ex.getMessage());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid number format");
                }
            }
        });

        delete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1 && !role.equals("Student")) {
                String studentIdVal = (String) model.getValueAt(row, 0);
                String month = (String) model.getValueAt(row, 1);
                String year = (String) model.getValueAt(row, 2);
                try (Connection conn = DatabaseHelper.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM attendance WHERE student_id = ? AND month = ? AND year = ?")) {
                    pstmt.setString(1, studentIdVal);
                    pstmt.setString(2, month);
                    pstmt.setInt(3, Integer.parseInt(year));
                    pstmt.executeUpdate();
                    model.removeRow(row);
                    JOptionPane.showMessageDialog(this, "Attendance deleted successfully");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error deleting attendance: " + ex.getMessage());
                }
            }
        });

        search.addActionListener(e -> {
            String id = searchId.getText();
            String month = (String) searchMonth.getSelectedItem();
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM attendance WHERE student_id = ? AND month = ?")) {
                pstmt.setString(1, id);
                pstmt.setString(2, month);
                ResultSet rs = pstmt.executeQuery();
                model.setRowCount(0);
                while (rs.next()) {
                    model.addRow(new Object[]{
                            rs.getString("student_id"),
                            rs.getString("month"),
                            rs.getInt("year"),
                            rs.getInt("working_days"),
                            rs.getInt("attended_days"),
                            String.format("%.2f", rs.getDouble("percentage"))
                    });
                }
                if (model.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(this, "No attendance records found");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error searching attendance: " + ex.getMessage());
            }
        });

        back.addActionListener(e -> {
            new Dashboard(role);
            dispose();
        });

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(bgColor);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; inputPanel.add(labels[0], gbc);
        gbc.gridx = 1; inputPanel.add(studentId, gbc);
        gbc.gridx = 0; gbc.gridy = 1; inputPanel.add(labels[1], gbc);
        gbc.gridx = 1; inputPanel.add(monthCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 2; inputPanel.add(labels[2], gbc);
        gbc.gridx = 1; inputPanel.add(yearCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 3; inputPanel.add(labels[3], gbc);
        gbc.gridx = 1; inputPanel.add(workingDays, gbc);
        gbc.gridx = 0; gbc.gridy = 4; inputPanel.add(labels[4], gbc);
        gbc.gridx = 1; inputPanel.add(attendedDays, gbc);
        gbc.gridx = 0; gbc.gridy = 5; inputPanel.add(labels[5], gbc);
        gbc.gridx = 1; inputPanel.add(percentageLabel, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(bgColor);
        buttonPanel.add(add); buttonPanel.add(edit); buttonPanel.add(delete);

        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBackground(bgColor);
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        gbc.gridx = 0; gbc.gridy = 0; searchPanel.add(labels[6], gbc);
        gbc.gridx = 1; searchPanel.add(searchId, gbc);
        gbc.gridx = 0; gbc.gridy = 1; searchPanel.add(labels[7], gbc);
        gbc.gridx = 1; searchPanel.add(searchMonth, gbc);
        gbc.gridx = 2; searchPanel.add(search, gbc);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(bgColor);
        topPanel.add(inputPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        topPanel.add(searchPanel, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.setBackground(bgColor);
        bottomPanel.add(back);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        setVisible(true);
    }

    private void loadAttendanceData() {
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM attendance")) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("student_id"),
                        rs.getString("month"),
                        rs.getInt("year"),
                        rs.getInt("working_days"),
                        rs.getInt("attended_days"),
                        String.format("%.2f", rs.getDouble("percentage"))
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading attendance: " + ex.getMessage());
        }
    }
}

class MarksModule extends JFrame {
    DefaultTableModel model;
    JTable table;
    private final Map<String, Double> gradePoints = new HashMap<>();
    JTextField studentId, subjectCode, subjectName, creditField, searchId;
    JComboBox<Integer> semesterCombo, searchSemester;
    JComboBox<String> gradeCombo;

    MarksModule(String role) {
        setTitle("Marks and GPA Management");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Color bgColor = new Color(255, 245, 238);
        getContentPane().setBackground(bgColor);

        String[] columns = {"Student ID", "Semester", "Subject Code", "Subject Name", "Grade", "Grade Point", "Credit", "Result"};
        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 16));
        table.setForeground(Color.BLACK);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(173, 216, 250));
        table.setRowHeight(30);

        // Custom renderer for Result column
        table.getColumnModel().getColumn(7).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                String result = (String) value;
                if ("Pass".equals(result)) {
                    c.setForeground(Color.GREEN);
                } else if ("Fail".equals(result)) {
                    c.setForeground(Color.RED);
                } else {
                    c.setForeground(Color.BLACK);
                }
                if (isSelected) {
                    c.setBackground(table.getSelectionBackground());
                    c.setForeground(Color.BLACK);
                } else {
                    c.setBackground(Color.WHITE);
                }
                return c;
            }
        });

        studentId = new JTextField(10);
        semesterCombo = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8});
        subjectCode = new JTextField(10);
        subjectName = new JTextField(15);
        gradeCombo = new JComboBox<>(new String[]{"O", "A+", "A", "B+", "B", "C", "F"});
        creditField = new JTextField(5);
        searchId = new JTextField(10);
        searchSemester = new JComboBox<>(new Integer[]{1, 2, 3, 4, 5, 6, 7, 8});

        JButton add = new JButton("Add");
        JButton edit = new JButton("Edit");
        JButton delete = new JButton("Delete");
        JButton search = new JButton("View Mark Sheet");
        JButton generateReport = new JButton("Generate Report");
        JButton back = new JButton("Back to Dashboard");

        JLabel[] labels = {
            new JLabel("Student ID:"), new JLabel("Semester:"), new JLabel("Subject Code:"),
            new JLabel("Subject Name:"), new JLabel("Grade:"), new JLabel("Credit:"),
            new JLabel("Search ID:"), new JLabel("Search Semester:")
        };

        for (JTextField field : new JTextField[]{studentId, subjectCode, subjectName, creditField, searchId}) {
            StudentInformationSystem.applyStyle(field, Color.WHITE);
        }
        StudentInformationSystem.applyStyle(semesterCombo, Color.WHITE);
        StudentInformationSystem.applyStyle(gradeCombo, Color.WHITE);
        StudentInformationSystem.applyStyle(searchSemester, Color.WHITE);
        StudentInformationSystem.applyStyle(add, new Color(144, 238, 144));
        StudentInformationSystem.applyStyle(edit, new Color(135, 206, 250));
        StudentInformationSystem.applyStyle(delete, new Color(255, 182, 193));
        StudentInformationSystem.applyStyle(search, new Color(240, 230, 140));
        StudentInformationSystem.applyStyle(generateReport, new Color(147, 112, 219));
        StudentInformationSystem.applyStyle(back, new Color(216, 191, 216));
        for (JLabel label : labels) StudentInformationSystem.applyStyle(label, bgColor);

        gradePoints.put("O", 10.0);
        gradePoints.put("A+", 9.0);
        gradePoints.put("A", 8.0);
        gradePoints.put("B+", 7.0);
        gradePoints.put("B", 6.0);
        gradePoints.put("C", 5.0);
        gradePoints.put("F", 0.0);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) {
                        studentId.setText((String) model.getValueAt(row, 0));
                        semesterCombo.setSelectedItem(model.getValueAt(row, 1));
                        subjectCode.setText((String) model.getValueAt(row, 2));
                        subjectName.setText((String) model.getValueAt(row, 3));
                        gradeCombo.setSelectedItem(model.getValueAt(row, 4));
                        creditField.setText(model.getValueAt(row, 6).toString());
                    }
                }
            }
        });

        loadMarksData();

        add.addActionListener(e -> {
            if (!role.equals("Student")) {
                try {
                    double credit = Double.parseDouble(creditField.getText());
                    String grade = (String) gradeCombo.getSelectedItem();
                    double gradePoint = gradePoints.get(grade);
                    String result = grade.equals("F") ? "Fail" : "Pass";
                    int semester = (Integer) semesterCombo.getSelectedItem();
                    try (Connection conn = DatabaseHelper.getConnection();
                         PreparedStatement pstmt = conn.prepareStatement(
                                 "INSERT INTO marks (student_id, semester, subject_code, subject_name, grade, grade_point, credit) VALUES (?, ?, ?, ?, ?, ?, ?)")) {
                        pstmt.setString(1, studentId.getText());
                        pstmt.setInt(2, semester);
                        pstmt.setString(3, subjectCode.getText());
                        pstmt.setString(4, subjectName.getText());
                        pstmt.setString(5, grade);
                        pstmt.setDouble(6, gradePoint);
                        pstmt.setDouble(7, credit);
                        pstmt.executeUpdate();
                        model.addRow(new Object[]{studentId.getText(), semester, subjectCode.getText(), subjectName.getText(),
                                grade, gradePoint, credit, result});
                        JOptionPane.showMessageDialog(this, "Subject added successfully");
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error adding marks: " + ex.getMessage());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid number format");
                }
            } else {
                JOptionPane.showMessageDialog(this, "Access Denied");
            }
        });

        edit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1 && !role.equals("Student")) {
                try {
                    double credit = Double.parseDouble(creditField.getText());
                    String grade = (String) gradeCombo.getSelectedItem();
                    double gradePoint = gradePoints.get(grade);
                    String result = grade.equals("F") ? "Fail" : "Pass";
                    int semester = (Integer) semesterCombo.getSelectedItem();
                    boolean hasChanges = !studentId.getText().equals(model.getValueAt(row, 0)) ||
                                        semester != (Integer)model.getValueAt(row, 1) ||
                                        !subjectCode.getText().equals(model.getValueAt(row, 2)) ||
                                        !subjectName.getText().equals(model.getValueAt(row, 3)) ||
                                        !grade.equals(model.getValueAt(row, 4)) ||
                                        credit != (Double)model.getValueAt(row, 6);
                    if (!hasChanges) {
                        JOptionPane.showMessageDialog(this, "No changes detected");
                        return;
                    }
                    try (Connection conn = DatabaseHelper.getConnection();
                         PreparedStatement pstmt = conn.prepareStatement(
                                 "UPDATE marks SET subject_name = ?, grade = ?, grade_point = ?, credit = ? WHERE student_id = ? AND semester = ? AND subject_code = ?")) {
                        pstmt.setString(1, subjectName.getText());
                        pstmt.setString(2, grade);
                        pstmt.setDouble(3, gradePoint);
                        pstmt.setDouble(4, credit);
                        pstmt.setString(5, studentId.getText());
                        pstmt.setInt(6, semester);
                        pstmt.setString(7, subjectCode.getText());
                        int updatedRows = pstmt.executeUpdate();
                        if (updatedRows > 0) {
                            model.setValueAt(studentId.getText(), row, 0);
                            model.setValueAt(semester, row, 1);
                            model.setValueAt(subjectCode.getText(), row, 2);
                            model.setValueAt(subjectName.getText(), row, 3);
                            model.setValueAt(grade, row, 4);
                            model.setValueAt(gradePoint, row, 5);
                            model.setValueAt(credit, row, 6);
                            model.setValueAt(result, row, 7);
                            JOptionPane.showMessageDialog(this, "Subject updated successfully");
                            table.clearSelection();
                        }
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error updating marks: " + ex.getMessage());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid number format");
                }
            }
        });

        delete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1 && !role.equals("Student")) {
                String studentIdVal = (String) model.getValueAt(row, 0);
                int semester = (Integer) model.getValueAt(row, 1);
                String subjectCodeVal = (String) model.getValueAt(row, 2);
                try (Connection conn = DatabaseHelper.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM marks WHERE student_id = ? AND semester = ? AND subject_code = ?")) {
                    pstmt.setString(1, studentIdVal);
                    pstmt.setInt(2, semester);
                    pstmt.setString(3, subjectCodeVal);
                    pstmt.executeUpdate();
                    model.removeRow(row);
                    JOptionPane.showMessageDialog(this, "Subject deleted successfully");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error deleting marks: " + ex.getMessage());
                }
            }
        });

        search.addActionListener(e -> {
            String id = searchId.getText();
            int semester = (Integer) searchSemester.getSelectedItem();
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM marks WHERE student_id = ? AND semester = ?")) {
                pstmt.setString(1, id);
                pstmt.setInt(2, semester);
                ResultSet rs = pstmt.executeQuery();
                model.setRowCount(0);
                StringBuilder markSheet = new StringBuilder("Mark Sheet for Student ID: " + id + ", Semester: " + semester + "\n\n");
                markSheet.append(String.format("%-12s %-30s %-6s %-8s %-6s %-6s\n", "Sub Code", "Subject Name", "Grade", "Point", "Credit", "Result"));
                markSheet.append("-".repeat(80)).append("\n");
                double totalCredits = 0;
                double weightedPoints = 0;
                while (rs.next()) {
                    double credit = rs.getDouble("credit");
                    double gradePoint = rs.getDouble("grade_point");
                    String grade = rs.getString("grade");
                    String result = grade.equals("F") ? "Fail" : "Pass";
                    totalCredits += credit;
                    weightedPoints += credit * gradePoint;
                    model.addRow(new Object[]{
                            rs.getString("student_id"),
                            rs.getInt("semester"),
                            rs.getString("subject_code"),
                            rs.getString("subject_name"),
                            grade,
                            gradePoint,
                            credit,
                            result
                    });
                    markSheet.append(String.format("%-12s %-30s %-6s %-8.1f %-6.1f %-6s\n",
                            rs.getString("subject_code"), rs.getString("subject_name"),
                            grade, gradePoint, credit, result));
                }
                double gpa = totalCredits > 0 ? weightedPoints / totalCredits : 0;
                double cgpa = calculateCGPA(id);
                markSheet.append("-".repeat(80)).append("\n");
                markSheet.append(String.format("Total Credits: %.1f\nGPA: %.2f\nCGPA: %.2f", totalCredits, gpa, cgpa));
                JTextArea textArea = new JTextArea(markSheet.toString());
                textArea.setFont(new Font("Monospaced", Font.PLAIN, 14));
                textArea.setEditable(false);
                JOptionPane.showMessageDialog(this, new JScrollPane(textArea), "Mark Sheet", JOptionPane.INFORMATION_MESSAGE);
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error retrieving mark sheet: " + ex.getMessage());
            }
        });

        generateReport.addActionListener(e -> {
            String id = searchId.getText();
            int semester = (Integer) searchSemester.getSelectedItem();
            if (id.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Please enter a Student ID");
                return;
            }
            try (Connection conn = DatabaseHelper.getConnection();
                 PreparedStatement pstmt = conn.prepareStatement("SELECT * FROM marks WHERE student_id = ? AND semester = ?")) {
                pstmt.setString(1, id);
                pstmt.setInt(2, semester);
                ResultSet rs = pstmt.executeQuery();
                DefaultTableModel reportModel = new DefaultTableModel(
                        new String[]{"Sub Code", "Subject Name", "Credit", "Grade", "Grade Point", "Result"}, 0);
                double totalCredits = 0;
                double weightedPoints = 0;
                while (rs.next()) {
                    double credit = rs.getDouble("credit");
                    double gradePoint = rs.getDouble("grade_point");
                    String grade = rs.getString("grade");
                    String result = grade.equals("F") ? "Fail" : "Pass";
                    totalCredits += credit;
                    weightedPoints += credit * gradePoint;
                    reportModel.addRow(new Object[]{
                            rs.getString("subject_code"),
                            rs.getString("subject_name"),
                            credit,
                            grade,
                            gradePoint,
                            result
                    });
                }
                double gpa = totalCredits > 0 ? weightedPoints / totalCredits : 0;
                double cgpa = calculateCGPA(id);

                JFrame reportFrame = new JFrame("Marks Report - Student ID: " + id + ", Semester: " + semester);
                reportFrame.setExtendedState(JFrame.MAXIMIZED_BOTH);
                reportFrame.setLocationRelativeTo(null);
                reportFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                reportFrame.getContentPane().setBackground(new Color(245, 245, 220));

                JTable reportTable = new JTable(reportModel);
                reportTable.setFont(new Font("Arial", Font.PLAIN, 14));
                reportTable.setForeground(Color.BLACK);
                reportTable.setBackground(Color.WHITE);
                reportTable.setRowHeight(25);
                reportTable.setGridColor(Color.LIGHT_GRAY);
                reportTable.setShowGrid(true);

                // Apply custom renderer to Result column in report table
                reportTable.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
                    @Override
                    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                        String result = (String) value;
                        if ("Pass".equals(result)) {
                            c.setForeground(Color.GREEN);
                        } else if ("Fail".equals(result)) {
                            c.setForeground(Color.RED);
                        } else {
                            c.setForeground(Color.BLACK);
                        }
                        if (isSelected) {
                            c.setBackground(table.getSelectionBackground());
                            c.setForeground(Color.BLACK);
                        } else {
                            c.setBackground(Color.WHITE);
                        }
                        return c;
                    }
                });

                JLabel summaryLabel = new JLabel(String.format(
                        "<html>Total Credits: %.1f<br>GPA: %.2f<br>CGPA: %.2f</html>", totalCredits, gpa, cgpa));
                StudentInformationSystem.applyStyle(summaryLabel, new Color(245, 245, 220));
                summaryLabel.setHorizontalAlignment(SwingConstants.CENTER);

                JButton closeButton = new JButton("Close");
                StudentInformationSystem.applyStyle(closeButton, new Color(216, 191, 216));
                closeButton.addActionListener(e1 -> reportFrame.dispose());

                JPanel topPanel = new JPanel(new BorderLayout());
                topPanel.setBackground(new Color(245, 245, 220));
                topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
                topPanel.add(summaryLabel, BorderLayout.NORTH);
                topPanel.add(new JScrollPane(reportTable), BorderLayout.CENTER);

                JPanel bottomPanel = new JPanel(new FlowLayout());
                bottomPanel.setBackground(new Color(245, 245, 220));
                bottomPanel.add(closeButton);

                reportFrame.add(topPanel, BorderLayout.CENTER);
                reportFrame.add(bottomPanel, BorderLayout.SOUTH);
                reportFrame.setVisible(true);

                if (reportModel.getRowCount() == 0) {
                    JOptionPane.showMessageDialog(this, "No marks found for this student and semester");
                    reportFrame.dispose();
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error generating report: " + ex.getMessage());
            }
        });

        back.addActionListener(e -> {
            new Dashboard(role);
            dispose();
        });

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBackground(bgColor);
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; inputPanel.add(labels[0], gbc);
        gbc.gridx = 1; inputPanel.add(studentId, gbc);
        gbc.gridx = 0; gbc.gridy = 1; inputPanel.add(labels[1], gbc);
        gbc.gridx = 1; inputPanel.add(semesterCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 2; inputPanel.add(labels[2], gbc);
        gbc.gridx = 1; inputPanel.add(subjectCode, gbc);
        gbc.gridx = 0; gbc.gridy = 3; inputPanel.add(labels[3], gbc);
        gbc.gridx = 1; inputPanel.add(subjectName, gbc);
        gbc.gridx = 0; gbc.gridy = 4; inputPanel.add(labels[4], gbc);
        gbc.gridx = 1; inputPanel.add(gradeCombo, gbc);
        gbc.gridx = 0; gbc.gridy = 5; inputPanel.add(labels[5], gbc);
        gbc.gridx = 1; inputPanel.add(creditField, gbc);

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.setBackground(bgColor);
        buttonPanel.add(add); buttonPanel.add(edit); buttonPanel.add(delete);

        JPanel searchPanel = new JPanel(new GridBagLayout());
        searchPanel.setBackground(bgColor);
        searchPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        gbc.gridx = 0; gbc.gridy = 0; searchPanel.add(labels[6], gbc);
        gbc.gridx = 1; searchPanel.add(searchId, gbc);
        gbc.gridx = 0; gbc.gridy = 1; searchPanel.add(labels[7], gbc);
        gbc.gridx = 1; searchPanel.add(searchSemester, gbc);
        gbc.gridx = 2; gbc.gridy = 0; gbc.gridheight = 2; searchPanel.add(search, gbc);
        gbc.gridx = 3; searchPanel.add(generateReport, gbc);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBackground(bgColor);
        topPanel.add(inputPanel, BorderLayout.CENTER);
        topPanel.add(buttonPanel, BorderLayout.SOUTH);
        topPanel.add(searchPanel, BorderLayout.EAST);

        JPanel bottomPanel = new JPanel(new FlowLayout());
        bottomPanel.setBackground(bgColor);
        bottomPanel.add(back);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
        setVisible(true);
    }

    private double calculateGPA(String studentId, int semester) {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT credit, grade_point FROM marks WHERE student_id = ? AND semester = ?")) {
            pstmt.setString(1, studentId);
            pstmt.setInt(2, semester);
            ResultSet rs = pstmt.executeQuery();
            double totalCredits = 0;
            double weightedPoints = 0;
            while (rs.next()) {
                double credit = rs.getDouble("credit");
                double gradePoint = rs.getDouble("grade_point");
                totalCredits += credit;
                weightedPoints += credit * gradePoint;
            }
            return totalCredits > 0 ? weightedPoints / totalCredits : 0;
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error calculating GPA: " + ex.getMessage());
            return 0;
        }
    }

    private double calculateCGPA(String studentId) {
        try (Connection conn = DatabaseHelper.getConnection();
             PreparedStatement pstmt = conn.prepareStatement("SELECT semester, credit, grade_point FROM marks WHERE student_id = ?")) {
            pstmt.setString(1, studentId);
            ResultSet rs = pstmt.executeQuery();
            Map<Integer, Double> semesterWeightedPoints = new HashMap<>();
            Map<Integer, Double> semesterCredits = new HashMap<>();
            while (rs.next()) {
                int semester = rs.getInt("semester");
                double credit = rs.getDouble("credit");
                double gradePoint = rs.getDouble("grade_point");
                semesterWeightedPoints.merge(semester, credit * gradePoint, Double::sum);
                semesterCredits.merge(semester, credit, Double::sum);
            }
            double totalGPA = 0;
            int semesterCount = 0;
            for (int semester : semesterWeightedPoints.keySet()) {
                double credits = semesterCredits.get(semester);
                if (credits > 0) {
                    totalGPA += semesterWeightedPoints.get(semester) / credits;
                    semesterCount++;
                }
            }
            return semesterCount > 0 ? totalGPA / semesterCount : 0;
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error calculating CGPA: " + ex.getMessage());
            return 0;
        }
    }

    private void loadMarksData() {
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM marks")) {
            while (rs.next()) {
                String grade = rs.getString("grade");
                String result = grade.equals("F") ? "Fail" : "Pass";
                model.addRow(new Object[]{
                        rs.getString("student_id"),
                        rs.getInt("semester"),
                        rs.getString("subject_code"),
                        rs.getString("subject_name"),
                        grade,
                        rs.getDouble("grade_point"),
                        rs.getDouble("credit"),
                        result
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading marks: " + ex.getMessage());
        }
    }
}

class FeesModule extends JFrame {
    DefaultTableModel model;
    JTable table;
    JTextField roll, due, paid;

    FeesModule(String role) {
        setTitle("Fees Module");
        setExtendedState(JFrame.MAXIMIZED_BOTH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        Color bgColor = new Color(240, 248, 255);
        getContentPane().setBackground(bgColor);

        String[] columns = {"Roll No", "Amount Due", "Amount Paid", "Balance"};
        model = new DefaultTableModel(columns, 0);
        table = new JTable(model);
        table.setFont(new Font("Arial", Font.PLAIN, 18));
        table.setForeground(Color.BLACK);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(173, 216, 250));
        table.setRowHeight(30);

        roll = new JTextField(5);
        due = new JTextField(5);
        paid = new JTextField(5);
        JButton add = new JButton("Add");
        JButton edit = new JButton("Edit");
        JButton delete = new JButton("Delete");
        JButton back = new JButton("Back to Dashboard");
        JLabel rollLabel = new JLabel("Roll No:");
        JLabel dueLabel = new JLabel("Amount Due:");
        JLabel paidLabel = new JLabel("Amount Paid:");

        StudentInformationSystem.applyStyle(roll, Color.WHITE);
        StudentInformationSystem.applyStyle(due, Color.WHITE);
        StudentInformationSystem.applyStyle(paid, Color.WHITE);
        StudentInformationSystem.applyStyle(add, new Color(144, 238, 144));
        StudentInformationSystem.applyStyle(edit, new Color(173, 216, 230));
        StudentInformationSystem.applyStyle(delete, new Color(255, 182, 193));
        StudentInformationSystem.applyStyle(back, new Color(216, 191, 216));
        StudentInformationSystem.applyStyle(rollLabel, bgColor);
        StudentInformationSystem.applyStyle(dueLabel, bgColor);
        StudentInformationSystem.applyStyle(paidLabel, bgColor);

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) {
                        roll.setText((String) model.getValueAt(row, 0));
                        due.setText(model.getValueAt(row, 1).toString());
                        paid.setText(model.getValueAt(row, 2).toString());
                    }
                }
            }
        });
         loadFeesData();

        add.addActionListener(e -> {
            try {
                double amountDue = Double.parseDouble(due.getText());
                double amountPaid = Double.parseDouble(paid.getText());
                double balance = amountDue - amountPaid;
                try (Connection conn = DatabaseHelper.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(
                             "INSERT INTO fees (roll_no, amount_due, amount_paid, balance) VALUES (?, ?, ?, ?)")) {
                    pstmt.setString(1, roll.getText());
                    pstmt.setDouble(2, amountDue);
                    pstmt.setDouble(3, amountPaid);
                    pstmt.setDouble(4, balance);
                    pstmt.executeUpdate();
                    model.addRow(new Object[]{roll.getText(), amountDue, amountPaid, balance});
                    JOptionPane.showMessageDialog(this, "Fees added successfully");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error adding fees: " + ex.getMessage());
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Invalid number format");
            }
        });

        edit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                try {
                    double amountDue = Double.parseDouble(due.getText());
                    double amountPaid = Double.parseDouble(paid.getText());
                    double balance = amountDue - amountPaid;
                    boolean hasChanges = !roll.getText().equals(model.getValueAt(row, 0)) ||
                                        amountDue != (Double)model.getValueAt(row, 1) ||
                                        amountPaid != (Double)model.getValueAt(row, 2);
                    if (!hasChanges) {
                        JOptionPane.showMessageDialog(this, "No changes detected");
                        return;
                    }
                    try (Connection conn = DatabaseHelper.getConnection();
                         PreparedStatement pstmt = conn.prepareStatement(
                                 "UPDATE fees SET amount_due = ?, amount_paid = ?, balance = ? WHERE roll_no = ?")) {
                        pstmt.setDouble(1, amountDue);
                        pstmt.setDouble(2, amountPaid);
                        pstmt.setDouble(3, balance);
                        pstmt.setString(4, roll.getText());
                        int updatedRows = pstmt.executeUpdate();
                        if (updatedRows > 0) {
                            model.setValueAt(roll.getText(), row, 0);
                            model.setValueAt(amountDue, row, 1);
                            model.setValueAt(amountPaid, row, 2);
                            model.setValueAt(balance, row, 3);
                            JOptionPane.showMessageDialog(this, "Fees updated successfully");
                            table.clearSelection();
                        }
                    }
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error updating fees: " + ex.getMessage());
                } catch (NumberFormatException ex) {
                    JOptionPane.showMessageDialog(this, "Invalid number format");
                }
            }
        });

        delete.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String rollNo = (String) model.getValueAt(row, 0);
                try (Connection conn = DatabaseHelper.getConnection();
                     PreparedStatement pstmt = conn.prepareStatement("DELETE FROM fees WHERE roll_no = ?")) {
                    pstmt.setString(1, rollNo);
                    pstmt.executeUpdate();
                    model.removeRow(row);
                    JOptionPane.showMessageDialog(this, "Fees deleted successfully");
                } catch (SQLException ex) {
                    JOptionPane.showMessageDialog(this, "Error deleting fees: " + ex.getMessage());
                }
            }
        });

        back.addActionListener(e -> {
            new Dashboard(role);
            dispose();
        });

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(bgColor);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(Color.GRAY, 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0; gbc.gridy = 0; panel.add(rollLabel, gbc);
        gbc.gridx = 1; panel.add(roll, gbc);
        gbc.gridx = 0; gbc.gridy = 1; panel.add(dueLabel, gbc);
        gbc.gridx = 1; panel.add(due, gbc);
        gbc.gridx = 0; gbc.gridy = 2; panel.add(paidLabel, gbc);
        gbc.gridx = 1; panel.add(paid, gbc);
        gbc.gridx = 0; gbc.gridy = 3; panel.add(add, gbc);
        gbc.gridx = 1; panel.add(edit, gbc);
        gbc.gridx = 2; panel.add(delete, gbc);
        gbc.gridx = 3; panel.add(back, gbc);

        add(panel, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        setVisible(true);
    }

    private void loadFeesData() {
        try (Connection conn = DatabaseHelper.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM fees")) {
            while (rs.next()) {
                model.addRow(new Object[]{
                        rs.getString("roll_no"),
                        rs.getDouble("amount_due"),
                        rs.getDouble("amount_paid"),
                        rs.getDouble("balance")
                });
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error loading fees: " + ex.getMessage());
        }
    }
}
