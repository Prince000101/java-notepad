import javax.swing.*;
import javax.swing.undo.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.print.PrinterException;
import java.io.*;
import java.util.regex.*;

public class AdvancedNotepad extends JFrame {
    private JTextArea textArea;
    private JLabel statusBar;
    private JFileChooser fileChooser;
    private File currentFile;
    private UndoManager undoManager = new UndoManager();
    private int fontSize = 14;

    public AdvancedNotepad() {
        super("Untitled - AdvancedNotepad");
        setSize(900, 650);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);

        textArea = new JTextArea();
        textArea.setFont(new Font("Consolas", Font.PLAIN, fontSize));
        textArea.setMargin(new Insets(5, 5, 5, 20));
        textArea.setLineWrap(true);
        textArea.setWrapStyleWord(true);
        textArea.getDocument().addUndoableEditListener(undoManager);
        textArea.addCaretListener(e -> updateStatus());

        JScrollPane scrollPane = new JScrollPane(textArea);
        add(scrollPane, BorderLayout.CENTER);

        statusBar = new JLabel("Ln 1, Col 1");
        statusBar.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
        add(statusBar, BorderLayout.SOUTH);

        fileChooser = new JFileChooser();

        setJMenuBar(createMenuBar());
        setupFontShortcuts();

        setVisible(true);
    }

    private void setupFontShortcuts() {
        InputMap im = textArea.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = textArea.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, InputEvent.CTRL_DOWN_MASK), "increaseFont");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_PLUS, InputEvent.CTRL_DOWN_MASK), "increaseFont");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ADD, InputEvent.CTRL_DOWN_MASK), "increaseFont");

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, InputEvent.CTRL_DOWN_MASK), "decreaseFont");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_SUBTRACT, InputEvent.CTRL_DOWN_MASK), "decreaseFont");

        am.put("increaseFont", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                adjustFontSize(2);
            }
        });

        am.put("decreaseFont", new AbstractAction() {
            public void actionPerformed(ActionEvent e) {
                adjustFontSize(-2);
            }
        });
    }

    private JMenuBar createMenuBar() {
        JMenuBar mb = new JMenuBar();

        JMenu file = new JMenu("File");
        file.add(createMenuItem("New", KeyEvent.VK_N, e -> newFile()));
        file.add(createMenuItem("Open", KeyEvent.VK_O, e -> openFile()));
        file.add(createMenuItem("Save", KeyEvent.VK_S, e -> saveFile()));
        file.add(createMenuItem("Save As...", 0, e -> saveFileAs()));
        file.addSeparator();
        file.add(createMenuItem("Print", 0, e -> printText()));
        file.addSeparator();
        file.add(createMenuItem("Exit", 0, e -> exit()));

        JMenu edit = new JMenu("Edit");
        edit.add(createMenuItem("Undo", KeyEvent.VK_Z, e -> undo()));
        edit.add(createMenuItem("Redo", KeyEvent.VK_Y, e -> redo()));
        edit.addSeparator();
        edit.add(createMenuItem("Cut", KeyEvent.VK_X, e -> textArea.cut()));
        edit.add(createMenuItem("Copy", KeyEvent.VK_C, e -> textArea.copy()));
        edit.add(createMenuItem("Paste", KeyEvent.VK_V, e -> textArea.paste()));
        edit.addSeparator();
        edit.add(createMenuItem("Find/Replace", KeyEvent.VK_F, e -> new FindReplaceDialog(this).setVisible(true)));

        JMenu format = new JMenu("Format");
        JMenu fontSizeMenu = new JMenu("Font Size");
        fontSizeMenu.add(new JMenuItem(new AbstractAction("Increase") {
            public void actionPerformed(ActionEvent e) { adjustFontSize(2); }
        }));
        fontSizeMenu.add(new JMenuItem(new AbstractAction("Decrease") {
            public void actionPerformed(ActionEvent e) { adjustFontSize(-2); }
        }));
        fontSizeMenu.add(new JMenuItem(new AbstractAction("Reset") {
            public void actionPerformed(ActionEvent e) { setFontSize(14); }
        }));
        format.add(fontSizeMenu);

        JMenu view = new JMenu("View");
        JCheckBoxMenuItem showStatus = new JCheckBoxMenuItem("Show Status Bar", true);
        showStatus.addActionListener(e -> statusBar.setVisible(showStatus.isSelected()));
        view.add(showStatus);

        JMenu help = new JMenu("Help");
        help.add(new JMenuItem(new AbstractAction("About") {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(AdvancedNotepad.this,
                        "<html><center><b>AdvancedNotepad</b><br>Created by Prince<br>Expert Academy, 2025<br>Java Project</center></html>",
                        "About", JOptionPane.INFORMATION_MESSAGE);
            }
        }));

        mb.add(file);
        mb.add(edit);
        mb.add(format);
        mb.add(view);
        mb.add(help);
        return mb;
    }

    private JMenuItem createMenuItem(String title, int key, ActionListener act) {
        JMenuItem item = new JMenuItem(title);
        item.addActionListener(act);
        if (key != 0) {
            item.setAccelerator(KeyStroke.getKeyStroke(key, InputEvent.CTRL_DOWN_MASK));
        }
        return item;
    }

    private void newFile() {
        if (!confirmSave()) return;
        textArea.setText("");
        currentFile = null;
        setTitle("Untitled - AdvancedNotepad");
    }

    private void openFile() {
        if (!confirmSave()) return;
        if (fileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            try (BufferedReader reader = new BufferedReader(new FileReader(currentFile))) {
                textArea.read(reader, null);
                setTitle(currentFile.getName() + " - AdvancedNotepad");
                undoManager.discardAllEdits();
            } catch (IOException ex) {
                showError("Could not open file.");
            }
        }
    }

    private void saveFile() {
        if (currentFile == null) {
            saveFileAs();
        } else {
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile))) {
                textArea.write(writer);
            } catch (IOException ex) {
                showError("Could not save file.");
            }
        }
    }

    private void saveFileAs() {
        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            currentFile = fileChooser.getSelectedFile();
            saveFile();
            setTitle(currentFile.getName() + " - AdvancedNotepad");
        }
    }

    private boolean confirmSave() {
        if (textArea.getText().isEmpty()) return true;
        int option = JOptionPane.showConfirmDialog(this, "Save changes?", "Confirm", JOptionPane.YES_NO_CANCEL_OPTION);
        if (option == JOptionPane.CANCEL_OPTION) return false;
        if (option == JOptionPane.YES_OPTION) saveFile();
        return true;
    }

    private void printText() {
        try {
            textArea.print();
        } catch (PrinterException e) {
            showError("Printing failed.");
        }
    }

    private void exit() {
        if (confirmSave()) System.exit(0);
    }

    private void undo() {
        if (undoManager.canUndo()) undoManager.undo();
    }

    private void redo() {
        if (undoManager.canRedo()) undoManager.redo();
    }

    private void adjustFontSize(int delta) {
        setFontSize(fontSize + delta);
    }

    private void setFontSize(int size) {
        fontSize = Math.max(8, size);
        textArea.setFont(new Font(textArea.getFont().getName(), Font.PLAIN, fontSize));
    }

    private void updateStatus() {
        try {
            int pos = textArea.getCaretPosition();
            int line = textArea.getLineOfOffset(pos);
            int col = pos - textArea.getLineStartOffset(line);
            statusBar.setText("Ln " + (line + 1) + ", Col " + (col + 1));
        } catch (BadLocationException e) {
            statusBar.setText("Ln 1, Col 1");
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(this, message, "Error", JOptionPane.ERROR_MESSAGE);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(AdvancedNotepad::new);
    }

    // Optional: Add FindReplaceDialog class if needed
    class FindReplaceDialog extends JDialog {
        private JTextField findField, replaceField;

        public FindReplaceDialog(JFrame parent) {
            super(parent, "Find/Replace", false);
            setLayout(new GridLayout(3, 2));
            setSize(400, 150);
            setLocationRelativeTo(parent);

            findField = new JTextField();
            replaceField = new JTextField();

            add(new JLabel("Find:"));
            add(findField);
            add(new JLabel("Replace With:"));
            add(replaceField);

            JButton findBtn = new JButton("Find");
            JButton replaceBtn = new JButton("Replace");

            findBtn.addActionListener(e -> findText());
            replaceBtn.addActionListener(e -> replaceText());

            add(findBtn);
            add(replaceBtn);
        }

        private void findText() {
            String text = textArea.getText();
            String toFind = findField.getText();
            Pattern pattern = Pattern.compile(Pattern.quote(toFind));
            Matcher matcher = pattern.matcher(text);
            if (matcher.find()) {
                textArea.setSelectionStart(matcher.start());
                textArea.setSelectionEnd(matcher.end());
                textArea.requestFocus();
            }
        }

        private void replaceText() {
            String toFind = findField.getText();
            String toReplace = replaceField.getText();
            textArea.setText(textArea.getText().replaceFirst(Pattern.quote(toFind), Matcher.quoteReplacement(toReplace)));
        }
    }
}
