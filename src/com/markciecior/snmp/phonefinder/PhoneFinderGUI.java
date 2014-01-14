/*Copyright (C) 2013 Mark Ciecior

    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License along
    with this program; if not, write to the Free Software Foundation, Inc.,
    51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/

package com.markciecior.snmp.phonefinder;

import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.UIManager;

import javax.xml.soap.SOAPBody;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class PhoneFinderGUI extends JPanel{
	
	private static final long serialVersionUID = 1L;

	protected static final String licenseText = "Copyright (C) 2014 Mark Ciecior\r\n\r\n" +

											    "This program is free software; you can redistribute it and/or modify\r\n" +
											    "it under the terms of the GNU General Public License as published by\r\n" +
											    "the Free Software Foundation; either version 2 of the License, or\r\n" +
											    "(at your option) any later version.\r\n\r\n" +
											    
											    "This program is distributed in the hope that it will be useful,\r\n" +
											    "but WITHOUT ANY WARRANTY; without even the implied warranty of\r\n" +
											    "MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the\r\n" +
											    "GNU General Public License for more details.\r\n\r\n" +
											
											    "You should have received a copy of the GNU General Public License along\r\n" +
											    "with this program; if not, write to the Free Software Foundation, Inc.,\r\n" +
											    "51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.\r\n\r\n" +
											    
											    "http://www.gnu.org/licenses/gpl.html\r\n\r\n"+
											    
											    "This program makes use of the jSoup and SNMP4j APIs.\r\n" +
											    "Their licenses can be found below:\r\n\r\n" +
											    "SNMP4j: http://www.snmp4j.org/LICENSE-2_0.txt\r\n" +
											    "jSoup: http://jsoup.org/license";
    
    protected static final String aboutText = "Mark's Phone Finder\r\n" +
    										  "Version 1.1 (14 January 2014)\r\n" +
    										  "by Mark Ciecior, CCIE #28274\r\n" +
    										  "www.github.com/markciecior/PhoneFinder";
    
    protected static final String helpText = "1) Enter the address/hostname and SNMP v2c community string of the\r\n" +
    										 "  access switch whose phones you want to scan.\r\n" +
    										 "2) Enter the address/hostname and user/pass of a CUCM user\r\n" +
    										 "  with access to the AXL API\r\n" +
    										 "3) Click the GO! button.\r\n" +
    										 "4) Wait for the app to find the phones and grab their info\r\n" +
    										 "5) View the output in the center pane\r\n" +
    										 "6) Click the Save button to save the HTML output\r\n" +
    										 "7) Enter a new address/hostname/community string and start again!\r\n";


	static HashMap<String,String> IFINDEX_TO_CAPABILITY = new HashMap<String,String>();
	static HashMap<String,String> IFINDEX_TO_ADDRESS = new HashMap<String,String>();
	static HashMap<String,String> IFINDEX_TO_IFNAME = new HashMap<String,String>();
	static HashMap<String,String> IFNAME_TO_CAPABILITY = new HashMap<String,String>();
	static HashMap<String,String> IFNAME_TO_ADDRESS = new HashMap<String,String>();
	static HashMap<String,String> IFINDEX_TO_NAME = new HashMap<String,String>();
	static HashMap<String,String> IFNAME_TO_NAME = new HashMap<String,String>();
	
	private static boolean TESTING = false;
	private static int maxPhones = 6666;
	
	static JFrame frame;
	
	JTextField switchText;
	JTextField snmpText;
	JTextField cucmAddressText;
	JTextField cucmUserText;
	JTextField cucmPassText;
	JLabel statusLabel;
	
	JEditorPane editorPane;
	JScrollPane editorScrollPane;
	
	private String ACCESS_ADDR;
    private String ACCESS_SNMP;
    private String CUCM_ADDRESS;
    private String CUCM_USER;
    private String CUCM_PASS;
    
    
    
	
	private static String css = "<style type='text/css'>\r\n" + 
								"body,\r\n" + 
								"html {\r\n" +
								"font-family:serif" +
								"margin:0;\r\n" +
								"padding:0;\r\n" +
								"color:#000;\r\n" +
//								"background:#a7a09a;\r\n" +
								"}\r\n"  +
								
								"body {\r\n" +
								"width:1024px;\r\n" +
								"margin:0 auto;\r\n" +
//								"background:#99c;\r\n" +
								"}\r\n" +
								
								"#center {\r\n" +
//								"background:#ddd;\r\n" +
								"}\r\n" +
								"#left {\r\n" +
//								"background:#c99;\r\n" +
								"}\r\n" +
								
								"#center {\r\n" +
								"width:750px;\r\n" +
								"float:right;\r\n" +
//								"background:#9c9;\r\n" +
								"}\r\n" +
								"#left {\r\n" +
								"float:left;\r\n" +
								"width:250px;\r\n" +
//								"background:#c9c;\r\n" +
								"}\r\n" +
								
								"</style>\r\n";

	public static void main(String[] args) throws Exception {
		
		SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                 //Turn off metal's use of bold fonts
        UIManager.put("swing.boldMetal", Boolean.FALSE);
        createAndShowGUI();
            }
        });
	}
	
	private static void createAndShowGUI() {
        //Create and set up the window.
        frame = new JFrame("Mark's Phone Finder");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        
        //Add the File/Help menu bar
        frame.setJMenuBar(createMenu());
        //Add content to the window.
        frame.add(new PhoneFinderGUI());

        //Display the window.
        frame.pack();
        frame.setVisible(true);
    }
	
	public PhoneFinderGUI() {
		super();
		
		try {
    		ACCESS_ADDR = Config.getSetting("accessSwitchAddr");
    		if (ACCESS_ADDR == "null") { ACCESS_ADDR = ""; }
    		
    		ACCESS_SNMP = Config.getSetting("accessSwitchSNMP");
    		if (ACCESS_SNMP == "null") { ACCESS_SNMP = ""; }
    		
    		CUCM_ADDRESS = Config.getSetting("cucmAddress");
    		if (CUCM_ADDRESS == "null") { CUCM_ADDRESS = ""; }
    		
    		CUCM_USER = Config.getSetting("cucmUser");
    		if (CUCM_USER == "null") { CUCM_USER = ""; }
    		
    		CUCM_PASS = Config.getSetting("cucmPass");
    		if (CUCM_PASS == "null") { CUCM_PASS = ""; }
    		
    	} catch (Exception e) {
    		
    	}
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(6,2));
        JLabel switchLabel = new JLabel("Switch IP/Hostname:");
        JLabel snmpLabel = new JLabel("SNMPv2c Community String:");
        switchText = new JTextField(ACCESS_ADDR);
        snmpText = new JTextField(ACCESS_SNMP);
        inputPanel.add(switchLabel);
        inputPanel.add(switchText);
        switchLabel.setLabelFor(switchText);
        inputPanel.add(snmpLabel);
        inputPanel.add(snmpText);
        snmpLabel.setLabelFor(snmpText);
        
        JLabel cucmAddressLabel = new JLabel("CUCM IP/Hostname:");
        JLabel cucmUserLabel = new JLabel("CUCM Username:");
        JLabel cucmPassLabel = new JLabel("CUCM Password:");
        cucmAddressText = new JTextField(CUCM_ADDRESS);
        cucmUserText = new JTextField(CUCM_USER);
        cucmPassText = new JTextField(CUCM_PASS);
        cucmAddressLabel.setLabelFor(cucmAddressText);
        cucmUserLabel.setLabelFor(cucmUserText);
        cucmPassLabel.setLabelFor(cucmPassText);
        inputPanel.add(cucmAddressLabel);
        inputPanel.add(cucmAddressText);
        inputPanel.add(cucmUserLabel);
        inputPanel.add(cucmUserText);
        inputPanel.add(cucmPassLabel);
        inputPanel.add(cucmPassText);
        
        
        JButton startButton = new JButton("GO!");
        startButton.addActionListener(new StartButtonListener());
        inputPanel.add(startButton);
        statusLabel = new JLabel();
        statusLabel.setPreferredSize(new Dimension(300,0));
        inputPanel.add(statusLabel);
        add(inputPanel);
        
        editorPane = new JEditorPane();
        editorPane.setEditable(false);

        //Put the editor pane in a scroll pane.
        editorScrollPane = new JScrollPane(editorPane);
        editorScrollPane.setVerticalScrollBarPolicy(
                        JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        editorScrollPane.setPreferredSize(new Dimension(250, 145));
        editorScrollPane.setMinimumSize(new Dimension(10, 10));
        add(editorScrollPane);
        
        JPanel savePanel = new JPanel();
        JButton saveButton = new JButton("Save Output as HTML");
        saveButton.addActionListener(new SaveButtonListener());
        savePanel.add(saveButton);
        add(savePanel);
	}

	@SuppressWarnings({ "unused", "rawtypes", "unchecked" })
	public static LinkedList findElementValues(SOAPBody body) {
		LinkedList retVal = new LinkedList();
		NodeList rowList = body.getFirstChild().getFirstChild().getChildNodes();
		
		for (int h=0; h < rowList.getLength(); h++) {
			Node row = rowList.item(h);
			//System.out.println("PhoneDesc: " + rowList.item(h).getFirstChild().getTextContent());
			retVal.add(rowList.item(h).getFirstChild().getTextContent());
			//System.out.println("DN: " + rowList.item(h).getFirstChild().getNextSibling().getTextContent());
			retVal.add(rowList.item(h).getFirstChild().getNextSibling().getTextContent());
			//System.out.println("DN Desc: " + rowList.item(h).getFirstChild().getNextSibling().getNextSibling().getTextContent());
			retVal.add(rowList.item(h).getFirstChild().getNextSibling().getNextSibling().getTextContent());
		}
		
		return retVal;
	}
	
	class StartButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e){
			
			Config.setSetting("accessSwitchAddr", switchText.getText());
        	Config.setSetting("accessSwitchSNMP", snmpText.getText());
        	Config.setSetting("cucmAddress", cucmAddressText.getText());
        	Config.setSetting("cucmUser", cucmUserText.getText());
        	Config.setSetting("cucmPass", cucmPassText.getText());
        	
        	statusLabel.setText("Scanning " + switchText.getText() + "...");
        	editorPane.setText("");
        		
			PhoneWorker myWorker = new PhoneWorker();
			myWorker.execute();
			
			//String comm = "select description from numplan where dnorpattern='5705'";
			
			
		}
	}
	
	class SaveButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent e){
		/*try {	
			PrintWriter out = new PrintWriter("/home/mark/Desktop/test.html");
			out.print(editorPane.getText());
			out.close();
		} catch (Exception f){
			f.printStackTrace();
		}*/
			FileWriter writer = null;
			String path = System.getProperty("java.io.tmpdir");
        	try {
        		FileDialog fDialog = new FileDialog(frame, "Save", FileDialog.SAVE);
        		fDialog.setFile("phones.html");
        		fDialog.setVisible(true);
                path = fDialog.getDirectory() + fDialog.getFile();
                  writer = new FileWriter(path);
        	      editorPane.write(writer);
        	    } catch (IOException exception) {
        	      System.err.println("Save oops at path: " + path);
        	      exception.printStackTrace();
        	    } finally {
        	      if (writer != null) {
        	        try {
        	          writer.close();
        	        } catch (IOException exception) {
        	          System.err.println("Error closing writer");
        	          exception.printStackTrace();
        	        }
        	      }
        	    }
			
		}
	}
	
	class PhoneWorker extends SwingWorker<Void,String> {

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		protected Void doInBackground() throws Exception {
			SNMPManager man = new SNMPManager();
			try {
				man.start();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			if (!TESTING) {
				
				String addr = switchText.getText();
				String comm = snmpText.getText();
				
				statusLabel.setText("Querying switch via CDP");
				IFINDEX_TO_ADDRESS = man.getCDPAddress(addr, comm);
				IFINDEX_TO_CAPABILITY = man.getCDPCapability(addr, comm);
				IFINDEX_TO_NAME = man.getCDPName(addr, comm);
				IFINDEX_TO_IFNAME = man.getIfIndexToIfName(addr, comm);
				
				IFNAME_TO_CAPABILITY = man.getIfNameToCapability(IFINDEX_TO_CAPABILITY, IFINDEX_TO_IFNAME);
				IFNAME_TO_ADDRESS = man.getIfNameToCapability(IFINDEX_TO_ADDRESS, IFINDEX_TO_IFNAME);
				IFNAME_TO_NAME = man.getIfNameToCapability(IFINDEX_TO_NAME, IFINDEX_TO_IFNAME);
				
				
				/*save(IFINDEX_TO_ADDRESS, "/home/mark/Desktop/phone/indexToAddress.txt");
				save(IFINDEX_TO_CAPABILITY, "/home/mark/Desktop/phone/indexToCap.txt");
				save(IFINDEX_TO_IFNAME, "/home/mark/Desktop/phone/indexToName.txt");
				save(IFINDEX_TO_NAME, "/home/mark/Desktop/phone/indexTocdpName.txt");
				save(IFNAME_TO_CAPABILITY, "/home/mark/Desktop/phone/ifnameToCap.txt")
				save(IFNAME_TO_ADDRESS, "/home/mark/Desktop/phone/ifnameToAddress.txt")
				save(IFNAME_TO_NAME, "/home/mark/Desktop/phone/ifnameToName.txt")
				 */
			} else {
			
				IFINDEX_TO_ADDRESS = open("/home/mark/Desktop/phone/indexToAddress.txt");
				IFINDEX_TO_CAPABILITY = open("/home/mark/Desktop/phone/indexToCap.txt");
				IFINDEX_TO_IFNAME = open("/home/mark/Desktop/phone/indexToName.txt");
				IFINDEX_TO_NAME = open("/home/mark/Desktop/phone/indexTocdpName.txt");
				IFNAME_TO_CAPABILITY = open("/home/mark/Desktop/phone/ifnameToCap.txt");
				IFNAME_TO_ADDRESS = open("/home/mark/Desktop/phone/ifnameToAddress.txt");
				IFNAME_TO_NAME = open("/home/mark/Desktop/phone/ifnameToName.txt");
			}
			
			
			/*printHashMap(IFNAME_TO_CAPABILITY);
			printHashMap(IFNAME_TO_ADDRESS);
			printHashMap(IFNAME_TO_NAME);*/
			
			Iterator iter = IFNAME_TO_CAPABILITY.entrySet().iterator();
			
			String retVal = "";
			LinkedList<String> phoneList = new LinkedList<String>();
			int j=0;
			retVal += "<html><head>" + css + "</head><title>Mark's Phone Finder</title>\r\n<body>\r\n<div id='center'>\r\n<center><h1>Phones on switch " + switchText.getText() + "</h1></center>\r\n";
			
			try {
			
			while (iter.hasNext()) {
				Map.Entry pairs = (Map.Entry)iter.next();
	    		String myIfName = (String)pairs.getKey();
	    		String myCap = (String)pairs.getValue();
	    		String myAdd;
	    		String myCdpName = IFNAME_TO_NAME.get(myIfName);
	    		try {
	    			myAdd = hexToAddress(IFNAME_TO_ADDRESS.get(myIfName));
	    		} catch (NumberFormatException n) {
	    			myAdd = "";
	    			//continue;
	    		}
	    		
	    		if (isPhone(myCap) && j < maxPhones) {
	    			j++;
	    			statusLabel.setText("Querying CUCM about " + myCdpName);
	    			LinkedList phoneInfo = getPhoneInfo(myCdpName.toUpperCase(Locale.ENGLISH));
	    			phoneList.add(myIfName);
	    			if (phoneInfo.size() >= 1) {
	    				phoneList.add((String) phoneInfo.get(0));
	    			} else {
	    				phoneList.add("");
	    			}
	    			retVal += "<a name='" + myIfName + "'>";
	    			retVal += "<hr><center><b>Phone on Interface " + myIfName + " with CDP name " + myCdpName + " and CDP address " + myAdd + "</b></center>";
	    			for (int l=0; l < phoneInfo.size() / 3; l++) {
	    				retVal += "<center><i>";
	    				if (l == 0) {
	    					retVal += "Phone Description:" + phoneInfo.get(l * 3) + "<br>\r\n";
	    				}
	    				retVal += "DN:" + phoneInfo.get(l * 3 + 1) + ", \r\n";
	    				retVal += "Description:" + phoneInfo.get(l * 3 + 2) + "<br>\r\n";
	    				retVal += "</i></center>\r\n";
	    			}
	    			retVal += "<table>\r\n";
	    			//System.out.println("Looking up " + myAdd);
	    			//System.out.println("retVal Length: " + retVal.length() + " - " + myIfName);
	    			statusLabel.setText("Scraping table from " + myAdd);
	    			retVal += getPhoneTable(myAdd);
	    			retVal += "</table>\r\n";
	    		}
			}
			
			retVal += "</div><div id='left'>";
			retVal += "<u>Interface - Phone Description</u></br>\r\n";
			for (int k=0; k < phoneList.size(); k += 2) {
			//for (String item : phoneList) {
				retVal += "<a href='#" + phoneList.get(k) + "'>" + phoneList.get(k) + " - " + phoneList.get(k+1) +"</br>\r\n";
				//System.out.println("retVal Length: " + retVal.length() + " - " + phoneList.get(k));
			}
			} catch (Exception g) {
				JOptionPane.showMessageDialog(frame, g.getStackTrace(), g.getMessage(), JOptionPane.ERROR_MESSAGE);
				g.printStackTrace(); System.exit(1); 
			}
			retVal += "</div>";
			
			retVal += "</body></html>";
			
			/*try {
				PrintWriter out = new PrintWriter("/home/mark/Desktop/test.html");
				out.print(retVal);
				out.close();
			} catch (Exception e){
				e.printStackTrace();
			}*/
			//System.out.println(retVal);
			publish(retVal);
			
			return null;
		}
		
		public void process(List<String> chunks) {
			editorPane.setText(chunks.get(0));
		}
		
		public void done() {
			statusLabel.setText("Scanning complete");
		}
	}
	
	class CUCMWorker extends SwingWorker<Void,String> {
		
		String command;
		AxlSqlToolkit toolkit;
		
		public CUCMWorker(String command, AxlSqlToolkit toolkit) {
			this.command = command;
			this.toolkit = toolkit;
		}

		@SuppressWarnings("unused")
		@Override
		protected Void doInBackground() throws Exception {
			SOAPBody output = null;
			try {
				//ast.parseArgs(args);
	            toolkit.init();
	            output = toolkit.execute(command).getSOAPPart().getEnvelope().getBody();
	        }
	        catch (Exception e) {
	            e.printStackTrace();
	        }
			//return findElementValues(output);
			//publish
			return null;
		}
		
		public void process(List<String> chunks) {
			System.out.println(chunks.get(0));
		}
		
		public void done() {
			//statusLabel.setText("Scanning complete");
		}
		
	}
	
	@SuppressWarnings({ "rawtypes", "unused" })
	public LinkedList getPhoneInfo(String phoneName) {
		ArrayList retVal = new ArrayList();
		AxlSqlToolkit ast = new AxlSqlToolkit(cucmAddressText.getText(), cucmUserText.getText(), cucmPassText.getText());
		String command = "select d.description as PHONEDESC,n.dnorpattern as DN,n.description as DNDESC from DeviceNumPlanMap dn, device d, NumPlan n where d.name='" + phoneName + "' and dn.fkDevice = d.pkid and dn.fkNumPlan = n.pkid";
		
		SOAPBody output = null;
		try {
			//ast.parseArgs(args);
            ast.init();
            output = ast.execute(command).getSOAPPart().getEnvelope().getBody();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
		return findElementValues(output);
		
		/*CUCMWorker myCWorker = new CUCMWorker(comm, ast);
		retVal = myCWorker.execute();
		
		return retVal;*/
	}
	
	private static JMenuBar createMenu() {
    	JMenuBar menuBar;
        JMenu fileMenu, helpMenu;
        JMenuItem fileExit, helpHowTo, helpLicense, helpAbout;
        
        menuBar = new JMenuBar();
        fileMenu = new JMenu("File");
        helpMenu = new JMenu("Help");
        fileExit = new JMenuItem("Exit");
        helpHowTo = new JMenuItem("How To");
        helpLicense = new JMenuItem("License");
        helpAbout = new JMenuItem("About");
        
        fileExit.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, ActionEvent.ALT_MASK));
        fileExit.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		System.exit(1);
        	}
        });
        
        helpHowTo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_H, ActionEvent.ALT_MASK));
        helpHowTo.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		JOptionPane.showMessageDialog(frame, helpText, "How to Use", JOptionPane.PLAIN_MESSAGE);
        	}
        });
        
        helpLicense.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));
        helpLicense.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		JOptionPane.showMessageDialog(frame, licenseText, "License", JOptionPane.PLAIN_MESSAGE);
        	}
        });
        
        helpAbout.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, ActionEvent.ALT_MASK));
        helpAbout.addActionListener(new ActionListener(){
        	public void actionPerformed(ActionEvent e) {
        		JOptionPane.showMessageDialog(frame, aboutText, "About Mark's Device Collector", JOptionPane.PLAIN_MESSAGE);
        	}
        });

        fileMenu.add(fileExit);
        helpMenu.add(helpHowTo);
        helpMenu.add(helpLicense);
        helpMenu.add(helpAbout);
        menuBar.setVisible(true);
        
        menuBar.add(fileMenu);
        menuBar.add(helpMenu);
        
        return menuBar;

    }
	
	public static boolean isPhone(String cap) {
		String[] hexArray = cap.split("[:]");
		return hexArray[3].equals("90");
	}
	
	public static String hexToAddress(String hexString) throws NumberFormatException{
		String retVal = "";
		String[] hexArray = hexString.split("[:]");
		for (int i=0; i<4; i++) {
			if (i!=0) { retVal += ".";}
			retVal += Integer.parseInt(hexArray[i], 16);
		}
		return retVal;
	}
	
	public static String getPhoneTable(String address) {
		String retVal = "";
		//File input = new File("/home/mark/index.html");
		try {
			//Document doc = Jsoup.parse(input, "UTF-8");
			Document doc = Jsoup.connect("http://" + address + "/").get();
			Element centerDiv = doc.select("div[align=center]").first();
			Element table = centerDiv.select("table").first();
			//System.out.println(table.html());
			retVal = table.html();
		} catch (Exception e) {
			retVal = "";
			//e.printStackTrace();
		}
		return retVal;
	}
	
	@SuppressWarnings("rawtypes")
	public static void printHashMap(HashMap myMap){
    	Iterator iter = myMap.entrySet().iterator();
    	
    	while (iter.hasNext()){
    		Map.Entry pairs = (Map.Entry)iter.next();
    		String myKey = (String) pairs.getKey();
    		Object myValue = pairs.getValue();
    		System.out.println(myKey + " : " + myValue);
    	}
    	
    }

	@SuppressWarnings("rawtypes")
	public static void save(HashMap savedMap, String path) throws NotSerializableException{
		try {
			ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(path));
			oos.writeObject(savedMap);
			oos.flush();
			oos.close();
		} catch (Exception e){
			e.printStackTrace();
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static HashMap open(String path){
		HashMap table = new HashMap();
		
		try {
			ObjectInputStream ois = new ObjectInputStream(new FileInputStream(path));
			table = (HashMap)ois.readObject();
			ois.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return table;
	}

}
