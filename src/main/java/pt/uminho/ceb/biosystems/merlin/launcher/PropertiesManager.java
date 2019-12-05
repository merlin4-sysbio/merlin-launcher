package pt.uminho.ceb.biosystems.merlin.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * Class that manage all the Properties needed in OptFlux from a specific file
 *
 */
class PropertiesManager {
	
	// Path of XML file
	private String filePath;
	
	// HashMap of properties <Name Property, Value of property>
	private HashMap<String, String> propertiesDictionary;
	
	// Map that will be different according to the OS
	private Map<String, String> osDictionary;
	
	// Map with all the system properties
	private Map<String, String> systemProperties;
	
	
	// XML document
	private Document doc;
	
	// Tags from Property File
	public static final String PROPERTIES = "properties";
	public static final String PROPERTIESFILE = "propertiesFILE";
	public static final String PROPERTY = "property";
	public static final String PROPERTYFILE = "propertyFILE";
	public static final String NAME = "name";
	public static final String VALUE = "value";
	public static final String FILE = "file";
	public static final String UNIQUE = "unique";
	public static final String APPEND = "append";
	public static final String SYSTEMSOURCE = "systemsource";
	
	public static final String RESTART = "restart";
	public static final String AIBENCHLAUNCHER = "aibenchlauncher";
	public static final String LOG = "log";
	public static final String LOGERROR = "logerror";
	public static final String PLUGINSBIN = "pluginsbin";
	public static final String PLUGINSINSTALL = "pluginsinstall";
	public static final String DELETEMACFILES = "deletemacfiles";
	public static final String DELETEWINFILES = "deletewinfiles";
	public static final String DELETELINUXFILES = "deletelinuxfiles";
	
	
	public static final String SEPARATOR = System.getProperty("path.separator");
	private OSystem os;
	
	// Dictionary Linux/Windows
	@SuppressWarnings("serial")
	private static final Map<String, String> dictWIN = java.util.Collections.unmodifiableMap(
		    new HashMap<String, String>() {
		    	{
			        put("PATH", "Path");
			        put("LD_LIBRARY_PATH", "Path");
			        put(":", SEPARATOR);
		    	}
		    });
	
	// Dictionary Linux/Mac
	@SuppressWarnings("serial")
	private static final Map<String, String> dictMAC = java.util.Collections.unmodifiableMap(
		    new HashMap<String, String>() {
		    	{
			        //put("PATH", "DYLD_LIBRARY_PATH");
			        put("LD_LIBRARY_PATH" , "DYLD_LIBRARY_PATH");
			        put(":", SEPARATOR);
		    	}
		    });
	
	/**
	 * Create Manager with existent dictionary(HashMap) of properties
	 * @param filePath - Path and Name of file with properties
	 * @param propDict - Dictionary with properties(HashMap)
	 * @throws FileNotFoundException 
	 */
	public PropertiesManager(String filePath, HashMap<String, String> propDict) throws FileNotFoundException{
		this.setFilePath(filePath);
		
		if(!(new File(filePath).exists()))
			throw new FileNotFoundException("File not found: "+ filePath);
		
		if(propDict!=null)
			setPropertiesDictionary(propDict);
		else
			setPropertiesDictionary(new HashMap<String, String>());
		
		os = detectOS();
		setOSDictionary();
		setSystemProperties();
	}

