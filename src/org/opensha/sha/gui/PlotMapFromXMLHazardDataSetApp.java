/*******************************************************************************
 * Copyright 2009 OpenSHA.org in partnership with
 * the Southern California Earthquake Center (SCEC, http://www.scec.org)
 * at the University of Southern California and the UnitedStates Geological
 * Survey (USGS; http://www.usgs.gov)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package org.opensha.sha.gui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.Collator;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ListIterator;
import java.util.StringTokenizer;

import javax.swing.JApplet;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;
import org.opensha.commons.data.xyz.ArbDiscrGeoDataSet;
import org.opensha.commons.data.xyz.ArbDiscrXYZ_DataSet;
import org.opensha.commons.data.xyz.XYZ_DataSet;
import org.opensha.commons.geo.GriddedRegion;
import org.opensha.commons.geo.Location;
import org.opensha.commons.mapping.gmt.GMT_MapGenerator;
import org.opensha.commons.mapping.gmt.gui.GMT_MapGuiBean;
import org.opensha.commons.mapping.gmt.gui.ImageViewerWindow;
import org.opensha.commons.param.ParameterList;
import org.opensha.commons.param.editor.impl.ConstrainedStringParameterEditor;
import org.opensha.commons.param.editor.impl.ParameterListEditor;
import org.opensha.commons.param.event.ParameterChangeEvent;
import org.opensha.commons.param.event.ParameterChangeListener;
import org.opensha.commons.param.impl.StringParameter;
import org.opensha.commons.util.FileUtils;
import org.opensha.sha.calc.hazardMap.MakeXYZFromHazardMapDir;
import org.opensha.sha.gui.beans.IMLorProbSelectorGuiBean;


/**
 * <p>Title: PlotMapFromHazardDataSetApp </p>
 * <p>Description: This applet is needed for viewing the data sets generated by
 * HazardMapApplet. It connects to servlet hosted on web server gravity.usc.edu
 * gets all HazardMap datasets existing on server, selects one of the datasets.
 * User also has the option of selecting subset of the region for selected
 * dataset. He then sets the GMT parameters and punches "Make Map" button,
 * which will contact the servlet to create the map.</p>
 * @author : Nitin Gupta & Vipin Gupta
 * @version 1.0
 */


public class PlotMapFromXMLHazardDataSetApp extends JApplet implements ParameterChangeListener, ActionListener {
	public static String SERVLET_URL  = "http://gravity.usc.edu/OpenSHA/servlet/HazardMapXMLViewerServlet";
	private boolean isStandalone = false;
	Border border1;

	// paramter list and editor to be made for specifyinh min/max lat/lon
	//and gridspacing
	ParameterList sitesParamList ;
	ParameterListEditor sitesEditor;

	Document localDoc = null;
	GriddedRegion localRegion = null;
	String localDir = "";

	ArrayList<Document> serverDocs = new ArrayList<Document>();
	ArrayList<GriddedRegion> serverRegions = new ArrayList<GriddedRegion>();
//	ArrayList<HazardMapJob> jobs = new ArrayList<HazardMapJob>();

	// parameter names for min/max lat/lon and gridspacing
	private final static String MIN_LAT_PARAM_NAME = "Min Lat";
	private final static String MAX_LAT_PARAM_NAME = "Max Lat";
	private final static String MIN_LON_PARAM_NAME = "Min Lon";
	private final static String MAX_LON_PARAM_NAME = "Max Lon";
	private final static String GRIDSPACING_PARAM_NAME = "GridSpacing";
	private final static String SITES_TITLE = "Choose Region";

	// message to display if no data exits
	private static final String NO_DATA_EXISTS = "No Hazard Map Data Exists";
	// title of the window
	private static final String TITLE = "Hazard Map Viewer";

	// width and height
	private static final int W = 950;
	private static final int H = 750;

	// gui beans used here
	private IMLorProbSelectorGuiBean imlProbGuiBean;
	private GMT_MapGuiBean mapGuiBean;

