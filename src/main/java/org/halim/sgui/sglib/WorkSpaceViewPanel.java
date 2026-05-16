package org.halim.sgui.sglib;

import org.halim.dlake.OntologyClass;
import org.halim.hport.OntoDirectoryService;
import javax.swing.*;

public abstract class WorkSpaceViewPanel extends JPanel implements WorkspaceListener {

//// Default abstract No-Ops so views only override what they care about
//@Override
//public void onSelectedLakeChange(OntoDirectoryService.DataLakeService dataLakeService) {
//	// Default No-Op
//}
//
//@Override
//public void onSelectedClassChange(OntologyClass ontologyClass) {
//	// Default No-Op
//}
//
//@Override
//public void onDomainChange(OntoDirectoryService.DataLakeService dataLakeService) {
//	// Default No-Op
//}

/** ComingSoonPanel remains as a stable fallback visual stub */
public static class ComingSoonPanel extends WorkSpaceViewPanel {
	public ComingSoonPanel() {
		setLayout(new java.awt.BorderLayout());
		JLabel label = new JLabel("Coming Soon", SwingConstants.CENTER);
		label.setForeground(Utilities.ELEMENT_TEXT_SECONDARY);
		add(label, java.awt.BorderLayout.CENTER);
		setBackground(Utilities.WP_BG);
	}
	
	// Default abstract No-Ops so views only override what they care about
	@Override
	public void onSelectedLakeChange(OntoDirectoryService.DataLakeService dataLakeService) {
		// Default No-Op
	}
	
	@Override
	public void onSelectedClassChange(OntologyClass ontologyClass) {
		// Default No-Op
	}
	
	@Override
	public void onDomainChange(OntologyClass ontologyClass) {
		// Default No-Op
	}
}
}