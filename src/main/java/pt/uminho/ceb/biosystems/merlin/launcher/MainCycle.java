package pt.uminho.ceb.biosystems.merlin.launcher;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import es.uvigo.ei.aibench.Launcher;
import es.uvigo.ei.aibench.repository.PluginInstaller;


public class MainCycle {
	
	private static int DEFAULT_RESTART_SIGNAL = 10;
	public static final int LOG_HISTORY_LIMIT  = 7;
	public static final String LOGS_PATH  = "./logs";
	private static String OS = System.getProperty("os.name").toLowerCase();
	private static String DELETEFILES = "deletefiles";
	private static String DELETEFILESLIST = "deletefileslist";
	private static String mainConfFile = "conf/main.conf";
	private static String DEPENDENCIESFILE = "conf/dependenciesfiles";
	
	private static String aibenchLauncher;
	private static String log;
	private static String logError;
	private static String pluginsBin;
	private static String pluginsInstall;
	
	
	private static List<String> extraFilesWindows = 
			java.util.Collections.unmodifiableList(java.util.Arrays.asList("merlin.sh", 
					new File(System.getProperty("user.dir")).getParent()+"/MacOS/merlin", 
					new File(System.getProperty("user.dir")).getParent()+"MacOS"));
	
	private static List<String> extraFilesLinux = 
			java.util.Collections.unmodifiableList(java.util.Arrays.asList("merlin.bat", "run.bat", 
					new File(System.getProperty("user.dir")).getParent()+"/MacOS/merlin", 
					new File(System.getProperty("user.dir")).getParent()+"MacOS"));
	
	private static List<String> extraFilesMac = 
			java.util.Collections.unmodifiableList(java.util.Arrays.asList("merlin.sh", "merlin.bat", "run.bat"));
	
	
	
	private static void getExtraConfigInfo(PropertiesManager propManager)
	{
		aibenchLauncher = "es.uvigo.ei.aibench.Launcher";
		log = "merlin.log";
		logError = "merlin.log.err";
		pluginsBin = "plugins_bin";
		pluginsInstall = "plugins_install";
		
		
		if(propManager.getPropertiesDictionary().get(PropertiesManager.AIBENCHLAUNCHER) != null)
			aibenchLauncher = propManager.getPropertiesDictionary().get(PropertiesManager.AIBENCHLAUNCHER);
		
		if(propManager.getPropertiesDictionary().get(PropertiesManager.LOG) != null)
			log = propManager.getPropertiesDictionary().get(PropertiesManager.LOG);
		
		if(propManager.getPropertiesDictionary().get(PropertiesManager.LOGERROR) != null)
			logError = propManager.getPropertiesDictionary().get(PropertiesManager.LOGERROR);
		
		if(propManager.getPropertiesDictionary().get(PropertiesManager.PLUGINSBIN) != null)
			pluginsBin = propManager.getPropertiesDictionary().get(PropertiesManager.PLUGINSBIN);
		
		if(propManager.getPropertiesDictionary().get(PropertiesManager.PLUGINSINSTALL) != null)
			pluginsInstall = propManager.getPropertiesDictionary().get(PropertiesManager.PLUGINSINSTALL);
	}
	