	//formatting of the text double Decimal numbers for 2 places of decimal.
	DecimalFormat d= new DecimalFormat("0.00##");
	// default insets
	private Insets defaultInsets = new Insets( 4, 4, 4, 4 );
	private JPanel jPanel1 = new JPanel();
	private JSplitPane siteSplitPane = new JSplitPane();
	private JPanel dataSetPanel = new JPanel();
	private JPanel gmtPanel = new JPanel();
	private JButton mapButton = new JButton();
	private JComboBox dataSetCombo = new JComboBox();
	private JSplitPane gmtSplitPane = new JSplitPane();
	private JPanel sitePanel = new JPanel();
	private JLabel jLabel2 = new JLabel();
	private JButton refreshButton = new JButton();
	private JLabel jLabel1 = new JLabel();
	private JPanel imlProbPanel = new JPanel();
	private JSplitPane mainSplitPane = new JSplitPane();
	private BorderLayout borderLayout1 = new BorderLayout();
	private GridBagLayout gridBagLayout1 = new GridBagLayout();
	private GridBagLayout gridBagLayout3 = new GridBagLayout();
	private GridBagLayout gridBagLayout4 = new GridBagLayout();
	private GridBagLayout gridBagLayout5 = new GridBagLayout();
	private JScrollPane metadataScrollPane = new JScrollPane();
	private JTextArea dataSetText = new JTextArea();
	private GridBagLayout gridBagLayout2 = new GridBagLayout();

	JPanel selectPanel = new JPanel(new BorderLayout());
	JPanel serverSelectPanel = new JPanel(new BorderLayout());
	JPanel localSelectPanel = new JPanel(new BorderLayout());

	JTextField localFileField = new JTextField();

	public final static String SOURCE_PARAM_NAME = "Data Source";
	public final static String SOURCE_LOCAL = "Local Data";
	public final static String SOURCE_SERVER = "Server Data";

	StringParameter fileSource;
	JButton fileChooserButton = new JButton("Browse");
	ConstrainedStringParameterEditor sourceEdit;

	JFileChooser fileChooser;

	//Get a parameter value
	public String getParameter(String key, String def) {
		return isStandalone ? System.getProperty(key, def) :
			(getParameter(key) != null ? getParameter(key) : def);
	}

	//Construct the applet
	public PlotMapFromXMLHazardDataSetApp() {
	}

	//Initialize the applet
	public void init() {
		try {
			loadDataSets();
			jbInit();
			this.initIML_ProbGuiBean();
			this.initMapGuiBean();
			fileSource.setValue(SOURCE_SERVER);
			sourceEdit.refreshParamEditor();
//			this.par
			selectPanel.add(serverSelectPanel, BorderLayout.CENTER);
			addDataInfo();
			fillLatLonAndGridSpacing();
		}
		catch(Exception e) {
//			ExceptionWindow bugWindow = new ExceptionWindow(this,e.getStackTrace(),"Problem occured "+
//			"while initializing the application");
//			bugWindow.setVisible(true);
//			bugWindow.pack();
			e.printStackTrace();
		}
	}

