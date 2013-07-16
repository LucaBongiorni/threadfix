package com.denimgroup.threadfix.service.framework;

import com.denimgroup.threadfix.data.entities.Finding;
import com.denimgroup.threadfix.data.entities.Scan;
import com.denimgroup.threadfix.service.merge.ScanMergeConfiguration;

public class SpringMVCTranslator extends AbstractPathUrlTranslator {

	public SpringMVCTranslator(ScanMergeConfiguration scanMergeConfiguration, Scan scan) {
		super(scanMergeConfiguration, scan);
		
		log.info("Using Spring MVC URL - Path translator.");
	}

	@Override
	public String getFileName(Finding dynamicFinding) {
		log.warn("Spring's unimplemented getFileName method was called.");
		
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getUrlPath(Finding staticFinding) {
		log.warn("Spring's unimplemented getUrlPath method was called.");
		
		// TODO Auto-generated method stub
		return null;
	}
	
}