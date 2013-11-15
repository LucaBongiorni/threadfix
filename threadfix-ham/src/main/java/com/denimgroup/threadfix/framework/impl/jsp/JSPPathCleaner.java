package com.denimgroup.threadfix.framework.impl.jsp;

import com.denimgroup.threadfix.framework.engine.cleaner.DefaultPathCleaner;
import com.denimgroup.threadfix.framework.engine.partial.PartialMapping;
import com.denimgroup.threadfix.framework.util.CommonPathFinder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class JSPPathCleaner extends DefaultPathCleaner {

    private final String jspRoot;

    public JSPPathCleaner(List<PartialMapping> partialMappings) {
        super(partialMappings);
        jspRoot = CommonPathFinder.findOrParseProjectRoot(
                partialMappings, ".jsp");
    }

    public JSPPathCleaner(String staticRoot, String dynamicRoot) {
        super(staticRoot, dynamicRoot);
        jspRoot = "";
    }

    @Nullable
    @Override
    public String getDynamicPathFromStaticPath(@NotNull String filePath) {
        String cleanedPath = filePath;

        if (jspRoot != null) {
            if (cleanedPath.contains("\\")) {
                cleanedPath = cleanedPath.replace('\\', '/');
            }

            if (!cleanedPath.startsWith(jspRoot) &&
                    cleanedPath.indexOf("/") != 0) {
                cleanedPath = "/" + cleanedPath;
            }

            if (cleanedPath.startsWith(jspRoot)) {
                cleanedPath = cleanedPath.substring(jspRoot.length());
            }
        }

        return cleanedPath;
    }

    @NotNull
    @Override
    public String toString() {
        return "[JSP PathCleaner jspRoot=" + jspRoot + ", dynamicRoot=" +
                dynamicRoot + ", staticRoot=" + staticRoot + "]";
    }

}
