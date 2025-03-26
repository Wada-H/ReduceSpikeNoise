package hw.reducespikenoise;

import static java.util.Comparator.naturalOrder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

//import com.amd.aparapi.Kernel;
//import com.amd.aparapi.Kernel.EXECUTION_MODE;
import com.aparapi.Kernel;


import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.Roi;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.LUT;

/*
 * aparapi使用を考えたが、うまくいかない。
 * aparapi用に考えた方法をcpuのみに応用してみる。
 */

public class ReduceSpikeNoiseAparapi {
	
	ImagePlus imp;
	
	
	int limit_count = 1;
	int radius = 1;

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

	
	ArrayList<String> margin_fill_types = new ArrayList<String>(Arrays.asList("Zero","Reflect","Repeat"));
	String fill_type = "Zero";
	
	ArrayList<String> median_radius_types = new ArrayList<String>(Arrays.asList("Adjacent-4","Adjacent-8","SameRadius"));
	String median_radius = "Adjacent-8";
	
	
	public ReduceSpikeNoiseAparapi(ImagePlus i) {
		imp = i;
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

		ImageStack stackImage = imp.duplicate().getStack();
		//ImageStack stackImage = new ImageStack(width, height);
		ImagePlus result = new ImagePlus();

		final int roiWidth = (radius * 2) + 1;
		final int roiSize = roiWidth * roiWidth; //radius 1 = 3 x 3 = 9
		final int startR = -radius;
		final int endR = radius;
		final int unitWidth = width;
		final int unitHeight = height;

		///*
		//IntStream i_stream = IntStream.range(0, 50);
		//i_stream.parallel().forEach(n ->{


			///*//GPGPU
			//float[] imageArray = this.convertToFlatArray(imp.getStack().getProcessor(n+1).getFloatArray());
			//float[] resultFloatArray = new float[width * height];			
			//ReduceSpikeNoiseKernel rsnk = new ReduceSpikeNoiseKernel();
			
			//ImageProcessor ip = stackImage.getProcessor(n+1);
			//rsnk.reduceSpikeNoise(ip, radius, 2);
						
			//stackImage.setProcessor(ip, n+1);
			
			//*/
			
			/*//通常バージョン
			for(int y = 0; y < height; y++) {
				for(int x = 0; x < width; x++) {
					float[] around_array = new float[roiSize -1];
					float[] sorted_array = new float[roiSize];
					int pixelIndex = (y * width) + x;
					float value = imageArray[pixelIndex];

					int count = 0;
					for(int h = startR; h <= endR; h++) { // around_arrayの取得
						for(int w = startR; w <= endR; w++) {
							
							if((h != 0)||(w != 0)) {
								int index = (pixelIndex + w) + (h * unitWidth); //このindexだとedge部分がうまく出来ないので最初にもとの画像に余白を作った画像を用いることで回避できそう
								if((index < 0)||(index >= unitWidth*unitHeight)) {
									around_array[count] = 0;	
								}else {
									around_array[count] = imageArray[index];
								}
								count = count + 1;
							}							
						}
					}
					
					if(checkNoise(value, around_array)) {
						resultFloatArray[pixelIndex] = this.getMedian(value, around_array, sorted_array);
						//value = 100.0f;
						//resultFloatArray[pixelIndex] = value;
					}else {
						resultFloatArray[pixelIndex] = value;
					}
										
				}
			}
			stackImage.getProcessor(n+1).setFloatArray(this.convertTo2DArray(resultFloatArray, width, height));
			//ImageProcessor test = new FloatProcessor(this.convertTo2DArray(resultFloatArray, width, height));
			//stackImage.addSlice(test);
			*/
			
			/*//通常バージョン第二弾
			
			ArrayList<ArrayList<Integer>> buffList = this.makeAroundList(imp.getStack().getProcessor(n+1)); //ここが激オソ。。。
			int[] resultIntArray = new int[width * height];
			//float[] test = this.convertToFloatArray(buffList);
			
			for(int m = 0; m < width*height; m++) {
				ArrayList<Integer> list = buffList.get(m);
				System.out.println(list.size());
				int medianPoint = (int)(list.size() / 2.0);
				list.sort(naturalOrder());
				int meanValue = list.get(medianPoint);
				resultIntArray[m] = meanValue;				
			}
			
			stackImage.getProcessor(n+1).setIntArray(this.convertTo2DArray(resultIntArray, width, height));
			*/
		//});
		
		/* ImageStackで全部GPUへ */
		
		ReduceSpikeNoiseKernel rsnk = new ReduceSpikeNoiseKernel();
		rsnk.reduceSpikeNoise(stackImage, radius, 2);	
		
		result.setStack(stackImage);
		result.setDimensions(c, z, t);
		result.setTitle("ReduceSpikeNoise_" + "r-" + radius + "_l-" + limit_count  + "_" + imp.getTitle());
		rsnk.dispose();
		long end_time = System.currentTimeMillis();
		System.out.println(end_time - start_time + "msec");
		
		
		
		return result;
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
	
	public float[] convertToFlatArray(float[][] f_array) {
		float[] result = new float[(f_array.length * f_array[0].length)] ;
		
		IntStream i_stream = IntStream.range(0, f_array.length);
		i_stream.parallel().forEach(x ->{
			for(int y = 0; y < f_array[0].length; y++) {
				int index = (y * width) + x;

				result[index] = f_array[x][y];
			}
			
		});
		
		return result;
	}
	
	public float[][] convertTo2DArray(float[] f_array, int xlength, int ylength) {
		float[][] result = new float[xlength][ylength];
		
		IntStream i_stream = IntStream.range(0, xlength);
		i_stream.parallel().forEach(x ->{
			for(int y = 0; y < ylength; y++) {
				int index = (y * width) + x;
				result[x][y] = f_array[index];
			}
			
		});
		return result;
	}
	
	public int[][] convertTo2DArray(int[] i_array, int xlength, int ylength) {
		int[][] result = new int[xlength][ylength];
		
		IntStream i_stream = IntStream.range(0, xlength);
		i_stream.parallel().forEach(x ->{
			for(int y = 0; y < ylength; y++) {
				int index = (y * width) + x;
				result[x][y] = i_array[index];
			}
			
		});
		return result;
	}
	
	
	public boolean checkNoise(float v, float[] aroundArray) {
		boolean b = true;
		
		return b;
	}

	public float getMedian(float v, float[]around_array, float[] sorted_array) {
		int[] intArray = new int[sorted_array.length];
		
		int middlePoint = (int)(sorted_array.length / 2.0);
		for(int i = 0; i < around_array.length; i++) {
			sorted_array[i] = around_array[i];
		}
		sorted_array[sorted_array.length -1] = v;
		
		//this.quick_sort(sorted_array, 0, sorted_array.length -1); //再帰、同class内
		//v = sorted_array[middlePoint];
		//return sorted_array[middlePoint];
		//return sorted_array[roiSize-1]; //元の値のはず
		
		
		// 外部クラス 非再帰//
		for(int i = 0; i < intArray.length; i++) {
			intArray[0] = (int)sorted_array[0];
		}
		QuickSort qs = new QuickSort(intArray);
		qs.sort();
		v = intArray[middlePoint];
		return (float)intArray[middlePoint];

	}
	
	
    public void quick_sort(float[] d, int left, int right) {
    	
        if (left>=right) {
            return;
        }
        int p = (int)d[(left+right)/2];
        int l = left, r = right;
        	float tmp;

        while(l<=r) {
            while(d[l] < p) { l++; }
            while(d[r] > p) { r--; }
            if (l<=r) {
                tmp = d[l];
                d[l] = d[r];
                d[r] = tmp;
                l++;
                r--;
            }
        }
        quick_sort(d, left, r);  // ピボットより左側をクイックソート
        quick_sort(d, l, right); // ピボットより右側をクイックソート
    
    }
	
    public ImagePlus expandImage(ImagePlus image, String type) {
    		ImagePlus result = new ImagePlus();
    		return result;
    }
    
    /*
    public ArrayList<ArrayList<Integer>> makeAroundList(ImageProcessor ip) {//しこたま時間ががかる、、、ダメだ。
    		int roiSize = ((radius * 2) * (radius * 2))  -1;
    		
    		ArrayList<ArrayList<Integer>> result = new ArrayList<ArrayList<Integer>>();

    		IntStream buff_stream = IntStream.range(0, (width * height));
    		buff_stream.parallel().forEach(i ->{
    			ArrayList<Integer> buff = new ArrayList<Integer>();
    			result.add(buff);
    		});
    		
    		//System.out.println(result.size());
    		IntStream i_stream = IntStream.range(0, height);
    		i_stream.parallel().forEach(y ->{
    			for(int x = 0; x < width; x++) {
    				int startX = x - radius;
    				int startY = y - radius;
    				int endX = x + radius;
    				int endY = y + radius;
    				
    				if(startX < 0) {
    					startX = 0;
    				}else if(endX > width - 1 ) {
    					endX = width - 1;
    				}
    					
    				if(startY < 0) {
    					startY = 0;
    				}else if(endY > height - 1) {
    					endY = height - 1;
    				}
    				
    				ArrayList<Integer> buffList = new ArrayList<Integer>();
    	    			for(int ry = startY; ry < endY + 1; ry++) {
    	    				for(int rx = startX; rx < endX + 1; rx++) {
    	    					if((rx == x)&&(ry == y)) {
    	    						//注目画素はとばす
    	    					}else {
    	    						buffList.add(ip.get(rx, ry));
    	    					}

    	    				}
    	    			}
    	    			int fill_type_index = margin_fill_types.indexOf(fill_type);
    	    			if(fill_type_index == 0) { //周囲にゼロ代入タイプ
	    	    			int sub = roiSize - buffList.size();
	    				if(sub > 0) {
	    					for(int i = 0; i < sub; i++) {
	    						buffList.add(0);    						
	    					}
	    				}
    	    			}
    	    			int index = (y * width) + x;
      			result.add(index, buffList);
    			}
    		});
    		    		
    		return result;
    }
    */
        
    
    public float[] convertToFloatArray(ArrayList<ArrayList<Integer>> list) {
    		float[] result = new float[list.get(0).size() * list.size()];
    		IntStream i_stream = IntStream.range(0, list.size());
    		i_stream.parallel().forEach(p -> {
    			int startIndex = 0;
    			for(int i = 0; i < p; i++) { //list.get()でサイズ違いがあるため
    				startIndex = startIndex + list.get(i).size();
    			}
    			for(int i = 0; i < list.get(p).size(); i++) {
    				result[startIndex + i] = (float)list.get(p).get(i);
    			}
    			
    		});
    		
    		return result;
    }
	
	
}
