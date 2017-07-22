package org.cytoscape.graphspace.cygraphspace.internal;

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.swing.AbstractCyAction;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.work.TaskIterator;
import org.json.JSONArray;
import org.json.JSONObject;
import org.cytoscape.graphspace.cygraphspace.internal.gui.AuthenticationDialog;
import org.cytoscape.graphspace.cygraphspace.internal.gui.PostGraphDialog;
import org.cytoscape.graphspace.cygraphspace.internal.gui.UpdateGraphDialog;
import org.cytoscape.graphspace.cygraphspace.internal.singletons.CyObjectManager;
import org.cytoscape.graphspace.cygraphspace.internal.singletons.Server;

import javax.swing.*;

/**
 *
 * @author David Welker
 * Creates a new menu item in the Apps|NDex menu to upload an Cytoscape network to the current NDEx server.
 */
public class PostGraphMenuAction extends AbstractCyAction
{
    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private JFrame loadingFrame;
	public PostGraphMenuAction(String menuTitle, CyApplicationManager applicationManager)
    {
        super(menuTitle, applicationManager, null, null);
        // We want this menu item to appear under the App|NDEx menu. The actual name of the menu item is set in
        // org.cytoscape.ndex.internal.CyActivator as "Upload Network"
        setPreferredMenu("File.Export");
    }

    @Override
    /**
     * This method displays the upload network dialog.
     * It is called when the menu item is selected.  
     */
    public void actionPerformed(ActionEvent e)
    {
        JFrame parent = CyObjectManager.INSTANCE.getApplicationFrame();

        CyNetwork currentNetwork = CyObjectManager.INSTANCE.getCurrentNetwork();
        
        loadingFrame = new JFrame("Checking if update Possible");
		ImageIcon loading = new ImageIcon(this.getClass().getClassLoader().getResource("loading.gif"));
		JLabel loadingLabel = new JLabel("Checking if you're trying to update an existing graph", loading, JLabel.CENTER);
		loadingLabel.setHorizontalTextPosition(JLabel.CENTER);
		loadingLabel.setVerticalTextPosition(JLabel.BOTTOM);
		loadingFrame.add(loadingLabel);
		loadingFrame.setSize(400, 300);
		
        if( currentNetwork == null )
        {
            String msg = "There is no graph to export.";
            String dialogTitle = "No Graph Found";
            JOptionPane.showMessageDialog(parent, msg, dialogTitle, JOptionPane.ERROR_MESSAGE );
            return;
        }
        if (Server.INSTANCE.isAuthenticated()){
			loadingFrame.setVisible(true);
    		try {
				populate(parent);
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
        }
        else{
        	AuthenticationDialog dialog = new AuthenticationDialog(parent);
            dialog.setLocationRelativeTo(parent);
            dialog.setVisible(true);
            dialog.addWindowListener(new WindowAdapter(){
            	@Override
            	public void windowClosed(WindowEvent e){
					loadingFrame.setVisible(true);
            		try {
						populate(parent);
					} catch (Exception e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					}
            	}
            });
        }
    }
    
    private void populate(Frame parent) throws Exception{
		JSONObject graphJSON = exportNetworkToJSON();
		JSONObject styleJSON = exportStyleToJSON();
		String graphName = graphJSON.getJSONObject("data").getString("name");
		System.out.println(graphName);
		boolean isGraphPublic = false;
		if(Server.INSTANCE.updatePossible(graphName)){
			loadingFrame.dispose();
			JSONObject responseFromGraphSpace = Server.INSTANCE.client.getGraphByName(graphName);
			int isPublic = responseFromGraphSpace.getInt("is_public");
			if (isPublic==1){
				isGraphPublic = true;
			}
			UpdateGraphDialog updateDialog = new UpdateGraphDialog(parent, graphName, graphJSON, styleJSON, isGraphPublic, null);
			updateDialog.setLocationRelativeTo(parent);
			updateDialog.setVisible(true);
		}
		else{
			loadingFrame.dispose();
			PostGraphDialog postDialog = new PostGraphDialog(parent, graphName, graphJSON, styleJSON, isGraphPublic, null);
		    postDialog.setLocationRelativeTo(parent);
		    postDialog.setVisible(true);
		}
    }
    
    private JSONObject exportNetworkToJSON() throws IOException{
		File tempFile = File.createTempFile("CyGraphSpaceExport", ".cyjs");
		CyNetwork network = CyObjectManager.INSTANCE.getApplicationManager().getCurrentNetwork();
		TaskIterator ti = CyObjectManager.INSTANCE.getExportNetworkTaskFactory().createTaskIterator(network, tempFile);
		CyObjectManager.INSTANCE.getTaskManager().execute(ti);
		String graphJSONString = FileUtils.readFileToString(tempFile, "UTF-8");
		int count = 0;
		while(graphJSONString.isEmpty()){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			graphJSONString = FileUtils.readFileToString(tempFile, "UTF-8");
			count++;
			if (count>=10){
				return null;
			}
		}
		tempFile.delete();
		graphJSONString = graphJSONString.replaceAll("(?m)^*.\"shared_name\".*", "");
		graphJSONString = graphJSONString.replaceAll("(?m)^*.\"id_original\".*", "");
		graphJSONString = graphJSONString.replaceAll("(?m)^*.\"shared_interaction\".*", "");
		graphJSONString = graphJSONString.replaceAll("(?m)^*.\"source_original\".*", "");
		graphJSONString = graphJSONString.replaceAll("(?m)^*.\"target_original\".*", "");
		JSONObject graphJSON = new JSONObject(graphJSONString);
        return graphJSON;
	}
	
	private JSONObject exportStyleToJSON() throws IOException{
		File tempFile = File.createTempFile("CyGraphSpaceStyleExport", ".json");
		TaskIterator ti = CyObjectManager.INSTANCE.getExportVizmapTaskFactory().createTaskIterator(tempFile);
		CyObjectManager.INSTANCE.getTaskManager().execute(ti);
		String styleJSONString = FileUtils.readFileToString(tempFile, "UTF-8");
		int count = 0;
		while(styleJSONString.isEmpty()){
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			styleJSONString = FileUtils.readFileToString(tempFile, "UTF-8");
			count++;
			if (count>=10){
				return null;
			}
		}
		tempFile.delete();
		styleJSONString = styleJSONString.replaceAll("(?m)^*.\"shared_name\".*", "");
		styleJSONString = styleJSONString.replaceAll("(?m)^*.\"id_original\".*", "");
		styleJSONString = styleJSONString.replaceAll("(?m)^*.\"shared_interaction\".*", "");
		styleJSONString = styleJSONString.replaceAll("(?m)^*.\"source_original\".*", "");
		styleJSONString = styleJSONString.replaceAll("(?m)^*.\"target_original\".*", "");
		JSONArray styleJSONArray = new JSONArray(styleJSONString);
        return styleJSONArray.getJSONObject(0);
	}
	
}
