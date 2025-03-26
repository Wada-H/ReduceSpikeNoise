package hw.reducespikenoise;

import static java.util.Comparator.naturalOrder;

import java.util.ArrayList;
import java.util.stream.IntStream;

//import com.amd.aparapi.Kernel;
import com.aparapi.Kernel;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;

/*
 * ����������萧�����������BGPU�ɔC���܂ł̃f�[�^�̐������H�v����K�v������B
 * ���ꂪ����Ƃ���B�܂��A1��ڂ��܂������Ă���ꍇ�ł�2��ڂɃG���[���ł邱�Ƃ��B
 * 
 * aparapi�̍ŐV1.40 or 41�@�ł͑������z���������B
 * sorce����build����ꍇ�Agit�Ń_�E�����[�h����com.amd.aparapi.jni��build.xml����<target name="gcc_mac" if="use.gcc_mac">���ڂɉ�����
 * <arg value="-I/Applications/Xcode.app/Contents/Developer/Platforms/MacOSX.platform/Developer/SDKs/MacOSX.sdk/System/Library/Frameworks/JavaVM.framework/Versions/A/Headers" />
 * ��ǉ�����K�v����B*jni.h not found���ł邽�߁B
 * ����build�����s�����libaparapi_x86_64.dylib������Ȃ��B
 * 
 * *��L���C�����āA�ŏ�ʂ̃f�B���N�g���Ɉڂ肻���� ant dist �Ŋ�{�I�ɂ�o.k
 */


public class ReduceSpikeNoiseKernel extends Kernel{
	
	ImageProcessor ip;
	float[] imageArray;
	int width;
	int height;
	int roiWidth;
	int roiSize;
	int startR;
	int endR;
	int unitWidth;
	int unitHeight;
	int middlePoint;
	int radius;
	

	float[] aroundArray;
	float[] sortedArray;

	//float[] resultFloatArray;
	
	// �������e�X�g //
	float[][][] imageFloatArray;
	float[][][] resultFloatArray;
	
	int[][][] roiRegionArray;
	
	float[][] testImage;

	float[][] around2DArray;
	float[][][] around3DArray;

	float[][][] around8Array;
	float[][][] around4Array;

	float[][] sorted2DArray;
	float[][][] sorted3DArray;
	float[][] testResult;
	
