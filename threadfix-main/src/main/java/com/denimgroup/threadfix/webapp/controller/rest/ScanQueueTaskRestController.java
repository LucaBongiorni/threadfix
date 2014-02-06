package com.denimgroup.threadfix.webapp.controller.rest;

import com.denimgroup.threadfix.data.entities.Application;
import com.denimgroup.threadfix.data.entities.ScanQueueTask;
import com.denimgroup.threadfix.data.entities.Task;
import com.denimgroup.threadfix.plugin.scanner.service.ScanTypeCalculationService;
import com.denimgroup.threadfix.plugin.scanner.service.channel.ScanImportStatus;
import com.denimgroup.threadfix.remote.response.RestResponse;
import com.denimgroup.threadfix.service.*;
import com.denimgroup.threadfix.webapp.controller.ScanCheckResultBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/rest/tasks/")
public class ScanQueueTaskRestController extends RestController {
	
	public final static String OPERATION_QUEUE_SCAN = "queueScan";
	public final static String OPERATION_TASK_STATUS_UPDATE = "taskStatusUpdate";
	public final static String OPERATION_COMPLETE_TASK = "completeTask";
	public final static String OPERATION_FAIL_TASK = "failTask";
	public final static String TASK_KEY_SUCCESS = "Secure task key was accepted.";
	public final static String TASK_KEY_NOT_FOUND_ERROR = "No secure task key found error.";
	public final static String TASK_KEY_ERROR = "Secure task key was not recognized.";
	
	private DocumentService documentService;
	private ScanQueueService scanQueueService;
	private ScanTypeCalculationService scanTypeCalculationService;
	private ScanService scanService;
	private ScanMergeService scanMergeService;
	
	@Autowired
	public ScanQueueTaskRestController(APIKeyService apiKeyService,
			DocumentService documentService,
			ScanQueueService scanQueueService,
			ScanTypeCalculationService scanTypeCalculationService,
			ScanService scanService,
			ScanMergeService scanMergeService) {
		
		super(apiKeyService);
		
		this.apiKeyService = apiKeyService;
		this.documentService = documentService;
		this.scanQueueService = scanQueueService;
		this.scanTypeCalculationService = scanTypeCalculationService;
		this.scanService = scanService;
		this.scanMergeService = scanMergeService;
	}
	
	/**
	 * Queue a new scan
	 *
     * @see com.denimgroup.threadfix.remote.ThreadFixRestClient#queueScan(String, String)
	 * @param applicationId
	 * @param scannerType
	 * @return
	 */
	@RequestMapping(headers="Accept=application/json", value="queueScan", method=RequestMethod.POST)
	public @ResponseBody RestResponse<ScanQueueTask> queueScan(HttpServletRequest request,
			@RequestParam("applicationId") int applicationId,
			@RequestParam("scannerType") String scannerType) {

		log.info("Received REST request for a queueing a new " + scannerType
					+ " scan for applicationId " + applicationId);

		String result = checkKey(request, OPERATION_QUEUE_SCAN);
		if (!result.equals(API_KEY_SUCCESS)) {
			return RestResponse.failure(result);
		}

		ScanQueueTask task = scanQueueService.queueScan(applicationId, scannerType);
		
		return RestResponse.success(task);
	}
	
	/**
	 * TODO - Add scanner versions and OS/version to the incoming parameters
	 * TODO move to CLI library
     *
     * @see com.denimgroup.threadfix.remote.ThreadFixRestClient#requestTask(String, String)
	 * @param request
	 * @param scanners comma-separated list of scanners available from the agent
	 * @param agentConfig information about the agent's environment
	 * @return
	 */
	@RequestMapping(headers="Accept=application/json", value="requestTask", method=RequestMethod.POST)
	public @ResponseBody RestResponse<Task> requestTask(HttpServletRequest request,
			@RequestParam("scanners") String scanners,
			@RequestParam("agentConfig") String agentConfig) {

		log.info("Received a REST request to get a scan to run");
		
		String result = checkKey(request, OPERATION_TASK_STATUS_UPDATE);
		if (!result.equals(API_KEY_SUCCESS)) {
			return RestResponse.failure(result);
		}
		
		String secureTaskKey = this.apiKeyService.generateNewSecureRandomKey();

        Task returnTask = null;
        String errorMessage = null;

        try {
            returnTask = this.scanQueueService.requestTask(scanners, agentConfig, secureTaskKey);
            if (returnTask == null) {
                log.info("No exception was thrown and we didn't get a return task from ScanQueueService.requestTask.");
                errorMessage = "No task found.";
            }
        } catch (ScanQueueTaskConfigException e) {
            errorMessage = e.getMessage();
            log.error("Exception thrown while trying to get task information. Message: " + e.getMessage());
        }

        if (returnTask == null) {
            return RestResponse.failure(errorMessage);
        } else {
            return RestResponse.success(returnTask);
        }
	}

