package com.reverseXSL.util;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
 
public class __WindowMenu extends JFrame implements ActionListener {
	private static final long serialVersionUID = 6747236815373524606L;
	
	
// Use instance variables to keep track of menu items and popup menus
JMenuItem thinkItem = new JMenuItem("Think", new ImageIcon("brain.gif"));
JMenuItem copyItem = new JMenuItem("Copy");
JMenuItem newItem = new JMenuItem("New");
JMenuItem openItem = new JMenuItem("Open");
JMenuItem saveAsItem = new JMenuItem("Save As");
JMenuItem findItem = new JMenuItem("Find");
JMenuItem replaceItem = new JMenuItem("Replace");

// Some items for use in the Settings menu
JMenuItem appleItem = new JRadioButtonMenuItem("Apples");
JMenuItem orangeItem = new JRadioButtonMenuItem("Oranges");
JMenuItem bannanaItem = new JRadioButtonMenuItem("Bannanas");

// Use instance variables to keep track of a popup menu and its items
JPopupMenu popupMenu = new JPopupMenu();
JMenuItem helpItem = new JMenuItem("help");
JMenuItem inspectItem = new JMenuItem("inspect");

// Add a constructor for our frame.
public __WindowMenu(String title) {

super(title);

// Create a menu bars and its pull-down menus
JMenuBar menuBar = new JMenuBar();
JMenu fileMenu = new JMenu("File");
JMenu editMenu = new JMenu("Edit");
JMenu settingsMenu = new JMenu("Settings");
JMenu searchMenu = new JMenu("Search");

// set menu accelerators for Alt keys
fileMenu.setMnemonic('F');
editMenu.setMnemonic('E');
settingsMenu.setMnemonic('S');
newItem.setMnemonic('N');
openItem.setMnemonic('O');

// set receiver as the object to handle menu item selection requests
newItem.addActionListener(this);
openItem.addActionListener(this);
saveAsItem.addActionListener(this);
thinkItem.addActionListener(this);
copyItem.addActionListener(this);
findItem.addActionListener(this);
replaceItem.addActionListener(this);
helpItem.addActionListener(this);
inspectItem.addActionListener(this);

ButtonGroup fruits = new ButtonGroup();
fruits.add(appleItem);
fruits.add(orangeItem);
fruits.add(bannanaItem);

// add menu items in the menus
fileMenu.add(newItem);
fileMenu.add(openItem);
fileMenu.add(saveAsItem);
editMenu.add(thinkItem);
editMenu.add(copyItem);
searchMenu.add(findItem);
searchMenu.add(replaceItem);
settingsMenu.add(appleItem);
settingsMenu.add(orangeItem);
settingsMenu.add(bannanaItem);

// add items to the popup menu
popupMenu.add(helpItem);
popupMenu.add(inspectItem);

//create the cascaded menu
editMenu.add(searchMenu);

// add menus to the menu bar
menuBar.add(fileMenu);
menuBar.add(editMenu);
menuBar.add(settingsMenu);

// add menu bar to window and popup menu to the window pane
setJMenuBar(menuBar);

// register the event handler for the popup menu
getContentPane().addMouseListener(new MouseAdapter() {
public void mouseReleased(MouseEvent event){
if (event.isPopupTrigger())
popupMenu.show(event.getComponent(), event.getX(), event.getY());
}
});

// Set the background of the frame
setBackground(Color.lightGray);

}
// dispatch menu selections
public void actionPerformed(ActionEvent event){

if (event.getSource() == newItem)
reactToNewMenuSelection();
else if (event.getSource() == openItem)
reactToOpenMenuSelection();
else if (event.getSource() == saveAsItem)
reactToSaveAsMenuSelection();
else if (event.getSource() == copyItem)
reactToCopyMenuSelection();
else if (event.getSource() == thinkItem)
reactToThinkMenuSelection();
else if (event.getSource() == findItem)
reactToFindMenuSelection();
else if (event.getSource() == replaceItem)
reactToReplaceMenuSelection();
else if (event.getSource() == helpItem)
reactToHelpMenuSelection();
else if (event.getSource() == inspectItem)
reactToInspectMenuSelection();

}
// Here are all the react methods for the menu items
public void reactToNewMenuSelection() {
System.out.println("reacting to NEW selection from menu");
}
public void reactToOpenMenuSelection() {
System.out.println("reacting to OPEN selection from menu");
}
public void reactToSaveAsMenuSelection() {
System.out.println("reacting to SAVE AS selection from menu");
}
public void reactToThinkMenuSelection() {
System.out.println("reacting to THINK selection from menu");
}
public void reactToCopyMenuSelection() {
System.out.println("reacting to COPY selection from menu");
}
public void reactToFindMenuSelection() {
System.out.println("reacting to FIND selection from menu");
}
public void reactToReplaceMenuSelection() {
System.out.println("reacting to REPLACE selection from menu");
}
public void reactToHelpMenuSelection() {
System.out.println("reacting to HELP selection from popup menu");
}
public void reactToInspectMenuSelection() {
System.out.println("reacting to INSPECT selection from popup menu");
}
public static void main(String args[]) {
__WindowMenu frame = new __WindowMenu("FileMenu Example");
frame.setDefaultCloseOperation(EXIT_ON_CLOSE);
frame.setSize(300, 300);
frame.setVisible(true);
}

}
