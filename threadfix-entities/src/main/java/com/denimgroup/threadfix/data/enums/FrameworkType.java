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
package com.denimgroup.threadfix.data.enums;

public enum FrameworkType {
	NONE("None"), DETECT("Detect"), JSP("JSP"), SPRING_MVC("Spring MVC");
	
	FrameworkType(String displayName) {
		this.displayName = displayName;
	}
	
	private String displayName;
	public String getDisplayName() { return displayName; }
	
    public static FrameworkType getFrameworkType(String input) {
		FrameworkType type = DETECT; // default framework type
		
		if (input != null) {
			for (FrameworkType frameworkType : values()) {
				if (frameworkType.toString().equals(input)) {
					type = frameworkType;
					break;
				}
			}
		}
		
		return type;
	}
}