    /**
     *
     * @see com.denimgroup.threadfix.remote.ThreadFixRestClient#taskStatusUpdate(String, String)
     *
     * @param request
     * @param scanQueueTaskId
     * @param message
     * @return
     */
	@RequestMapping(headers="Accept=application/json", value="taskStatusUpdate", method=RequestMethod.POST)
	public @ResponseBody RestResponse<String> taskStatusUpdate(HttpServletRequest request,
			@RequestParam("scanQueueTaskId") int scanQueueTaskId,
			@RequestParam("message") String message) {

		log.info("Received a REST request to update the status of scan " + scanQueueTaskId);
		
		String result = checkKey(request, OPERATION_TASK_STATUS_UPDATE);
		if (!result.equals(API_KEY_SUCCESS)) {
			return RestResponse.failure(result);
		}
		
		if (this.scanQueueService.taskStatusUpdate(scanQueueTaskId, message)) {
            return RestResponse.success("Successfully updated ScanQueueTask");
        } else {
            return RestResponse.failure("Failed to update the ScanQueueTask");
        }
	}

    /**
     * @see com.denimgroup.threadfix.remote.ThreadFixRestClient#setTaskConfig(String, String, String)
     */
	@RequestMapping(headers="Accept=application/json", value="setTaskConfig", method=RequestMethod.POST)
	public @ResponseBody RestResponse<String> setTaskConfig(HttpServletRequest request,
			@RequestParam("appId") int appId,
			@RequestParam("scannerType") String scannerType,
			@RequestParam("file") MultipartFile file) {

		if(!ScanQueueTask.validateScanner(scannerType)) {
            String message = "Bad scanner type of: " + scannerType + " provided. Will not save scan config.";
			log.warn(message);
            return RestResponse.failure(message);
		} else {
			String filename = ScanQueueTask.makeScanAgentConfigFileName(scannerType);
			String returnedFilename = this.documentService.saveFileToApp(appId, file, filename);
			log.debug("Filename of: " + filename + " resulted in final filename of: " + returnedFilename);
            String message = "Scan configuration for scanner: " + scannerType + " saved for appId: " + appId;
			log.info(message);
			return RestResponse.success(message);
		}
	}
	
