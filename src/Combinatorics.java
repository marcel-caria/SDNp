import java.math.BigInteger;

public class Combinatorics {
	
	/*
	 * This method iteratively calculates n!, which is the
	 * product of all positive integers less than or equal to n.
	 * 
	 * @param	n	the input parameter
	 * @return		the factorial of n
	 */
	public static long factorial(int n){
		if (n==1)
			return 1;
		return factorial(n-1) * n;
	}
	
	/*
	 * Returns the number of ways to choose k elements out of a set of n elements.
	 * 
	 * @param	n	the size of the set to choose from
	 * @param	k	the number of elements to choose
	 * @return		the binomial coefficient
	 */
	public static long binomialCoefficient(int n, int k){
		if (k==0 || k==n)
			return 1L;
		if (k>n || k<0)
			return 0L;
		int min = 0+k;
		if (n-k < k)
			min = n-k;
		long val = 1L;
		for (int i=1; i<=min; i++){
			val *= (n+1-i);
			if (val<0)
				return -1;
			val /= i;
		}
		return val;
	}
	
	
	/*
	 * Returns the number of ways to choose k elements out of a set of n elements as BigInteger.
	 * 
	 * @param	n	the size of the set to choose from
	 * @param	k	the number of elements to choose
	 * @return		the binomial coefficient in a BigInteger
	 */
	public static BigInteger BIG_binomialCoefficient(int n, int k){
		if (k==0 || k==n)
			return BigInteger.valueOf(1);
		if (k>n || k<0)
			return BigInteger.valueOf(0);
		int min = 0+k;
		if (n-k < k)
			min = n-k;
		BigInteger val = BigInteger.valueOf(1);
		for (int i=1; i<=min; i++){
			val = val.multiply(BigInteger.valueOf(n+1-i));
			if (val.compareTo(BigInteger.valueOf(0))==-1)
				return BigInteger.valueOf(-1);
			val = val.divide(BigInteger.valueOf(i));
		}
		return val;
	}
	
	/*
	 * Returns the number of ways to choose k elements out of a set of n elements as double.
	 * 
	 * @param	n	the size of the set to choose from
	 * @param	k	the number of elements to choose
	 * @return		the binomial coefficient
	 */
	public static double DOUBLE_binomialCoefficient(int n, int k){
		if (k==0 || k==n)
			return 1.0;
		if (k>n || k<0)
			return 0.0;
		int min = 0+k;
		if (n-k < k)
			min = n-k;
		BigInteger val = BigInteger.valueOf(1);
		for (int i=1; i<=min; i++){
			val = val.multiply(BigInteger.valueOf(n+1-i));
			if (val.compareTo(BigInteger.valueOf(0))==-1)
				return -1.0;
			val = val.divide(BigInteger.valueOf(i));
		}
		return val.doubleValue();
	}
	
	/*
	 * Returns the maximum n for a given k such that the binomial coefficient
	 * of n and k is not exceeding a given upper bound
	 * 
	 * @param	k		the number of elements to choose
	 * @param	bound	the upper bound on the number of possible combinations
	 * @return	n		the maximum size of the set to choose from
	 */
	public static int get_max_n(int k , long bound){
		int n = k;
		while (binomialCoefficient(n, k) <= bound)
			n++;
		return n-1;
	}
	
	/*
	 * Returns the minimum k (above the combinatorial explosion, i.e., for k close to n) for a 
	 * given n such that the binomial coefficient of n and k is not exceeding a given upper bound
	 * 
	 * @param	n		the size of the set to choose from
	 * @param	bound	the upper bound on the number of possible combinations
	 * @return	k		the maximum number of elements to choose
	 */
	public static int get_min_k(int n , long bound){
		int k = n;
		while (binomialCoefficient(n, k) <= bound)
			k--;
		return k+1;
	}
	
	/*
	 * Returns the next index array to choose k out of n.
	 * 
	 * @param	currentIndex	the current index array
	 * @param	n				the number of elements in the original set to choose from
	 * @return					the next index or null, if there is no next index
	 */
	public static int[] nextIndex(int[] currentIndex, int n){
		if (!isaValidIndex(currentIndex, n))
			return null;
		int k = currentIndex.length;
		int position = k - 1;
		while (position>=0){
			if (currentIndex[position] < n - k + position){
				currentIndex[position]++;
				position++;
				for (int p=position; p<k; p++)
					currentIndex[p] = currentIndex[p-1] + 1;
				return currentIndex;
			}
			position--;
		}
		return null;
	}
	
	/*
	 * Returns the next index array to choose k out of n without validation of the input index array.
	 * WARNING: Invalid input index arrays lead to invalid output index arrays!
	 * 
	 * @param	currentIndex	the current index array
	 * @param	n				the number of elements in the original set to choose from
	 * @return					the next index or null, if there is no next index
	 */
	public static int[] uncheckedNextIndex(int[] currentIndex, int n){
		int k = currentIndex.length;
		int position = k - 1;
		while (position>=0){
			if (currentIndex[position] < n - k + position){
				currentIndex[position]++;
				position++;
				for (int p=position; p<k; p++)
					currentIndex[p] = currentIndex[p-1] + 1;
				return currentIndex;
			}
			position--;
		}
		return null;
	}
	