	//Component initialization
	private void jbInit() throws Exception {
		
		border1 = new EtchedBorder(EtchedBorder.RAISED,new Color(248, 254, 255),new Color(121, 124, 136));
		this.getContentPane().setLayout(borderLayout1);
		jPanel1.setLayout(gridBagLayout1);
		siteSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		dataSetPanel.setLayout(new BorderLayout());
		gmtPanel.setLayout(gridBagLayout5);
		mapButton.setForeground(new Color(80, 80, 133));
		mapButton.setText("Make Map");
		mapButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				mapButton_actionPerformed(e);
			}
		});
		dataSetCombo.setForeground(new Color(80, 80, 133));
		dataSetCombo.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dataSetCombo_actionPerformed(e);
			}
		});
		gmtSplitPane.setLeftComponent(siteSplitPane);
		gmtSplitPane.setRightComponent(gmtPanel);
		sitePanel.setLayout(gridBagLayout4);
		jLabel2.setForeground(new Color(80, 80, 133));
		jLabel2.setText("Data Set Info:");
		refreshButton.setText("Refresh");
		refreshButton.setForeground(new Color(80, 80, 133));
		refreshButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(ActionEvent e) {
				refreshButton_actionPerformed(e);
			}
		});
		jLabel1.setForeground(new Color(80, 80, 133));
		jLabel1.setText("Choose Data Set:");
		imlProbPanel.setLayout(gridBagLayout3);
		mainSplitPane.setMinimumSize(new Dimension(50, 578));
		mainSplitPane.setBottomComponent(gmtSplitPane);
		mainSplitPane.setLastDividerLocation(150);
		mainSplitPane.setLeftComponent(null);
		mainSplitPane.setRightComponent(gmtSplitPane);
		dataSetText.setBorder(border1);
		dataSetText.setLineWrap(true);
		dataSetPanel.setMinimumSize(new Dimension(50, 581));
		this.getContentPane().add(jPanel1, BorderLayout.CENTER);
		jPanel1.add(mainSplitPane,  new GridBagConstraints(0, 0, 1, 1, 1.0, 1.0
				,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(10, 10, 10, 9), 600, 543));
		gmtSplitPane.add(gmtPanel, JSplitPane.RIGHT);
		gmtSplitPane.add(siteSplitPane, JSplitPane.LEFT);
		siteSplitPane.add(sitePanel, JSplitPane.LEFT);
		siteSplitPane.add(imlProbPanel, JSplitPane.RIGHT);
		mainSplitPane.add(dataSetPanel, JSplitPane.TOP);
		mainSplitPane.add(gmtSplitPane, JSplitPane.BOTTOM);

		ArrayList<String> sources = new ArrayList<String>();
		sources.add(SOURCE_LOCAL);
		sources.add(SOURCE_SERVER);

		fileSource = new StringParameter(SOURCE_PARAM_NAME, sources);
		fileSource.addParameterChangeListener(this);

		sourceEdit = new ConstrainedStringParameterEditor(fileSource);

		selectPanel.add(sourceEdit, BorderLayout.NORTH);

		serverSelectPanel.add(dataSetCombo, BorderLayout.EAST);
		serverSelectPanel.add(jLabel1, BorderLayout.WEST);
		serverSelectPanel.add(refreshButton, BorderLayout.SOUTH);

		localSelectPanel.add(localFileField, BorderLayout.CENTER);
		localSelectPanel.add(fileChooserButton, BorderLayout.EAST);
		fileChooserButton.addActionListener(this);

		localFileField.setEditable(false);

//		localSelectPanel.add(comp)

