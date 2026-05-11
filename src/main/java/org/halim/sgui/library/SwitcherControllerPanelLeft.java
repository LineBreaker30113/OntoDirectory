package org.halim.sgui.library;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.MatteBorder;
import java.awt.*;
import java.util.ArrayList;

@Deprecated
public class SwitcherControllerPanelLeft extends JPanel {

private static final int padding = 6, hPadding = padding / 2;

private final Border idleBorder, choseBorder, idleBorderHighlater, choseBorderHighlater,
	  thePaddingBorder = new EmptyBorder(padding, padding, padding, padding);

private final JPanel buttonsPanel = new JPanel(), panelsPanel;
private final CardLayout cardLayout = new CardLayout();
private final ArrayList<String> names = new ArrayList<String>();
private final ArrayList<JComponent> panels = new ArrayList<JComponent>();
private final ArrayList<JButton> buttons = new ArrayList<JButton>();

private Color chosenColor, idleColor;
private int viewIndex = 0;

public SwitcherControllerPanelLeft(Color chosenColor, Color idleColor, Color borderColor, Color buttonPanelColor) {
	super();
	this.idleBorderHighlater = new MatteBorder(hPadding, hPadding, hPadding, hPadding, borderColor);
	this.idleBorder = new CompoundBorder(idleBorderHighlater, thePaddingBorder);
	this.choseBorderHighlater = new MatteBorder(hPadding, padding, hPadding, 0, borderColor);
	this.choseBorder = new CompoundBorder(choseBorderHighlater, thePaddingBorder);
	this.chosenColor = chosenColor;
	this.idleColor = idleColor;
	super.setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
	this.panelsPanel = new JPanel(cardLayout);
	this.buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.Y_AXIS));
	this.buttonsPanel.add(Box.createRigidArea(new Dimension(hPadding, 0)));
	this.buttonsPanel.setBackground(buttonPanelColor);
	super.setBackground(buttonPanelColor);
	super.add(buttonsPanel);
	super.add(panelsPanel);
}

public SwitcherControllerPanelLeft() {
	this(
		  Utilities.SCPSB_Chosen,
		  Utilities.SCPSB_Idle,
		  Utilities.SCPSB_Border,
		  Utilities.SCPSB_BG
	);
}

public void changeView(String viewName) {
	cardLayout.show(panelsPanel, viewName);
	
	buttons.get(viewIndex).setBackground(idleColor);
	buttons.get(viewIndex).setBorder(idleBorder);
	buttons.get(viewIndex);
	
	viewIndex = names.indexOf(viewName);
	buttons.get(viewIndex).setBackground(chosenColor);
	buttons.get(viewIndex).setBorder(choseBorder);
}

private @NotNull JButton createSwitchButton(String viewName, JComponent cardPanel) {
	JButton button = new JButton(viewName);
	button.setFocusable(false);
	button.setAlignmentX(Component.RIGHT_ALIGNMENT);
	button.addActionListener((__) -> { changeView(viewName); });
	return button;
}

public JButton addPanel(String viewName, JComponent viewPanel) {
	this.names.add(viewName);
	this.panels.add(viewPanel);
	JButton button = createSwitchButton(viewName, viewPanel);
	this.buttons.add(button);
	this.buttonsPanel.add(button, viewName);
	this.buttonsPanel.add(Box.createRigidArea(new Dimension(hPadding, 0)));
	this.panelsPanel.add(viewPanel, viewName);
	changeView(viewName);
	return button;
}




private static JPanel createColoredPanel(String label, Color color) {
	JPanel panel = new JPanel();
	panel.setBackground(color);
	panel.add(new JLabel("This is the " + label));
	return panel;
}

public static void main(String[] args) {
	SwingUtilities.invokeLater(() -> {
		JFrame frame = new JFrame("Switching Frames Test");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		SwitcherControllerPanelLeft scp = new SwitcherControllerPanelLeft(
			  Utilities.SCPSB_Chosen,
			  Utilities.SCPSB_Idle,
			  Utilities.SCPSB_Border,
			  Utilities.SCPSB_BG);
		scp.addPanel("10", createColoredPanel("10", Color.GRAY));
		scp.addPanel("20", createColoredPanel("20", Color.ORANGE));
		scp.addPanel("30", createColoredPanel("30", Color.RED));
		scp.addPanel("40", createColoredPanel("40", Color.GREEN));
		scp.addPanel("50", createColoredPanel("50", Color.BLUE));
		scp.addPanel("60", createColoredPanel("60", Color.CYAN));
		scp.addPanel("70", createColoredPanel("70", Color.MAGENTA));
		scp.addPanel("80", createColoredPanel("80", Color.YELLOW));
		frame.add(scp);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);
	});
	
}

}
