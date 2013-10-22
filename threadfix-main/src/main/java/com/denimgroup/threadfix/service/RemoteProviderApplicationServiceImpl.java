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
package com.denimgroup.threadfix.service;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BindingResult;

import com.denimgroup.threadfix.data.dao.ApplicationChannelDao;
import com.denimgroup.threadfix.data.dao.ApplicationDao;
import com.denimgroup.threadfix.data.dao.RemoteProviderApplicationDao;
import com.denimgroup.threadfix.data.entities.Application;
import com.denimgroup.threadfix.data.entities.ApplicationChannel;
import com.denimgroup.threadfix.data.entities.ChannelType;
import com.denimgroup.threadfix.data.entities.RemoteProviderApplication;
import com.denimgroup.threadfix.data.entities.RemoteProviderType;
import com.denimgroup.threadfix.data.entities.Scan;
import com.denimgroup.threadfix.service.RemoteProviderTypeService.ResponseCode;
import com.denimgroup.threadfix.service.queue.QueueSender;
import com.denimgroup.threadfix.service.remoteprovider.RemoteProviderFactory;

@Service
@Transactional(readOnly = false)
public class RemoteProviderApplicationServiceImpl implements
		RemoteProviderApplicationService {
	
	private final SanitizedLogger log = new SanitizedLogger("RemoteProviderApplicationService");
	
	private RemoteProviderApplicationDao remoteProviderApplicationDao = null;
	private ScanMergeService scanMergeService = null;
	private ApplicationDao applicationDao = null;
	private ApplicationChannelDao applicationChannelDao = null;
	private QueueSender queueSender = null;
	
	@Autowired
	public RemoteProviderApplicationServiceImpl(
			RemoteProviderApplicationDao remoteProviderApplicationDao,
			ScanMergeService scanMergeService,
			ApplicationDao applicationDao,
			QueueSender queueSender,
			ApplicationChannelDao applicationChannelDao) {
		this.remoteProviderApplicationDao = remoteProviderApplicationDao;
		this.scanMergeService = scanMergeService;
		this.applicationDao = applicationDao;
		this.applicationChannelDao = applicationChannelDao;
		this.queueSender = queueSender;
	}
	
	@Override
	public RemoteProviderApplication load(int id) {
		return remoteProviderApplicationDao.retrieveById(id);
	}
	
	@Override
	public List<RemoteProviderApplication> loadAllWithTypeId(int id) {
		return remoteProviderApplicationDao.retrieveAllWithTypeId(id);
	}

	@Override
	public void store(RemoteProviderApplication remoteProviderApplication) {
		remoteProviderApplicationDao.saveOrUpdate(remoteProviderApplication);
	}
	
	@Override
	public void updateApplications(RemoteProviderType remoteProviderType) {

		List<RemoteProviderApplication> newApps =
				RemoteProviderFactory.fetchApplications(remoteProviderType);
		
		// We can't use remoteProviderType.getRemoteProviderApplications() 
		// because the old session is closed
		List<RemoteProviderApplication> appsForType = loadAllWithTypeId(
														remoteProviderType.getId());
		
		if (newApps == null || newApps.size() == 0) {
			return;
		} else {
			
			Set<String> appIds = new TreeSet<String>();
			if (appsForType != null && appsForType.size() > 0) {
				for (RemoteProviderApplication app : appsForType) {
					if (app == null || app.getNativeId() == null) {
						continue;
					}
					
					if (app.getNativeId().length() >= RemoteProviderApplication.NATIVE_ID_LENGTH) {
						log.warn("A Remote Provider application came out of the database with more than " 
									+ RemoteProviderApplication.NATIVE_ID_LENGTH 
									+ " characters in it. This shouldn't be possible.");
						appIds.add(app.getNativeId().substring(0, RemoteProviderApplication.NATIVE_ID_LENGTH-1));
					} else {
						appIds.add(app.getNativeId());
					}
				}
			}
			
			for (RemoteProviderApplication app : newApps) {
				if (app != null && !appIds.contains(app.getNativeId())) {
					app.setRemoteProviderType(remoteProviderType);
					appsForType.add(app);
					remoteProviderType.setRemoteProviderApplications(appsForType);
					store(app);
				}
			}
		}
	}
	
	@Override
	public List<RemoteProviderApplication> getApplications(
			RemoteProviderType remoteProviderType) {
		if (remoteProviderType == null) {
			return null;
		}
		
		List<RemoteProviderApplication> newApps = 
				RemoteProviderFactory.fetchApplications(remoteProviderType);
		
		if (newApps == null || newApps.size() == 0) {
			return null;
		}
		
		if (newApps != null && newApps.size() > 1) {
			Collections.sort(newApps,
				new Comparator<RemoteProviderApplication>() {
					public int compare(RemoteProviderApplication f1, 
							RemoteProviderApplication f2)
		            {
		                return f1.getNativeId().compareTo(f2.getNativeId());
		            }
		        });
		}
		
		for (RemoteProviderApplication app : newApps) {
			if (app == null) {
				continue;
			}
			
			if (app.getNativeId() != null && 
					app.getNativeId().length() >= RemoteProviderApplication.NATIVE_ID_LENGTH) {
				log.warn("A Remote Provider application was parsed that has more than " 
							+ RemoteProviderApplication.NATIVE_ID_LENGTH 
							+ " characters in it. The name is being trimmed but this"
							+ " should not prevent use of the application");
				app.setNativeId(app.getNativeId().substring(0, RemoteProviderApplication.NATIVE_ID_LENGTH-1));
			}
			
			app.setRemoteProviderType(remoteProviderType);
		}
		
		return newApps;
	}
	
	@Override
	public void deleteApps(RemoteProviderType remoteProviderType) {
		if (remoteProviderType != null && remoteProviderType
				.getRemoteProviderApplications() != null) {
			log.info("Deleting apps for Remote Provider type " + remoteProviderType.getName() +
					" (id=" + remoteProviderType.getId() + ")");
			for (RemoteProviderApplication app : remoteProviderType
					.getRemoteProviderApplications()) {
				log.info("Deleting Remote Application " + app.getNativeId() + 
						" (id = " + app.getId() + ", type id=" + remoteProviderType.getId() + ")");
				remoteProviderApplicationDao.delete(app);
			}
		}
	}
	
	@Override
	@Transactional
	public ResponseCode importScansForApplication(
			RemoteProviderApplication remoteProviderApplication) {
		if (remoteProviderApplication == null)
			return ResponseCode.ERROR_OTHER;
		
		List<Scan> resultScans = RemoteProviderFactory.fetchScans(remoteProviderApplication);
		
		ResponseCode success = ResponseCode.ERROR_OTHER;
		if (resultScans != null && resultScans.size() > 0) {
			Collections.sort(resultScans, new Comparator<Scan>() {
				public int compare(Scan scan1, Scan scan2){
					Calendar scan1Time = scan1.getImportTime();
					Calendar scan2Time = scan2.getImportTime();
					
					if (scan1Time == null || scan2Time == null) 
						return 0;
					
					return scan1Time.compareTo(scan2Time);
				}
			});
			
			int noOfScanNotFound = 0;
			int noOfNoNewScans = 0;
			for (Scan resultScan : resultScans) {
				if (resultScan == null || resultScan.getFindings() == null 
						|| resultScan.getFindings().size() == 0) {
					log.warn("Remote Scan import returned a null scan or a scan with no findings.");
					noOfScanNotFound++;
					
				} else if (remoteProviderApplication.getLastImportTime() != null && 
							(resultScan.getImportTime() == null ||
							!remoteProviderApplication.getLastImportTime().before(
									resultScan.getImportTime()))) {
					log.warn("Remote Scan was not newer than the last imported scan " +
							"for this RemoteProviderApplication.");
					noOfNoNewScans++;
					
				} else {
					log.info("Scan was parsed and has findings, passing to ScanMergeService.");
					
					remoteProviderApplication.setLastImportTime(resultScan.getImportTime());
					
					remoteProviderApplicationDao.saveOrUpdate(remoteProviderApplication);
					
					if (resultScan.getApplicationChannel() == null) {
						if (remoteProviderApplication.getApplicationChannel() != null) {
							resultScan.setApplicationChannel(remoteProviderApplication.getApplicationChannel());
						} else {
							log.error("Didn't have enough application channel information.");
						}
					}
					
					if (resultScan.getApplicationChannel() != null) {
						if (resultScan.getApplicationChannel().getScanList() == null) {
							resultScan.getApplicationChannel().setScanList(new ArrayList<Scan>());
						}
						
						if (!resultScan.getApplicationChannel().getScanList().contains(resultScan)) {
							resultScan.getApplicationChannel().getScanList().add(resultScan);
						}
					
						scanMergeService.processRemoteScan(resultScan);
						success = ResponseCode.SUCCESS;
					}
				}
			}
			
			if (!success.equals(ResponseCode.SUCCESS)) {
				if (noOfNoNewScans > 0)
					success = ResponseCode.ERROR_NO_NEW_SCANS;
				else if (noOfScanNotFound > 0)
					success = ResponseCode.ERROR_NO_SCANS_FOUND;
			}
		}
		return success;
	}
	
	@Override
	public String processApp(BindingResult result, 
			RemoteProviderApplication remoteProviderApplication, Application application) {

		application = applicationDao.retrieveById(application.getId());

		if (application == null) {
			return "Application choice was invalid.";
		}

		List<RemoteProviderApplication> rpApps = application.getRemoteProviderApplications();
		for (RemoteProviderApplication rpa: rpApps ) {
			if (rpa.getRemoteProviderType().getId() == remoteProviderApplication.getRemoteProviderType().getId()) {
				return "Application already have Mapping for this Remote Provider Type. Please choose another application.";
			}
		}
		
		if (application.getRemoteProviderApplications() == null) {
			application.setRemoteProviderApplications(
					new ArrayList<RemoteProviderApplication>());
		} 
		
		if (!application.getRemoteProviderApplications().contains(remoteProviderApplication)) {
			application.getRemoteProviderApplications().add(remoteProviderApplication);
			remoteProviderApplication.setApplication(application);
		}
		
		ChannelType type = remoteProviderApplication.getRemoteProviderType().getChannelType();
		
		if (application.getChannelList() == null || application.getChannelList().size() == 0) {
			application.setChannelList(new ArrayList<ApplicationChannel>());
		}
		
		Integer previousId = null;
		
		if (remoteProviderApplication.getApplicationChannel() != null) {
			previousId = remoteProviderApplication.getApplicationChannel().getId();
		}
		
		remoteProviderApplication.setApplicationChannel(null);
		
		for (ApplicationChannel applicationChannel : application.getChannelList()) {
			if (applicationChannel.getChannelType().getName().equals(type.getName())) {
				remoteProviderApplication.setApplicationChannel(applicationChannel);
				if (applicationChannel.getScanList() != null && 
						applicationChannel.getScanList().size() > 0) {
					List<Scan> scans = applicationChannel.getScanList();
					Collections.sort(scans,Scan.getTimeComparator());
					remoteProviderApplication.setLastImportTime(
							scans.get(scans.size() - 1).getImportTime());
				} else {
					remoteProviderApplication.setLastImportTime(null);
				}
				break;
			}
		}
		
		if (remoteProviderApplication.getApplicationChannel() == null) {
			ApplicationChannel channel = new ApplicationChannel();
			channel.setApplication(application);
			if (remoteProviderApplication.getRemoteProviderType() != null && 
				  remoteProviderApplication.getRemoteProviderType().getChannelType() != null) {
				channel.setChannelType(remoteProviderApplication.
						getRemoteProviderType().getChannelType());
				applicationChannelDao.saveOrUpdate(channel);
			}
			remoteProviderApplication.setLastImportTime(null);
			remoteProviderApplication.setApplicationChannel(channel);
			application.getChannelList().add(channel);
		}
		
		if (remoteProviderApplication.getApplicationChannel() == null
				|| previousId == null
				|| !previousId.equals(remoteProviderApplication
						.getApplicationChannel().getId())) {
			
			store(remoteProviderApplication);
			applicationDao.saveOrUpdate(application);
		}
		
		return "";
	}
	
	@Override
	public List<RemoteProviderApplication> loadAllWithMappings() {
		return remoteProviderApplicationDao.retrieveAllWithMappings();
	}

	@Override
	public void addBulkImportToQueue(RemoteProviderType remoteProviderType) {
		if (remoteProviderType == null || remoteProviderType.getRemoteProviderApplications() == null ||
				remoteProviderType.getRemoteProviderApplications().isEmpty()) {
			log.error("Null remote provider type passed to addBulkImportToQueue. Something went wrong.");
			return;
		}
		
		if (remoteProviderType.getHasConfiguredApplications()) {
			log.info("At least one application is configured.");
			queueSender.addRemoteProviderImport(remoteProviderType);
		} else {
			log.error("No apps were configured with applications.");
		}
	}

	@Override
	public String deleteMapping(BindingResult result,
			RemoteProviderApplication remoteProviderApplication,
			int appId) {

		Application application = applicationDao.retrieveById(appId);
		String returnStr = "";
		
		List<RemoteProviderApplication> rpAppList = application.getRemoteProviderApplications();
		if (rpAppList != null && !rpAppList.isEmpty()) {
			
			for (RemoteProviderApplication rpa: rpAppList) {
				if (rpa.getRemoteProviderType().getId().equals(
						remoteProviderApplication.getRemoteProviderType().getId())) {
					if (rpa.getApplicationChannel().getScanList() != null && 
							!rpa.getApplicationChannel().getScanList().isEmpty()) 
						returnStr = "But this application has Scans associated with the Remote Provider Application!";
				}
			}
			
		}

		if (application.getRemoteProviderApplications() == null) {
			application.setRemoteProviderApplications(
					new ArrayList<RemoteProviderApplication>());
		} 

		if (application.getRemoteProviderApplications().contains(remoteProviderApplication)) {
			application.getRemoteProviderApplications().remove(remoteProviderApplication);
			remoteProviderApplication.setApplication(null);
		}

		store(remoteProviderApplication);
		applicationDao.saveOrUpdate(application);

		return returnStr;
	}
}