//		dataSetPanel.add(dataSetCombo,  new GridBagConstraints(1, 0, 2, 1, 1.0, 0.0
//		,GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(24, 6, 0, 37), 18, 1));
//		dataSetPanel.add(refreshButton,  new GridBagConstraints(2, 1, 1, 1, 0.0, 0.0
//		,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 44), 41, 1));
//		dataSetPanel.add(mapButton,  new GridBagConstraints(0, 4, 3, 1, 0.0, 0.0
//		,GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(17, 81, 24, 133), 29, 11));
//		dataSetPanel.add(jLabel2,  new GridBagConstraints(0, 2, 2, 1, 0.0, 0.0
//		,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(20, 15, 0, 0), 74, 6));
//		dataSetPanel.add(jLabel1,  new GridBagConstraints(0, 0, 1, 1, 0.0, 0.0
//		,GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(24, 15, 0, 0), 17, 4));
//		dataSetPanel.add(metadataScrollPane,  new GridBagConstraints(0, 3, 3, 1, 1.0, 1.0
//		,GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 15, 0, 18), 0, 354));
		dataSetPanel.add(selectPanel, BorderLayout.NORTH);
		dataSetPanel.add(metadataScrollPane, BorderLayout.CENTER);
		dataSetPanel.add(mapButton, BorderLayout.SOUTH);
		metadataScrollPane.getViewport().add(dataSetText, null);
		siteSplitPane.setDividerLocation(300);
		gmtSplitPane.setDividerLocation(280);
		mainSplitPane.setDividerLocation(375);

	}



	//Get Applet information
	public String getAppletInfo() {
		return "Applet Information";
	}

	//Main method
	public static void main(String[] args) {
		PlotMapFromXMLHazardDataSetApp application = new PlotMapFromXMLHazardDataSetApp();
		application.isStandalone = true;
		Frame frame;
		frame = new Frame() {
			protected void processWindowEvent(WindowEvent e) {
				super.processWindowEvent(e);
				if (e.getID() == WindowEvent.WINDOW_CLOSING) {
					System.exit(0);
				}
			}
			public synchronized void setTitle(String title) {
				super.setTitle(title);
				enableEvents(AWTEvent.WINDOW_EVENT_MASK);
			}
		};
		frame.setTitle(TITLE);
		frame.add(application, BorderLayout.CENTER);
		application.init();
		application.start();
		frame.setSize(W,H);
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation((d.width - frame.getSize().width) / 2, (d.height - frame.getSize().height) / 2);
		frame.setVisible(true);
	}


	//static initializer for setting look & feel
	static {
		String osName = System.getProperty("os.name");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch(Exception e) {
		}
	}



	/**
	 * Load all the available data sets by checking the data sets directory
	 */
	private void loadDataSets() {
		try{

			URL hazardMapViewerServlet = new URL(this.SERVLET_URL);

			URLConnection servletConnection = hazardMapViewerServlet.openConnection();

			// inform the connection that we will send output and accept input
			servletConnection.setDoInput(true);
			servletConnection.setDoOutput(true);

			// Don't use a cached version of URL connection.
			servletConnection.setUseCaches (false);
			servletConnection.setDefaultUseCaches (false);
			// Specify the content type that we will send binary data
			servletConnection.setRequestProperty ("Content-Type","application/octet-stream");

			ObjectOutputStream outputToServlet = new
			ObjectOutputStream(servletConnection.getOutputStream());

			// send the flag to servlet indicating to load the names of available datatsets
			outputToServlet.writeObject(org.opensha.sha.gui.servlets.HazardMapViewerServlet.GET_DATA);

			outputToServlet.flush();
			outputToServlet.close();

			// Receive the "destroy" from the servlet after it has received all the data
			ObjectInputStream inputToServlet = new
			ObjectInputStream(servletConnection.getInputStream());
			
			serverDocs.clear();

			serverDocs=(ArrayList<Document>)inputToServlet.readObject();

			inputToServlet.close();

		}catch (Exception e) {
			e.printStackTrace();
//			ExceptionWindow bugWindow = new ExceptionWindow(this,e.getStackTrace(), getParametersInfo());
//			bugWindow.setVisible(true);
//			bugWindow.pack();
//			System.out.println("Exception in connection with servlet:" +e);
		}

		// fill the combo box with available data sets
		dataSetCombo.removeAllItems();
		serverRegions.clear();
			
		Collections.sort(serverDocs, new HazardXMLDocumentComparator());
		
		for (Document doc : serverDocs) {
			if (doc == null)
				System.out.println("DOC IS NULL!");
			Element root = doc.getRootElement();
			if (root == null)
				System.out.println("ROOT IS NULL!");
//			XMLWriter writer;
//
//			OutputFormat format = OutputFormat.createPrettyPrint();
//			try {
//				writer = new XMLWriter(System.out, format);
//				writer.write(doc);
//				writer.close();
//			} catch (UnsupportedEncodingException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			HazardMapJob job = HazardMapJob.fromXMLMetadata(root.element(HazardMapJob.XML_METADATA_NAME));
			Element jobElem = root.element("hazardMapJob");
			String jobName = jobElem.attributeValue("jobName");
			GriddedRegion region = GriddedRegion.fromXMLMetadata(root.element(GriddedRegion.XML_METADATA_NAME));
			serverRegions.add(region);
//			jobs.add(job);
			this.dataSetCombo.addItem(jobName);
		}
	}


	private String getParametersInfo(){

		String metadata = "";

//		metadata = "Selected Dataset Name : " +(String)this.dataSetCombo.getSelectedItem()+"\n\n"+

//		"Region Param List: "+"\n"+
//		"----------------"+"\n"+
//		sitesEditor.getVisibleParametersCloned().getParameterListMetadataString()+"\n"+
//		"\n"+"Map Type Param List: "+"\n"+
//		"---------------"+"\n"+
//		imlProbGuiBean.getVisibleParametersCloned().getParameterListMetadataString()+"\n"+
//		"\n"+"GMT Param List: "+"\n"+
//		"--------------------"+"\n"+
//		mapGuiBean.getVisibleParametersCloned().getParameterListMetadataString() ;

		return  metadata;

	}


	/**
	 * It will read the sites.info file and fill the min and max Lat and Lon
	 */
	private void fillLatLonAndGridSpacing() {

		String val = (String)fileSource.getValue();
		System.out.println("File Source Val: " + val);
		GriddedRegion region = null;
		if (val.equals(SOURCE_LOCAL)) {
			if (localRegion == null)
				return;
			region = localRegion;
		} else {
			if (serverRegions.size() == 0)
				return;
			region = serverRegions.get(dataSetCombo.getSelectedIndex());
		}
		
		System.out.println("Adding region info...");

		// get the min and max lat and lat spacing
		double minLat = region.getMinGridLat();
		double maxLat = region.getMaxGridLat();
		double minLon = region.getMinGridLon();
		double maxLon = region.getMaxGridLon();

		// make the min and max lat param
		StringParameter minLatParam = new StringParameter(MIN_LAT_PARAM_NAME,
				minLat + "");
		StringParameter maxLatParam = new StringParameter(MAX_LAT_PARAM_NAME,
				maxLat + "");
		// make the min and max lon param
		StringParameter minLonParam = new StringParameter(MIN_LON_PARAM_NAME,
				minLon + "");
		StringParameter maxLonParam = new StringParameter(MAX_LON_PARAM_NAME,
				maxLon + "");

		StringParameter gridSpacingParam = new StringParameter(GRIDSPACING_PARAM_NAME,
				region.getSpacing() + "");


		// add the params to the list
		this.sitesParamList = new ParameterList();
		sitesParamList.addParameter(minLatParam);
		sitesParamList.addParameter(maxLatParam);
		sitesParamList.addParameter(minLonParam);
		sitesParamList.addParameter(maxLonParam);
		sitesParamList.addParameter(gridSpacingParam);
		this.sitesEditor = new ParameterListEditor(sitesParamList);
		sitesEditor.setTitle(SITES_TITLE);

		// show this gui bean the JPanel
		sitePanel.removeAll();
		this.sitePanel.add(sitesEditor,new GridBagConstraints( 0, 0, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, defaultInsets, 0, 0 ));
		// also set it in map gui bean
		this.mapGuiBean.setRegionParams(minLat, maxLat, minLon, maxLon, region.getSpacing());
		sitePanel.validate();
		sitePanel.repaint();
	}


	/**
	 * reads the metadata file for each selected item in the combo box
	 * and puts the info of the dataset in the textarea.
	 */
	private void addDataInfo(){
		String dataSetDescription="Temporary Metadata\nWe'll figure out what to do with this later!";
		this.dataSetText.setEditable(true);
		dataSetText.setText(dataSetDescription);
		dataSetText.setEditable(false);
	}

	/**
	 * initialize the IML prob selector GUI bean
	 */
	private void initIML_ProbGuiBean() {
		imlProbGuiBean = new IMLorProbSelectorGuiBean();
		// show this gui bean the JPanel
		this.imlProbPanel.add(this.imlProbGuiBean,new GridBagConstraints( 0, 0, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, defaultInsets, 0, 0 ));
	}

	/**
	 * initialize the map gui bean
	 */
	private void initMapGuiBean() {
		mapGuiBean = new GMT_MapGuiBean();
		// show this gui bean the JPanel
		this.gmtPanel.add(this.mapGuiBean,new GridBagConstraints( 0, 0, 1, 1, 1.0, 1.0,
				GridBagConstraints.CENTER, GridBagConstraints.BOTH, defaultInsets, 0, 0 ));
		mapGuiBean.showRegionParams(false);
		mapGuiBean.setMapToBeShownInSeperateWindow(true);
		mapGuiBean.getParameterList().getParameter(GMT_MapGenerator.LOG_PLOT_NAME).setValue(false);
		mapGuiBean.getParameterEditor(GMT_MapGenerator.LOG_PLOT_NAME).refreshParamEditor();
	}

	/**
	 * this function is called when user chooses  "show Map"
	 * @param e
	 */
	void mapButton_actionPerformed(ActionEvent e) {
		
		// get he min/max lat/lon and gridspacing
		double minLat = Double.parseDouble((String)sitesParamList.getParameter(this.MIN_LAT_PARAM_NAME).getValue());
		double maxLat = Double.parseDouble((String)sitesParamList.getParameter(this.MAX_LAT_PARAM_NAME).getValue());
		double minLon = Double.parseDouble((String)sitesParamList.getParameter(this.MIN_LON_PARAM_NAME).getValue());
		double maxLon = Double.parseDouble((String)sitesParamList.getParameter(this.MAX_LON_PARAM_NAME).getValue());
		double gridSpacing = Double.parseDouble((String)sitesParamList.getParameter(this.GRIDSPACING_PARAM_NAME).getValue());

		if(minLat >= maxLat){
			JOptionPane.showMessageDialog(this, "Min. Lat must be less than Max Lat.",
					"Input Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		if(minLon >= maxLon){
			JOptionPane.showMessageDialog(this, "Min. Lon must be less than Max Lon.",
					"Input Error", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		String val = (String)fileSource.getValue();
		if (val.equals(SOURCE_LOCAL)) {
			boolean isProbAt_IML = (imlProbGuiBean.getSelectedOption().equals(IMLorProbSelectorGuiBean.PROB_AT_IML));
			double value = imlProbGuiBean.getIML_Prob();
			String outFile = localDir + "/" + "xyzCurves_inv";
			if (isProbAt_IML)
				outFile = outFile + "_PROB";
			else
				outFile = outFile + "_IML";
			outFile = outFile + "_" + value + ".txt";
			
			try {
				if (!(new File(outFile).exists())) { // if the file hasn't already been created
					MakeXYZFromHazardMapDir maker = new MakeXYZFromHazardMapDir(localDir + "/curves", false, true);
					maker.writeXYZFile(isProbAt_IML, value, outFile);
				}
				makeLocalMap(outFile);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
			
		} else {
			// set the lat and lon limits in mao gui bean
			double region[] = {minLat, maxLat, minLon, maxLon, gridSpacing};
			this.region = region;
			mapGuiBean.setRegionParams(minLat, maxLat, minLon, maxLon, gridSpacing);
			boolean isProbAt_IML = (imlProbGuiBean.getSelectedOption().equals(IMLorProbSelectorGuiBean.PROB_AT_IML));
			double value = imlProbGuiBean.getIML_Prob();
			String jobName = (String)this.dataSetCombo.getSelectedItem();
			
			System.out.println("Making a server map:" + jobName);
			
			//establishes the connection with the servlet
			makeServerMap(jobName, isProbAt_IML, value);
		}
		
	}
	
	void makeServerMap(String jobName, boolean isProbAt_IML, double value) {
		try {
			
			URL hazardMapViewerServlet = new URL(SERVLET_URL);
			URLConnection servletConnection = hazardMapViewerServlet.openConnection();

			// inform the connection that we will send output and accept input
			servletConnection.setDoInput(true);
			servletConnection.setDoOutput(true);

			// Don't use a cached version of URL connection.
			servletConnection.setUseCaches (false);
			servletConnection.setDefaultUseCaches (false);
			// Specify the content type that we will send binary data
			servletConnection.setRequestProperty ("Content-Type","application/octet-stream");

			ObjectOutputStream toServlet = new
			ObjectOutputStream(servletConnection.getOutputStream());
			toServlet.writeObject(org.opensha.sha.gui.servlets.HazardMapViewerServlet.MAKE_MAP);
			//sending the user which dataSet is selected
			toServlet.writeObject(jobName);
			
			toServlet.writeObject(mapGuiBean.getGMTObject());
			
			toServlet.writeObject(isProbAt_IML);
			
			toServlet.writeObject(value);

			toServlet.flush();
			toServlet.close();

			// Receive the URL of the jpeg file from the servlet after it has received all the data
			ObjectInputStream fromServlet = new ObjectInputStream(servletConnection.getInputStream());

			Object output = fromServlet.readObject();
			if (!(output instanceof Boolean)) {
				String imgName=output.toString();
				fromServlet.close();
				// show the map in  a new window
				String metadata = "Temp Metadata!\n";
				String link = imgName.substring(0, imgName.lastIndexOf('/'));
				metadata +="<br><p>Click:  "+"<a href=\""+link+"\">"+link+"</a>"+"  to download files.</p>";
				String metadataAsHTML = metadata.replaceAll("\n","<br>");
				ImageViewerWindow imgView = new ImageViewerWindow(imgName, metadataAsHTML, true);
			}

			
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	void makeLocalMap(String fileName) {

		ArbDiscrGeoDataSet xyzData = null;
		if(fileName != null){
			xyzData = new ArbDiscrGeoDataSet(true);
			try{
				ArrayList<String> fileLines = FileUtils.loadFile(fileName);
				ListIterator<String> it = fileLines.listIterator();
				while(it.hasNext()){
					StringTokenizer st = new StringTokenizer((String)it.next());
					double lat = new Double(st.nextToken().trim());
					double lon = new Double(st.nextToken().trim());
					double val = new Double(st.nextToken().trim());
					Location loc = new Location(lat, lon);
					xyzData.set(loc, val);
				}
			}catch(Exception ee){
				JOptionPane.showMessageDialog(this,new String("Please enter URL or File Name"),"Error", JOptionPane.OK_OPTION);
				ee.printStackTrace();
			}
		}
		String metadata = "You can download the jpg or postscript files for:\n\t"+
		fileName+"\n\n"+
		"From (respectively):";

		mapGuiBean.makeMap(xyzData,metadata);
	}

	double region[];

	/**
	 * sets up the connection with the servlet on the server (scec.usc.edu)
	 */
	void openConnection() {
		try{

			URL hazardMapViewerServlet = new URL(SERVLET_URL);
			URLConnection servletConnection = hazardMapViewerServlet.openConnection();

			// inform the connection that we will send output and accept input
			servletConnection.setDoInput(true);
			servletConnection.setDoOutput(true);

			// Don't use a cached version of URL connection.
			servletConnection.setUseCaches (false);
			servletConnection.setDefaultUseCaches (false);
			// Specify the content type that we will send binary data
			servletConnection.setRequestProperty ("Content-Type","application/octet-stream");

			ObjectOutputStream toServlet = new
			ObjectOutputStream(servletConnection.getOutputStream());
			toServlet.writeObject(org.opensha.sha.gui.servlets.HazardMapViewerServlet.MAKE_MAP);
			//sending the user which dataSet is selected
			toServlet.writeObject((String)this.dataSetCombo.getSelectedItem());

			//sending the GMT params object to the servlet
//			toServlet.writeObject(mapGuiBean.getGMTObject());

			toServlet.writeObject(region);

			//sending the IML or Prob Selection to the servlet
			toServlet.writeObject(imlProbGuiBean.getSelectedOption());

			//sending the IML or Prob Selected value
			toServlet.writeObject(new Double(imlProbGuiBean.getIML_Prob()));

			// metadata for this map
			String metadata = dataSetText.getText()+"\nGMT Param List: \n"+
			"--------------------\n"+
			mapGuiBean.getVisibleParameters().getParameterListMetadataString();
			metadata = metadata +"\nMap Type Param List: \n"+
			"--------------------\n"+
			this.imlProbGuiBean.getVisibleParameters().getParameterListMetadataString();
			metadata = metadata +"\nMap Region Param List: \n"+
			"--------------------\n"+
			this.sitesEditor.getVisibleParameters().getParameterListMetadataString();

			toServlet.writeObject(metadata);

			toServlet.flush();
			toServlet.close();

			// Receive the URL of the jpeg file from the servlet after it has received all the data
			ObjectInputStream fromServlet = new ObjectInputStream(servletConnection.getInputStream());

			String imgName=fromServlet.readObject().toString();
			fromServlet.close();
			// show the map in  a new window
			String metadataAsHTML = metadata.replaceAll("\n","<br>");
			String link = imgName.substring(0, imgName.lastIndexOf('/'));
			metadata +="<br><p>Click:  "+"<a href=\""+link+"\">"+link+"</a>"+"  to download files.</p>";
			ImageViewerWindow imgView = new ImageViewerWindow(imgName, metadataAsHTML, true);

		}catch (Exception e) {
//			ExceptionWindow bugWindow = new ExceptionWindow(this,e.getStackTrace(),getParametersInfo());
//			bugWindow.setVisible(true);
//			bugWindow.pack();
//			System.out.println("Exception in connection with servlet:" +e);
			e.printStackTrace();
		}
	}

	void refreshButton_actionPerformed(ActionEvent e) {
		loadDataSets();
	}

	/**
	 * Whenever user chooses a data set in the combo box,
	 * this function is called
	 * It fills the data set infomation in text area and also the site info is filled
	 * @param e
	 */

	void dataSetCombo_actionPerformed(ActionEvent e) {
		if(dataSetCombo.getItemCount()>0){
			addDataInfo();
			fillLatLonAndGridSpacing();
		}
	}

	public void parameterChange(ParameterChangeEvent event) {
		if (event.getParameter() == fileSource) {
			selectPanel.removeAll();
			selectPanel.add(sourceEdit, BorderLayout.NORTH);
			String val = (String)fileSource.getValue();
			if (val.equals(SOURCE_LOCAL)) {
				selectPanel.add(localSelectPanel);
			} else {
				selectPanel.add(serverSelectPanel);
			}
			selectPanel.validate();
			selectPanel.repaint();
			addDataInfo();
			fillLatLonAndGridSpacing();
		}
	}

	public void loadLocalFile(File file) {
		SAXReader reader = new SAXReader();
		try {
			System.out.println("Loading: " + file.getAbsolutePath());
			localDir = file.getParent();
			Document document = reader.read(file);
			this.localDoc = document;
			Element root = document.getRootElement();
			GriddedRegion region = GriddedRegion.fromXMLMetadata(root.element(GriddedRegion.XML_METADATA_NAME));
			this.localRegion = region;
			localFileField.setText(file.getAbsolutePath());
			addDataInfo();
			fillLatLonAndGridSpacing();
		} catch (DocumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == fileChooserButton) {
			System.out.println("???");
			if (fileChooser == null) {
				fileChooser = new JFileChooser();
			}
			int returnVal = fileChooser.showOpenDialog(this);
			if (returnVal == JFileChooser.APPROVE_OPTION) {
				File file = fileChooser.getSelectedFile();
				loadLocalFile(file);
			}
		}

	}
	
	class HazardXMLDocumentComparator implements Comparator {
		private Collator c = Collator.getInstance();

		public int compare(Object o1, Object o2) {
			if(o1 == o2)
				return 0;

			Document doc1 = (Document) o1;
			Document doc2 = (Document) o2;
			
			Element jobElem = doc1.getRootElement().element("hazardMapJob");
			String jobName = jobElem.attributeValue("jobName");
			System.out.println("JobName: " + jobName);
			
			jobElem = doc2.getRootElement().element("hazardMapJob");
			if (jobElem == null) {
				jobElem = doc2.getRootElement().element("GridJob");
			}
			String jobName2 = jobElem.attributeValue("jobName");
			System.out.println("comparing " + jobName + " to " + jobName2);
			
			String name1 = doc1.getRootElement().element("hazardMapJob").attributeValue("jobName");
			String name2 = doc2.getRootElement().element("hazardMapJob").attributeValue("jobName");

			return c.compare(name1, name2);
		}
	}

}

