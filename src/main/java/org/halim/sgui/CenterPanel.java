package org.halim.sgui;

import javax.swing.*;
import java.awt.*;

public class CenterPanel extends JPanel {

public final GUI_RootPanel owner;
public final CardLayout layout;

public final WelcomePage welcomePage;

public CenterPanel(GUI_RootPanel owner) {
	this.owner = owner;
	this.layout = new CardLayout();
	this.setLayout(layout);
	
	this.welcomePage = new WelcomePage(owner);
	this.add(welcomePage, "WELCOME");
	
	InstructionsPage instructionsPage = new InstructionsPage();
	this.add(instructionsPage, "INSTRUCTIONS");
}

public void showPage(String pageName) {
	layout.show(this, pageName);
}
}