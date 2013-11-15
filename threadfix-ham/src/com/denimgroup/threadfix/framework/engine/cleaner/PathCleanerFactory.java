package com.denimgroup.threadfix.framework.engine.cleaner;

import java.util.List;

import com.denimgroup.threadfix.framework.engine.partial.PartialMapping;
import com.denimgroup.threadfix.framework.enums.FrameworkType;
import com.denimgroup.threadfix.framework.impl.spring.SpringPathCleaner;

public class PathCleanerFactory {
	
	private PathCleanerFactory(){}
	
	// TODO add an option for manual roots
	
	public static PathCleaner getPathCleaner(FrameworkType frameworkType, List<PartialMapping> partialMappings) {
		PathCleaner returnCleaner = null;
		
		if (frameworkType == FrameworkType.SPRING_MVC) {
			returnCleaner = new SpringPathCleaner(partialMappings);
		} else {
			returnCleaner = new DefaultPathCleaner(partialMappings);
		}
		
		return returnCleaner;
	}

}
