import hw.reducespikenoise.ReduceSpikeNoise;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.plugin.PlugIn;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;


/*20170928 Aparapi version 開発開始
 *
 * Aparapiは保留
 *
 * 20201008 0および最大値の場合、> ro <を用いてい判定していたものを >=, <=に変更
 * *Dup地獄がpreviewごとに起こる不具合あり
 */


public class ReduceSpikeNoise_ implements PlugIn, ItemListener{
	ImagePlus imp;
	
	
	String version = "1.1";
	int radius = 4;
	int limit_count = 1;
	
	String[] fill_types = {"Repeat","Zero","Reflect"};
	String selected_fill_type = "Zero";

	String[] median_types = {"Adjacent-4","Adjacent-8","SameRadius"};
	String selected_median_radius = "Adjacent-8";
	
    ///// dialog用 ////
    Panel gd_panel;
    JLabel radius_label;
    JComboBox<String> radius_box;

    JLabel limit_label;
    JComboBox<String> limit_box;
    
    JLabel fill_type_label;
    JComboBox<String> fill_type_box;

    JLabel median_radius_label;
    JComboBox<String> median_radius_box;
    
    JLabel preview_label;
    JCheckBox preview_check;
    //////////////////
	
	//// preview ////
    ImagePlus imp_buff_for_preview;
    
	
	public ReduceSpikeNoise_(){
		
	}
	
	public void run(String arg){
		//現在のイメージの取得
		imp = WindowManager.getCurrentImage();

		if (imp == null) {
			IJ.noImage();
			return;
		}

		if(imp.getBitDepth() >= 24){
			IJ.showMessage("Please convert to 8bit or 16bit image.");
			return;
		}
		
		if(showDialog()){
			long start_time = System.currentTimeMillis();

			
			ReduceSpikeNoise rsn = new ReduceSpikeNoise(imp);
			//ReduceSpikeNoiseAparapi rsn = new ReduceSpikeNoiseAparapi(imp);


			rsn.setRadius(radius);
			rsn.setLimtCount(limit_count);
			rsn.setFillType(selected_fill_type);
			rsn.setMedianRadius(selected_median_radius);
			ImagePlus newImage = rsn.reduceSpikeNoise();
			newImage.show();
			newImage.setPosition(imp.getCurrentSlice()); //showしてらかでないと、場所は反映されるがスライダーの位置が何をしても反映されない
			newImage.updateAndDraw();

			long end_time = System.currentTimeMillis();
			IJ.showStatus(end_time - start_time + "msec");

		}else{
			return;
		}
	}	

