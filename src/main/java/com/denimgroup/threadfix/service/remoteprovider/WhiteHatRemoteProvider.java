////////////////////////////////////////////////////////////////////////
//
//     Copyright (c) 2009-2011 Denim Group, Ltd.
//
//     The contents of this file are subject to the Mozilla Public License
//     Version 1.1 (the "License"); you may not use this file except in
//     compliance with the License. You may obtain a copy of the License at
//     http://www.mozilla.org/MPL/
//
//     Software distributed under the License is distributed on an "AS IS"
//     basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
//     License for the specific language governing rights and limitations
//     under the License.
//
//     The Original Code is Vulnerability Manager.
//
//     The Initial Developer of the Original Code is Denim Group, Ltd.
//     Portions created by Denim Group, Ltd. are Copyright (C)
//     Denim Group, Ltd. All Rights Reserved.
//
//     Contributor(s): Denim Group, Ltd.
//
////////////////////////////////////////////////////////////////////////
package com.denimgroup.threadfix.service.remoteprovider;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.GetMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.denimgroup.threadfix.data.dao.ChannelSeverityDao;
import com.denimgroup.threadfix.data.dao.ChannelTypeDao;
import com.denimgroup.threadfix.data.dao.ChannelVulnerabilityDao;
import com.denimgroup.threadfix.data.dao.VulnerabilityMapLogDao;
import com.denimgroup.threadfix.data.entities.ChannelType;
import com.denimgroup.threadfix.data.entities.Finding;
import com.denimgroup.threadfix.data.entities.RemoteProviderApplication;
import com.denimgroup.threadfix.data.entities.Scan;

public class WhiteHatRemoteProvider extends RemoteProvider {
	
	private static String SITES_URL = "https://sentinel.whitehatsec.com/api/site/";
	private static String VULNS_URL = "https://sentinel.whitehatsec.com/api/vuln/";
	private static String EXTRA_PARAMS = "&display_attack_vectors=1&query_status=open&query_site=";
	
	private String apiKey = null;

	@Autowired
	public WhiteHatRemoteProvider(ChannelTypeDao channelTypeDao,
			ChannelVulnerabilityDao channelVulnerabilityDao, ChannelSeverityDao channelSeverityDao,
			VulnerabilityMapLogDao vulnerabilityMapLogDao) {
		this.channelVulnerabilityDao = channelVulnerabilityDao;
		this.channelTypeDao = channelTypeDao;
		this.channelSeverityDao = channelSeverityDao;
		this.vulnerabilityMapLogDao = vulnerabilityMapLogDao;

		setChannelType(ChannelType.SENTINEL);
	}

	@Override
	public Scan getScan(RemoteProviderApplication remoteProviderApplication) {
		log.info("Made it into WhiteHat getScan() method.");

		apiKey = remoteProviderApplication.getRemoteProviderType().getApiKeyString();
		
		InputStream labelSiteIdStream = httpGet(SITES_URL + "?key=" + apiKey);
		
		String appName = remoteProviderApplication.getNativeId();
		
		WhiteHatSitesParser parser = new WhiteHatSitesParser();
		
		parse(labelSiteIdStream, parser);
		
		String siteId = parser.map.get(appName);
		
		if (siteId == null) {
			log.warn("No build ID was parsed.");
			return null; // we failed.
		} else {
			log.info("Retrieved build ID " + siteId + " for application " + appName);
		}
		
		String url = VULNS_URL + "?key=" + apiKey + EXTRA_PARAMS + siteId;
		
		log.info("Requesting site ID " + siteId);

		inputStream = httpGet(url);

		WhiteHatVulnerabilitiesParser scanParser = new WhiteHatVulnerabilitiesParser();
		Scan resultScan = parseSAXInput(scanParser);
		
		resultScan.setApplicationChannel(remoteProviderApplication.getApplicationChannel());
		
		return resultScan;
	}

