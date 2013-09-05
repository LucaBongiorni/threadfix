////////////////////////////////////////////////////////////////////////
//
//     Copyright (c) 2009-2013 Denim Group, Ltd.
//
//     The contents of this file are subject to the Mozilla Public License
//     Version 2.0 (the "License"); you may not use this file except in
//     compliance with the License. You may obtain a copy of the License at
//     http://www.mozilla.org/MPL/
//
//     Software distributed under the License is distributed on an "AS IS"
//     basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//     License for the specific language governing rights and limitations
//     under the License.
//
//     The Original Code is ThreadFix.
//
//     The Initial Developer of the Original Code is Denim Group, Ltd.
//     Portions created by Denim Group, Ltd. are Copyright (C)
//     Denim Group, Ltd. All Rights Reserved.
//
//     Contributor(s): Denim Group, Ltd.
//
////////////////////////////////////////////////////////////////////////

package com.denimgroup.threadfix.scanagent;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

import com.denimgroup.threadfix.cli.ThreadFixRestClient;
import com.denimgroup.threadfix.scanagent.configuration.OperatingSystem;
import com.denimgroup.threadfix.scanagent.configuration.Scanner;
import com.denimgroup.threadfix.scanagent.util.JsonUtils;
import com.denimgroup.threadfix.data.entities.Task;

public final class ScanAgentRunner {

	public static final String SCAN_AGENT_VERSION = "2.0.0-DEVELOPMENT-1";
	static Logger log = Logger.getLogger(ScanAgentRunner.class);
	
	private String threadFixServerUrl;
	private String threadFixApiKey;
	private int pollIntervalInSeconds;
	private OperatingSystem operatingSystem;
	private List<Scanner> availableScanners;
	private Map<String,AbstractScanAgent> scannerMap;
	private String baseWorkDir;
	
	private int numTasksAttempted = 0;
	private int maxTasks;
	
	private String agentConfig;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		boolean status;
		
		System.out.println("Starting ThreadFix generic scan agent version " + SCAN_AGENT_VERSION);
		BasicConfigurator.configure();
		log.debug("Logging configured and running");
		log.info("Starting ThreadFix generic scan agent version " + SCAN_AGENT_VERSION);

		ScanAgentRunner myAgent = new ScanAgentRunner();
		
		try {
			Configuration config = new PropertiesConfiguration("scanagent.properties");
			status = myAgent.readConfiguration(config);
			if(!status) {
				log.warn("Issues detected while reading configuration");
			}
		} catch (ConfigurationException e) {
			log.error("Problems reading configuration: " + e.getMessage(), e);
		}
		
		log.info("Scan agent configured");
		
		myAgent.logConfiguration();
		
		//	Main polling loop
		//	TODO - Determine if we want to move this inside of the ScanAgent class proper
		while(myAgent.keepPolling()) {
			Task currentTask = myAgent.requestTask();
			File taskResult = myAgent.doTask(currentTask);
			
			//	TOFIX - Send task results back to ThreadFix server
		}
		log.info("Reached max number of tasks: " + myAgent.numTasksAttempted + ". Shutting down");
		
