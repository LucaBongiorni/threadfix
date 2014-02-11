////////////////////////////////////////////////////////////////////////
//
//     Copyright (c) 2009-2014 Denim Group, Ltd.
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
package com.denimgroup.threadfix.webapp.controller;

import com.denimgroup.threadfix.data.entities.GenericSeverity;
import com.denimgroup.threadfix.data.entities.GenericVulnerability;
import com.denimgroup.threadfix.data.entities.Permission;
import com.denimgroup.threadfix.data.entities.SeverityFilter;
import com.denimgroup.threadfix.logging.SanitizedLogger;
import com.denimgroup.threadfix.service.*;
import com.denimgroup.threadfix.service.util.PermissionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Controller
@SessionAttributes("severityFilter")
public class SeverityFilterController {

    @Autowired
	public SeverityFilterService severityFilterService;
    @Autowired
	public OrganizationService organizationService;
    @Autowired
	public ApplicationService applicationService;
    @Autowired
	public VulnerabilityFilterService vulnerabilityFilterService;
    @Autowired
	public GenericVulnerabilityService genericVulnerabilityService;
    @Autowired
	public GenericSeverityService genericSeverityService;
	
	private final SanitizedLogger log = new SanitizedLogger(SeverityFilterController.class);

	
	@ModelAttribute("genericVulnerabilities")
	public List<GenericVulnerability> getGenericVulnerabilities() {
		return genericVulnerabilityService.loadAll();
	}
	
	@ModelAttribute("genericSeverities")
	public List<GenericSeverity> getGenericSeverities() {
		List<GenericSeverity> severities = genericSeverityService.loadAll();
		
		GenericSeverity ignoreSeverity = new GenericSeverity();
		ignoreSeverity.setId(-1);
		ignoreSeverity.setName("Ignore");
		severities.add(ignoreSeverity);
		
		return severities;
	}
	
	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setAllowedFields("showInfo", "showLow", "showMedium", "showHigh",
				"showCritical", "id", "global", "enabled", "organization.id", "application.id");
	}
	
	@RequestMapping(value = "/configuration/severityFilter/set", method = RequestMethod.POST)
	public String setGlobalSeverityFilters(SeverityFilter severityFilter,
			BindingResult bindingResult, SessionStatus status, Model model,
			HttpServletRequest request) {

		if (!PermissionUtils.isAuthorized(Permission.CAN_MANAGE_APPLICATIONS, null, null)) {
			return "403";
		}
		
		return doSet(severityFilter, bindingResult, status, model, -1, -1, request);
	}
	
	@RequestMapping(value = "/organizations/{orgId}/severityFilter/set", method = RequestMethod.POST)
	public String setApplicationSeverityFilters(SeverityFilter severityFilter,
			BindingResult bindingResult, SessionStatus status, Model model,
			HttpServletRequest request, @PathVariable int orgId) {

		if (!PermissionUtils.isAuthorized(Permission.CAN_MANAGE_APPLICATIONS, orgId, null)) {
			return "403";
		}
		
		return doSet(severityFilter, bindingResult, status, model, orgId, -1, request);
	}
	
	@RequestMapping(value = "/organizations/{orgId}/applications/{appId}/severityFilter/set", method = RequestMethod.POST)
	public String setTeamSeverityFilters(SeverityFilter severityFilter,
			BindingResult bindingResult, SessionStatus status, Model model,
			@PathVariable int appId, @PathVariable int orgId,
			HttpServletRequest request) {

		if (!PermissionUtils.isAuthorized(Permission.CAN_MANAGE_APPLICATIONS, orgId, appId)) {
			return "403";
		}
		
		return doSet(severityFilter, bindingResult, status, model, orgId, appId, request);
	}
	
	private String doSet(SeverityFilter severityFilter,
			BindingResult bindingResult, SessionStatus status, Model model,
			int orgId, int appId,
			HttpServletRequest request) {
		
		String returnPage = null;
		
		if (bindingResult.hasErrors()) {
			
			model.addAttribute("contentPage", "filters/severityFilterForm.jsp");
			returnPage = "ajaxFailureHarness";
			log.warn("Severity Filter settings were not saved successfully.");
			
		} else {
			updateSeverityFilter(severityFilter, orgId, appId);
			severityFilterService.clean(severityFilter, orgId, appId);
			severityFilterService.save(severityFilter, orgId, appId);
			vulnerabilityFilterService.updateVulnerabilities(orgId, appId);
			
			log.info("Severity Filter settings saved successfully.");
			returnPage = returnSuccess(model, orgId, appId);
		}
		
		return returnPage;
	}
	
	private void updateSeverityFilter(SeverityFilter severityFilter, int orgId, int appId) {
		
		if (severityFilter != null) {
			if (orgId == -1 && appId == -1) {
				severityFilter.setGlobal(true);
				severityFilter.setApplication(null);
				severityFilter.setOrganization(null);
			} else if (appId != -1) {
				severityFilter.setGlobal(false);
				severityFilter.setApplication(applicationService.loadApplication(appId));
				severityFilter.setOrganization(null);
			} else {
				severityFilter.setGlobal(false);
				severityFilter.setApplication(null);
				severityFilter.setOrganization(organizationService.loadOrganization(orgId));
			}
		}
	}
	
	public String returnSuccess(Model model, int orgId, int appId) {
		model.addAttribute("vulnerabilityFilter", vulnerabilityFilterService.getNewFilter(orgId, appId));
		model.addAttribute("vulnerabilityFilterList", vulnerabilityFilterService.getPrimaryVulnerabilityList(orgId, appId));
		model.addAttribute("type", getType(orgId, appId));
		model.addAttribute("severitySuccessMessage", "Severity Filter settings saved successfully.");
		model.addAttribute("contentPage", "filters/tab.jsp");
		return "ajaxSuccessHarness";
	}
	
	public String getType(int orgId, int appId) {
		if (orgId == -1 && appId == -1) {
			return "Global";
		} else if (appId != -1) {
			return "Application";
		} else {
			return "Organization";
		}
	}
}
