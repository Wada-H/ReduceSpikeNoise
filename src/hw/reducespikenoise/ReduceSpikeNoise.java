package hw.reducespikenoise;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;
import ij.process.LUT;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.stream.IntStream;

import static java.util.Comparator.naturalOrder;

public class ReduceSpikeNoise{
	ImagePlus imp;
	ImagePlus result_imp;
	ImagePlus pre_median;
	
	int width;
	int height;
	int stackSize;
	int c;
	int z;
	int t;
	int bitDepth;
	LUT[] luts;
	double maxIntensity;
	double shotnoiseLimit = 0.15; // rate of range
	
	double unitOfLimit;
	
	int limit_count = 1;
	int radius = 1;
	
	ArrayList<String> margin_fill_types = new ArrayList<String>(Arrays.asList("Zero","Reflect","Repeat"));
	String fill_type = "Zero";
	
	ArrayList<String> median_radius_types = new ArrayList<String>(Arrays.asList("Adjacent-4","Adjacent-8","SameRadius"));
	String median_radius = "Adjacent-8";
	
	public ReduceSpikeNoise(ImagePlus i){
		imp = i;
		result_imp = imp.duplicate();

		width = i.getWidth();
		height = i.getHeight();
		stackSize = i.getStackSize();
		c = i.getNChannels();
		z = i.getNSlices();
		t = i.getNFrames();
		luts = i.getLuts();
		bitDepth = i.getBitDepth();
		maxIntensity = i.getProcessor().getMax();
		unitOfLimit = maxIntensity * 0.01;
	}
	
	public ImagePlus reduceSpikeNoise(){
		long start_time = System.currentTimeMillis();

		
		IntStream s_stream = IntStream.rangeClosed(1, stackSize);
		
		ArrayList<Integer> count = new ArrayList<Integer>();
		s_stream.parallel().forEach(slice ->{

			reduceProcess(slice);			
			count.add(slice);
			double d = (double)count.size();
			IJ.showStatus("Now Processing..." + String.valueOf(Math.round((d/stackSize) * 100)) + "%");

		});
		
		
		result_imp.setTitle("ReduceSpikeNoise_" + "r-" + radius + "_l-" + limit_count  + "_" + imp.getTitle());
		long end_time = System.currentTimeMillis();
		System.out.println(end_time - start_time + "msec");
		return result_imp;
	}
	
	public void preview(){ //一枚の時の対応を

		int slice = imp.getCurrentSlice();
		ImageProcessor reduce_ip = this.getReducedProcessor(slice);
		
		if(stackSize == 1){
			//imp.getStack().setProcessor(reduce_ip, slice);
			imp.setProcessor(reduce_ip);
		}else{
			if(slice == stackSize){
				imp.setPosition(slice - 1); //この仕組をしないとHyperstack時にupdateAndDraw()では反映しない
				imp.getStack().setProcessor(reduce_ip, slice);
				imp.setPosition(slice);			
			}else{
				imp.setPosition(slice + 1); //この仕組をしないとHyperstack時にupdateAndDraw()では反映しない
				imp.getStack().setProcessor(reduce_ip, slice);
				imp.setPosition(slice);
			}	
		}
		imp.updateAndDraw();
	}

	
	public ImageProcessor getReducedProcessor(int slice){
		reduceProcess(slice);
		return result_imp.getStack().getProcessor(slice);
	}
	
	
	public void reduceProcess(int slice){
		ImageProcessor ip = imp.getStack().getProcessor(slice);
		ImageProcessor result_ip = result_imp.getStack().getProcessor(slice);
		IntStream x_stream = IntStream.range(0, width);


		x_stream.parallel().forEach(x -> {
		//for(int x = 0; x < width; x++){


            for(int y = 0; y < height; y++){
				int value = ip.get(x, y);

				ArrayList<Integer> around_array = this.getAround(ip, x, y);
				ArrayList<Integer> for_median_array;



				if(median_radius == "Adjacent-4"){
					for_median_array = this.getAround4(ip, x, y);
				}else if(median_radius == "Adjacent-8"){
					for_median_array = this.getAround8(ip, x, y);
				}else{
					for_median_array = around_array;
				}

				if(isNoise(value, around_array) == true){ //ノイズ判定でtrueの場合処理する

					int median_value = this.getMedian(value, for_median_array);
					result_ip.set(x, y, median_value);

				}



			}
		//}
		});
	}
		
	
	public void setFillType(String type){
		fill_type = type;
	}
	public void setRadius(int n){
		radius = n;
	}
	
	public void setLimtCount(int n){
		limit_count = n;
	}
	
	public void setMedianRadius(String type){
		median_radius = type;
	}
	
