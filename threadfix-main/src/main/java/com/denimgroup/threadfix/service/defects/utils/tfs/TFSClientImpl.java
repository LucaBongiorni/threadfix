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
package com.denimgroup.threadfix.service.defects.utils.tfs;

import com.denimgroup.threadfix.importer.util.ResourceUtils;
import com.denimgroup.threadfix.logging.SanitizedLogger;
import com.denimgroup.threadfix.service.defects.DefectMetadata;
import com.microsoft.tfs.core.TFSTeamProjectCollection;
import com.microsoft.tfs.core.clients.workitem.WorkItem;
import com.microsoft.tfs.core.clients.workitem.WorkItemClient;
import com.microsoft.tfs.core.clients.workitem.fields.FieldDefinitionCollection;
import com.microsoft.tfs.core.clients.workitem.project.Project;
import com.microsoft.tfs.core.clients.workitem.project.ProjectCollection;
import com.microsoft.tfs.core.clients.workitem.query.WorkItemCollection;
import com.microsoft.tfs.core.exceptions.TECoreException;
import com.microsoft.tfs.core.exceptions.TFSUnauthorizedException;
import com.microsoft.tfs.core.httpclient.Credentials;
import com.microsoft.tfs.core.httpclient.UsernamePasswordCredentials;
import com.microsoft.tfs.core.ws.runtime.exceptions.UnauthorizedException;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TFSClientImpl implements TFSClient {

    protected static final SanitizedLogger LOG = new SanitizedLogger("TFSClientImpl");

    // We need to load the native libraries and this seems to be the best spot.
    // The idea is to use the same code for loading all the libraries but use
    // string values to specify which folder they are in and which names to look up.
    static {
        String osName = System.getProperty("os.name"), osArch = System.getProperty("os.arch");
        LOG.info("Attempting to load libraries for " + osName + ".");

        String folderName = null, prefix = null, suffix = null;
        String[] names = null;

        if (osName == null) {
            LOG.error("Received null from System.getProperty(\"os.name\"), " +
                    "something is wrong here.");
        } else if (osName.startsWith("Windows")) {
            folderName = "/tfs-native/win32/x86";
            if (osArch != null && osArch.contains("64")) {
                folderName += "_64";
            }
            prefix = "native_";
            suffix = ".dll";
            names = new String[] { "synchronization", "auth", "console",
                    "filesystem", "messagewindow", "misc", "registry" };

        } else if (osName.startsWith("Mac OS")) {
            folderName = "/tfs-native/macosx";
            prefix = "libnative_";
            suffix = ".jnilib";
            names = new String[] { "auth", "console", "filesystem", "keychain",
                    "misc", "synchronization" };
        } else if (osName.startsWith("Linux")) {
            String archExtension = osArch;
            if (osArch.equals("amd64")) {
                archExtension = "x86_64";
            } else if (osArch.equals("i386")) {
                archExtension = "x86";
            }

            folderName = "/tfs-native/linux/" + archExtension;
            prefix = "libnative_";
            suffix = ".so";
            names = new String[] { "auth", "console", "filesystem", "misc",
                    "synchronization" };

        } else if (osName.equals("hpux") || osName.equals("aix")
                || osName.equals("solaris")) {
            folderName = "/tfs-native/" + osName + "/";
            prefix = "libnative_";
            suffix = ".so";
            if (osArch != null && osArch.equals("PA_RISC")) {
                suffix = ".sl";
            } else if (osArch != null && osArch.equals("ppc")) {
                suffix = ".a";
            }
        } else {
            LOG.error("OS name not supported by TFS. " +
                    "The TFS integration will fail.");
        }

        if (folderName != null && names != null) {
            try {

                URL url = ResourceUtils.getUrl(folderName);

                if (url != null) {
                    String base = url.toURI().getPath()
                            .replaceFirst("file:", "");
                    try {
                        for (String library : names) {
                            System.load(base + prefix + library + suffix);
                        }

                        LOG.info("Successfully loaded native libraries for "
                                + osName + ".");
                    } catch (UnsatisfiedLinkError e) {
                        LOG.error("Unable to locate one of the libraries.", e);
                    }
                }
            } catch (URISyntaxException e) {
                LOG.error("Unable to convert the path String to a URI.", e);
            }

        } else {
            LOG.error("Attempt to load TFS native libraries failed..");
        }
    }

    ConnectionStatus lastStatus = ConnectionStatus.INVALID;
    WorkItemClient client = null;

    @Override
    public void updateDefectIdMaps(String ids, Map<String, String> stringStatusMap, Map<String, Boolean> openStatusMap) {
        if (lastStatus != ConnectionStatus.VALID || client == null) {
            LOG.error("Please configure the tracker properly before trying to submit a defect.");
            return;
        }

        String wiqlQuery = "Select ID, State from WorkItems where (id in ("
                + ids + "))";

        // Run the query and get the results.
        WorkItemCollection workItems = client.query(wiqlQuery);

        for (int i = 0; i < workItems.size(); i++) {
            WorkItem workItem = workItems.getWorkItem(i);

            stringStatusMap.put(String.valueOf(workItem.getID()),
                    (String) workItem.getFields().getField("State")
                            .getOriginalValue());
            openStatusMap.put(String.valueOf(workItem.getID()),
                    workItem.isOpen());
        }

        client.close();
    }

    @Override
    public List<String> getPriorities() {
        if (lastStatus != ConnectionStatus.VALID || client == null) {
            LOG.error("Please configure the tracker properly before trying to submit a defect.");
            return null;
        }

        List<String> returnPriorities = new ArrayList<>();

        FieldDefinitionCollection collection = client
                .getFieldDefinitions();

        Collections.addAll(returnPriorities, collection.get("Priority")
                .getAllowedValues().getValues());

        client.close();

        return returnPriorities;
    }

    @Override
    public List<String> getDefectIds(String projectName) {
        if (lastStatus != ConnectionStatus.VALID || client == null) {
            LOG.error("Please configure the tracker properly before trying to get defect IDs.");
            return null;
        }

        String wiqlQuery = "Select [System.Id] from WorkItems Where [System.TeamProject] = '" + projectName + "'";

        // Run the query and get the results.
        WorkItemCollection workItems = client.query(wiqlQuery);

        List<String> ids = new ArrayList<>();

        for (int i = 0; i < workItems.size(); i++) {
            ids.add(String.valueOf(workItems.getWorkItem(i).getID()));
        }

        client.close();

        return ids;
    }

    @Override
    public List<String> getProjectNames() {
        if (lastStatus != ConnectionStatus.VALID || client == null) {
            LOG.error("Please configure the tracker properly before trying to submit a defect.");
            return null;
        }

        try {
            ProjectCollection collection = client.getProjects();

            List<String> strings = new ArrayList<>();

            for (Project project : collection) {
                strings.add(project.getName());
            }

            return strings;
        } catch (UnauthorizedException | TFSUnauthorizedException e) {
            LOG.warn("Ran into TFSUnauthorizedException while trying to retrieve products.");
            return null;
        } finally {
            client.close();
        }
    }

    @Override
    public String getProjectId(String projectName) {
        if (lastStatus != ConnectionStatus.VALID || client == null) {
            LOG.error("Please configure the tracker properly before trying to submit a defect.");
            return null;
        }

        try {
            Project project = client.getProjects().get(projectName);

            return project == null ? null : String.valueOf(project.getID());

        } catch (UnauthorizedException | TFSUnauthorizedException e) {
            LOG.warn("Ran into TFSUnauthorizedException while trying to retrieve products.");
            return null;
        } finally {
            client.close();
        }
    }

    @Override
    public ConnectionStatus configure(String url, String username, String password) {
        Credentials credentials = new UsernamePasswordCredentials(
                username, password);

        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        TFSTeamProjectCollection projects = new TFSTeamProjectCollection(uri, credentials);
        try {
            client = projects.getWorkItemClient();
            lastStatus = client == null ? ConnectionStatus.INVALID : ConnectionStatus.VALID;
        } catch (UnauthorizedException | TFSUnauthorizedException e) {
            LOG.warn("TFSUnauthorizedException encountered, unable to connect to TFS. " +
                    "Check credentials and endpoint.");
        }

        return lastStatus;
    }

    @Override
    public String createDefect(String projectName, DefectMetadata metadata, String description) {

        if (lastStatus != ConnectionStatus.VALID || client == null) {
            LOG.error("Please configure the tracker properly before trying to submit a defect.");
            return null;
        }

        try {
            Project project = client.getProjects().get(projectName);

            if (project == null) {
                LOG.warn("Product was not found. Unable to create defect.");
                return null;
            }

            WorkItem item = client.newWorkItem(project
                    .getVisibleWorkItemTypes()[0]);

            if (item == null) {
                LOG.warn("Unable to create item in TFS.");
                return null;
            }

            item.setTitle(metadata.getDescription());
            item.getFields().getField("Description").setValue(description);
            item.getFields().getField("Priority").setValue(metadata.getPriority());

            item.save();

            String itemId = String.valueOf(item.getID());

            client.close();

            return itemId;

        } catch (UnauthorizedException | TFSUnauthorizedException e) {
            LOG.warn("Ran into TFSUnauthorizedException while trying to retrieve products.", e);
            return null;
        } finally {
            client.close();
        }
    }

    private void close() {

    }

    @Override
    public ConnectionStatus checkUrl(String url) {
        Credentials credentials = new UsernamePasswordCredentials("", "");

        URI uri;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            LOG.warn("Invalid syntax for the URL.",e);
            return ConnectionStatus.INVALID;
        }

        TFSTeamProjectCollection projects = new TFSTeamProjectCollection(uri,
                credentials);

        try {
            projects.getWorkItemClient().getProjects();
            LOG.info("No UnauthorizedException was thrown when attempting to connect with blank credentials.");
            return ConnectionStatus.VALID;
        } catch (UnauthorizedException | TFSUnauthorizedException e) {
            LOG.info("Got an UnauthorizedException, which means that the TFS url was good.");
            return ConnectionStatus.VALID;
        } catch (TECoreException e) {
            if (e.getMessage().contains("unable to find valid certification path to requested target")) {
                LOG.warn("An invalid or self-signed certificate was found.");
                return ConnectionStatus.INVALID_CERTIFICATE;
            } else {
                return ConnectionStatus.INVALID;
            }
        }
    }

}
