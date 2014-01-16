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

package com.denimgroup.threadfix.webapp.controller;

import com.denimgroup.threadfix.data.entities.*;
import com.denimgroup.threadfix.service.queue.scheduledjob.ScheduledScanScheduler;
import com.denimgroup.threadfix.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/organizations/{orgId}/applications/{appId}/scheduledScans")
@SessionAttributes(value= {"scanQueueTaskList", "scanQueueTask"})
public class ScheduledScanController {

	private final SanitizedLogger log = new SanitizedLogger(ScheduledScanController.class);

	private ScheduledScanService scheduledScanService;
    private ApplicationService applicationService;
	private PermissionService permissionService;
    private ChannelTypeService channelTypeService;

    @Autowired
    private ScheduledScanScheduler scheduledScanScheduler;

	@Autowired
	public ScheduledScanController(ScheduledScanService scheduledScanService,
                                   PermissionService permissionService,
                                   ApplicationService applicationService,
                                   ChannelTypeService channelTypeService) {
		this.scheduledScanService = scheduledScanService;
		this.permissionService = permissionService;
        this.applicationService = applicationService;
        this.channelTypeService = channelTypeService;
	}
	
	@RequestMapping(value = "/addScheduledScan", method = RequestMethod.POST)
	public String addScheduledScan(@PathVariable("appId") int appId, @PathVariable("orgId") int orgId,
                                   @Valid @ModelAttribute ScheduledScan scheduledScan,
                                   BindingResult result,
                                   HttpServletRequest request, Model model) {

		log.info("Start adding scheduled scan to application " + appId);
		if (!permissionService.isAuthorized(Permission.CAN_MANAGE_APPLICATIONS,orgId,appId)){
            return "403";
        }

        scheduledScanService.validateScheduledDate(scheduledScan, result);

        if (result.hasErrors()) {
            List<String> scannerTypeList = new ArrayList<>();
            List<ChannelType> channelTypeList = channelTypeService.getChannelTypeOptions(null);
            for (ChannelType type: channelTypeList) {
                scannerTypeList.add(type.getName());
            }

            Collections.sort(scannerTypeList);
            model.addAttribute("scannerTypeList", scannerTypeList);
            model.addAttribute("frequencyTypes", ScheduledScan.ScheduledFrequencyType.values());
            model.addAttribute("periodTypes", ScheduledScan.ScheduledPeriodType.values());
            model.addAttribute("scheduledDays", ScheduledScan.DayInWeek.values());
            model.addAttribute("contentPage", "applications/forms/addScheduledScanForm.jsp");
            return "ajaxFailureHarness";
        }

        int scheduledScanId = scheduledScanService.saveScheduledScan(appId, scheduledScan);

		if (scheduledScanId < 0) {
			ControllerUtils.addErrorMessage(request,
					"Adding Scheduled Scan was failed.");
			model.addAttribute("contentPage", "/organizations/" + orgId + "/applications/" + appId);
			return "ajaxFailureHarness";
		}

        //Add new job to scheduler
        if (scheduledScanScheduler.addScheduledScan(scheduledScan))
            log.info("Successfully added new scheduled scan to scheduler");
        else
            log.warn("Failed to add new scheduled scan to scheduler");

		ControllerUtils.addSuccessMessage(request,
				"Scheduled Scan ID " + scheduledScanId + " was successfully added to the application.");
		model.addAttribute("contentPage", "/organizations/" + orgId + "/applications/" + appId);
		log.info("Ended adding scheduled scan to application " + appId);
		return "ajaxRedirectHarness";
	}

	@RequestMapping(value = "/scheduledScan/{scheduledScanId}/delete", method = RequestMethod.POST)
	public String deleteScheduledScan(@PathVariable("appId") int appId,
			@PathVariable("orgId") int orgId,
			@PathVariable("scheduledScanId") int scheduledScanId,
			HttpServletRequest request, Model model) {
		
		log.info("Start deleting scheduled scan from application with id " + appId);
		if (!permissionService.isAuthorized(Permission.CAN_MANAGE_APPLICATIONS,orgId,appId)){
			return "403";
		}
        ScheduledScan scheduledScan = scheduledScanService.loadScheduledScanById(scheduledScanId);
        if (scheduledScan == null) {
            ControllerUtils.addErrorMessage(request, "The scheduled scan submitted was invalid, unable to delete");
            return "redirect:/organizations/" + orgId + "/applications/" + appId;
        }

        //Remove job from scheduler
        if (scheduledScanScheduler.removeScheduledScan(scheduledScan))
            log.info("Successfully deleted scheduled scan from scheduler");
        else
            log.warn("Failed to delete scheduled scan from scheduler");

        String ret = scheduledScanService.deleteScheduledScan(scheduledScan);
        if (ret != null) {
            ControllerUtils.addErrorMessage(request, ret);
        } else {
            ControllerUtils.addSuccessMessage(request,
                    "Scheduled scan ID " + scheduledScanId + " was successfully deleted");
        }
        log.info("Ended deleting scheduled scan from application with Id " + appId);

        return "redirect:/organizations/" + orgId + "/applications/" + appId;
	}

}