	public boolean isNoise(int center, ArrayList<Integer> around){

		int threshold_value_high;
		int threshold_value_low;
		boolean check = true;

		//center値と周りの最大値との差によってふるい分ける
		
		if(center == maxIntensity){ //この分岐でなにやらおかしい。処理する毎に若干ずれる。
			threshold_value_high = (int)(maxIntensity);
			threshold_value_low = (int)(maxIntensity - unitOfLimit);
		}else if(center == 0){
			threshold_value_high = (int)(unitOfLimit);
			threshold_value_low = 0;
		}else{
			threshold_value_high = (int)(center * (1.0 + shotnoiseLimit)); 
			threshold_value_low = (int)(center * (1.0 - shotnoiseLimit));
		}
		


		int hit = 0;

		//System.out.println("center:" + center + ",threshold:" + threshold_value_high + "-" + threshold_value_low);
		for(int value: around){

			if((threshold_value_low <= value)&&(value <= threshold_value_high)){
				hit++;
			}
			if(hit >= limit_count){
				check = false;
				break;
			}
		}

        return check;
	}
		
	
	public ArrayList<Integer> getAround(ImageProcessor ip, int x, int y){

		ArrayList<Integer> int_array = new ArrayList<Integer>();
		
		int start_position_x = x - radius;
		int start_position_y = y - radius;
		int end_position_x = x + radius;
		int end_position_y = y + radius;
		
		for(int cx = start_position_x; cx < end_position_x + 1; cx++){
			for(int cy = start_position_y; cy < end_position_y + 1; cy++){
				
				if((cx == x)&&(cy == y)){
					//とばす
				}else{
					int value = 0;
					if((cx < 0) | (cy < 0) | (cx > width) | (cy > height)){
						value = this.getMarginValue(ip, cx, cy);
					}else{
						value = ip.getPixel(cx, cy);
					}
					int_array.add(value);
				}
			}
			
		}


		return int_array;
	}
	
	public ArrayList<Integer> getAround8(ImageProcessor ip, int x, int y){

        ArrayList<Integer> int_array = new ArrayList<Integer>();
		
		int start_position_x = x - 1;
		int start_position_y = y - 1;
		int end_position_x = x + 1;
		int end_position_y = y + 1;
		
		for(int cx = start_position_x; cx < end_position_x + 1; cx++){
			for(int cy = start_position_y; cy < end_position_y + 1; cy++){
				
				if((cx == x)&&(cy == y)){
					//とばす
				}else{
					int value = 0;
					if((cx < 0) | (cy < 0) | (cx > width) | (cy > height)){
						value = this.getMarginValue(ip, cx, cy);
					}else{
						value = ip.getPixel(cx, cy);
					}
					int_array.add(value);
				}
			}
			
		}

		return int_array;
	}



	
	public ArrayList<Integer> getAround4(ImageProcessor ip, int x, int y){
		ArrayList<Integer> int_array = new ArrayList<Integer>();
		
		int start_position_x = x - 1;
		int start_position_y = y - 1;
		int end_position_x = x + 1;
		int end_position_y = y + 1;
		
		for(int cx = start_position_x; cx < end_position_x + 1; cx++){
			for(int cy = start_position_y; cy < end_position_y + 1; cy++){
				
				if((cx == x)&&(cy == y)){
					//とばす
				}else if((cx == x) | (cy == y)){
					int value = 0;
					if((cx < 0) | (cy < 0) | (cx > width) | (cy > height)){
						value = this.getMarginValue(ip, cx, cy);
					}else{
						value = ip.getPixel(cx, cy);
					}
					int_array.add(value);
				}
			}
			
		}

		return int_array;
	}
	
	
	public int getMedian(int center, ArrayList<Integer> around_array){
		int result;
		around_array.add(center);
		int median = (around_array.size()) / 2 ;
		
		around_array.sort(naturalOrder());//どちらも同じくらいのようだ。
		//Collections.sort(around_array); //どうやらこのソートが遅い？
		result = around_array.get(median);

        return result;
	}

	public int getMedian(int center, TreeSet<Integer> around_array){
		int result = 0;
		Iterator<Integer> itr = around_array.iterator();
		for(int i = 0; i < center; i++){
			result = itr.next();
		}
		return result;
	}


	public int getMarginValue(ImageProcessor ip, int x, int y){
		
		int result = 0;
		int fill_type_index = margin_fill_types.indexOf(fill_type);
		
		switch(fill_type_index){
			case 0: //Zero
				result = 0;
				break;
				
			case 1: //Reflect
				int reflect_x = x;
				int reflect_y = y;
				if(x < 0){
					reflect_x = - x;
				}
				if(y < 0){
					reflect_y = - y;
				}
				
				if(x > (width-1)){

					reflect_x = width - (x - (width-1));
				}
				
				if(y > (height-1)){
					reflect_y = height - (y - (height-1));
				}
				result  = ip.get(reflect_x, reflect_y);
				break;
			case 2: //Repeat
				int repeat_x = x;
				int repeat_y = y;
				if(x < 0){
					repeat_x = 0;
				}
				if(y < 0){
					repeat_y = 0;
				}
				
				if(x > (width-1)){
					repeat_x = (width-1);
				}
				
				if(y > (height-1)){
					repeat_y = (height-1);
				}
				
				result  = ip.get(repeat_x, repeat_y);
				
				
				break;
		}
		
		return result;
	}
	

}