	@Override
	public List<RemoteProviderApplication> fetchApplications() {
		if (remoteProviderType == null || remoteProviderType.getApiKeyString() == null) {
			log.warn("Insufficient credentials.");
			return null;
		}
		
		apiKey = remoteProviderType.getApiKeyString();
		
		WhiteHatSitesParser parser = new WhiteHatSitesParser();
		
		InputStream stream = httpGet(SITES_URL + "?key=" + apiKey);
				
		parse(stream, parser);
		
		try {
			if (stream != null) {
				stream.close();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return parser.getApplications();
	}
	
	public static InputStream httpGet(String urlStr) {
		GetMethod get = new GetMethod(urlStr);
		
		HttpClient client = new HttpClient();
		try {
			int status = client.executeMethod(get);
			if (status != 200) {
				System.err.println("Status was not 200.");
			}
			
			InputStream responseStream = get.getResponseBodyAsStream();
			
			if (responseStream != null) {
				return responseStream;
			}
		} catch (HttpException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public class WhiteHatSitesParser extends DefaultHandler {
		
		public Map<String, String> map = new HashMap<String,String>();
		
		private String currentId = null;
		private boolean grabLabel;
		
		public List<RemoteProviderApplication> getApplications() {
			List<RemoteProviderApplication> apps = new ArrayList<RemoteProviderApplication>();
			for (String label : map.keySet()) {
				RemoteProviderApplication remoteProviderApplication = new RemoteProviderApplication();
	    		remoteProviderApplication.setNativeId(label);
	    		remoteProviderApplication.setRemoteProviderType(remoteProviderType);
	    		apps.add(remoteProviderApplication);
			}
			return apps;
		}
		
	    public void startElement(String uri, String name, String qName, Attributes atts) throws SAXException {
	    	if ("site".equals(qName)) {
	    		currentId = atts.getValue("id");
	    	} else if ("label".equals(qName)) {
	    		grabLabel = true;
	    	}
	    }
	    
	    public void characters (char ch[], int start, int length) {
	    	if (grabLabel) {
	    		map.put(getText(ch,start,length), currentId);
	    		currentId = null;
	    		grabLabel = false;
	    	}
	    }
	}
	
	public class WhiteHatVulnerabilitiesParser extends DefaultHandler {
		
		public Finding finding = new Finding();
		
		private Map<String, String> map = new HashMap<String, String>();
		
		private boolean creatingVuln = false;
		
		private void addFinding() {
			Finding finding = constructFinding(map);
			
			if (finding == null) {
				log.warn("Finding was null.");
			} else {
				finding.setNativeId(map.get("nativeId"));
			}
			
			saxFindingList.add(finding);
			finding = new Finding();
		}
		
	    public void startElement (String uri, String name, String qName, Attributes atts) throws SAXException {
	    
	    	if ("vulnerability".equals(qName) && "open".equals(atts.getValue("status"))) {
	    		creatingVuln = true;
	    		map.clear();

	    		map.put("nativeId", atts.getValue("id"));
	    		map.put(CHANNEL_VULN_KEY, atts.getValue("class"));
	    		map.put(CHANNEL_SEVERITY_KEY, atts.getValue("severity"));
	    	} else if (creatingVuln) {
		    	if (qName.equals("request")) {
		    		map.put(PATH_KEY, atts.getValue("url"));
		    	} else if (qName.equals("param")) {
		    		map.put(PARAMETER_KEY, atts.getValue("name"));
		    	}
		    	
		    	if (map.get("nativeId") != null &&
		    			map.get(CHANNEL_VULN_KEY) != null &&
		    			map.get(CHANNEL_SEVERITY_KEY) != null &&
		    			map.get(PATH_KEY) != null &&
		    			map.get(PARAMETER_KEY) != null) {
		    		creatingVuln = false;
		    		addFinding();
		    	}
	    	}
	    }
	    
	    @Override
	    public void endElement (String uri, String localName, String qName) throws SAXException {	    	
	    	if (qName.equals("attack_vectors")) {
	    		addFinding();
	    		creatingVuln = false;
	    	}
	    }
	}
}