	/*
	 * Auxiliary method to check whether an index is valid.
	 * An index is invalid if any of its digits index[i] is either greater or equal to n,
	 * or less or equal to its predecessor index[i-1].
	 * 
	 * @param	index	the index array
	 * @param	n		the number of elements in the original set to choose from
	 * @return			true if the index is valid, false otherwise
	 */
	private static boolean isaValidIndex(int[] index, int n){
		if (index==null || index[0] >= n)
			return false;
		for (int i=1; i<index.length; i++)
			if (index[i] <= index[i-1] || index[i] >= n)
				return false;
		return true;
	}
	
	/*
	 * Compares two index arrays a and b.
	 * 
	 * @param	a		the first index array
	 * @param	b		the first index array
	 * @return			-1 if input is invalid; 0 if arrays are equal; 1 if a>b; 2 if a<b;
	 */
	public static int compare(int[] a, int[] b){
		if (a==null || b==null || a.length!=b.length)
			return -1;
		for (int i=0; i<a.length; i++){
			if (a[i] > b[i])
				return 1;
			if (a[i] < b[i])
				return 2;
		}
		return 0;
	}
	
	
	
	/*
	 * Returns the i^th index array to choose k out of n.
	 * For this method we assume the following:
	 * The integer array "index[]" represents a choice of k elements out of n elements.
	 * Each value of index[] is the index of a single element of the input set.
	 * When all possible combinations "choosing k out of n" are ordered lexicographically
	 * in a list, this method returns the index array in that list at a specific position.
	 * This method provides the reverse function of position_of(index, n).
	 * 
	 * @param	n		the number of elements in the original set to choose from
	 * @param	k		the number of elements to be chosen from the original set
	 * @param	i		the position of the index in the lexicographically ordered list
	 * @return			the index array at the given position or null if the position is invalid
	 */
	public static int[] indexArray_at(int n, int k, long i){
		if (i < 0 || i >= binomialCoefficient(n, k))
			return null;
		int[] index = new int[k];
		for (int a=0; a<k; a++)
			index[a] = a;
		int probeDigit = 0;
		int digitValue = 0;
		long currentPosition = position_of(index, n);
		while (i != currentPosition){
			int[] nextProbe = new int[k];
			for (int a=0; a<k; a++){
				if (a < probeDigit)
					nextProbe[a] = index[a];
				if (a == probeDigit)
					nextProbe[a] = digitValue + 1;
				if (a > probeDigit)
					nextProbe[a] = nextProbe[a-1] + 1;
			}
			long nextPosition = position_of(nextProbe, n);
			if (nextPosition < i){
				currentPosition = nextPosition;
				index = nextProbe;
				digitValue++;
			}
			if (nextPosition == i){
				return nextProbe;
			}
			if (nextPosition > i){
				probeDigit++;
				digitValue = index[probeDigit];
			}
		}
		return index;
	}
	
	/*
	 * Returns the position of an index array in the lexicographically ordered list.
	 * For this method we assume the following:
	 * The integer array "index[]" represents a choice of k elements out of n elements.
	 * Each value of index[] is the index of a single element of the input set.
	 * When all possible combinations "choosing k out of n" are ordered lexicographically
	 * in a list, this method returns the position of "index[]" in that list.
	 * This method provides the reverse function of index_at(n, k, position).
	 * 
	 * @param	index	the index array of which the position is searched
	 * @param	n		the number of elements in the original set to choose from
	 * @return			the position of the input index or -1 in case index is invalid
	 */
	public static long position_of(int[] index, int n){
		if (!isaValidIndex(index, n))
			return -1;
		int k = index.length;
		long returnValue = 0;
		for (int p=0; p<index[0]; p++){
			returnValue += binomialCoefficient(n-1-p, k-1);
		}
		if (k == 1)
			return returnValue;
		int[] new_index = new int[k-1];
		for (int i=0; i<k-1; i++)
			new_index[i] = index[i+1] - index[0] - 1;
		return returnValue + position_of(new_index, n-index[0]-1);
	}
	
	
	// just for testing
	public static void main(String[] args) {
		
		System.out.println(binomialCoefficient(6, 2));
		System.out.println(get_max_n(2, 15));

		
//		for (int bn=0; bn<=200; bn++){
//			long maxCombinations = 1000000000L;
//			int k = 200 - bn;
//			int n = get_max_n(k, maxCombinations);
////			System.out.println(bn + "\t" + k + "\t" + n + "\t" + (n-k));
//			System.out.println(bn + "\t" + k + "\t" + n);
//			if (n>1000)
//				break;
//		}
		
		
		
//		int n = 15;
//		int k = 10;
//		System.out.println(binomialCoefficient(n, k) + " possible ways to choose " + k + " out of " + n + ".");
//		int[] index_array = {20,21,22,23,24,25,26,27,28,29,30,31,32,33,49};
//		long position = position_of(index_array, n);
//		System.out.println("Index Array at lexicographical position " + position + ":");
//		index_array = indexArray_at(n, k, position);
//		for (int i=0; i<k; i++)
//			System.out.println(i + ": " + index_array[i]);
	}

}