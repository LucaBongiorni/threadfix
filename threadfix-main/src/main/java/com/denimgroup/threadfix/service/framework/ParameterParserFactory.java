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
package com.denimgroup.threadfix.service.framework;

import com.denimgroup.threadfix.service.merge.ScanMergeConfiguration;

public class ParameterParserFactory {
	
	public static ParameterParser getParameterParser(ScanMergeConfiguration scanMergeConfiguration) {
		ParameterParser parser = null;
		
		if (scanMergeConfiguration != null) {
			switch (scanMergeConfiguration.getFrameworkType()) {
				case SPRING_MVC: 
					SpringEntityMappings mappings = null;
					if (scanMergeConfiguration.getWorkTree() != null) {
						mappings = new SpringEntityMappings(scanMergeConfiguration.getWorkTree());
					}
					parser = new SpringDataFlowParser(mappings); 
					
					break;
				case JSP: 
					JSPMappings jspMappings = null;
					if (scanMergeConfiguration.getWorkTree() != null) {
						jspMappings = new JSPMappings(scanMergeConfiguration.getWorkTree());
					}
					parser = new JSPDataFlowParser(jspMappings, scanMergeConfiguration); 
					
					break;
				default:
			}
		}
		
		return parser;
	}
}
