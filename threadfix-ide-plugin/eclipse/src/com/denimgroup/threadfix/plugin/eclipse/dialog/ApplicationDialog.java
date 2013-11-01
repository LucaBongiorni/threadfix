package com.denimgroup.threadfix.plugin.eclipse.dialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;

public class ApplicationDialog extends TitleAreaDialog {
	
//	private List<Button> buttons = new ArrayList<Button>();
	private List<TreeItem> treeNodes = new ArrayList<TreeItem>();

	private Set<String> appIds = null;

	private final Map<String, String> appIdMap;
	private final Map<String, List<List<String>>> appTeamMap;
	private final Set<String> alreadyChecked;

	public ApplicationDialog(Shell parentShell, Map<String, String> appIdMap,
			Set<String> alreadyChecked) {
		super(parentShell);
		this.appIdMap = appIdMap;
		this.alreadyChecked = alreadyChecked;
		appTeamMap = organizeAppsByTeam();
	}

	@Override
	public void create() {
		super.create();
		setTitle("Pick Applications");
	}

//	@Override
//	protected Control createDialogArea(Composite parent) {
//		Composite area = (Composite) super.createDialogArea(parent);
//		Composite container = new Composite(area, SWT.NONE);
//		container.setLayoutData(new GridData(GridData.FILL_BOTH));
//		GridLayout layout = new GridLayout(1, false);
//		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
//		container.setLayout(layout);
//
//		createApplicationSelection(container);
//
//		return area;
//	}
	
	@Override
	protected Control createDialogArea(Composite parent) {
		Composite area = (Composite) super.createDialogArea(parent);
//		Display display = new Display();
//		Shell shell = new Shell(display); 
//		shell.setLayout(new FillLayout());
		Composite container = new Composite(area, SWT.BORDER);
		container.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, true, 1, 1));
		container.setBounds(0, 4, 255, 254);
		container.setLayout(new FillLayout());

		createApplicationSelectionTree(container);
		return area;
	}
	
	private void  createApplicationSelectionTree(Composite container){
		Tree tree = new Tree (container, SWT.BORDER | SWT.CHECK);
	    tree.addListener(SWT.Selection, new Listener() {
	        public void handleEvent(Event event) {
	            if (event.detail == SWT.CHECK) {
	                TreeItem item = (TreeItem) event.item;
	                boolean checked = item.getChecked();
	                checkItems(item, checked);
	                checkPath(item.getParentItem(), checked, false);
	            }
	        }
	    });
		for(String Team : appTeamMap.keySet()){
			TreeItem teamItem = new TreeItem (tree, 0);
			teamItem.setText (Team);
			for(List<String> app : appTeamMap.get(Team)){
				TreeItem appItem = new TreeItem(teamItem,0);
				appItem.setText(app.get(0));
				appItem.setData(app.get(0),app.get(1));
				if (alreadyChecked.contains(app.get(1)) && !appItem.getChecked()) {
					appItem.setChecked(true);
					treeNodes.add(appItem);
				}
			}
		}
	}
	
	private Map<String,List<List<String>>> organizeAppsByTeam(){
		Map<String,List<List<String>>> teamMap = new HashMap<>();
		for(String teamApp : appIdMap.keySet()){
			String[] comps = teamApp.split("/");
			if(comps.length==2){
				List<List<String>> temp = teamMap.get(comps[0]);
				List<String> sub = new ArrayList<>();
				sub.add(comps[1]);
				sub.add(appIdMap.get(teamApp));
				if(temp!=null){
					temp.add(sub);
				
				}else{
					temp = new ArrayList<>();
					temp.add(sub);
				}
				teamMap.put(comps[0], temp);
			}
		}
		return teamMap;
	}

//	private void createApplicationSelection(Composite container) {
//		for (String appIdentifier : new TreeSet<String>(appIdMap.keySet())) {
//			Button button = new Button(container, SWT.CHECK);
//			button.setText(appIdentifier);
//			if (alreadyChecked.contains(appIdMap.get(appIdentifier))) {
//				button.setSelection(true);
//			}
//			buttons.add(button);
//		}
//	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	// We need to save the values of the Text fields into Strings because the UI
	// gets disposed and the Text fields are not accessible any more.
	private void saveInput() {
		appIds = getAppIdsFromTreeNodes();
	}
	
//	private Set<String> getAppIdsFromButtons() {
//		Set<String> returnSet = new HashSet<String>();
//	
//		for (Button button : buttons) {
//			if (button.getSelection()) {
//				returnSet.add(appIdMap.get(button.getText()));
//			}
//		}
//	
//		return returnSet;
//	}
	
	private Set<String> getAppIdsFromTreeNodes(){
		Set<String> returnSet = new HashSet<String>();
		for(TreeItem node : treeNodes){
			if (node.getChecked()){
				returnSet.add((String) node.getData(node.getText()));
			}
		}
		
		return returnSet;
	}

	@Override
	protected void okPressed() {
		saveInput();
		super.okPressed();
	}
	
	public Set<String> getAppIds() {
		return appIds;
	}
	
	private void checkPath(TreeItem item, boolean checked, boolean grayed) {
	    if (item == null) return;
	    if (grayed) {
	        checked = true;
	    } else {
	        int index = 0;
	        TreeItem[] items = item.getItems();
	        while (index < items.length) {
	            TreeItem child = items[index];
	            if (child.getGrayed() || checked != child.getChecked()) {
	                checked = grayed = true;
	                treeNodes.add(child);
	                break;
	            }
	            index++;
	        }
	    }
	    item.setChecked(checked);
	    item.setGrayed(grayed);
	    checkPath(item.getParentItem(), checked, grayed);
	}

	private void checkItems(TreeItem item, boolean checked) {
	    item.setGrayed(false);
	    item.setChecked(checked);
	    treeNodes.add(item);
	    TreeItem[] items = item.getItems();
	    for (int i = 0; i < items.length; i++) {
	        checkItems(items[i], checked);
	    }
	}
}