	public static int getRestartSignal(){
		PropertiesManager propManager;
		int ret = DEFAULT_RESTART_SIGNAL;
		try {
			propManager = new PropertiesManager(mainConfFile);
			propManager.loadProperties();
			ret = getRestartSignal(propManager);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	    
		return ret;
	}
	
	private static int getRestartSignal(PropertiesManager propManager){	    	 
		String restart = propManager.getPropertiesDictionary().get(PropertiesManager.RESTART);
	    return (restart != null) ? Integer.parseInt(restart) : DEFAULT_RESTART_SIGNAL;
	}
	
	private static void defineFilesToDelete(PropertiesManager propManager){
	    
	    String linuxFiles = propManager.getPropertiesDictionary().get(PropertiesManager.DELETELINUXFILES);
	    if(linuxFiles != null)
	    {
	    	linuxFiles = linuxFiles.replace("USERDIR",new File(System.getProperty("user.dir")).getParent());
	    	extraFilesLinux = Arrays.asList(linuxFiles.split(";"));
	    }
	    
	    String macFiles = propManager.getPropertiesDictionary().get(PropertiesManager.DELETEMACFILES);
	    if(macFiles != null)
	    {
	    	macFiles = macFiles.replace("USERDIR",new File(System.getProperty("user.dir")).getParent());
	    	extraFilesMac = Arrays.asList(macFiles.split(";"));
	    }
	    
	    String winFiles = propManager.getPropertiesDictionary().get(PropertiesManager.DELETEWINFILES);
	    if(winFiles != null)
	    {
	    	winFiles = winFiles.replace("USERDIR",new File(System.getProperty("user.dir")).getParent());
	    	extraFilesWindows = Arrays.asList(winFiles.split(";"));
	    }
	}
	
	public static void main(String... strings ) throws IOException, ClassNotFoundException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException, InterruptedException{
		
		System.out.println(OS);
		
	    int x = getRestartSignal();
	    int signal = x;
	    
	    mainConfFile = (strings != null && strings.length >1 && strings[0]!=null)? strings[0]: "conf/main.conf";
	    do{    	
	    	PropertiesManager propManager = new PropertiesManager(mainConfFile);
		    propManager.loadProperties();
		    
		    getExtraConfigInfo(propManager);
		    defineFilesToDelete(propManager);
		    
		    // Create merlin process
		    Set<String> javaOSArgs = propManager.getJavaArgsOS();
		    List<String> command = new ArrayList<>();
		    command.add(propManager.getJRE());
		    if(javaOSArgs.size()>0) command.addAll(javaOSArgs);
		    command.add("-cp");
		    command.add(propManager.getPropertiesDictionary().get("JAVAPATH"));
		    command.add(aibenchLauncher);
		    command.add(pluginsBin);
		    
		    LauncherUtilities.setRestartSignal(getRestartSignal());
		    ProcessBuilder pb = new ProcessBuilder(command);
		    
		    // Set Environment Properties in merlin process
		    propManager.setPropertiesInEnvironment(pb);
		    
		    // Delete files from another OS
		    deleteExtraFiles();
		    
		    // Delete files from property list or file
		    List<String> filesToDelete = new ArrayList<String>();
		    List<String> delFiles = getListFromDictionaryProperty(propManager, DELETEFILES);
		    List<String> delFilesList = getListFromDictionaryProperty(propManager, DELETEFILESLIST);
		    if(delFiles != null)
		    	filesToDelete.addAll(delFiles);
		    if(delFilesList != null)
		    	filesToDelete.addAll(delFilesList);
		    
		    if(!filesToDelete.isEmpty())
		    	LauncherUtilities.deleteFiles(filesToDelete);
		    
		    pb.redirectOutput(new File(log));
		    pb.redirectError(new File(logError));
		    Process proc = pb.start();
		    
		    x = proc.waitFor();		    
		    
			PluginInstaller installer = new PluginInstaller(pluginsBin, pluginsInstall,	".");
		
		    installer.installPlugins(true, false);
		    
		    File dependenciesFile = new File(DEPENDENCIESFILE);
		    if(dependenciesFile.exists())
		    	LauncherUtilities.deleteAllFilesExceptInFileList(dependenciesFile, new File("lib"));
		    
		    signal = getRestartSignal();
		    
		    
		    Integer historyLimit = null;
		    
		    try {
		    	System.out.println(propManager.getPropertiesDictionary().get("logDaysHistoryLimit"));
				historyLimit = Integer.valueOf(propManager.getPropertiesDictionary().get("logDaysHistoryLimit"));
				
				String logsPath = LauncherUtilities.readLogsPathDirectory();
				
				LauncherUtilities.cleanOldFilesFromDirectory(logsPath, historyLimit);
			} 
		    catch (Exception e) {
		    	LauncherUtilities.cleanOldFilesFromDirectory(LOGS_PATH, LOG_HISTORY_LIMIT);
				e.printStackTrace();
			}
		    
	    }while(x==signal);
	    
	    System.exit(x);	
       	
	}
	
	private static List<String> getListFromDictionaryProperty(PropertiesManager propManager, String property){
		List<String> list = new ArrayList<String>();
		if(!propManager.getPropertiesDictionary().containsKey(property) || propManager.getPropertiesDictionary().get(property).equals(""))
			return null;
		
		String allFilesString = propManager.getPropertiesDictionary().get(property);	
		list.addAll(Arrays.asList(allFilesString.split(";")));
		
		if(property.equals(DELETEFILESLIST)){
			for (String filePath : Arrays.asList(allFilesString.split(";")))
				list.addAll(LauncherUtilities.getListFromFile(filePath));
		}
		
		return list;
	}
	
	private static void deleteExtraFiles()
	{
		List<String> extraFiles = extraFilesWindows;
		OSystem os = PropertiesManager.detectOS();
		
		if(os.equals(OSystem.LINUX))
			extraFiles = extraFilesLinux;
		else if (os.equals(OSystem.MACOS))
			extraFiles = extraFilesMac;
		
		for (String file: extraFiles) {
			try {
				LauncherUtilities.delete(file);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	
	
	// The dependenciesFile is a file that should contain all the libs (or other files) that 	
	
}
