package rtss.util;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import org.apache.commons.text.StringEscapeUtils;

import java.awt.BorderLayout;
import java.awt.Frame;

public class UI
{
    public static void messageBox(String text, String buttonTitle)
    {
        Runnable showDialog = () ->
        {
            JDialog dialog = new JDialog((Frame) null, "Message", true);
            dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            JLabel label = new JLabel(
                                      "<html><body style='width: 250px'>" + escapeHtml(text) + "</body></html>",
                                      SwingConstants.CENTER);
            label.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

            JButton closeButton = new JButton(buttonTitle);
            closeButton.addActionListener(e -> dialog.dispose());

            JPanel buttonPanel = new JPanel();
            buttonPanel.add(closeButton);

            dialog.getContentPane().setLayout(new BorderLayout());
            dialog.getContentPane().add(label, BorderLayout.CENTER);
            dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);

            dialog.pack();
            dialog.setLocationRelativeTo(null);
            dialog.setVisible(true);
        };

        if (SwingUtilities.isEventDispatchThread())
        {
            showDialog.run();
        }
        else
        {
            SwingUtilities.invokeLater(showDialog);
        }
    }

    private static String escapeHtml(String s)
    {
        if (s == null)
            return "";
        
        return StringEscapeUtils.escapeHtml4(s);
    }
}