		log.info("ThreadFix generic scan agent version " + SCAN_AGENT_VERSION + " stopping...");
	}
	
	public ScanAgentRunner() {
		this.cacheAgentConfig();
	}
	
	private boolean keepPolling() {
		boolean retVal;
		
		if(this.maxTasks > 0) {
			//	Only supposed to run for a limited number of times
			if(this.numTasksAttempted >= this.maxTasks) {
				//	We've reached the limit
				retVal = false;
			} else {
				//	Haven't reached the limit
				retVal = true;
			}
		} else {
			//	Supposed to run forever (default)
			retVal = true;
		}
		
		return(retVal);
	}
	
	private static String makeScannerList(List<Scanner> scanners) {
		StringBuilder sb = new StringBuilder();
		String prefix="";
		
		for(Scanner scanner : scanners) {
			sb.append(prefix);
			sb.append(scanner.getName());
			prefix = ",";
		}
		
		return(sb.toString());
	}
	
	/**
	 * Get some data about the local agent configuration to help identify this
	 * agent to the server. This isn't intended to be a secure unique identifier,
	 * but is instead intended to provide some debugging support. This is then
	 * cached so it can be sent along with requests to the ThreadFix server.
	 */
	private void cacheAgentConfig() {
		StringBuilder sb = new StringBuilder();
		
		String prefix;
		
		//	Grab some OS/user/Java environment properties
		sb.append(makeSystemPropertyString("os.arch"));
		sb.append(makeSystemPropertyString("os.name"));
		sb.append(makeSystemPropertyString("os.version"));
		sb.append(makeSystemPropertyString("user.name"));
		sb.append(makeSystemPropertyString("user.dir"));
		sb.append(makeSystemPropertyString("user.home"));
		sb.append(makeSystemPropertyString("java.home"));
		sb.append(makeSystemPropertyString("java.vendor"));
		sb.append(makeSystemPropertyString("java.version"));
		
		//	Pull some info about the network configuration of the scan agent
		Enumeration<NetworkInterface> nets = null;
		try {
			nets = NetworkInterface.getNetworkInterfaces();

	        for (NetworkInterface netint : Collections.list(nets)) {
	        	String interfaceName = netint.getDisplayName();
	        	sb.append("NETWORK:");
	        	sb.append(netint.getDisplayName());
	        	sb.append("=");
	        	
	        	prefix = "";
	        	for(java.net.InterfaceAddress address : netint.getInterfaceAddresses()) {
	        		InetAddress inetAddress = address.getAddress();
	        		sb.append(prefix);
	        		sb.append(inetAddress.getHostAddress());
	        		prefix = ",";
	        	}
	        	sb.append("\n");
	        }
		} catch (SocketException e) {
			String message = "Problems checking network interfaces when trying to gather agent config: " + e.getMessage();
			log.warn(message, e);
			sb.append("\nERROR=");
			sb.append(message);
		}
		
		this.agentConfig = sb.toString();
		
		log.debug("About to dump agent config");
		log.debug(this.agentConfig);
	}
	
	/**
	 * Grab a system property and return a string in the format:
	 * key=value\n
	 * (note the trailing newline)
	 * 
	 * @param propertyName
	 * @return
	 */
	private static String makeSystemPropertyString(String propertyName) {
		String retVal;
		
		retVal = propertyName + "=" + System.getProperty(propertyName) + "\n";
		
		return(retVal);
	}
	
	/**
	 * 
	 * @return
	 */
	private String getAgentConfig() {
		return(this.agentConfig);
	}
	
	/**
	 * TOFIX - Actually pull this from the ThreadFix server
	 * @return
	 */
	private Task requestTask() {
		
		log.info("Requesting a new task");
		Task retVal = null;
		
		log.info("Returning new task");
		
		ThreadFixRestClient tfClient = new ThreadFixRestClient(this.threadFixServerUrl, this.threadFixApiKey);
		String scannerList = makeScannerList(this.availableScanners);
		Object theReturn = tfClient.requestTask(scannerList, this.getAgentConfig());
		if(theReturn == null) {
			log.warn("Got a null task back from the ThreadFix server.");
		} else {
			String sReturn = (String)theReturn;
			if(sReturn.length() == 0) {
				log.warn("Got an empty string back in lieu of a task from the ThreadFix server.");
			} else {
				log.debug("Here's what we got back from the ThreadFix server: '" + sReturn + "'");
				retVal = JsonUtils.convertJsonStringToTask(sReturn);
			}
		}

		return(retVal);
	}
	
	private File doTask(Task theTask) {
		File taskResult = null;
		
		this.numTasksAttempted++;
		
		if(theTask == null) {
			log.warn("Task(" + this.numTasksAttempted + ") was null. Not going to do anything for now.");
		} else {
			log.info("Going to attempt task(" + this.numTasksAttempted + "): " + theTask);
			
			String taskType = theTask.getTaskType();
			AbstractScanAgent theAgent = this.scannerMap.get(taskType);
			taskResult = theAgent.doTask(theTask.getTaskConfig());
			if(taskResult != null) {
				log.info("Task appears to have completed successfully: " + theTask);
				log.info("Results from task shoudl be located at: " + taskResult.getAbsolutePath());
			} else {
				log.warn("Task appears not to have completed successfully: " + theTask);
			}
			
			log.info("Finished attempting task: " + theTask);
		}
		
		return(taskResult);
	}
	
	private boolean readConfiguration(Configuration config)
	{
		boolean retVal = false;
		
		this.threadFixServerUrl = config.getString("scanagent.threadFixServerUrl");
		log.debug("scanagent.threadFixServerUrl=" + this.threadFixServerUrl);
		
		this.threadFixApiKey = config.getString("scanagent.threadFixApiKey");
		
		this.pollIntervalInSeconds = config.getInt("scanagent.pollInterval");
		log.debug("scanagent.pollInterval=" + this.pollIntervalInSeconds);
		
		this.maxTasks = config.getInt("scanagent.maxTasks");
		log.debug("scanagent.maxTasks=" + this.maxTasks);
		
		//	TODO - Auto-detect the operating system and version
		this.operatingSystem = new OperatingSystem("osx", "10.8.2");
		this.baseWorkDir = config.getString("scanagent.baseWorkDir");
		
		this.availableScanners = new ArrayList<Scanner>();
		
		String scannerList = config.getString("scanagent.scanners");
		if(scannerList == null) {
			log.warn("Missing 'scanagent.scanners' property - No scanners will be configured");
		} else {
			log.debug("List of scanners: " + scannerList);
			String[] scanners = scannerList.split(",");
			this.scannerMap = new HashMap<String,AbstractScanAgent>();
			for(String scannerName : scanners) {
				String scannerVersion = config.getString(scannerName + ".version");
				String scannerClassname = config.getString(scannerName + ".className");
				try {
					log.debug("Attempting to load new instance of class " + scannerClassname);
					Object obj = Class.forName(scannerClassname).newInstance();
					if(obj instanceof AbstractScanAgent) {
						AbstractScanAgent newAgent = (AbstractScanAgent)obj;
						
						log.debug("Instantiating scanner class seems to have worked. Attempting configuration");
						
						String agentWorkDir = this.baseWorkDir + File.separator + scannerName + File.separator;
						File dirCheck = new File(agentWorkDir);
						if(dirCheck.exists()) {
							if(!dirCheck.isDirectory()) {
								log.warn("Agent work directory: " + agentWorkDir + " exists, but is not a directory");
							} else {
								log.debug("Agent work directory: " + agentWorkDir + " exists and is a directory. Good.");
							}
						} else {
							log.warn("Agent work directory: " + agentWorkDir + " does not exist. Attempting to create.");
							boolean result = dirCheck.mkdirs();
							if(!result) {
								log.error("Unable to create agent work directory: " + agentWorkDir + ". This will likely lead to errors.");
							} else {
								log.info("Agent work directory: " + agentWorkDir + " successfully created.");
							}
						}
						log.debug("Agent work directory will be: " + agentWorkDir);
						newAgent.setWorkDir(agentWorkDir);
						
						boolean configStatus = newAgent.readConfig(config);
						if(configStatus) {
							log.debug("Configuration successful");
						} else {
							log.warn("Configuration apprears to have run into problems");
						}
						
						this.scannerMap.put(scannerName, newAgent);
						this.availableScanners.add(new Scanner(scannerName, scannerVersion));
						log.info("Added scanner of type " + scannerName + " and implementing class "
									+ scannerClassname + " to the available scanners");
						
					} else {
						log.warn("Class: " + scannerClassname + "does not appear to be a subclass of com.denimgroup.threadfix.scanagent.AbstractScanAgent");
					}
				} catch (InstantiationException | IllegalAccessException
						| ClassNotFoundException e) {
					log.error("Unable to load class: " + scannerClassname + " for scanner " + scannerName, e);
				}
				
				
			}
			
			retVal = true;
		}
		
		return(retVal);
	}
	
	private void logConfiguration() {
		log.info("GenericScanAgent configuration:");
		if(operatingSystem != null) {
			log.info(this.operatingSystem);
		} else {
			log.info("No operating system configured (NULL)");
		}
		int i = 0;
		if(availableScanners != null) {
			if(availableScanners.size() == 0) {
				log.info("No scanners configured");
			} else {
				log.info("Scanners:");
				for(Scanner s : availableScanners) {
					log.info("[" + i + "]" + s);
					i++;
				}
			}
		} else {
			log.info("No scanners configured (NULL)");
		}
	}
}