	/**
	 * Create Manager and new dictionary(HashMap) of properties
	 * @param filePath - Path and Name of file with properties
	 * @throws FileNotFoundException 
	 */
	public PropertiesManager(String filePath) throws FileNotFoundException{
		this(filePath, null);
	}
	
	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}

	public HashMap<String, String> getPropertiesDictionary() {
		return propertiesDictionary;
	}

	public void setPropertiesDictionary(HashMap<String, String> propertiesDictionary) {
		this.propertiesDictionary = propertiesDictionary;
	}
	
	/** Check current OS and instantiates the correspondent Dictionary */
	private void setOSDictionary(){
		
		if(os.equals(OSystem.WINDOWS))
			osDictionary = dictWIN;
		else if (os.equals(OSystem.MACOS))
			osDictionary = dictMAC;
	}
	
	/** Collect all system properties in a single map */
	private void setSystemProperties(){
		
		// Add System Environment Properties
		systemProperties = new HashMap<String, String>(System.getenv());
	    
		// Add System Properties
		Properties p = System.getProperties();
		for (Object prop : p.keySet())
			systemProperties.put((String)prop, (String)p.get(prop));
	}
	
	/** Initialize XML Reader */
	private void initReader(){
		
		File fXmlFile = new File(getFilePath());
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = null;
		
		// A XML validation is needed
		try {
			dBuilder = dbFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			e.printStackTrace();
		}
		
		try {
			doc = dBuilder.parse(fXmlFile);
		} catch (SAXException e) {
			System.out.println("Error in file: "+ filePath);
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(doc != null)
			doc.getDocumentElement().normalize();
	}
	
	/**
	 * Put Property in Dictionary. 
	 * <p>If Property already exists then add, if not then create new.
	 * <p>If there is no Value then the Property is not placed in the Dictionary 
	 * @param key - Name of Property
	 * @param value - Value of Property
	 */
	public void putProperty(String key, String value){
		// Change key correspondent to the key of OS
		if(osDictionary != null && osDictionary.containsKey(key))
			key = osDictionary.get(key);
		
		// Maybe it will be necessary to make the same for Values
		
		if(getPropertiesDictionary().containsKey(key))
			getPropertiesDictionary().put(key, getPropertiesDictionary().get(key) + SEPARATOR + value);
		else
			getPropertiesDictionary().put(key, value);
	}
	
	/**
	 * Put Properties from Map in Dictionary. 
	 * <p>If Property already exists then add, if not then create new.
	 * <p>If there is no Value then the Property is not placed in the Dictionary 
	 * @param key - Name of Property
	 * @param value - Value of Property
	 */
	public void putProperties(Map<String,String> newProperties){
		for (String property : newProperties.keySet()) {
			putProperty(property, newProperties.get(property));
		}
	}
	
	/**
	 * Put Unique Property in Dictionary. 
	 * <p>If Property already exists then replace.
	 * @param key - Property Name
	 * @param value - Property Value
	 */
	public void putUniqueProperty(String key, String value){
		// Change key correspondent to the key of OS
		if(osDictionary != null && osDictionary.containsKey(key))
			key = osDictionary.get(key);
		
		// Maybe it will be necessary to make the same for Values
		getPropertiesDictionary().put(key, value);
	}
	
	/**
	 * Put in Dictionary(HashMap) all the Properties defined in the files
	 */
	public void loadProperties(){
		
		initReader();
		if(doc != null)
		{
			getStaticProperties();
			getFileProperties();
		}
	}
	
	/**
	 * Set Properties previously loaded in the Environment of the ProcessBuilder
	 * @param pb - ProcessBuilder where the Properties will be set
	 */
	public void setPropertiesInEnvironment(ProcessBuilder pb){
		for(String property : propertiesDictionary.keySet()){
			pb.environment().put(property, propertiesDictionary.get(property));
		}
	}
	
	/**
	 * Given a list of Properties this method set them in the ProcessBuilder Environment
	 * @param pb - ProcessBuilder where the Properties will be set
	 * @param propDict - Dictionary of Properties to be set in the ProcessBuilder Environment
	 */
	public static void setPropertiesInEnvironmentFromDictionary(ProcessBuilder pb, Map<String, String> propDict){
		for(String property : propDict.keySet())
			if(propDict.get(property) != null)
				pb.environment().put(property, propDict.get(property));
	}
	
	/**
	 * Put in Dictionary(HashMap) the Properties that are defined in the present file
	 */
	private void getStaticProperties()
	{
		NodeList nList = doc.getElementsByTagName(PROPERTY);
		
		for (int i = 0; i < nList.getLength(); i++) {
			 
			Node nNode = nList.item(i);
			
			if(!addPropertyFromNode(nNode))
				System.out.println("Warning - Property ignored, value not found in: " +((Element)nNode).getAttribute(NAME));
			
		}
	}
	
	private boolean addPropertyFromNode(Node nNode){
		Element ele = ((Element)nNode);
		
		// return false if Node does not has value
		if(ele.getAttribute(VALUE).equals(""))
			return false;
		
		// Property will be unique in dictionary if Node has UNIQUE attribute
		if(ele.hasAttribute(UNIQUE))
			putUniqueProperty(ele.getAttribute(NAME), ele.getAttribute(VALUE));
		else
			putProperty(ele.getAttribute(NAME), ele.getAttribute(VALUE));
			
		// Append System Property if Node has APPEND attribute
		// Can be from System Prop. with same name or different (SYSTEMSOURCE) 
		if(ele.hasAttribute(APPEND)){
			if(ele.hasAttribute(SYSTEMSOURCE) && !ele.getAttribute(SYSTEMSOURCE).equals(""))
				appendPropertyValue(ele.getAttribute(NAME), ele.getAttribute(SYSTEMSOURCE));
			else
				appendPropertyValue(ele.getAttribute(NAME));
		}
		
		return true;
	}
	
	/** Append system property to Properties Dictionary */
	private void appendPropertyValue(String propertyName){
		String propName = convertSystemProperty(propertyName);
		if(systemProperties.containsKey(propName)){
			putProperty(propName, systemProperties.get(propName));
		}
	}
	
	/** Append different system property to Properties Dictionary */
	private void appendPropertyValue(String propertyName, String systemSource){
		if(systemProperties.containsKey(systemSource))
			putProperty(propertyName, systemProperties.get(systemSource));
	}
	
	private String convertSystemProperty(String propertyName) {
		if(os.equals(OSystem.WINDOWS)){
			if(propertyName.equals("PATH"))
				return "Path";
		}
		return propertyName;
	}
	
	/**
	 * Put in Dictionary(HashMap) the Properties that are defined in another file
	 */
	private void getFileProperties()
	{
		NodeList nList = doc.getElementsByTagName(PROPERTIESFILE);
		
		for (int temp = 0; temp < nList.getLength(); temp++) {
			 
			Node nNodeFile = nList.item(temp);
			Element eElement = (Element) nNodeFile;
			
			NodeList nNodeListFileProperty = eElement.getElementsByTagName(PROPERTYFILE);
		
			// File validation missing
			File fileWithProperties = new File(eElement.getAttribute(FILE));
			if(fileWithProperties.exists()){
				
				Properties p = new Properties();
			    try {
					p.load(new FileReader(fileWithProperties));
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			    
				
				for (int i = 0; i < nNodeListFileProperty.getLength(); i++) {
					
					Node nNodeProperty = nNodeListFileProperty.item(i);
	
					// Verify if Property exists on File
					if(p.containsKey(((Element)nNodeProperty).getAttribute(VALUE)))
					{
						// Verify if Property has any value
						if(p.getProperty(((Element)nNodeProperty).getAttribute(VALUE)).compareTo("") != 0)
							putProperty(((Element)nNodeProperty).getAttribute(NAME), 
									p.getProperty(((Element)nNodeProperty).getAttribute(VALUE)));					
						else
							System.out.println("Warning - Property in file ignored, value not found in: " 
									+((Element)nNodeProperty).getAttribute(VALUE) + " |File location: "+fileWithProperties.getPath());
					}
					else
						System.out.println("Warning - Property not found in file: "+((Element)nNodeProperty).getAttribute(VALUE) + " |File location: "+fileWithProperties.getPath());
				}
			}
			else
				System.out.println("Warning - File not found: " + fileWithProperties.getPath());
		}
	}
			
	public static OSystem detectOS() {
		
		OSystem os = OSystem.LINUX;
		if(System.getProperty("os.name").toLowerCase().indexOf("win") >= 0)
			os = OSystem.WINDOWS;
		else if (System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0)
			os = OSystem.MACOS;
		return os;
	}
	
	public Set<String> getJavaArgsOS(){
		Set<String> javaArgs = new LinkedHashSet<String>();
		if(os.equals(OSystem.MACOS)){
			javaArgs.add("-Dapple.laf.useScreenMenuBar=true");
			javaArgs.add("-Xdock:name=merlin");
			javaArgs.add("-Xdock:icon=conf/merlin_icon.png");
		}
		
		String params = propertiesDictionary.get("JAVA_PARAM");
		if(params!=null){
			String data[] = params.split(SEPARATOR);
			for(String d : data)
				javaArgs.add(d);
		}
		
		return javaArgs;
	}
	
	public ProcessBuilder constructProcess(){
		
		List<String> command = new ArrayList<>();
		command.add("java");
		return new ProcessBuilder(command);
		
	}
	
	public static void main(String[] args) throws Exception
	{	
		PropertiesManager propReader = new PropertiesManager("conf/main.conf");
		
		propReader.loadProperties();
		
//		for (String string : propReader.getPropertiesDictionary().keySet()) {
//			System.out.println("Property: " + string + " Value: " + propReader.getPropertiesDictionary().get(string));
//		}
		
		
		System.out.println(System.getenv().get("HOME"));
		System.out.println(propReader.getPropertiesDictionary().get("PATH"));
		//System.out.println("../guiutilities/bin:../utilities/bin:../jecoli3/bin:../jecoli3/lib/*:../biocomponents/lib/*:../biocomponents/bin:../metabolic3/bin:../metabolic3/lib/*:../solvers2/bin:../availablemodelsapi/bin:../availablemodelsapi/lib/*:../biovisualizercore/bin:../biovisualizercore/lib/*:../metabolicvisualizer4optflux3/lib/*:../biologicalnetscore/bin:../biologicalnetscore/lib/*:../optfluxcore3/lib/*:../optfluxcore3/plugins_bin/*:/opt/ibm/ILOG/CPLEX_Studio125/cplex/lib/cplex.jar");
		
	}

	public String getJRE() {
		
		String ret = null;
		if(!os.equals(OSystem.MACOS))
			ret = getPropertiesDictionary().get("JRE");
		if(ret == null) ret = "java";
		return ret;
	}
}
