package hw.reducespikenoise;

/*
 * http://d.hatena.ne.jp/kkobayashi_a/20070104/p1
 * 
 */

public abstract class Sort {
	   protected int a[];

	   Sort(int array[]){
	      a = new int[array.length];
	      for(int i=0; i<array.length; i++){
	         a[i] = array[i];
	      }
	   }
	   public abstract void sort();
	   protected void swap(int a[], int i, int j){
	      int tmp = a[i];
	      a[i] = a[j];
	      a[j] = tmp;
	   }
	   protected void print(){
	      for(int i=0; i<a.length; i++){
	         System.out.println(a[i]);
	      }
	   }
}
