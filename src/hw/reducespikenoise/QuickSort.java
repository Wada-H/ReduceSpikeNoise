package hw.reducespikenoise;

/*
 * http://d.hatena.ne.jp/kkobayashi_a/20070104/p1
 * 
 */


class QuickSort extends Sort{
	   static final int STACKSIZE = 32;
	   QuickSort(int array[]){super(array);}
	   public void sort(){ // non-recursive
	      int lstack[] = new int[STACKSIZE];
	      int rstack[] = new int[STACKSIZE];
	      int sp = 1, l, r, pivot;
	      lstack[0] = 0;
	      rstack[0] = a.length-1;
	      while(sp > 0){
	         sp--;
	         l = lstack[sp];
	         r = rstack[sp];
	         if(l < r){
	            pivot = partition(a, l, r, (l + r)/2);
	            lstack[sp] = l;
	            rstack[sp] = pivot-1;
	            lstack[sp+1] = pivot+1;
	            rstack[sp+1] = r;
	            sp+=2;
	         }
	      }
	   }
	   private int partition(int a[], int left, int right, int pivot){
	      int l=left+1, r=right, x=a[pivot];
	      swap(a, left, pivot); // set pivot as the first element of partition
	      while(true){
	         while(l < right && a[l] <= x) l++;
	         while(r > left  && a[r] >  x) r--;
	         if(l >= r) break;
	         swap(a, l, r);
	      }
	      swap(a, left, r); // swap pivot for the last element of low-partition
	      return r;
	   }
	}