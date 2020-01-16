package pt.uminho.ceb.biosystems.merlin.launcher;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LauncherUtilities {
	
	public static Integer restartSignal;
	
	public static void setRestartSignal(int restartSignal) {
		LauncherUtilities.restartSignal = restartSignal;
	}
	
	public static int getRestartSignal() {
		if(restartSignal == null) throw new RuntimeException();
		return restartSignal;
	}
	
	public static void restart(){
		if(restartSignal == null)
			restartSignal = MainCycle.getRestartSignal();
		System.exit(restartSignal);
	}
	
	public static void deleteFiles(List<String> filesToDelete){
		if(filesToDelete!=null && filesToDelete.size()>0)		
			for (String file: filesToDelete) {
				try {
					delete(file);
				} catch (IOException e) {
					System.out.println("Could not delete file: " + file);
					e.printStackTrace();
				}
			}
	}
	
	public static void deleteFilesOnExit(List<String> filesToDelete) throws IOException{
		if(filesToDelete!=null && filesToDelete.size()>0)		
			for (String file: filesToDelete) {
				cleanAndDeleteOnExit(file);
			}
	}
	
	
	private static void deleteOnExit(String fileName) {
		File file = new File(fileName);
		
		if(file.exists()){
	    	if(file.isDirectory()){
	 
	    		System.out.println("Deleted DIRECTORY: " +file);
	    		//directory is empty, then delete it
	    		if(file.list().length==0)
	    		   file.deleteOnExit();
	    		
	    	}else{
	    		//if file, then delete it
	    		file.deleteOnExit();
	    		System.out.println("Deleted on exit FILE: " + file.getAbsolutePath());
	    	}
		}
		
	}
	
	public static void cleanAndDeleteOnExit(String fileName) throws IOException{
		
		FileChannel sourceChannel = null, outputChannel = null;
		File out = null;
		try {
			File tempFile = File.createTempFile("toremove", "");
			out = new File(fileName);
			sourceChannel = new FileInputStream(tempFile).getChannel();
			outputChannel = new FileOutputStream(out).getChannel();
			
			sourceChannel.transferTo(0, sourceChannel.size(), outputChannel);
		} catch (IOException e) {
			throw e;
		} finally {
			try {
				sourceChannel.close();
			} catch (IOException e) {}
			try {
				outputChannel.close();
			} catch (IOException e) {}
		}
		out.deleteOnExit();
	}

	// Method for folder/file deletion
	public static void delete(String fileName) throws IOException{
		File file = new File(fileName);
		
		if(file.exists()){
	    	if(file.isDirectory()){
	 
	    		System.out.println("Deleted DIRECTORY: " +file);
	    		//directory is empty, then delete it
	    		if(file.list().length==0)
	    		   file.delete();
	    		
	    	}else{
	    		//if file, then delete it
	    		file.delete();
	    		System.out.println("Deleted FILE: " + file.getAbsolutePath());
	    	}
		}
    }
	
	public static List<String> getListFromFile(String filePath){
		List<String> list = new LinkedList<String>();
		
		try {
			File file = new File(filePath);
			if(!file.exists())
				return list;
			FileReader fileReader = new FileReader(file);
			BufferedReader bufferedReader = new BufferedReader(fileReader);
			String line;
			while ((line = bufferedReader.readLine()) != null) {
				list.add(line);
			}
			fileReader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return list;
	}
	
	public static void deleteAllFilesExceptInFileList(File fileWithList, File folder, boolean mvnListDependencies){
		if(!folder.isDirectory())
			return;
		
		List<String> filesToKeepAux = getListFromFile(fileWithList.getAbsolutePath());
		filesToKeepAux.add("launcher.jar");
		List<String> filesToKeep = new LinkedList<>();
		// Append folder to filesToKeep
		for (String keep : filesToKeepAux) {
			
			if(mvnListDependencies && keep.trim().split(":").length>2) {
				
				String[] keepSplit = keep.trim().split(":");
				String keepName = keepSplit[1].concat("-").concat(keepSplit[3]).concat(".").concat(keepSplit[2]);
				filesToKeep.add(folder + File.separator + keepName.trim());
			}
			else {
				
				filesToKeep.add(folder + File.separator + keep.trim());
			}
		}
			
		List<String> allFilesInFolder = new ArrayList<String>();
		File[] fileList = folder.listFiles();
		for (File file : fileList)
				allFilesInFolder.add(folder.getName() + File.separator + file.getName());
		
		List<String> filesToDelete = new ArrayList<String>(allFilesInFolder);
		filesToDelete.removeAll(filesToKeep);
				
		try {
			deleteFilesOnExit(filesToDelete);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void cleanOldFilesFromDirectory (String directory, Integer daysThreshold) {
		
		try {
	
			File folder = new File(directory);
			File[] listOfFiles = folder.listFiles();
			SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
			
			if(listOfFiles == null) { // wrong input, if the folder does not exist there is nothing to clean
				throw new Exception("Input directory does not exist");
			}
			
			for (File file : listOfFiles) {
			    if (file.isFile()) {
			    	
			    	Date lastModified = sdf.parse(sdf.format(file.lastModified())); // last modified date of file
			    	Calendar c = Calendar.getInstance();
			    	Date currentDate = new Date(System.currentTimeMillis()); // current time
			    	c.setTime(currentDate);
			    	c.add(Calendar.DAY_OF_YEAR, -daysThreshold); // go back "input" days in time
			    	Date thresholdDate = c.getTime();
			    	Long difference = lastModified.getTime() - thresholdDate.getTime(); 
			    	long parsedDifference = TimeUnit.DAYS.convert(difference,  TimeUnit.MILLISECONDS); // difference in days between the last modified date of the file and today's date
			    	
			    	if(parsedDifference < 0) { // file is older than the input threshold, thus it must be deleted
			    		file.delete();			    		
			    	}
			    }
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String readLogsPathDirectory() {
		
			String pattern = null;

			String path = "./conf/logback.xml";
			ArrayList<String> listLines= new ArrayList<>(); 
			File configFile= new File(path); 
			try {
				Scanner file= new Scanner(configFile); 
				while(file.hasNextLine()==true) {
					listLines.add(file.nextLine());
				}
				file.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			int i = 0;

			while (i < listLines.size()) { 

				String line = listLines.get(i);

				Pattern p = Pattern.compile(".*<file>(.*)</file>");
				Matcher m = p.matcher(line);

				if(m.find()) {
					pattern = m.group(1);	
					
					int index = pattern.lastIndexOf("ç");
					
					if(index > 0)
						pattern = pattern.substring(0, index);
					else 
						return null;
				}
				i++;
			}	
			
			return pattern;
	}
}
