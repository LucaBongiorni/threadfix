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

import com.denimgroup.threadfix.data.entities.VulnerabilityFilter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.SessionStatus;

@Controller
@RequestMapping("/organizations/{orgId}/filters")
@SessionAttributes("vulnerabilityFilter")
public class OrganizationVulnFilterController extends AbstractVulnFilterController {

	@InitBinder
	public void setAllowedFields(WebDataBinder dataBinder) {
		dataBinder.setAllowedFields("sourceGenericVulnerability.name", "targetGenericSeverity.id");
	}

	@RequestMapping(method = RequestMethod.GET)
	public String index(@PathVariable int orgId, Model model) {
		return indexBackend(model, orgId, -1);
	}
	
	@RequestMapping(value = "/tab", method = RequestMethod.GET)
	public String tab(@PathVariable int orgId, Model model) {
		return tabBackend(model, orgId, -1);
	}
	
	@RequestMapping(value = "/new", method = RequestMethod.POST)
	public String submitNew(@PathVariable int orgId,
			VulnerabilityFilter vulnerabilityFilter,
			BindingResult bindingResult,
			SessionStatus status,
			Model model) {
		return submitNewBackend(vulnerabilityFilter, bindingResult, status, model, orgId, -1);
	}
	
	@RequestMapping(value = "/{filterId}/edit", method = RequestMethod.POST)
	public String submitEdit(
			@PathVariable int orgId,
			@PathVariable int filterId,
			VulnerabilityFilter vulnerabilityFilter,
			BindingResult bindingResult,
			SessionStatus status,
			Model model) {
		return submitEditBackend(vulnerabilityFilter, bindingResult, status, model, orgId, -1, filterId);
	}
	
	@RequestMapping(value = "/{filterId}/delete", method = RequestMethod.POST)
	public String submitDelete(
			@PathVariable int orgId,
			@PathVariable int filterId,
			Model model) {
		return submitDeleteBackend(model, orgId, -1, filterId);
	}
}