	public boolean showDialog(){
        GenericDialog gd = new GenericDialog("ReduceSpikeNoise ver." + version, IJ.getInstance());

    	//FlowLayout gd_layout = new FlowLayout();
        GridLayout gd_layout = new GridLayout(5,1);
    	//gd_layout.setAlignment(FlowLayout.RIGHT);
        
    	gd_panel = new Panel(gd_layout); ///Panelでないとボタンが効かなくなる、、、、
    	gd_panel.setPreferredSize(new Dimension(250, 150));
        
    	/// Radius box ///
    	JPanel radius_panel = new JPanel(new GridLayout(1,2));
    	radius_panel.setPreferredSize(new Dimension(100, 50));

    	radius_label = new JLabel("Radius");
    	radius_label.setVerticalAlignment(JLabel.CENTER);
    	radius_label.setHorizontalAlignment(JLabel.CENTER);
    	radius_box = new JComboBox<String>();
    	radius_box.addItemListener(this);

    	for(int i = 1; i < 11; i++){
    		radius_box.addItem(String.valueOf(i));
    	}
    	radius_panel.add(radius_label);
    	radius_panel.add(radius_box);

    	radius_box.setSelectedIndex(radius -1);

    	/// Limit box ///
    	JPanel limit_panel = new JPanel(new GridLayout(1,2));
    	limit_panel.setPreferredSize(new Dimension(100, 50));
    	limit_label = new JLabel("Limit");
    	limit_label.setVerticalAlignment(JLabel.CENTER);
    	limit_label.setHorizontalAlignment(JLabel.CENTER);
    	limit_box = new JComboBox<String>();
    	for(int i = 1; i < 9; i++){
    		limit_box.addItem(String.valueOf(i));
    	}
    	limit_panel.add(limit_label);
    	limit_panel.add(limit_box);
    	
    	/// fill type box ///
    	JPanel fill_type_panel = new JPanel(new GridLayout(1,2));
    	fill_type_panel.setPreferredSize(new Dimension(100, 50));

    	fill_type_label = new JLabel("MarginFillType");
    	fill_type_label.setVerticalAlignment(JLabel.CENTER);
    	fill_type_label.setHorizontalAlignment(JLabel.CENTER);
    	fill_type_box = new JComboBox<String>();
    	fill_type_box.addItem(fill_types[0]);
    	fill_type_box.addItem(fill_types[1]);
    	fill_type_box.addItem(fill_types[2]);
    	fill_type_panel.add(fill_type_label);
    	fill_type_panel.add(fill_type_box);
    	
    	fill_type_box.setSelectedItem(selected_fill_type);

    	/// median radius box ///
    	JPanel median_radius_panel = new JPanel(new GridLayout(1,2));
    	fill_type_panel.setPreferredSize(new Dimension(100, 50));

    	median_radius_label = new JLabel("MedianType");
    	median_radius_label.setVerticalAlignment(JLabel.CENTER);
    	median_radius_label.setHorizontalAlignment(JLabel.CENTER);
    	median_radius_box = new JComboBox<String>();
    	median_radius_box.addItem(median_types[0]);
    	median_radius_box.addItem(median_types[1]);
    	median_radius_box.addItem(median_types[2]);
    	median_radius_panel.add(median_radius_label);
    	median_radius_panel.add(median_radius_box);
    	median_radius_box.setSelectedItem(selected_median_radius);
    	
    	/// preview checkbox ///
    	JPanel preview_panel = new JPanel(new GridLayout(1,2));
    	preview_panel.setPreferredSize(new Dimension(100, 50));

    	preview_label = new JLabel("Preview");
    	preview_label.setVerticalAlignment(JLabel.CENTER);
    	preview_label.setHorizontalAlignment(JLabel.CENTER);
    	preview_check = new JCheckBox();
    	preview_check.addItemListener(this);
    	preview_panel.add(preview_label);
    	preview_panel.add(preview_check);
    	
    	/// add items ///
    	gd_panel.add(radius_panel);
    	gd_panel.add(limit_panel);
    	gd_panel.add(fill_type_panel);
    	gd_panel.add(median_radius_panel);
    	gd_panel.add(preview_panel);
    	
    	gd.addPanel(gd_panel);
    	

    	
        gd.showDialog();
        
        
        if (gd.wasCanceled()) {
        	if(preview_check.isSelected()){
        		removePreview();
        	}
            return false;
        }
        
        if(preview_check.isSelected()){
        	removePreview();
        }
        
        radius = radius_box.getSelectedIndex() + 1;
        limit_count = limit_box.getSelectedIndex() + 1;
        selected_fill_type = fill_types[fill_type_box.getSelectedIndex()];
        selected_median_radius = median_types[median_radius_box.getSelectedIndex()];

        return true;
	}

	@Override
	public void itemStateChanged(ItemEvent e) {
		if(e.getSource() == radius_box){
			
			int r = radius_box.getSelectedIndex() + 1;
			int limit = (int)Math.pow((r*2)+1, 2);
			if(limit_box != null){
				limit_box.removeAllItems();
				for(int i = 1; i < limit; i++){
					limit_box.addItem(String.valueOf(i));
				}
			}
		}else if(e.getSource() == preview_check){
			if(preview_check.isSelected()){
				setPreview();
			}else{
				removePreview();
			}
		}
	}
	
	public void setPreview(){
		imp_buff_for_preview = imp.duplicate();

        radius = radius_box.getSelectedIndex() + 1;
        limit_count = limit_box.getSelectedIndex() + 1;
        selected_fill_type = fill_types[fill_type_box.getSelectedIndex()];
        selected_median_radius = median_types[median_radius_box.getSelectedIndex()];

		ReduceSpikeNoise rsn = new ReduceSpikeNoise(imp);
		rsn.setRadius(radius);
		rsn.setLimtCount(limit_count);
		rsn.setFillType(selected_fill_type);
		rsn.setMedianRadius(selected_median_radius);
		rsn.preview();
	}
	
	public void removePreview(){
		imp.setImage(imp_buff_for_preview);
	}
	
}