	/*
	public ImageProcessor reduceSpikeNoise(ImageProcessor p, int r, int aroundType) {
		ImageProcessor result = p;
		
		ip = p;
		radius = r;
		
		imageArray = this.convertToFlatArray(ip.getFloatArray());
		width = ip.getWidth();
		height = ip.getHeight();
		roiWidth = (radius * 2) + 1;
		roiSize = roiWidth * roiWidth; //radius 1 = 3 x 3 = 9
		startR = -radius;
		endR = radius;
		unitWidth = width;
		unitHeight = height;
		middlePoint = (roiSize) / 2;
		
		aroundArray = new float[(roiSize -1) * width * height]; //�����ł̎w��͕ςȂ��ƂɂȂ�C�����邪�A�A�Arun����new�ł��Ȃ��A�A�A ->���ׂĂ�1�����z��ɋl�ߍ��ށH�摜���傫����overflow�̉\������B
		sortedArray = new float[(roiSize) * width * height]; //�����̂��Ă��ł����̂��H
		
		resultFloatArray = new float[width * height];
		
		// �������e�X�g //
		testImage = ip.getFloatArray();
		around3DArray = new float[width][height][roiSize];
		around8Array = new float[width][height][8];
		around4Array = new float[width][height][];
		sorted3DArray = new float[width][height][roiSize];
		testResult = new float[width][height];
		
		//this.put(testArray);//���ܓǂݍ���ł���o�[�W�����ł͖������ۂ��B1.40�Ȃ炢�������B
		
		//this.execute(width * height);
		aroundArray = new float[roiSize];
		this.put(testImage);
		this.put(around3DArray);
		this.put(around8Array);
		this.put(around4Array);
		this.put(sorted3DArray);
		this.put(testResult);
		//this.execute(width, height);
		//System.out.println(this.isRunningCL());
		
		//float[][] result2Darray = this.convertTo2DArray(resultFloatArray, width, height);
		//System.out.println(result2Darray[0][0]);
		
		//result.setFloatArray(result2Darray);
		//ImagePlus test = new ImagePlus();
		//test.setProcessor(result);
		//test.show();
		
		result.setFloatArray(testResult);		
		
		return result;
	}
		
	@Override
	public void run() {
		
		
		int xPosition = this.getGlobalId();
		int yPosition = this.getPassId();
				
		int startX = xPosition - radius;
		int startY = yPosition - radius;
		int endX = xPosition + radius;
		int endY = yPosition + radius;
		
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
		
		
		int count = 0;
		for(int ry = startY; ry < endY + 1; ry++) {
			for(int rx = startX; rx < endX + 1; rx++) {
				if((rx == xPosition)&&(ry == yPosition)) {
					//���ډ�f�͂Ƃ΂�
				}else {
					around3DArray[xPosition][yPosition][count] = testImage[rx][ry];
					count = count + 1;
				}
			}
		}
		
		
		float value = testImage[xPosition][yPosition];
		
		if(this.checkNoise(value, around3DArray[xPosition][yPosition])) {
			value = this.getMedian(around3DArray[xPosition][yPosition], sorted3DArray[xPosition][yPosition], value);
		}
		testResult[xPosition][yPosition] = value;
		 
	}
	
	
	/*
	@Override
	public void run() {
		
		int pixelIndex = getGlobalId();
		int index = 0;
		int count = 0;
		float value = imageArray[pixelIndex];
		//System.out.println(pixelIndex);
		for(int h = startR; h <= endR; h++) {
			for(int w = startR; w <= endR; w++) {
				if((h != 0)||(w != 0)) {
					index = (pixelIndex + w) + (h * unitWidth); //����index����edge���������܂��o���Ȃ��̂ōŏ��ɂ��Ƃ̉摜�ɗ]����������摜��p���邱�Ƃŉ���ł�����
					if((index < 0)||(index >= unitWidth*unitHeight)) {
						aroundArray[count] = 0;
					}else {
						aroundArray[count] = imageArray[index];
					}
					count = count + 1;					
				}
			}
		}
		if(checkNoise(value, aroundArray)) {
			resultFloatArray[pixelIndex] = this.getMedian(value);
			if((pixelIndex % 100)==0) {
				resultFloatArray[pixelIndex] = 200;
			}
			//value = 0.0f;									
		}else {
			resultFloatArray[pixelIndex] = value;
		}
		
	}
	*/

	
	