	/**
	 *	Allows a remote ScanAgent to notify the server that a task has been completed successfully and
	 *	provide the results from the scanning.
	 *
     * @see com.denimgroup.threadfix.remote.ThreadFixRestClient#completeTask(String, String, String)
	 *	@param scanQueueTaskId id for the ScanQueueTask
	 *	@param file result file from the scanning operation
	 */
	@RequestMapping(headers="Accept=application/json", value="completeTask", method=RequestMethod.POST)
	public @ResponseBody RestResponse<ScanQueueTask> completeTask(HttpServletRequest request,
			@RequestParam("scanQueueTaskId") int scanQueueTaskId,
			@RequestParam("file") MultipartFile file) {
		
		log.info("Received REST request to complete scan queue task: " + scanQueueTaskId);

		String result = checkKey(request, OPERATION_COMPLETE_TASK);
		if (!result.equals(API_KEY_SUCCESS)) {
			return RestResponse.failure(result);
		}
		
		result = checkTaskKey(request, scanQueueTaskId);
		if (!result.equals(TASK_KEY_SUCCESS)) {
			return RestResponse.failure(result);
		}
				
		ScanQueueTask myTask = this.scanQueueService.retrieveById(scanQueueTaskId);
		Application taskApp = myTask.getApplication();
		
		//	TODO - Add some checking so you can't just upload any file as the result of a specific scanner's task
		//	For now, passing NULL should force the calculation
		Integer myChannelId = scanTypeCalculationService.calculateScanType(taskApp.getId(), file, null);
		
		try {
			String fileName = scanTypeCalculationService.saveFile(myChannelId, file);
			
			ScanCheckResultBean returnValue = scanService.checkFile(myChannelId, fileName);
			
			if (ScanImportStatus.SUCCESSFUL_SCAN == returnValue.getScanCheckResult()) {
				scanMergeService.saveRemoteScanAndRun(myChannelId, fileName);
				//	Scan has been saved. Let's update the ScanQueueTask
				this.scanQueueService.completeTask(scanQueueTaskId);
				log.info("Results from scan queue task: " + myTask.getId() + " saved successfully.");
				return RestResponse.success(myTask);
			} else if (ScanImportStatus.EMPTY_SCAN_ERROR == returnValue.getScanCheckResult()) {
				String message = "Task appeared to complete successfully, but results provided were empty.";
				this.scanQueueService.failTask(scanQueueTaskId, message);
				log.warn("When saving scan queue task: " + myTask.getId() + ": " + message);
				return RestResponse.failure(message);
			} else {
				String message = "Task appeared to complete successfully, but the scan upload attempt returned this message: " + returnValue.getScanCheckResult();
				this.scanQueueService.failTask(scanQueueTaskId, message);
				log.warn("When saving scan queue task: " + myTask.getId() + ": " + message);
				return RestResponse.failure(message);
			}
		} catch (Exception e) {
			//	Something went wrong trying to save the file. Mark the scan as a failure.
			String message = "Exception thrown while trying to save scan.";
			String longMessage = message + " Message was : " + e.getMessage();
			this.scanQueueService.failTask(scanQueueTaskId, longMessage);
			log.error(longMessage, e);
			return RestResponse.failure(message);
		}
	}
	
	/**
	 * Allows a remote ScanAgent to notify the server that a task has failed and provide a reason for the failure.
	 *
     * @see com.denimgroup.threadfix.remote.ThreadFixRestClient#failTask(String, String, String)
	 *	@param scanQueueTaskId id for the ScanQueueTask to fail
	 *	@param message scanagent-provided reason for the scan failure
	 *	@return true if the scan failure was accepted and noted, false if some sort of error occurred
	 */
	@RequestMapping(headers="Accept=application/json", value="failTask", method=RequestMethod.POST)
	public @ResponseBody RestResponse<String> failTask(HttpServletRequest request,
			@RequestParam("scanQueueTaskId") int scanQueueTaskId,
			@RequestParam("message") String message) {

		log.info("Received a REST request to fail for the scan " + scanQueueTaskId);
		
		String result = checkKey(request, OPERATION_FAIL_TASK);
		if (!result.equals(API_KEY_SUCCESS)) {
			return RestResponse.failure(result);
		}
		
		result = checkTaskKey(request, scanQueueTaskId);
		if (!result.equals(TASK_KEY_SUCCESS)) {
			return RestResponse.failure(result);
		}
		
		String serverMessage = "ScanAgent reported that the scan task: " + scanQueueTaskId
									+ " had failed client-side for the following reason: " + message;
		this.scanQueueService.failTask(scanQueueTaskId, serverMessage);
		log.info(serverMessage);

		return RestResponse.success(serverMessage);
	}
	
	private String checkTaskKey(HttpServletRequest request, int scanQueueTaskId){
		String taskKey = request.getParameter("secureTaskKey");

		if (taskKey == null) {
			log.warn("Request to " + request.getPathInfo()
					+ " did not contain an Task Key.");
			return TASK_KEY_NOT_FOUND_ERROR;
		}
		ScanQueueTask myTask = this.scanQueueService.retrieveById(scanQueueTaskId);
		
		if (myTask.getSecureKey() == null || !myTask.getSecureKey().equals(taskKey)) {
			log.warn("Task key " + taskKey + " is not correct");
			return TASK_KEY_ERROR;
		}

		log.info("Task key was verified ");
		return TASK_KEY_SUCCESS;
			
	}
}
