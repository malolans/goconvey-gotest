import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.editor.SelectionModel;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.VisualPosition;
import com.intellij.openapi.ui.Messages;
import org.apache.commons.lang.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CreateTest extends AnAction {

    public void actionPerformed(AnActionEvent e) {

        String fileName = this.getFileName(e);
        if (!this.isFileSupported(fileName)) {
            return;
        }

        Editor editor = e.getData(LangDataKeys.EDITOR);
        String contents = this.getEditorContents(editor);
        String functionNames = this.getFunctionNamesFromString(contents);

        String lines[] = functionNames.split("\n");

        String command = System.getenv("GOPATH") + "/bin/gotests -w -only=(?i)^(" + StringUtils.join(lines, "|") + ")$ " + fileName;

        Project project = e.getData(PlatformDataKeys.PROJECT);
//        Messages.showMessageDialog(project, command, "Information", Messages.getInformationIcon());

        try {
            Runtime.getRuntime().exec(command);
        } catch (IOException e1) {
            Messages.showMessageDialog(project, e1.getMessage(), "Information", Messages.getInformationIcon());
            e1.printStackTrace();
        }
    }

    private String getFileName(AnActionEvent e) {
        Project project = e.getProject();
        Document currentDoc = FileEditorManager.getInstance(project).getSelectedTextEditor().getDocument();
        VirtualFile currentFile = FileDocumentManager.getInstance().getFile(currentDoc);
        return currentFile.getPath();
    }

    private String getFunctionNamesFromString(String contents) {
        String lines[] = contents.split("\n");

        List<String> contentLines = new ArrayList<>();

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.startsWith("func")) {
                String[] words = line.split("\\s+");
                for (int j = 0; j < words.length; j++) {
                    if (words[j].equalsIgnoreCase("func")) {
                        String funcName = words[j + 1];
                        int braceIndex = funcName.indexOf("(");
                        if (braceIndex != -1) {
                            funcName = funcName.substring(0, braceIndex);
                        }
                        contentLines.add(funcName);
                    }
                }
            }
        }

        return this.arrayToStringConversion(contentLines);
    }

    private boolean isFileSupported(String fileName) {
        return fileName.endsWith(".go") && !fileName.contains("_test.go");
    }

    private String getEditorContents(Editor editor) {
        SelectionModel selectionModel = editor.getSelectionModel();
        Document document = editor.getDocument();

        if (selectionModel.hasSelection()) {
            return getSelectedText(selectionModel, editor);
        }

        return document.getText();
    }

    private String getSelectedText(SelectionModel selectionModel, Editor editor) {
        Document document = editor.getDocument();
        String lines[] = document.getText().split("\n");

        VisualPosition start = selectionModel.getSelectionStartPosition();
        VisualPosition end = selectionModel.getSelectionEndPosition();

        VisualPosition newStart = selectionModel.getSelectionStartPosition();

        while (!lines[newStart.getLine()].startsWith("}")) {
            int offset = newStart.getLine() - 1;
            if (offset < 0) {
                newStart = start;
                break;
            }
            newStart = new VisualPosition(offset, 0);
        }

        List<String> contentLines = new ArrayList<>();

        for (int i = newStart.getLine(); i <= end.getLine(); i++) {
            contentLines.add(lines[i]);
        }

        int diff = contentLines.get(0).length() - contentLines.get(0).trim().length();

        if (diff > 0) {
            for (int i = 0; i < contentLines.size(); i++) {
                contentLines.set(i, this.leftTrim(contentLines.get(i), diff));
            }
        }

        return this.arrayToStringConversion(contentLines);
    }

    private String leftTrim(String content, int amount) {
        if (amount > content.length()) return content;

        return content.substring(amount, content.length());
    }

    private String arrayToStringConversion(List<String> contentLines) {
        return StringUtils.join(contentLines, "\n");
    }
}