	public void reduceSpikeNoise(ImageStack stackImage, int r, int aroundType) {
		int stackSize = 50;//stackImage.getSize();
		radius = r;
		roiWidth = (radius * 2) + 1;
		roiSize = roiWidth * roiWidth; //radius 1 = 3 x 3 = 9
		
		width = stackImage.getWidth();
		height = stackImage.getHeight();
		
		around2DArray = new float[width*height][roiSize];
		around3DArray = new float[stackSize][width*height][roiSize];
		sorted2DArray = new float[width*height][roiSize];
		sorted3DArray = new float[stackSize][width*height][roiSize];
		roiRegionArray = new int[width][height][4];
		
		imageFloatArray = new float[stackSize][width][height];
		resultFloatArray = new float[stackSize][width][height];

		// convert image to float array //
		IntStream i_stream = IntStream.range(0, stackSize);
		i_stream.parallel().forEach(i ->{
			imageFloatArray[i] = stackImage.getProcessor(i+1).getFloatArray();
		});
		
		// create roi array // int[x][y][startX, endX, startY, endY]
		IntStream r_stream = IntStream.range(0, height);
		r_stream.parallel().forEach(y -> {
			for(int x = 0; x < width; x++) {
			
				int xPosition = x;
				int yPosition = y;
				
				int startX = xPosition - radius;
				int startY = yPosition - radius;
				int endX = xPosition + radius;
				int endY = yPosition + radius;
				
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
			
				roiRegionArray[x][y][0] = startX;
				roiRegionArray[x][y][1] = endX;
				roiRegionArray[x][y][2] = startY;
				roiRegionArray[x][y][3] = endY;
			}			
		});

		this.setExplicit(true); //�����I�������ړ�
		
		this.put(imageFloatArray);
		//this.put(resultFloatArray);
		this.put(around2DArray);
		//this.put(around3DArray);
		this.put(sorted3DArray);
		this.put(roiRegionArray);

		this.execute(width*height, stackSize);
		
		// �Q�Ƃ���K�v�̂Ȃ����̂�CPU���֖߂��Ȃ� //
		
		this.get(imageFloatArray);
		//this.get(resultFloatArray);
		//this.get(around3DArray);
		//this.get(roiRegionArray);
		


		
		// after execute, convert old images to new images //
		IntStream n_stream = IntStream.range(0, stackSize);
		n_stream.parallel().forEach(i ->{
			stackImage.getProcessor(i+1).setFloatArray(imageFloatArray[i]);
		});
		
		
	}
	
	
	@Override
	public void run() {
	
		
		
		int pixelIndex = this.getGlobalId();
		int stackIndex = this.getPassId();

		
		int xPosition = pixelIndex % width; // pixelIndex ���狁�߂���H
		int yPosition = pixelIndex / width;
		

		
		int startX = roiRegionArray[xPosition][yPosition][0];
		int endX = roiRegionArray[xPosition][yPosition][1];
		
		int startY = roiRegionArray[xPosition][yPosition][2];
		int endY = roiRegionArray[xPosition][yPosition][3];

		
		int count = 0;
		for(int ry = startY; ry < endY + 1; ry++) {
			for(int rx = startX; rx < endX + 1; rx++) {
				if((rx == xPosition)&&(ry == yPosition)) {
					//���ډ�f�͂Ƃ΂�
				}else {
					//around3DArray[stackIndex][pixelIndex][count] = 0; //���ꂪ�x���B���̔z��ւ̑���Ɏ��Ԃ��|������ۂ��B�B�B
					//float v = imageFloatArray[stackIndex][rx][ry]; //����قǒx���͂Ȃ�
					//around3DArray[stackIndex][pixelIndex][count] = imageFloatArray[stackIndex][rx][ry]; //����Ɏ��Ԃ�������悤���B
					around2DArray[pixelIndex][count] = imageFloatArray[stackIndex][rx][ry];
					count = count + 1;
				}
			}
		}
		
		
		float value = imageFloatArray[stackIndex][xPosition][yPosition];
		
		
		if(this.checkNoise(value, around2DArray[pixelIndex])) {
			value = this.getMedian(around2DArray[pixelIndex], sorted2DArray[pixelIndex], value);
		}
		
		//resultFloatArray[stackIndex][xPosition][yPosition] = value;
		imageFloatArray[stackIndex][xPosition][yPosition] = value;
		
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
	
	public boolean checkNoise(float v, float[] aroundArray) {
		boolean b = true;
		
		return b;
	}

	public float getMedian(float[] around, float[] sort, float v) {
		
		
		for(int i = 0; i < roiSize-1; i++) { //aroundArray.length���ƃG���[�o��
			sort[i] = around[i];
		}
		sort[roiSize-1] = v;
		
		this.sort(sort);
		
		return sort[middlePoint];
		//return sort[roiSize-1]; //���̒l�̂͂�
	}
	
	public void sort(float[] array) {
	    float tmp;
	    int j;
	    
	    for(int i = 1 ; i < roiSize ; i++){
			tmp = array[i] ;
			j = i-1 ;
			
			while( j >= 0 && array[j] > tmp ){
			    array[j+1] = array[j] ;
			    j-- ;
			}
			array[j+1] = tmp ;
	    }
	}
	

	
	
	
}
