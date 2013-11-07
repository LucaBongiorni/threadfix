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
package com.denimgroup.threadfix.framework.engine;

public class ClassMapping {

	private final String servletName, classWithPackage, contextConfigLocation;
	
	public static final String CLASSPATH_START = "classpath:";
	
	public ClassMapping(String servletName, String classWithPackage, String contextConfigLocation) {
		if (servletName == null) {
			throw new IllegalArgumentException("Servlet Name cannot be null.");
		}
		
		if (classWithPackage == null) {
			throw new IllegalArgumentException("Class cannot be null.");
		}
		
		this.servletName = servletName.trim();
		this.classWithPackage = classWithPackage.trim();
		
		
		if (contextConfigLocation != null && contextConfigLocation.startsWith(CLASSPATH_START)) {
			this.contextConfigLocation = contextConfigLocation.substring(CLASSPATH_START.length());
		} else {
			this.contextConfigLocation = contextConfigLocation;
		}
	}
	
	public String getServletName() {
		return servletName;
	}
	
	public String getClassWithPackage() {
		return classWithPackage;
	}
	
	public String getContextConfigLocation() {
		return contextConfigLocation;
	}
	
	@Override
	public String toString() {
		return getServletName() + " -> " + getClassWithPackage();
	}
	
}
