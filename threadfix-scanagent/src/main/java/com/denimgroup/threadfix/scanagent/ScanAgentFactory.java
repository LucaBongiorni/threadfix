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

import com.denimgroup.threadfix.data.entities.ScannerType;
import com.denimgroup.threadfix.scanagent.configuration.Scanner;

/**
 * 
 * @author stran
 *
 */
public class ScanAgentFactory {
	
	private ScanAgentFactory(){}
	
	/**
	 * Returns a Scan Agent implementation based on the scanner type name
	 * 
	 * @param scanner
	 * @param workDir
	 * @param serverConduit
	 * @return
	 */
	public static AbstractScanAgent getScanAgent(Scanner scanner, String workDir, ServerConduit serverConduit) {

		AbstractScanAgent agent = null;
		
		switch (ScannerType.getScannerType(scanner.getName())) {
			case ACUNETIX_WVS: 
				agent = AcunetixScanAgent.getInstance(scanner, workDir, serverConduit);
				break;
			case ZAPROXY:
				agent = ZapScanAgent.getInstance(scanner, workDir, serverConduit);
				break;
			default: break;
		}
		
//		if (ScannerType.ACUNETIX_WVS.getFullName().equalsIgnoreCase(scanner.getName()))
//			agent = AcunetixScanAgent.getInstance(scanner, workDir, serverConduit); 
//		else if (ScannerType.ZAPROXY.getFullName().equalsIgnoreCase(scanner.getName()))
//			agent = ZapScanAgent.getInstance(scanner, workDir, serverConduit);

		return agent;
	}
	
}
